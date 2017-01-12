#
# 
# Author: aagarwal
###############################################################################

require(lattice)

#read file
dataFile = read.table(commandArgs()[3], header=TRUE,sep="\t")

#output png file
png( commandArgs()[4],height=1000,width=1000)

# color scale
colfunc<-colorRampPalette(c("red","yellow","springgreen","royalblue"))(5)

# first subsets will be created for personId and cycleNumber and then data will be plotted for each of the subset. Point, line or both can be assigned to type.
xyplot(time ~ positionOnLink, groups=c(personId, cycleNumber), data=dataFile, col=colfunc[dataFile$col])

dev.off()

