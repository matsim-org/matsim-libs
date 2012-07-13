# the counts file can be obtained by the awk script "fromPassengerRecordsToCounts"
# rather than using this function, the count frame should be obtained with the shell
# script "mergeRecordsAndCounts.sh"
readCountFrame <- function( recordsFileName , countsFileName ) {
	merge(
		  read.table( recordsFileName , header=T ),
		  read.table( countsFileName , header=T ),
		  by="recordId");
}

boxplotdriverPerTod <- function(countFrame, ...) {
	boxplotnPerTod( countFrame , "nDriverTrips" , ylab="number of driver trips" , yaxt="n", ...);
}

boxplotpassengerPerTod <- function(countFrame,...) {
	boxplotnPerTod( countFrame, "nPassengerTrips", ylab="number of passenger trips", yaxt="n", ...)
}

boxplotnPerTod <- function(countFrame, countField, outline=F, range=0, plotrange=1.5,...) {
	at <- sort( unique( floor( countFrame$departureTime / 3600 ) ) ) + 1
	mini <- at[1]
	n <- length(at)
	maxi <- at[n]
	plt <- function (range, outline=F, ...) {
		boxplot(
				countFrame[[countField]] ~ I(floor( departureTime / 3600 )),
				countFrame,
				at=at,
				outline=outline,
				xlab="time of day (h)",
				xaxt="n",
				range=range,
				...)
	}
	plt(plotrange, ...)
	ylim <- par( "yaxp" )[1:2]
	plt(range, ylim=ylim, ...)
	if (outline) {
		plt(range, outline=T, ylim=ylim, ...)
	}
	means <- tapply(
					countFrame[[countField]],
					I(floor( countFrame$departureTime / 3600 )),
					mean)
	points(at, means, pch=18)

	by <- 6
	axis( 1 , at=seq(mini-0.5,maxi+0.5,by=by), labels=seq(mini-1,maxi,by=by) )
}

boxplotnJointTripsPerPassengerDist <- function(countFrame, binWidth=1000, ...) {
	boxplotnPerContinuous(
						   "nPassengerTrips" ,
						   "networkFreeFlowDist",
						   subset( countFrame , countFrame$networkFreeFlowDist < 30000 ),
						   xlab="distance (km)",
						   ylab="count",
						   scale=1/1000,
						   yaxt="n",
						   ...)
}

boxplotnDriverTripsPerDriverDist <- function(countFrame, binWidth=1000, ...) {
	boxplotnPerContinuous(
						   "nDriverTrips" ,
						   "networkFreeFlowDist",
						   subset( countFrame , countFrame$networkFreeFlowDist < 30000 ),
						   xlab="distance (km)",
						   ylab="count",
						   scale=1/1000,
						   yaxt="n",
						   ...)
}

boxplotnPerContinuous <- function(countName, continuousName, countFrame, binWidth=1000, scale=1, range=0, plotrange=1.5, ...) {
	origShift <- 1
	at <- sort( unique( floor( countFrame[[continuousName]] / binWidth ) ) )
	mini <- at[1]
	n <- length(at)
	maxi <- at[n]
	plt <- function(range, ...) {
		boxplot( countFrame[[countName]] ~ I(floor( countFrame[[continuousName]] / binWidth )) , countFrame , at=at, outline=F, xaxt="n", range=range, ...)
	}
	plt(plotrange)
	plt(range, ylim=par("yaxp")[1:2], ...)
	means <- tapply(countFrame[[countName]],I(floor( countFrame[[continuousName]] / binWidth )),mean)
	points(at, means, pch=18)
	xaxp <- par("xaxp")
	at <- seq(xaxp[1], xaxp[2], length.out=xaxp[3]+1)
	axis( 1 , at=at-0.5, labels=(at * binWidth)*scale )
}

histTw <- function( dataset , ... ) {
	histfromCounts( subset( dataset , dataset$binType == "tw" ), ... )
}

