plotshareevolution <- function( tripmodesharefile="tripmodeshares.dat", dataframe=read.table( tripmodesharefile , header=T ) ) {
	dataframe$mode <- as.factor( dataframe$mode )
	modes <- unique( dataframe$mode )

	counts <- function( themode ) {
		mydata = rep( 0 , max( dataframe$iter ) + 1 )
		mydata[ dataframe$iter[ dataframe$mode == themode ] + 1 ] = dataframe$nTrips[ dataframe$mode == themode ]
		mydata
	}

	current.counts <- 0
	cumulated.counts.per.mode <- list()

	for ( themode in modes ) {
		cumulated.counts.per.mode
		current.counts <- current.counts + counts( themode );
		cumulated.counts.per.mode[[ themode ]] <- current.counts
	}

	# to size plot area
	plot( cumulated.counts.per.mode[[ length( modes ) ]] ,
		 #main="average executed joint plan size" ,
		 main="" ,
		 xlab="Iteration" , ylab="Cumul Trips",
		 type="n", bty="n",
		 ylim=c(0, max( cumulated.counts.per.mode[[ length( modes ) ]] )) )

	prev <- rep( 0 , length( cumulated.counts.per.mode[[ 1 ]] ) )
	minIter <- 0
	maxIter <- max( dataframe$iter )
	the.colors <- rainbow( length( modes ) )
	for ( i in seq( 1 , length( modes ) ) ) {
		the.counts <- cumulated.counts.per.mode[[ i ]]
		the.mode <- names( cumulated.counts.per.mode[ i ] )
		the.color <- the.colors[ i ]
		polygon( c( seq( minIter , maxIter ) , seq( maxIter , minIter ) ),
				c( the.counts , rev( prev ) ) ,
				density=10,
				border=the.color,
				col = the.color);
		xlabel <- (maxIter - minIter) / 2
		text( xlabel , (the.counts[ xlabel ] + prev[ xlabel ]) / 2 , the.mode )
		prev <- the.counts
	}

	# trace lines OVER the polygons
	for ( l in cumulated.counts.per.mode ) lines( seq(minIter, maxIter) , l )

	abline( h=0 , lty=2 )
}


