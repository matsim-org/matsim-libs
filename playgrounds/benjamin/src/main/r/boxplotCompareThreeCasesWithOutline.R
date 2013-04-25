#Boxplot: urbanbase neben urban30
#gleiche Limits? -> Vergleichbarkeit
rm(list = ls())		# Clear all variables  
graphics.off()		# Close graphics windows
overall<-F
groupOrder <- c("URBAN", "COMMUTER","REV_COMMUTER","FREIGHT") #one plot for each grop
groupColors <- c("yellow","red","lightgreen","darkgreen") #base case, z30, pricing
meanColor <- c("darkgrey","black","green") 

#read files and set directories
inputDir <- commandArgs()[3]
outputDir <- commandArgs()[4]

distInfoBaCFile <- file.path(inputDir,"detailedCarDistanceInformation_baseCase_ctd_newCode.txt")
distInfoPriFile <- file.path(inputDir,"detailedCarDistanceInformation_policyCase_pricing_newCode.txt")
distInfoZ30File <- file.path(inputDir,"detailedCarDistanceInformation_policyCase_zone30.txt")

distInfoBaC <- read.table(file = distInfoBaCFile, header=T, sep = "\t", comment.char="")
distInfoZ30 <- read.table(file = distInfoZ30File, header=T, sep = "\t", comment.char="")
distInfoPri <- read.table(file = distInfoPriFile, header=T, sep = "\t", comment.char="")

outFileUrban <- file.path(outputDir, "plotDetailedCarTripDistance_WithOutline.pdf")

pdf(outFileUrban, width=7, height=7)

for(group in groupOrder){
#limits
distInfoBaCgroup<-distInfoBaC[(distInfoBaC$user.group==group),]
distInfoZ30group<-distInfoZ30[(distInfoZ30$user.group==group),] 
distInfoPrigroup<-distInfoPri[(distInfoPri$user.group==group),]
ylimitmax<-max(c(max(distInfoBaCgroup$total.car.distance..km.),max(distInfoZ30group$total.car.distance..km.),max(distInfoPrigroup$total.car.distance..km.)))

#boxplots
boxplot(distInfoBaCgroup$total.car.distance..km., notch = T, outline = T, boxwex = 0.3, col=groupColors[1], 
main= paste("Car distance for", group), ylab="Distance in km", at=1:1-0.3, ylimits=c(0, ylimitmax))
boxplot(distInfoZ30group$total.car.distance..km., notch = T, outline = T, boxwex = 0.3, col=groupColors[2], add=T, at=1:1+0.0)
boxplot(distInfoPrigroup$total.car.distance..km., notch = T, outline = T, boxwex = 0.3, col=groupColors[3], add=T, at=1:1+0.3)
axis(1, c(0.7,1,1.3), labels=c("Base Case","Zone 30","Pricing"), tick=F)

#means
aline <- tapply(distInfoBaCgroup$total.car.distance..km., distInfoBaCgroup$user.group==group ,mean)
bline <- tapply(distInfoZ30group$total.car.distance..km., distInfoZ30group$user.group==group ,mean)
cline <- tapply(distInfoPrigroup$total.car.distance..km., distInfoPrigroup$user.group==group ,mean)

#draw means as lines
segments(seq(along = aline) - 0.4, aline, seq(along = aline) - 0.2, aline, lwd = 2, col = meanColor[1]) 
segments(seq(along = bline) - 0.1, bline, seq(along = bline) + 0.1, bline, lwd = 2, col = meanColor[2]) 
segments(seq(along = cline) + 0.2, cline, seq(along = cline) + 0.4, cline, lwd = 2, col = meanColor[3]) 

}
dev.off()

