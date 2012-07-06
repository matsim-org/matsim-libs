data <- read.table(commandArgs()[3], header=T)
x <- as.numeric(row.names(data))
y <- as.numeric(read.table(commandArgs()[3], header=F, fill=T)[1,])
y <- y[c(1:length(y)-1)]

pdf(commandArgs()[4], width=10, height=10)

m <- as.matrix(data)
m <- m[nrow(m):1,]	# remove header (first line)
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

filled.contour(y,x,m,color.palette=colorRampPalette(c("black", "purple", "mediumblue", "lightblue", "red", "orange", "yellow", "green3", "darkgreen")),levels=pretty(zlimitits, numoflevels), asp=1, ylim=c(min(x)+nx*gridsize,min(x)), xlim=c(min(y),min(y)+ny*gridsize))

# en- or disable grid
#grid(ny, nx, lty="solid", lwd=0.5)

dev.off()