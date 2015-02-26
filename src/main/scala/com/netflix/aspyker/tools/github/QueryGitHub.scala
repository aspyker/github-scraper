package com.netflix.aspyker.tools.github

import com.stackmob.newman._
import com.stackmob.newman.dsl._
import scala.concurrent._
import scala.concurrent.duration._
import java.net.URL
import org.slf4j.LoggerFactory
import play.api.libs.json._
import org.kohsuke.github._
import scala.collection.JavaConversions._
import java.util.Date

object QueryGitHub {
  def logger = LoggerFactory.getLogger(getClass)
  def githubOrg = "netflix"

  def main(args: Array[String]): Unit = {
    val github = GitHub.connect()
    val orgs = github.getOrganization(githubOrg)
    val repos = orgs.listRepositories(100).asList()
    
    val (privateRepos, publicRepos) = repos.partition { repo => repo.isPrivate() }
    walkRepos(publicRepos.toList)
    System.exit(0)
  }
  
  def walkRepos(repos: List[GHRepository]) : Unit = {
    val reposJsonSeq = repos.map(repo => {
      logger.info(s"repo = ${repo.getName()}, forks = ${repo.getForks}, stars = ${repo.getWatchers}")
      val openPullRequests = repo.getPullRequests(GHIssueState.OPEN)
      logger.debug(s"  openIssues = ${repo.getOpenIssueCount()}, openPullRequests = ${openPullRequests.size()}")
      
      // TODO: Is there a faster way to only pull the last commit?
      val commits = repo.listCommits().asList()
      val orderedCommits = commits.sortBy(_.getCommitShortInfo.getCommitter().getDate())
      val lastCommitDate = orderedCommits(orderedCommits.length - 1).getCommitShortInfo().getCommitter().getDate()
      //logger.debug(s"commits, first = ${orderedCommits(0).getSHA1}, last = ${orderedCommits(orderedCommits.length - 1).getSHA1()}")
      val daysSinceLastCommit = daysBetween(lastCommitDate, new Date())
      logger.debug(s"  daysSinceLastCommit = ${daysSinceLastCommit}")
      
      val contributors = commits.filter { commit => Option(commit.getAuthor()).isDefined }
      val contributorEmails = contributors.map(contributor => contributor.getAuthor().getLogin()).distinct
      logger.debug(s"  numContribitors = ${contributorEmails.length}, contributorEmails = ${contributorEmails}")
      
      val closedIssues = repo.getIssues(GHIssueState.CLOSED)
      val timeToCloseIssue = closedIssues.map(issue => {
        val opened = issue.getCreatedAt()
        val closed = issue.getClosedAt()
        val difference = daysBetween(opened, closed)
        difference
      })
      val sumIssues = timeToCloseIssue.sum
      val avgIssues = timeToCloseIssue.size match {
        case 0 => 0
        case _ => sumIssues / timeToCloseIssue.size
      }
      logger.debug(s"    avg days to close ${closedIssues.size()} issues = ${avgIssues} days")
     
      // TODO: Look at refactoring with above into function
      val closedPRs = repo.getPullRequests(GHIssueState.CLOSED)
      val timeToClosePR = closedPRs.map(pr => {
        val opened = pr.getCreatedAt()
        val closed = pr.getClosedAt()
        val difference = daysBetween(opened, closed)
        difference
      })
      val sumPRs = timeToClosePR.sum
      val avgPRs = timeToClosePR.size match {
        case 0 => 0
        case _ => sumPRs / timeToClosePR.size
      }
      logger.debug(s"    avg days to close ${closedPRs.size()} pull requests = ${avgPRs} days")
      
      val repoJson: JsValue = Json.obj(
        "name" -> repo.getName(),
        "forks" -> repo.getForks(),
        "stars" -> repo.getWatchers(),
        "numContributors" -> contributorEmails.length,
        "issues" -> Json.obj(
            "openCount" -> repo.getOpenIssueCount(),
            "closedCount" -> closedIssues.size(),
            "avgTimeToCloseInDays" -> avgIssues
        ),
        "pullRequests" -> Json.obj(
            "openCount" -> openPullRequests.size(),
            "closedCount" -> closedPRs.size(),
            "avgTimeToCloseInDays" -> avgPRs
        ),
        "commits" -> Json.obj(
            "daysSinceLastCommit" -> daysSinceLastCommit
        ),
        "contributors" -> contributorEmails.toSeq
      )
      logger.debug("repo json = " + repoJson)
      repoJson
    })
    val allRepos = Json.obj("asOf" -> new Date().toGMTString(), "repos" -> reposJsonSeq)
    logger.info(s"reposJson = ${allRepos}")
  }
  
  def daysBetween(smaller: Date, bigger: Date): Int = {
    val diff = (bigger.getTime() - smaller.getTime()) / (1000 * 60 * 60 * 24)
    diff.toInt
  }
}
