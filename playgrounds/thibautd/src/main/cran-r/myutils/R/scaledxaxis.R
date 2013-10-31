scaledxaxis <-
function(scale) {
	xaxp <- par("xaxp")
	at <- seq(xaxp[1], xaxp[2], length.out=xaxp[3]+1)
	axis( 1 , at , labels = at * scale )
}

