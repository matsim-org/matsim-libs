data <- read.table(commandArgs()[3], header=T)
x <- as.numeric(row.names(data))
y <- as.numeric(read.table(commandArgs()[3], header=F, fill=T)[1,])
y <- y[c(1:length(y)-1)]

pdf(commandArgs()[4], width=10, height=8)

m <- as.matrix(data)
m <- m[nrow(m):1,] # remove header (first line)
m <- t(m)		   # transpose matrix

zlimitits = range(m, finite=TRUE)
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

color.palette=colorRampPalette(c("black", "purple", "mediumblue", "lightblue", "limegreen", "forestgreen", "yellow", "red", "white"))

image(y, x, m, xlim=c(min(y),min(y)+ny*gridsize), ylim=c(min(x)+nx*gridsize,min(x)), col = color.palette(numoflevels))

dev.off()