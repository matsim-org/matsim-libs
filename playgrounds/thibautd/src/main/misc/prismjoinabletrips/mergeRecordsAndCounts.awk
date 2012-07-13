#!/usr/bin/gawk -f

# merges the records and counts in one dataset
# usage:
# mergeRecordsAndCounts.awk records counts [largerCounts] > out
# records and counts are the files to merge
# larger counts is a count file to be used to get the distances and durations
# for records not in the first count file (optional)
# explicitly uses gawk, because mawk (the default debian version) cannot
# handle large arrays

function logmsg(msg) {
	print msg >> "/dev/stderr"
}

$1 ~ /[a-zA-Z]/ {
	logmsg("title line of file " FILENAME)
	filecount++
	c=0
	nextprint=1
	
	if (filecount == 1) titles = $0
	else if (filecount == 2) {
		i=1
		while (i++ < NF) {
			titles = titles OFS $i
		}
		print titles
	}
}

++c == nextprint {
	logmsg("reading line " c)
	nextprint = nextprint * 2
}

filecount == 1 && $1 !~ /[a-zA-Z]/ {
	records[$1] = $0
}

filecount == 2 && $1 !~ /[a-zA-Z]/ {
	line = records[$1]
	got[$1] = 1
	i=1
	while (i++ < NF) {
		line = line OFS $i
	}
	print line
}

filecount == 3 && $1 !~ /[a-zA-Z]/ {
	if (!got[$1]) distAndDur[$1] = $4 OFS $5
}

END {
	# we still need to consider the records with 0 trips.
	for (id in records) {
		if (!got[id]) {
			end = distAndDur[id]
			if (!end) end = "NA" OFS "NA"
			print records[id] OFS 0 OFS 0 OFS end
		}
	}
}
