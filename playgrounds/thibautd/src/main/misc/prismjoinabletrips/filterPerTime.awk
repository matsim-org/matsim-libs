#!/usr/bin/awk -f

# usage:
# zcat passengerTrips.gz | filterPerTime.awk [minTime=0] [maxTime=24] records - 1> out 2> logout

function logmsg( msg ) {
	print msg >> "/dev/stderr"
}

BEGIN {
	minTime=0
	maxTime=24
}

r {
	if ($7 >= minTime && $7 <= maxTime) {
		departure[$1] = 1
	}
}

# records file
$1 == "recordId" {
	logmsg( "entering file " FILENAME )
	minTime = minTime * 3600
	maxTime = maxTime * 3600
	logmsg( "minTime=" minTime )
	logmsg( "maxTime=" maxTime )
	r=1
	c=0
	nextp=1
}

p && departure[$1] {
	# print if driver departure in restriction
	print $0
}

# passenger records file
$1 == "driverRecordId" {
	logmsg( "entering file " FILENAME )
	c=0
	nextp=1

	r=0
	p=1
	print $0
}

# log
++c == nextp {
	logmsg( "line # " c )
	nextp *= 2
}
