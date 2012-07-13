#!/usr/bin/awk -f

# produces "histogram" datasets, that is, count per bin, on stdout
# information messages are sent to stderr
# typical call: gunzip -c passengerTrips.dat.gz | generateBarPlotDatasetsFromPassengerRecords.awk [twBinWidth=n] [detourBinWidth=d] > counts.dat
#
# various checks are performed to be sure the data is consistent and consistently handled.
# failing tests result in the following return codes:
# 1: negative detour
# 2: field not recognized as numeric
# 3: null time window but detour greater than one

function isString(field) {
	return ((field + 0) != field)
}

BEGIN {
	twBinWidth = 30
	detourBinWidth = 0.025
	nextp = 1
	fs[5]=0
	fs[6]=0
	fs[7]=0
	fs[3]=0
	fs[8]=0
	fs[9]=0
	fs[10]=0
	fs[4]=0
	fs[11]=0
}

NR == nextp {
	print "reading line # " NR >> "/dev/stderr"
	nextp *= 2
}

NR > 1 {
	for (f in fs) if (isString($f)) {
		print "non numeric value in line # " NR >> "/dev/stderr"
		print $f " is non numeric" >> "/dev/stderr"
		print "in " $0  >> "/dev/stderr"
		exit 2
	}

	detourDist = ($5 + $6 + $7) / $3
	detourDur = ($8 + $9 + $10) / $4
	tw = $11

	if ( detourDist < 0 || detourDur < 0) {
		print "negative value in line # " NR >> "/dev/stderr"
		print "detourdist=" detourDist >> "/dev/stderr"
		print "detourdur=" detourDur >> "/dev/stderr"
		print $0 >> "/dev/stderr"
		exit 1
	}

	if (tw > 0) {
		i = (tw / twBinWidth)
		i -= (tw % twBinWidth) / twBinWidth
		twBins[i]++
	}
	else {
		i = ((tw-1) / twBinWidth)
		i -= ((tw-1) % twBinWidth) / twBinWidth
		twBins[i]++
	}
	if (tw <= 0 && detourDur > 1) {
		print "got a null time window whereas the driver detour is positive" >> "/dev/stderr"
		print $0 >> "/dev/stderr"
		print "tw " >> "/dev/stderr"
		print "detour " detourDur >> "/dev/stderr"
		exit 3
	}

	i = (detourDist / detourBinWidth)
	i -= (detourDist % detourBinWidth) / detourBinWidth
	distBins[i]++

	i = (detourDur / detourBinWidth)
	i -= (detourDur % detourBinWidth) / detourBinWidth
	durBins[i]++
}

END {
	print "binType", "binMin", "binMax", "count"

	for (i in twBins) print "tw", (i * twBinWidth), ((i+1) * twBinWidth), twBins[i]
	for (i in distBins) print "distDetour", (i * detourBinWidth), ((i+1) * detourBinWidth), (distBins[i])
	for (i in durBins) print "durDetour", (i * detourBinWidth), ((i+1) * detourBinWidth), durBins[i]
}
