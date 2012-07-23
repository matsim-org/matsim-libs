#aus der Basetabelle eine neue generieren:
#gleiche Zeilen mit id, gruppe, distance
#weitere spalten hinzufuegen: relativ distance z30 und relative distance pri

rm(list = ls())		# Clear all variables  
graphics.off()		# Close graphics windows

groupColors <- c("yellow","red") #z30, pricing
meanColor <- c("darkgrey","black","green") 

setwd("/home/me/rFiles/plots/analyse")
directory <- getwd()
#read files and set directories
distInfoBaCFile <- file.path(directory,"detailedCarDistanceInformation_baseCase_ctd.txt")
distInfoPriFile <- file.path(directory,"detailedCarDistanceInformation_policyCase_pricing.txt")
distInfoZ30File <- file.path(directory,"detailedCarDistanceInformation_policyCase_zone30.txt")
distInfoBaCFile <- file.path(directory,"detailedCarDistanceInformation_baseCase_ctd_test.txt")
distInfoPriFile <- file.path(directory,"detailedCarDistanceInformation_policyCase_pricing_test.txt")
distInfoZ30File <- file.path(directory,"detailedCarDistanceInformation_policyCase_zone30_test.txt")

distInfoBaC <- read.table(file = distInfoBaCFile, header=T, sep = "\t", comment.char="")
distInfoZ30 <- read.table(file = distInfoZ30File, header=T, sep = "\t", comment.char="")
distInfoPri <- read.table(file = distInfoPriFile, header=T, sep = "\t", comment.char="")

outFile <- file.path(directory, "distanceComp.pdf")

distance <- array(dim=c(nrow(distInfoBaC),7)) #7 columns: person id, user group, total car distance base case,
	#total car distance zone 30, relative change of zone 30 compared to base case
	#total car distance pricing, relative change of pricing compared to base case
#print(colnames(distInfoBaC))
colnames(distance)<-c(colnames(distInfoBaC),"diffZ30", "relZ30", "diffPri", "relPri")
#TODO spalte personid loeschen?
rownames(distance)<-distInfoBaC[,"person.id"]

#basecase
for(i in 1:nrow(distInfoBaC)){
#usergroup column
	distance[i,"user.group"] <- distInfoBaC[i, "user.group"]
#total car distance
	distance[i, "total.car.distance..km."]<- distInfoBaC[i, "total.car.distance..km."]
}

print("start pricing")
#pricing
range<- 10
for(i in 1:nrow(distInfoPri)){
	if(i%%100==0){
		print(i)
		range<-range+10
		}
	pid<- distInfoPri[i, "person.id"]
	dis <- distInfoPri[i, "total.car.distance..km."]
	nearby<-F

	#nearby
	start<-max(1, (i-range))
	end<-min((i+range), nrow(distance))
	for(j in start:end){
		if((rownames(distance)[j])==pid){
				base<- distance[j, "total.car.distance..km."]
				if(length(dis-base)>0){
					distance[j, "diffPri"]<-dis-base
					distance[j, "relPri"]<-100*distance[j, "diffPri"]/base}
					nearby<-T
					break
		}
	}

	#search all
	if(nearby==F){	
	for(j in 1:nrow(distance)){
		if((rownames(distance)[j])==pid){
				base<- distance[j, "total.car.distance..km."]
				if(length(dis-base)>0){
					distance[j, "diffPri"]<-dis-base
					distance[j, "relPri"]<-100*distance[j, "diffPri"]/base}
					print(c(i,j))
					break
			
		}

	}
	}
}
print(distance)
print("start z30")
#z30
range<-10
for(i in 1:nrow(distInfoZ30)){
	if(i%%100==0){
		print(i)
		range<-range+10
		}
	pid<- distInfoZ30[i, "person.id"]
	dis <- distInfoZ30[i, "total.car.distance..km."]
	nearby<-F

	#nearby
	start<-max(1, (i-20))
	#start<-i-range	
	end<-min((i+20), nrow(distance))
	#end<-i+range
		for(j in start:end){
		if((rownames(distance)[j])==pid){
			base<- distance[j, "total.car.distance..km."]
			if(length(dis-base)>0){
				distance[j, "diffZ30"]<-dis-base
				distance[j, "relZ30"]<-100*distance[j, "diffZ30"]/base
				break
			}
		}
	}
	#search all
	if(nearby==F){
	for(j in 1:nrow(distance)){
		if((rownames(distance)[j])==pid){
			base<- distance[j, "total.car.distance..km."]
			if(length(dis-base)>0){
				distance[j, "diffZ30"]<-dis-base
				distance[j, "relZ30"]<-100*distance[j, "diffZ30"]/base
				break
			}
		}
	}
	}
}


print(distance)

#TODO datei evtl speichern? 

pdf(outFile, width=7, height=7)
boxplot(distance[,"diffZ30"], notch=T, outline=T, boxwex = 0.3, col=groupColors[1], 
main= "Difference to base case", xlab="Zone 30, Pricing", ylab="Distance [km]", at=1:1-0.3)
boxplot(distance[,"diffPri"], notch=T, outline=T, boxwex = 0.3, col=groupColors[2], add=T, at=1:1+0.3)

boxplot(distance[,"relZ30"], notch=T, outline=T, boxwex = 0.3, col=groupColors[1], 
main= "Relative difference in distance to base case", xlab="Zone 30, Pricing", ylab="Diffence [%}", at=1:1-0.3)
boxplot(distance[,"relPri"], notch=T, outline=T, boxwex = 0.3, col=groupColors[2], add=T, at=1:1+0.3)

dev.off()

#############################
#limits
#distInfoBaCgroup<-distInfoBaC[(distInfoBaC$usergroup==group),]
#distInfoZ30group<-distInfoZ30[(distInfoZ30$usergroup==group),] 
#distInfoPrigroup<-distInfoPri[(distInfoPri$usergroup==group),]
#ylimitmax<-max(c(max(distInfoBaCgroup$totalcardistance),max(distInfoZ30group$totalcardistance),max(distInfoPrigroup$totalcardistance)))

#boxplots
#boxplot(distInfoBaCgroup$totalcardistance, notch = F, outline = T, boxwex = 0.3, col=groupColors[1], 
#main= "Car distance", xlab=c(paste("User group", group),"Base Case, Zone 30, Pricing"), ylab="distance in km", at=1:1-0.3, ylimits=c(0, ylimitmax))
#boxplot(distInfoZ30group$totalcardistance, notch = F, outline = T, boxwex = 0.3, col=groupColors[2], add=T, at=1:1+0.0)
#boxplot(distInfoPrigroup$totalcardistance, notch = F, outline = T, boxwex = 0.3, col=groupColors[3], add=T, at=1:1+0.3)

#means
#aline <- tapply(distInfoBaCgroup$totalcardistance, distInfoBaCgroup$usergroup==group ,mean)
#bline <- tapply(distInfoZ30group$totalcardistance, distInfoZ30group$usergroup==group ,mean)
#cline <- tapply(distInfoPrigroup$totalcardistance, distInfoPrigroup$usergroup==group ,mean)

#draw means as lines
#segments(seq(along = aline) - 0.4, aline, seq(along = aline) - 0.2, aline, lwd = 2, col = meanColor[1]) 
#segments(seq(along = bline) - 0.1, bline, seq(along = bline) + 0.1, bline, lwd = 2, col = meanColor[2]) 
#segments(seq(along = cline) + 0.2, cline, seq(along = cline) + 0.4, cline, lwd = 2, col = meanColor[3]) 
#######################################
