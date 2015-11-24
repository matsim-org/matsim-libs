# ZZ_TODO: Add comment
# 
# Author: aagarwal
###############################################################################

inputFile = file.path(".",commandArgs()[3])
outputFile = file.path(".",commandArgs()[4])

countData = read.table(inputFile,header=TRUE,sep="\t")

SimVolumes = countData$MATSIM.volumes
countVolumes=countData$Count.volumes

png(outputFile,height=800,width=1200)

mar.default <- c(5,5,4,2) + 0.1 ## default is c(5,4,4,2) + 0.1
par(mar = mar.default + c(0, 4, 0, 0)) 

# create a blank canvas and set axes limits
plot(SimVolumes~countVolumes, type="n", log="xy",pch=19,col="black",
		xlim=c(100,100000),ylim=c(100,100000),cex.axis=2,cex.lab=3,
		xlab="CountVolumes [veh/24hr]",ylab="SimVolumes [veh/24hr]",main="Avg. Weekday Traffic Volumes, Iteration:200",cex.main=2)

# horizontal grid lines
abline(
		h   = c( seq( 100,900, 100 ), seq( 1000, 9000, 1000 ), seq( 10000, 100000, 10000 ) ),
		lty = 3,
		col = colors()[ 440 ],cex.axis=1,cex.lab=1 )

# vertical grid lines
abline(
		v   = c( seq( 100,900, 100 ), seq( 1000, 9000, 1000 ), seq( 10000, 100000, 10000 )  ),
		lty = 3,
		col = colors()[ 440 ])

p=seq(100,100000,100)
q=2*p
r=0.5*p

# half, equal and double count lines 
lines(q,p,col="blue",type="l",lwd=5)
lines(r,p,col="blue",type="l",lwd=5)
lines(p,p,col="blue",type="l",lwd=5)

#plot data now
points(SimVolumes~countVolumes,col="black" ,pch=19,cex=2)

dev.off()