histfromCounts <- function( dataset , scale=1, bxpData=NULL, width=0.1, freq=F, ...) {
	ord <- order( dataset$binMin )
	breaks <- c( dataset$binMin[ ord[1] ] , dataset$binMax[ ord[1] ] )
	counts <- dataset$count[ ord[1] ]

	for (i in ord[2:length(ord)]) {
		if ( dataset$binMin[ i ] != breaks[ length(breaks) ] ) {
			breaks <- c( breaks , dataset$binMin[ i ] )
			counts <- c( counts , 0 )
		}
		breaks <- c( breaks , dataset$binMax[ i ] )
		counts <- c( counts , dataset$count[ i ] )
	}

	densities <- counts / sum( counts )
	l <- length(breaks)
	mids <- breaks[1:l-1] + (breaks[2:l] - breaks[1:l-1]) / 2

	# scale
	breaks <- breaks * scale
	mids <- mids * scale

	h <- list(
			   breaks=breaks,
			   counts=counts,
			   density=densities,
			   intensities=densities,
			   mids=mids,
			   xname=levels(factor(dataset$binType)),
			   equidist=T,
			   ...)
	class( h ) <- "histogram"
	plot(h, yaxt="n", freq=freq, ...)
	if (is.null(bxpData)) {
		# approximate bxp (consider records uniformly distributed in the bin)
		n <- sum( counts )
		bxpData <- c()
		currentCount <- 0
		nextQuartile <- 0

		for (i in 1:length(counts)) {
			currentCount <- currentCount + counts[i]
			if (currentCount >= nextQuartile) {
				low <- currentCount - counts[i]
				high <- currentCount
				xlow <- breaks[i]
				xhigh <- breaks[i+1]
				a <- (high - low) / (xhigh - xlow)
				b <- low - a * xlow
				xQuartile <- (nextQuartile - b) / a
				bxpData <- c( bxpData , xQuartile )
				nextQuartile <- nextQuartile + n / 4
			}
		}
		interQuart <- bxpData[4] - bxpData[2]
		bxpData[1] = max( c(bxpData[1], bxpData[2] - 1.5*interQuart) )
		bxpData[5] = min( c(bxpData[5], bxpData[4] + 1.5*interQuart) )
	}
	ymax <- par( "usr" )[4]
	ymin <- -width * ymax
	plot(h, ylim=c(ymin,ymax), freq=freq, ...)
	##-- scale a range
	scale.r <- function(x1,x2, fact = 1.1)
		(x1+x2)/2 + c(-fact,fact) * (x2-x1)/2
	plotBxp(bxpData[1],
			bxpData[2],
			bxpData[3],
			bxpData[4],
			bxpData[5],
			scale.r(par("usr")[3], 0,
			 f = .8 - max(0, .15 - width)*(1+(par("mfg")[3] >= 3))))
	abline( h=0 );
}

compareODTod <- function (
						  countssameo,
						  countssamed,
						  plty=2,
						  dlty=3,
						  ppoint=1,
						  dpoint=2,
						  xlim=c(0,24),
						  ylim=c(0,2.5),
						  legx="topright",
						  legy=NULL,
						  ...) {
	getX <- function( aframe ) sort( unique( floor( aframe$departureTime / 3600 ) ) ) + 1
	getMean <- function( aframe , afield ) tapply( aframe[[afield]] , I(floor( aframe$departureTime / 3600 )), mean)
	frame();
	plot.window( xlim , ylim );
	xsameo <- getX( countssameo )
	xsamed <- getX( countssamed )
	meanop <- getMean( countssameo , "nPassengerTrips" )[xsameo <= 24]
	meanod <- getMean( countssameo , "nDriverTrips" )[xsameo <= 24]
	meandp <- getMean( countssamed , "nPassengerTrips" )[xsamed <= 24]
	meandd <- getMean( countssamed , "nDriverTrips" )[xsamed <= 24]
	xsameo <- xsameo[xsameo <= 24]
	xsamed <- xsamed[xsamed <= 24]
	lines( xsameo, meanop , lty=plty, col="red", ...)
	lines( xsamed, meandp , lty=plty, col="blue", ...)
	lines( xsameo, meanod , lty=dlty, col="red", ...)
	lines( xsamed, meandd , lty=dlty, col="blue", ...)

	points( xsameo, meanop , pch=ppoint, col="red", ...)
	points( xsamed, meandp , pch=ppoint, col="blue", ...)
	points( xsameo, meanod , pch=dpoint, col="red", ...)
	points( xsamed, meandd , pch=dpoint, col="blue", ...)

	axis(1, at=seq(0,24,by=6), ...)
	legend(legx, legy,
		   box.lty=0,
		   lty=c(2,3),
		   pch=c(1,2),
		   col=c("red","red","blue","blue"),
		   legend=c(
					"passenger, same orig.",
					"driver, same orig.",
					"passenger, same dest.",
					"driver, same dest."),
		   xpd=T) # allow ploting outside the plot
	title( xlab="time of day (h)" , ylab="number of identified trips" )
}
