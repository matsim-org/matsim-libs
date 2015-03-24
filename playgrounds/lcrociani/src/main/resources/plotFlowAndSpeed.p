set ylabel "flow/speed"
set xlabel "time in s"
set title "simulation"
plot datafile u ($1*0.3):2 title "flow" w l, datafile u ($1*0.3):($3/16*3.333333) title "speed" w l
