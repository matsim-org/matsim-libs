plot.score.evolution <-
	function( the.file="ScoreStats.dat",
			 dataset=read.table( the.file , header=T , sep="\t" ),
			 group="all",
			 col.max="green",
			 col.exec="black",
			 col.min="red",
			 legend.pos="bottomright",
			 xlab="Iteration",
			 ylab="Score",
			 bty="n",
			 the.lines=dataset$groupId == group,
			 ylim=c(min( dataset$min[ the.lines ] ) , max( dataset$max[ the.lines ] )),
			 ...) {
	plot( dataset$iter[ the.lines ] , dataset$max[ the.lines ] , type="l" , col=col.max ,
		 xlab=xlab, ylab=ylab,
		 ylim=ylim,
		 bty=bty,
		 ... )
	lines( dataset$iter[ the.lines ] , dataset$min[ the.lines ] , col=col.min , ... )
	lines( dataset$iter[ the.lines ] , dataset$exec[ the.lines ] , col=col.exec , ... )
	legend( legend.pos ,
		   c( "avg. min" , "avg. executed" , "avg. max") ,
		   col=c(col.min, col.exec, col.max ),
		   box.lty=0,
		   lty=1 )
}
