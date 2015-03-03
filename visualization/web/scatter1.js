var loadeddata = {};

function removeGraphs() {
  var svgs = d3.selectAll("svg > *");
  svgs.remove();
}

function addGraph(divname, xAxisLabel, yAxisLabel, jsonRelUrl, bubbleTTfirst, bubbleTTlast, bubbleSizeField, xMax, yMax) {
	if (arguments.length == 7) {
		xMax = -1;
		yMax = -1;
	}
	
	addGraphFunction = function(xMax, yMax, divname) {
	  var chart = nv.models.scatterChart()
	  	.color(d3.scale.category10().range());
	  
	  chart.xAxis.axisLabel(xAxisLabel);
	  chart.yAxis.axisLabel(yAxisLabel);
	  chart.showLegend(false);
	
	  chart.tooltipContent(function(groupName, x, y) {
		  //console.log(groupName);
		  //console.log(x);
		  //console.log(y);
		  for (ii = 0; ii < loadeddata[divname].length; ii++) {
			  if (loadeddata[divname][ii].x == x && loadeddata[divname][ii].y == y) {
				  // TODO: Look into string interpolation and/or templating long term
				  return loadeddata[divname][ii].label + bubbleTTfirst + loadeddata[divname][ii].bubbleTTmiddle + bubbleTTlast;
			  }
		  }
		  return "unknown";
	  });
	
	  var myData = myRealData(jsonRelUrl, bubbleSizeField, xMax, yMax, divname);
		  
	  d3.select('#' + divname + ' svg')
	      .datum(myData)
	      .call(chart);
	
	  nv.utils.windowResize(chart.update);
	
	  return chart;
	};
	
	nv.addGraph(addGraphFunction(xMax, yMax, divname));
}

function myRealData(jsonRelUrl, bubbleSizeField, xMax, yMax, divname) {
	var mydata = null;
	var data = [];
	var random = d3.random.normal();
	
	$.ajax({
		url: jsonRelUrl, 
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
    loadeddata[divname] = []
    for (ii = 0; ii < mydata.repos.length; ii++) {
    	if (xMax != -1) {
    		var xVal = mydata.repos[ii].forks;
    		var yVal = mydata.repos[ii].stars;
    		if (xVal > xMax || yVal > yMax) {
    			continue; // don't plot the point
    		}
    	}
    	size = accessProperty(mydata.repos[ii], bubbleSizeField);
    	console.log('bubble size field = ' + size)
    	var dataToLoad = {
    		label: mydata.repos[ii].name,
        	x: mydata.repos[ii].forks,
        	y: mydata.repos[ii].stars,
        	bubbleTTmiddle: size
        };
    	console.log(dataToLoad)
    	loadeddata[divname].push(dataToLoad)
    	data[0].values.push({
    		label: mydata.repos[ii].name,
    		x: mydata.repos[ii].forks,
    		y: mydata.repos[ii].stars,
    		size: size * 1000,
    		shape: "circle"
        });
    }

	return data;
}

function accessProperty (object, string){
	var explodedString = string.split('.');
	for (i = 0, l = explodedString.length; i<l; i++){
		object = object[explodedString[i]];
	}
	return object;
}
