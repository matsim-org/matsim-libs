library(maptools)
library(akima)

gpclibPermit()

data <- read.table(commandArgs()[3], header=T,sep="\t")

#set scale
#scale <- quantile(data$q, probs= seq(0,1,by=0.1), na.rm=T)	#linear deciles
scale <- 2710/2700 * c(0.000, 0.01, 0.05, 0.10, 0.25, 0.5, 0.75, 1.0)      #pseudo logarithmic

print( max(data$q) )

#get table headers
x <- data$k_bike
y <- data$k_car

print(paste("Scale: ", scale))

png(commandArgs()[4], width=600, height=600)
#par(oma=c(2,2,1,5))
par(mar=c(6,5,4,3),oma=c(1,0,0,0))

matrix = interp(x/135,y/135,data$q/2700)

ind.mat.na <- which(is.na(c(matrix$z)))

filled.contour(
matrix, nlevels=30, asp=1, xlim=c(0,1), ylim=c(0,1),
frame.plot=F,
levels=scale,
#xlab="Bike density",
#ylab="Car density", cex.lab=1.8,
plot.axes={axis(1,cex.axis=1.6)
		   axis(2,cex.axis=2) },
plot.title={title(xlab="Bike density",cex.lab=1.8)
			mtext("Car density",2,cex=1.8,adj=0.5,las=0,line=3.50) },		   
color.palette=colorRampPalette(c("green3","yellow", "red"),space = "rgb"),key.axes={axis(4,cex.axis=1.5)}
)

mtext(paste("Overall flow"), side=1, line=-4.5, adj=0.98, cex=1.5, outer=TRUE)
#mtext(paste("[PCU/h]"), side=1, line=-2.3, adj=0.92, cex=1.5, outer=TRUE)

dev.off()
