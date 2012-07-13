plotPlans <- function(
			plansFrame,
			leftmost=35,
			barwidth=0.3,
			id="agentId",
			info="peInfo",
			peStart="peStart",
			peEnd="peEnd",
			planScore="planScore",
			mainTitle=NULL,
			printScore=TRUE,
			printDeps=TRUE) {
	plansFrame[[id]] <- factor( plansFrame[[id]] )
	plansFrame[[info]] <- factor( plansFrame[[info]] )
	plansFrame <- setPuDoTimes( plansFrame , info , peStart , peEnd )

	# init plot
	plot.new()
	plot.window( c(0, leftmost) , c(0, nlevels( plansFrame[[id]] ) + 1 ) )

	# data extraction
	xleft <- plansFrame[[peStart]] / 3600
	xright <- plansFrame[[peEnd]] / 3600
	y <- unclass( plansFrame[[id]] )
	ybottom <- y - barwidth
	ytop <- y + barwidth

	xleft <- replace( xleft, which( is.na( xleft ) ), 0)
	xright <- replace( xright, which( is.na( xright ) ), 24)

	# trips departures
	# plotting before the rest implies that it passes "under" the colors
	if (printDeps) {
		pusInd <- grep( "pu" , plansFrame[[info]] )
		pus <- factor( plansFrame[[info]][pusInd] )
		xdep <- split( xright[ pusInd ] , pus ) 
		xarr <- split( xleft[ pusInd + 2 ] , pus ) 
		ydep <- split( y[ pusInd ] , pus ) 

		for (i in 1:length(xdep)) {
			cy <- ydep[[i]]
			n <- length( cy )
			ord <- sort( cy , index.return=TRUE )$ix
			segm <- function( cx ) {
				segments(
					 cx[ ord[1:(n-1)] ] , cy[ ord[1:(n-1)] ] + barwidth ,
					 cx[ ord[2:n] ] , cy[ ord[2:n] ] - barwidth )
			}
			segm( xdep[[i]] )
			segm( xarr[[i]] )
		}
	}

	myfills <- rainbow( nlevels( plansFrame[[info]] ) )
	rect( xleft , ybottom , xright , ytop , col=myfills[unclass( plansFrame[[info]] )] )

	# axis, legend...
	axis(1, at=seq(0,24,by=4))
	axis(2, at=seq( 1, nlevels(plansFrame[[id]]), by=1 ), labels=levels( plansFrame[[id]] ))

	if (printScore) {
		text( 1, unique(ytop + 0.1) , labels=sprintf( "score=%.2f" , unique( plansFrame[[planScore]] )) , pos=4 )
	}

	legend(c(25,leftmost),
		   c(0, nlevels( plansFrame[[id]] ) + 1),
		   levels( plansFrame[[info]] ),
		   fill=myfills,
		   bty="n")

	title( xlab="time (h)", ylab="agent" , main=mainTitle)
}

plotCliquePlan <- function( cliquesFrame , plansFrame , cliqueId , agId="agentId" , persId = "personId" , clId = "cliqueId", ... ) {
	plotPlans(
			  subset(
					 plansFrame ,
					 plansFrame[[agId]] %in%
						 subset( cliquesFrame[[persId]] , cliquesFrame[[clId]] == cliqueId) ),
			  id=agId,
			  ...)
}

plotPopulatedCliquePlanNr <- function( cliquesFrame , plansFrame , n , clId = "cliqueId", displayTitle=FALSE, ... ) {
	id <- unique( cliquesFrame[[clId]][ which( duplicated( cliquesFrame[[clId]] ) ) ] )[n]
	mainTitle <- NULL
	if (displayTitle) mainTitle <- paste("plan of clique",id)
	plotCliquePlan( cliquesFrame , plansFrame , id, clId=clId, mainTitle=mainTitle, ...)
}

setPuDoTimes <- function( plansFrame , info="peInfo" , peStart="peStart" , peEnd="peEnd" ) {
	acts <- grep( "pick" , plansFrame[[info]] )
	acts <- c( acts , grep( "drop" , plansFrame[[info]] ) )
	plansFrame[[peStart]][acts] <- plansFrame[[peEnd]][(acts - 1)]
	plansFrame[[peEnd]][acts] <- plansFrame[[peStart]][(acts + 1)]
	plansFrame
}
