#!/bin/sh

# usage: lineCounts file1 file2 ... > out

dbg () {
	echo "$*" > /dev/stderr
}

for f in $*
do
	if [ -f "$f" ]
	then
		msg="counting for $f"
		if echo $f | grep '.gz$' > /dev/null
		then
			dbg $msg "(gziped)"
			CAT=zcat
		else
			dbg $msg "(not gziped)"
			CAT=cat
		fi
		if content="$($CAT "$f")"
		then
			echo "$content" | awk 'END {print (NR - 1)}'
		else 
			dbg "file $f was skiped: $CAT returned with exit code $?"
		fi
	fi
done
