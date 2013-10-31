scaledyaxis <-
function(scale) {
	yaxp <- par("yaxp")
	at <- seq(yaxp[1], yaxp[2], length.out=yaxp[3]+1)
	axis( 2 , at , labels = at * scale )
}

