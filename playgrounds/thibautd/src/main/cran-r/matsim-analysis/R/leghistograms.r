plotleghistogram <- function(
							 file="legHistogram.txt" ,
							 dataset=read.table( file=file , header=T ),
							 the.mode="all",
							 ...) {
	col.names <- colnames( dataset )
	departures <- grep( paste( "^departures_" , the.mode , "$" , sep="" ) , col.names )
	if ( length( departures ) != 1 ) {
		stop( paste( "got" , length( departures ) , "departure columns instead of 1" ) )
	}

	en.routes <- grep( paste( "^en\\.route_" , the.mode , "$" , sep="" ) , col.names )
	if ( length( en.routes ) != 1 ) {
		stop( paste( "got" , length( en.routes ) , "en route columns instead of 1" ) )
	}

	arrivals <- grep( paste( "^arrivals_" , the.mode , "$" , sep="" ) , col.names )
	if ( length( arrivals ) != 1 ) {
		stop( paste( "got" , length( arrivals ) , "arrival columns instead of 1" ) )
	}

	plot( dataset$time.1 , dataset[[ en.routes ]] , type="l" , col="green" , ... )
	lines( dataset$time.1 , dataset[[ departures ]] , col="blue" , ... )
	lines( dataset$time.1 , dataset[[ arrivals ]] , col="red" , ... )
}
