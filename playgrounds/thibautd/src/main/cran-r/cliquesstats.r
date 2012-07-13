cliquesSizes <- function( cliqueFrame ) {
	sizes <- c()

	dup <- duplicated( cliqueFrame$cliqueId , fromLast=TRUE )

	count <- 1
	for ( v in dup ) {
		if (!v) {
			sizes <- c( sizes , count )
			count <- 1
		}
		else {
			count <- count + 1
		}
	}

	sizes
}

scoresOfPassengerPlans <- function( plansFrame , agentId="agentId" , info="peInfo" , score="planScore" ) {
	p <- grepl( "passenger" , plansFrame[[info]] )
	plansFrame <- subset( plansFrame , p )
	d <- duplicated( plansFrame[[agentId]] )
	plansFrame <- subset( plansFrame , d )
	plansFrame[[score]]
}
