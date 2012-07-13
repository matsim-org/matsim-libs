#!/usr/bin/awk -f

# filters a passenger records file to more restrictive parameters
# usage:
# zcat passengerRecords.gz | restrictPassengerRecordsParams.awk [tw=1700] [detour=1.5] > out
# not specifying (or negative) the variables leads to no filtering
# informative msgs on stderr

function logmsg( msg ) {
	print msg >> "/dev/stderr"
}

BEGIN {
	tw = -1
	detour = -1
	np = 1
}

NR == 1 {
	logmsg( "filtering " FILENAME )
	if (tw >= 0) logmsg( "tw = " tw )
	else logmsg( "tw unfiltered" )
	if (detour >= 0) logmsg( "detour = " detour )
	else logmsg( "detour unfiltered" )
	print $0
}

NR > 1 {
	toprint = 1
	if (tw >= 0) toprint = toprint && ($11 >= tw)
	if (detour >= 0) toprint = toprint && ((($8 + $9 + $10) / $4) >= detour)
	if (toprint) print $0
}

NR == np {
	logmsg("reading line # " NR)
	np = np * 2
}
