# use this script via terminal using "load" command
# adopt path to the dat file

set terminal postscript
set output "/Users/thomas/Documents/SVN_Studies/tnicolai/cupum/Data/Highway_Scenario/run_32.2010_12_25_13_45/indicators/zone_table-3_2000-2011_zone__employment_within_30min_travel_time.ps"

set title "Employment within 30 minutes"

set datafile commentschars "#%"

set grid

set xlabel "years"
set ylabel "employment"
#set xrange[0:10]
#set yrange[0:]
#set xtics ("2000" 0, "2001" 1, "2002" 2, "2003" 3, "2004" 4, "2005" 5, "2006" 6, "2007" 7, "2008" 8, "2009" 9, "2010" 10)

plot "/Users/thomas/Documents/SVN_Studies/tnicolai/cupum/Data/Highway_Scenario/run_32.2010_12_25_13_45/indicators/zone_table-3_2000-2011_zone__employment_within_30min_travel_time.dat" using 1:2 title "Highway Scenario" with linespoints,\
"/Users/thomas/Documents/SVN_Studies/tnicolai/cupum/Data/Ferry_Scenario/run_33.2010_12_26_09_30/indicators/zone_table-3_2000-2011_zone__employment_within_30min_travel_time.dat" using 1:2 title "Ferry Scenario" with linespoints