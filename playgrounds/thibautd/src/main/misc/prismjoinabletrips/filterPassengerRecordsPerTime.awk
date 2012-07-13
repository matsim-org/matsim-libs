#!/usr/bin/awk -f

# usage:
# zcat passengers.gz | filterPassengerRecordsPerTime [min=-1] [max=-1] [fieldName=jointDriverDist] > out

function logmsg( m ) {
	print m > "/dev/stderr"
}

BEGIN {
	fieldName = "jointDriverDist"
	min=-1
	max=-1
	nextlog=1
}

NR == nextlog {
	logmsg( "reading line # " NR )
	nextlog = nextlog * 2
}

NR==1 {
	print $0

	for (i=1; i <= NF; i++) {
		if ($i==fieldName) {
			column = i
		}
	}

	logmsg( "using column " column )
}

NR > 1 {
	if ( min > 0 && $column < min ) next
	if ( max > 0 && $column > min ) next
	print $0
}
