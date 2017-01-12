#
# 
# Author: aagarwal
###############################################################################

require(plyr)
require(plotrix)
require(RColorBrewer)

#read file
dataFile = read.table(commandArgs()[3], header=TRUE,sep="\t")

#output png file
png( commandArgs()[4],height=1000,width=1000)

#margin settings
old.par<-par(mar=c(5,5,4,4))

maxSpeed = 17

#x and y limits
ylim = c(0,3500)
xlim=c(0,3000)

#horizontal and vertical line data
x = c(1:94)
ys = linkLength * x

# create a blank canvas
plot(x,ys,type="n",axes=FALSE,ann=FALSE, xlim,ylim) 

#setting up axis
axis(side=3,at=seq(min(xlim),max(xlim),by=100),cex.axis=1.5)
mtext("distance (m)",side=3,line=2.5,cex=1.5)
axis(side=2,at=seq(max(ylim),min(ylim),by=-100),labels=seq(min(ylim),max(ylim),by=100),cex.axis=1.5)
mtext("time (sec)",side=2,line=3,cex=1.5)

#plotting horizontal lines to show node positions.
abline(v=ys,col="black",lty="dotdash")

sumOfMinMax=min(ylim)+max(ylim)

#add speedratio column to data

dataFile$speed = (dataFile$endPosition - dataFile$startPosition ) / ( dataFile$endPositionTime - dataFile$startPositionTime)
dataFile$speedRatio = dataFile$speed / maxSpeed

#setting color scale
#alternative coloring scheme
#cols <- colorRampPalette(c('red','blue'))
#dataFile$col =cols(10)[as.numeric(cut(dataFile$speedRatio,breaks = 10))]

scale = seq(0,1,by=0.1)
cols = colorRampPalette(brewer.pal(11,"RdYlGn"))(length(scale)-1)
dataFile$col = cols[findInterval(dataFile$speedRatio,scale)]

#plot line segments
segments(dataFile$startPosition, sumOfMinMax - dataFile$startPositionTime, dataFile$endPosition, sumOfMinMax - dataFile$endPositionTime, col=dataFile$col)

#legend
color.legend(max(xlim)+100, 0, max(xlim)+250, max(ylim), legend = scale, rect.col=cols, gradient="y", align="lb")
mtext(paste("Speed\nscale"), side=1, line=-4.50, adj=0.98, cex=1.5, outer=TRUE)

dev.off()