#aus der Basetabelle eine neue generieren:
#gleiche Zeilen mit id, gruppe, distance
#weitere spalten hinzufuegen: relativ distance z30 und relative distance pri

rm(list = ls())		# Clear all variables  
graphics.off()		# Close graphics windows

groupColors <- c("yellow","red") #z30, pricing
meanColor <- c("red","green") #z30, pricing
userGroupColors<-c("orange1", "orange2", "orange3","orange4")
groups<-c("URBAN","COMMUTER","REV_COMMUTER","FREIGHT")

#read files and set directories
inputDir <- commandArgs()[3]
outputDir <- commandArgs()[4]

priFile <- file.path(inputDir,"detailedCarDistanceInformation_policyCase_pricing_newCode-baseCase_ctd_newCode.txt")
z30File <- file.path(inputDir,"detailedCarDistanceInformation_policyCase_zone30-baseCase_ctd_newCode.txt")

z30 <- read.table(file = z30File, header=T, sep = "\t", comment.char="")
pri <- read.table(file = priFile, header=T, sep = "\t", comment.char="")
outFile <- file.path(outputDir, "distanceDifference.pdf")

maximum<-max(z30[,"total.car.distance..km."],pri[,"total.car.distance..km."])
minimum<-min(z30[,"total.car.distance..km."],pri[,"total.car.distance..km."])

pdf(outFile, width=7, height=7)
#first plot without outline
boxplot(z30[,"total.car.distance..km."], notch=T, outline=F, boxwex = 0.3, col=groupColors[1], 
main= "Difference to base case for all groups", ylab="Distance [km]", at=1:1-0.3)
boxplot(pri[,"total.car.distance..km."], notch=T, outline=F, boxwex = 0.3, col=groupColors[2], add=T, at=1:1+0.3)

#means
aline <- mean(z30[,"total.car.distance..km."])
bline <- mean(pri[,"total.car.distance..km."])

#draw means as lines
segments(seq(along = aline) - 0.4, aline, seq(along = aline) - 0.2, aline, lwd = 2, col = meanColor[1]) 
segments(seq(along = bline) + 0.2, bline, seq(along = bline) + 0.4, bline, lwd = 2, col = meanColor[2]) 
axis(1, c(0.7,1.3), labels=c("Zone 30","Pricing"), tick=F)

#second plot with outline
boxplot(z30[,"total.car.distance..km."], notch=T, outline=T, boxwex = 0.3, col=groupColors[1], 
main= "Difference to base case for all groups", ylab="Distance [km]", at=1:1-0.3)
boxplot(pri[,"total.car.distance..km."], notch=T, outline=T, boxwex = 0.3, col=groupColors[2], add=T, at=1:1+0.3)

#draw means as lines
segments(seq(along = aline) - 0.4, aline, seq(along = aline) - 0.2, aline, lwd = 2, col = meanColor[1]) 
segments(seq(along = bline) + 0.2, bline, seq(along = bline) + 0.4, bline, lwd = 2, col = meanColor[2]) 
axis(1, c(0.7,1.3), labels=c("Zone 30","Pricing"), tick=F)

#for each group
for (i in groups){

#first plot without outline
groupZ30<- subset(z30, z30$user.group==i)
groupPri<- subset(pri, pri$user.group==i)
groupMain <- paste("Difference to base case for user group", i)

boxplot(groupZ30[,"total.car.distance..km."], notch=T, outline=F, boxwex = 0.3, col=groupColors[1], 
main= groupMain, ylab="Distance [km]", at=1:1-0.3, range=1)
boxplot(groupPri[,"total.car.distance..km."], notch=T, outline=F, boxwex = 0.3, col=groupColors[2], add=T, at=1:1+0.3)

#means
aline <- mean(groupZ30[,"total.car.distance..km."])
bline <- mean(groupPri[,"total.car.distance..km."])

#draw means as lines
segments(seq(along = aline) - 0.4, aline, seq(along = aline) - 0.2, aline, lwd = 2, col = meanColor[1]) 
segments(seq(along = bline) + 0.2, bline, seq(along = bline) + 0.4, bline, lwd = 2, col = meanColor[2]) 
axis(1, c(0.7,1.3), labels=c("Zone 30","Pricing"), tick=F)

#second plot with outline
boxplot(groupZ30[,"total.car.distance..km."], notch=T, outline=T, boxwex = 0.3, col=groupColors[1], 
main= groupMain, ylab="Distance [km]", at=1:1-0.3)
boxplot(groupPri[,"total.car.distance..km."], notch=T, outline=T, boxwex = 0.3, col=groupColors[2], add=T, at=1:1+0.3)

#draw means as lines
segments(seq(along = aline) - 0.4, aline, seq(along = aline) - 0.2, aline, lwd = 2, col = meanColor[1]) 
segments(seq(along = bline) + 0.2, bline, seq(along = bline) + 0.4, bline, lwd = 2, col = meanColor[2]) 
axis(1, c(0.7,1.3), labels=c("Zone 30","Pricing"), tick=F) 

}


dev.off()
