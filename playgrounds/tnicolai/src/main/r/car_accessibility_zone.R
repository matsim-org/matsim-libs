library(gpclib)
library(maptools) # load maptools
library(RColorBrewer)
library(classInt)
gpclibPermit()

commandArgs()[2]
commandArgs()[3]
commandArgs()[4]

# create pdf
pdf(commandArgs()[4]) #, width=10, height=14

# read csv data
data <- read.csv(file=commandArgs()[3],header=TRUE,sep=",")

# load shape file (this work for zurich shape files only because of given CRS)
shape <- readShapePoly("/Users/thomas/Development/opus_home/data/zurich_parcel/shapefiles/zone")

# loading network shapefile
#network.mp <- readShapeLines("/Users/thomas/Development/opus_home/data/zurich_parcel/shapefiles/network/ivtch-osm")

acc <- c(data$car_accessibility)# contains accessibilites (but unordered)
tmp <- c(data$car_accessibility)# same as above
dat <- c(data$zone_id) 			# zone_id (unordered)
shp <- c(shape$ZONE_ID)			# desiered order for accessibilities

# for debugging
#print(data)
#print(acc)
#print(dat)
#print(shp)

for(i in 1:length(shp)){
	val <- as.numeric(shp[i])
	#print(val)
	for(j in 1:	length(dat)){
		val2 <- as.numeric(dat[j])
		if( val == val2){
			#print(val2)
			acc[i] <- tmp[j]
		}
	}
}

# for debugging
#print(acc)

# when using readShapeLines(network), measured values could be assigned to network links!

# create new shape attribute
shape$travel_time_logsum = acc

#select color palette and the number colors (levels of income) to represent on the map
# color palettes: 
# Blues BuGn BuPu GnBu Greens Greys Oranges OrRd PuBu PuBuGn PuRd Purples RdPu Reds YlGn YlGnBu YlOrBr YlOrRd
# BrBG PiYG PRGn PuOr RdBu RdGy RdYlBu RdYlGn Spectral
# Accent Dark2 Paired Pastel1 Pastel2 Set1 Set2 Set3 

colors <- brewer.pal(11, "RdYlGn")

# chosen style: one of "fixed", "sd", "equal", "pretty", "quantile", "kmeans", "hclust", "bclust", "fisher", or "jenks"
#brks<-classIntervals(shape$travel_time_logsum, n=11, style="pretty")
brks<-classIntervals(shape$travel_time_logsum, n=11, style="fixed", fixedBreaks=c(4.2,4.4,4.6,4.8,5.0,5.2,5.4,5.6,5.8,6.0,6.2))
brks<- brks$brks
brks2digits<- round(brks, digits=2) # limits the breaks to 2 digits after the comma

#prints shape file with travel time log sum data
plot(shape,col=colors[findInterval(shape$travel_time_logsum,brks,all.inside=TRUE)],axes=F)
#lines(network.mp, col="black", lwd=0.5)

#add a title
#title(paste ("Congested Car Travel Time in Accessibility Computation (Zone Level)"))

#add a legend
legend("topright", legend=leglabs(brks2digits), fill=colors, bty="o", bg="white",x.intersp = 1.0, y.intersp = 1.0, title="Accessibility Values")