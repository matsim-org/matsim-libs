# really more a cheatsheet than real functions!
	# rem:
	# dataframe[[ "columId" ]] == dataframe$columnId
	# => rigth exp cannot be used when passing column name unquoted.
	# => here, we need to quote the column id.
	# but in the interpreter, columnId (unquoted) can be used instead of dataframe[[ "columId" ]]

filterRegexpMatches <- function( regexp, dataframe, columnId) {
	subset( dataframe , grepl( regexp , dataframe[[columnId]] ) )
}

filterDuplicates <- function( dataframe , columnId ) {
	subset( dataframe , !duplicated( dataframe[[columnId]] ) )
}
