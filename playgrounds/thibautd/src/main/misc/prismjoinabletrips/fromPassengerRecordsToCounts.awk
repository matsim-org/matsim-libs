#!/usr/bin/awk -f

# takes a "passengerRecords" file as input
# produces agregated results on standard output
# information msgs are printed on stderr
#
# typical use:
# gunzip -c passengerRecords.dat.gz | fromPassengerRecordsToCounts.awk > counts.dat

BEGIN {
	nextp = 1
}

nextp == NR {
	print "reading line # " nextp > "/dev/stderr"
	nextp *= 2
}

#EACH LINE
{
	if (i)
	{
		# increment the number of times this trip is a driver trip
		d[$1]++;
		# and a passenger trip
		p[$2]++;
		# and record network distances and durations
		if (!dist[$1])
		{
			dist[$1] = $3
			dur[$1] = $4
		}
		if (!dist[$2])
		{
			dist[$2] = $6
			dur[$2] = $9
		}
	}
	else
	{
		# skip first line
		i=1
	}
}

END {
	print "reading line # " NR > "/dev/stderr"
	print "recordId", "nDriverTrips", "nPassengerTrips", "networkFreeFlowDist", "networkFreeFlowDur";
	nextp = 1
	for (id in dist)
	{
		c++
		if (c == nextp)
		{
			 print "writing line # " c > "/dev/stderr"
			 nextp *= 2
		}
		if (!p[id]) p[id]=0
		if (!d[id]) d[id]=0
		print id, d[id], p[id], dist[id], dur[id]
	}
	print "writing line # " c > "/dev/stderr"
}
