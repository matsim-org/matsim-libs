#!/usr/bin/awk -f

# produces filtered passenger records to specified output files: ofile for records where passenger and
# driver have the same origin; dfile when they have the same destination
# informative messages are printed to stdout
# usage: filterPassengerRecordsSameOd.awk ofile=outfileForSameO.dat dfile=outfileForSameD.dat records passengerRecords
# or, if passenger records are gziped:
# gunzip -c passengerRecords.gz | filterPassengerRecordsSameOd.awk ofile=outfileForSameO.dat dfile=outfileForSameD.dat records -

r {
	if (++c == nextp) {
		print "line # " c
		nextp *= 2
	}
	
	origin[$1] = $5
	destination[$1] = $6
}

# records file
$1 == "recordId" {
	print "entering file " FILENAME
	r=1
	c=0
	nextp=1
}

p {
	if (++c == nextp) {
		print "line # " c
		nextp *= 2
	}
	
	if (origin[$1] == origin[$2]) {
		oc++
		print $0 >> ofile
	}
	if (destination[$1] == destination[$2]) {
		dc++
		print $0 >> dfile
	}
}

# passenger records file
$1 == "driverRecordId" {
	print "entering file " FILENAME
	c=0
	nextp=1

	r=0
	p=1
	print $0 > ofile
	print $0 > dfile
}

END {
	print oc " records with one origin"
	print dc " records with one destination"
}
