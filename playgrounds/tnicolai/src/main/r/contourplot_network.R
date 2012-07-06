library(gpclib)
library(maptools)
gpclibPermit()

# loading network shapefile
network.mp <- readShapeLines("/Users/thomas/Development/opus_home/data/zurich_parcel/shapefiles/network/ivtch-osm")

data <- read.table(commandArgs()[3], header=T)
y <- as.numeric(row.names(data))
x <- as.numeric(read.table(commandArgs()[3], header=F, fill=T)[1,])
x <- x[c(1:length(x)-1)]

pdf(commandArgs()[4], width=10, height=10)

m <- as.matrix(data)
# m <- m[nrow(m):1,]# flip matrix at horizontal line
m <- t(m)			# transpose matrix

zlimitits = range(c(4.1,6.17), finite=TRUE)
zlimitits
numoflevels = 100 # default 20

gridsize <- 100
nx <- ceiling((max(x)-min(x))/gridsize)
min(x)
max(x)
nx
ny <- ceiling((max(y)-min(y))/gridsize)
min(y)
max(y)
ny

filled.contour(x, y, m, color.palette=colorRampPalette(c("black", "purple", "mediumblue", "lightblue", "red", "orange", "yellow", "green3", "darkgreen")),levels=pretty(zlimitits, numoflevels), plot.axes = { lines(network.mp, col="black", alpha=0.3, lwd=0.5); axis(1); axis(2) }, asp=1, xlim=c(min(x),min(x)+nx*gridsize), ylim=c(min(y),min(y)+ny*gridsize))

# en- or disable grid
#grid(ny, nx, lty="solid", lwd=0.5)

dev.off()