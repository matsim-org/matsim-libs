my.rbind <-
function( datasets,
		 dataset.name.col="dataset" ) {
	rbind.withcolcompl <- function( d1 , d2 ) {
		for ( name in colnames( d1 ) ) {
			if ( !(name %in% colnames( d2 )) ) {
				d2[[ name ]] <- NA
			}
		}
		for ( name in colnames( d2 ) ) {
			if ( !(name %in% colnames( d1 )) ) {
				d1[[ name ]] <- NA
			}
		}
		rbind( d1 , d2 )
	}

	the.names <- names( datasets )
	ds.with.name <- function( i ) {
		name <- the.names[ i ]
		if ( length( name ) == 0 ) name = as.character( i )
		d <- datasets[[ i ]]
		d[[ dataset.name.col ]] <- name
		d
	}

	merged <- ds.with.name( 1 )
	if ( length( datasets ) > 1 ) {
		for ( i in seq( 2 , length( datasets ) ) ) {
			merged <- rbind.withcolcompl( merged ,  ds.with.name( i ) )
		}
	}
	merged
}
