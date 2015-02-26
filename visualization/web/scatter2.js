var loadeddata = null;

nv.addGraph(function() {
  var chart = nv.models.scatterChart().color(d3.scale.category10().range());
  chart.xAxis.axisLabel("forks");
  chart.yAxis.axisLabel("stars");
  chart.showLegend(false);

  chart.tooltipContent(function(groupName, x, y) {
	  console.log(groupName);
	  console.log(x);
	  console.log(y);
	  for (ii = 0; ii < loadeddata.length; ii++) {
		  if (loadeddata[ii].x == x && loadeddata[ii].y == y) {
			  return loadeddata[ii].label + "(" + loadeddata[ii].openCount + " open issues)";
		  }
	  }
	  return "unknown";
  });

  var myData = myRealData();
	  
  d3.select('#chart svg')
      .datum(myData)
      .call(chart);

  nv.utils.windowResize(chart.update);

  return chart;
});

function myRealData() {
	var mydata = null;
	var data = [];
	var random = d3.random.normal();
	
	$.ajax({
		url: 'asOf-20150225.json', 
		async: false,
		dataType: 'json',
		success: function(data) {
			mydata = data;
		}
	});
    data.push({
    	key: 'Group 0',
    	values: []
	});
    loadeddata = []
    for (ii = 0; ii < mydata.repos.length; ii++) {
    	loadeddata.push({
    		label: mydata.repos[ii].name,
    		x: mydata.repos[ii].forks,
    		y: mydata.repos[ii].stars,
    		openCount: mydata.repos[ii].issues.openCount
    	})
    	data[0].values.push({
    		label: mydata.repos[ii].name,
    		x: mydata.repos[ii].forks,
    		y: mydata.repos[ii].stars,
    		size: mydata.repos[ii].issues.openCount,
    		shape: "circle"
        });
    }

	return data;
}
