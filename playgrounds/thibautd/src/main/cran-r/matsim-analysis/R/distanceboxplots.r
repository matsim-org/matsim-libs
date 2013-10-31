distance.bxp <- function( datasets ,
						 axes=F , outline=F ,
						 dist.factor=1/1000,
						 ylab="Trip Length (km)",
						 xlab="Mode",
						 lty.sep=1,
						 legendpos="topleft",
						 cols=gray( seq( 1 , 0.4 , length.out=length( datasets ) ) ),
						 modes.order=function( modes ) {
						 	order.index <- rep( Inf , length( modes ) )
						 	order.index[ modes == "car" ] <- 1
						 	order.index[ modes == "car_driver" ] <- 2
						 	order.index[ modes == "car_passenger" ] <- 3
						 	order.index[ modes == "pt" ] <- 4
						 	order.index[ modes == "bike" ] <- 5
						 	order.index[ modes == "walk" ] <- 6
						 	order.index
						 },
						 ... ) {
	full.dataset <- my.rbind( datasets )
	full.dataset$dataset <- reorder(
									as.factor( full.dataset$dataset ),
									full.dataset$dataset,
									FUN=function( v ) {
										val <- unique( v )
										if ( length( val ) != 1 ) stop( paste( "unexpected" , val ) )
										which( names( simul.datasets ) == val )
									} )

	full.dataset$main_mode <- reorder( as.factor( full.dataset$main_mode ) , modes.order( full.dataset$main_mode ) )
	full.dataset$total_dist <- full.dataset$total_dist * dist.factor
	bp <- boxplot( total_dist ~  dataset : main_mode , full.dataset , outline=outline , col=cols , axes=axes , ylab=ylab , xlab=xlab , ... )
	for ( x in seq( 0 , ncol( bp$stats ) ) ) abline( v=x*length( cols ) + .5 , lty=lty.sep )
	if ( !axes ) {
		axis( 2 )
		axis( 1 ,
			 at=seq(1,ncol( bp$stats ), by=length( cols ) )+(length( cols ) / 2)-.5 ,
			 labels=levels(full.dataset$main_mode),
			 tick=F)
	}
	legend( legendpos , legend=levels( full.dataset$dataset ) ,  fill=cols , bg="white" , box.lty=0 )
}
