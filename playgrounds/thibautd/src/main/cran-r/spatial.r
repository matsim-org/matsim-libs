library( "gplots" )

puDoHeatMap <- function(
			plansFrame,
			actPattern="drop_off",
			xlim=c(670000, 700000),
			ylim=c(230000, 260000),
			typeCol="peInfo",
			xCol="actXCoord",
			yCol="actYCoord",
			...) {
	xy <-  subset(
				  plansFrame ,
				  grepl( actPattern , plansFrame[[ typeCol ]]) ,
				  c(xCol, yCol))
	hist2d(
		   subset( xy , xy[[ xCol ]] > xlim[1] & xy[[ xCol ]] < xlim[2] &  xy[[ yCol ]] > ylim[1] & xy[[ yCol ]] < ylim[2] ),
		   # important setting!
		   # problem: the range is also equal.
		   # this can be solved using the lims, but the number
		   # of bins is still defined for the "wide" plot!
		   # same.scale=TRUE,
		   # xlim=xlim,
		   # ylim=ylim,
		   ...)
}
