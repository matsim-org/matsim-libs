plotBxp <-
function(mini,q1,med,q3,maxi,ys,border=NULL) {
  if (is.null(border)) border="black"
  ymin <- ys[1]
  ymax <- ys[2]
  m <- (ymax + ymin)/2
  llhh <- c(ymin, ymin, ymax, ymax)
  ## drawing the box
  lines(c(q1,q1, q3, q3, q1), c(ymax,llhh), lty=1,col=border)
  ## Median
  lines(rep.int(med, 2), c(ymin, ymax), lwd = 2, lty = 1,col=border) #
  ## Whiskers
  lines(c(mini,q1, NA, q3, maxi), rep.int(m, 5), lty = 1,col=border) #
}

histAndBxp <-
function(
					   x, #the data
					   width=0.1, # the width of the bxp
					   range=1.5, #the whisker range
					   border=NULL, # the color
					   ... ) {
	h <- hist( x , plot=F , ... )
	ymax <- max( h$counts )
	ymin <- -width * ymax
	plot(h, ylim=c(ymin,ymax), border=border, ...)

	bxpData <- boxplot( x , range=range , plot=F )$stats
	##-- scale a range
	scale.r <- function(x1,x2, fact = 1.1)
		(x1+x2)/2 + c(-fact,fact) * (x2-x1)/2
	plotBxp(bxpData[1],
			bxpData[2],
			bxpData[3],
			bxpData[4],
			bxpData[5],
			scale.r(par("usr")[3], 0,
			 f = .8 - max(0, .15 - width)*(1+(par("mfg")[3] >= 3))),
			border)
	abline( h=0 );
}

