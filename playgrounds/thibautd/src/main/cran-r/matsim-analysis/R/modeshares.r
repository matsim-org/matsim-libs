plotshareevolution <- function(
						   tripmodesharefile="tripModeShares.dat",
						   dataframe=read.table( tripmodesharefile , header=T ) ,
							the.colors=rainbow( length( unique( dataframe$mode ) ) ),
							   ...) {
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
	minIter <- 0
	maxIter <- max( dataframe$iter )
	plot( seq( minIter , maxIter ),
		 cumulated.counts.per.mode[[ length( modes ) ]] ,
		 #main="average executed joint plan size" ,
		 main="" ,
		 xlab="Iteration" , ylab="Cumul Trips",
		 type="n", bty="n",
		 ylim=c(0, max( cumulated.counts.per.mode[[ length( modes ) ]] )),
		 ...)

	prev <- rep( 0 , length( cumulated.counts.per.mode[[ 1 ]] ) )
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

plotshareevolution.nonstacked <- function(
						   tripmodesharefile="tripModeShares.dat",
						   dataframe=read.table( tripmodesharefile , header=T ) ,
						   cols=rainbow( length( unique( dataframe$mode ) ) ),
						   legendpos="topright",
						   ymin=0,
							   ...) {
	modes <- unique( dataframe$mode )
	plot( dataframe$iter[ dataframe$mode == modes[ 1 ] ],
		 dataframe$nTrips[ dataframe$mode == modes[ 1 ] ] ,
		 #main="average executed joint plan size" ,
		 main="" ,
		 xlab="Iteration" , ylab="# Trips",
		 type="l",
		 bty="n",
		 xlim=c(min( dataframe$iter ) , max( dataframe$iter ) ),
		 ylim=c(0, max( c(ymin, dataframe$nTrips) )),
		 col=cols[ 1 ],
		 ...)
	for ( i in seq(2,length(modes)) ) {
		lines(
			 dataframe$iter[ dataframe$mode == modes[ i ] ],
			 dataframe$nTrips[ dataframe$mode == modes[ i ] ] ,
			 col=cols[ i ] )
	}
	legend(
		   legendpos,
		   legend=modes,
		   col=cols,
		   lty=1,
		   box.lty=0)
}
