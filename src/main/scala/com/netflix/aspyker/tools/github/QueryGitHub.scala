package com.netflix.aspyker.tools.github

import org.slf4j.LoggerFactory
import play.api.libs.json._
import org.kohsuke.github._
import scala.collection.JavaConversions._
import java.util.Date

object QueryGitHub {
  def logger = LoggerFactory.getLogger(getClass)
  //def githubOrg = "netflix"
  def githubOrg = "facebook"

  def main(args: Array[String]): Unit = {
    val github = GitHub.connect()
    val orgs = github.getOrganization(githubOrg)
    val repos = orgs.listRepositories(100).asList()
    
    val (privateRepos, publicRepos) = repos.partition { repo => repo.isPrivate() }
    walkRepos(publicRepos.toList)
    System.exit(0)
  }
  
  // TODO: Is there a faster way to only pull the last commit?
  def commitInfo(repo: GHRepository) : (Int, Int, List[String]) = {
      val commits = repo.listCommits().asList()
      val orderedCommits = commits.sortBy(_.getCommitShortInfo.getCommitter().getDate())
      val lastCommitDate = orderedCommits(orderedCommits.length - 1).getCommitShortInfo().getCommitter().getDate()
      //logger.debug(s"commits, first = ${orderedCommits(0).getSHA1}, last = ${orderedCommits(orderedCommits.length - 1).getSHA1()}")
      val daysSinceLastCommit = daysBetween(lastCommitDate, new Date())
      logger.debug(s"  daysSinceLastCommit = ${daysSinceLastCommit}")
      
      val contributors = commits.filter { commit => Option(commit.getAuthor()).isDefined }
      val contributorLogins = contributors.map(contributor => contributor.getAuthor().getLogin()).distinct
      logger.debug(s"  numContribitors = ${contributorLogins.length}, contributorEmails = ${contributorLogins}")
      (commits.length, daysSinceLastCommit, contributorLogins.toList)
  }
  
  // TODO: Is there a faster way to only pull the last commit?
  def commitInfo2(repo: GHRepository) : (Int, Int, List[String]) = {
      val branches = repo.getBranches().filterKeys { key => key != "gh-pages" }
      val lastCommits = branches.map({
        case (branchName, branch) => {
          val lastCommit = branch.getSHA1()
          val lastCommitDate = repo.getCommit(lastCommit).getCommitShortInfo.getCommitter.getDate()
          logger.debug(s"last commit date for ${branchName} repo was ${lastCommitDate}")
          lastCommitDate
        }
      })
      val lastCommitDate = lastCommits.max
      
      //logger.debug(s"commits, first = ${orderedCommits(0).getSHA1}, last = ${orderedCommits(orderedCommits.length - 1).getSHA1()}")
      val daysSinceLastCommit = daysBetween(lastCommitDate, new Date())
      logger.debug(s"  daysSinceLastCommit = ${daysSinceLastCommit}")

      val contributorLogins = None;
//      val contributorLogins = repo.listCollaborators().map { collaborator => collaborator.getLogin }
//      val contributors = commits.filter { commit => Option(commit.getAuthor()).isDefined }
//      val contributorLogins = contributors.map(contributor => contributor.getAuthor().getLogin()).distinct
//      logger.debug(s"  numContribitors = ${contributorLogins.length}, contributorEmails = ${contributorLogins}")
      (-1, daysSinceLastCommit, contributorLogins.toList)
  }
  
  def getClosedIssuesStats(repo: GHRepository) : (Int, Int) = {
    (-1, -1)  
  }
  
  def getClosedIssuesStats2(repo: GHRepository) : (Int, Int) = {
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
      (closedIssues.size(), avgIssues)
  }
  
  def getClosedPullRequestsStats(repo: GHRepository) : (Int, Int) = {
    (-1, -1)
  }
  
  def getClosedPullRequestsStats2(repo: GHRepository) : (Int, Int) = {
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
      (closedPRs.size, avgPRs)
  }
  
  def walkRepos(repos: List[GHRepository]) : Unit = {
    val reposJsonSeq = repos.map(repo => {
      logger.info(s"repo = ${repo.getName()}, forks = ${repo.getForks}, stars = ${repo.getWatchers}")
      val openPullRequests = repo.getPullRequests(GHIssueState.OPEN)
      logger.debug(s"  openIssues = ${repo.getOpenIssueCount()}, openPullRequests = ${openPullRequests.size()}")
      
      val (numCommits, daysSinceLastCommit, contributorLogins) = commitInfo2(repo)      
      val (closedIssuesSize, avgIssues) = getClosedIssuesStats(repo) 
      val (closedPRsSize, avgPRs) = getClosedPullRequestsStats(repo)
      
      val repoJson: JsValue = Json.obj(
        "name" -> repo.getName(),
        "forks" -> repo.getForks(),
        "stars" -> repo.getWatchers(),
        "numContributors" -> contributorLogins.length,
        "issues" -> Json.obj(
            "openCount" -> repo.getOpenIssueCount(),
            "closedCount" -> closedIssuesSize,
            "avgTimeToCloseInDays" -> avgIssues
        ),
        "pullRequests" -> Json.obj(
            "openCount" -> openPullRequests.size(),
            "closedCount" -> closedPRsSize,
            "avgTimeToCloseInDays" -> avgPRs
        ),
        "commits" -> Json.obj(
            "daysSinceLastCommit" -> daysSinceLastCommit
        ),
        "contributors" -> contributorLogins.toSeq
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
