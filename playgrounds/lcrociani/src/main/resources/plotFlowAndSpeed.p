set ylabel "density/speed"
set xlabel "time in s"
set title "simulation"
set xtics 2
plot datafile u ($1*0.3-36):2 title "density" w l, datafile u ($1*0.3-36):($3/16*3.333333) title "speed" w l
