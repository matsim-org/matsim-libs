# more a cheatsheet than actual modular functions.

nameDataset <- function( frame , name ) {
	name <- factor( name );
	frame$datasetName <- name;
	frame
}

# TODO: a lot of copy paste here, change that!
# (just pass the formula)
comparativeBoxPlot <- function(
		    formula,
			data1 ,
			data2  ,
			data1Desc=levels(data1$datasetName)[1],
			data2Desc=levels(data2$datasetName)[1],
			xlab="clique size" ,
			ylab="",
			col=c("white","lightgrey"),
			legpos="topleft",
			outline="TRUE",
			linezero="TRUE",
			# main="Scores per clique size",
			...) {
	# takes two "named" datasets and produces a boxplot.
	merged <- rbind( data1 , data2 );
	boxplot(
			formula,
			merged,
			las = 3, # labels orthogonal to the axis
			col = col,
			xaxt="n", # no x axis (yet)
			xlab=xlab,
			ylab=ylab,
			outline=FALSE,
			# main=main,
			...);

	if (outline) {
		myYlim = par("usr")[3:4];
		boxplot(
				formula,
				merged,
				las = 3, # labels orthogonal to the axis
				col = col,
				xaxt="n", # no x axis (yet)
				xlab=xlab,
				ylab=ylab,
				ylim=myYlim,
				# main=main,
				...);
	}

	# custom x axis
	sizes <- factor( merged$cliqueSize );
	axis(1, at=seq(1, nlevels( sizes ), by=1) * 2 - 0.5, labels=levels( sizes ) )

	# separations between clique sizes
	for (x in seq(0, nlevels( sizes ), by=1) * 2 + 0.5) abline( v=x , lty=2 )

	# horizontal line at zero?
	if (linezero) abline( h=0 , lty=2 )

	# legend
	legend( legpos , legend=c(data1Desc, data2Desc), fill=col , bg="white");
}

comparativeBoxPlotScores <- function(ylab="score", ...) {
	comparativeBoxPlot(
		   agentScore ~ interaction( datasetName , cliqueSize ),
		   ylab=ylab,
		   linezero="FALSE", # would not make sense
		   ...)
}

comparativeBoxPlotTravelTimes <- function(
			ylab="travel time improvement (%)",
			...) {
	comparativeBoxPlot(
			ttImprovement ~ interaction( datasetName , cliqueSize ),
			ylab=ylab,
			...)
}

comparativeBoxPlotScoresImprovements <- function(
			ylab="score improvement",
			...) {
	comparativeBoxPlot(
			scoreImprovement ~ interaction( datasetName , cliqueSize ),
			ylab=ylab,
			...)
}

baseComparativeBoxPlot <- function( x , ... , dataNames , col=grey( 1:length( dataNames ) / length( dataNames ) ) , ylab="score" , legpos="bottomleft" , legspace=50) {
	boxplot( x , ... , outline=FALSE )
	myYlim = par("usr")[3:4] - c(legspace,0)
	boxplot( x , ... , ylim=myYlim , ylab=ylab , col=col , xaxt="n")
	legend( legpos , legend=dataNames, fill=col , bg="white");
}
