#in: avgTTInformation_baseCase_ctd
#in....

#out: modalsplit, V1: barplot eleven groups

rm(list=ls())
graphics.off()

colors<-c(1:11) #colors for eleven groups - will be repeated if necessary
#use colours()[1:11] for the other color package - colors nows only 8 colors

#read files and set directories
inputDir <- commandArgs()[3]
outputDir <- commandArgs()[4]

BaCFile <- file.path(inputDir,"avgTTInformation_baseCase_ctd_newCode.txt")
PriFile <- file.path(inputDir,"avgTTInformation_policyCase_pricing_newCode.txt")
Z30File <- file.path(inputDir,"avgTTInformation_policyCase_zone30.txt")

BaC <- read.table(file = BaCFile, header=T, sep = "\t", comment.char="")
Z30 <- read.table(file = Z30File, header=T, sep = "\t", comment.char="")
Pri <- read.table(file = PriFile, header=T, sep = "\t", comment.char="")

outputFile <- file.path(outputDir, "ModalSplit.pdf")
rownames(BaC)<-paste(BaC[,"mode"], BaC[,"user.group"])
rownames(Pri)<-paste(Pri[,"mode"], Pri[,"user.group"])
rownames(Z30)<-paste(Z30[,"mode"], Z30[,"user.group"])

BaCmodes<-tapply(BaC[,"departures"],BaC[,"mode"],sum)
Primodes<-tapply(Pri[,"departures"],Pri[,"mode"],sum)
Z30modes<-tapply(Z30[,"departures"],Z30[,"mode"],sum)

#generate new matrix, columns "case" "user group" 

pdf(outputFile)
par(mar=c(12,5,5,2)) #bottom,left,top,right

#V1: barplot eleven groups
barplot(BaC[,"departures"], names.arg=rownames(BaC), las=2, cex.lab=0.5, main="Number of trips base case", col=colors )
barplot(Pri[,"departures"], names.arg=rownames(Pri), las=2, cex.lab=0.5, main="Number of trips policy case pricing", col=colors )
barplot(Z30[,"departures"], names.arg=rownames(Z30), las=2, cex.lab=0.5, main="Number of trips policy case zone 30", col=colors )

#comparative plots
barplot(Z30[,"departures"]-BaC[,"departures"], names.arg=rownames(Z30), las=2, cex.lab=0.5, main="Number of trips: Diffence zone 30 to base case", col=colors )
barplot(Pri[,"departures"]-BaC[,"departures"], names.arg=rownames(Pri), las=2, cex.lab=0.5, main="Number of trips: Diffence pricing to base case", col=colors )

#comparative plots by usergroups
diffZ30<- subset(Z30[,"departures"]-BaC[,"departures"],Z30$user.group=="URBAN")
rownamesdiffZ30<-rownames(subset(Z30, Z30$user.group=="URBAN"))
barplot(diffZ30, names.arg=rownamesdiffZ30, las=2, cex.lab=0.5, main="Number of trips: Diffence zone 30 to base case, URBAN", col=colors )
diffZ30n<- subset(Z30[,"departures"]-BaC[,"departures"],Z30$user.group!="URBAN")
rownamesdiffZ30n<-rownames(subset(Z30, Z30$user.group!="URBAN"))
barplot(diffZ30n, names.arg=rownamesdiffZ30n, las=2, cex.lab=0.5, main="Number of trips: Diffence zone 30 to base case, not URBAN", col=colors )

#pie charts
#set new margins
par(mar=c(0,4,2,7), mfrow=c(1,1))

pie.data <- BaC[,"departures"]
names(pie.data)<- rownames(BaC)
pie(pie.data, main="Base case") #rotate with  'init.angle=45' #set colors with 'col=colors'

par(mfrow=c(2,2), oma=c(0,0,0,0), mar=c(2,2,4,2))
#pie(BaCmodes, main="Base case")
#pie(Primodes, main="Pricing")
#pie(Z30modes, main="Zone 30")
levelsUrb<- subset(BaC[,"mode"], BaC$user.group=="URBAN")
levelsCom<- subset(BaC[,"mode"], BaC$user.group=="COMMUTER")
levelsRev<- subset(BaC[,"mode"], BaC$user.group=="REV_COMMUTER")
levelsFre<- subset(BaC[,"mode"], BaC$user.group=="FREIGHT")

pie(subset(BaC[,"departures"], BaC$user.group=="URBAN"), labels=levelsUrb, main="URBAN")
pie(subset(BaC[,"departures"], BaC$user.group=="COMMUTER"), labels=levelsCom, main="COMMUTER")
pie(subset(BaC[,"departures"], BaC$user.group=="REV_COMMUTER"), labels=levelsRev, main="REV_COMMUTER")
pie(subset(BaC[,"departures"], BaC$user.group=="FREIGHT"), labels=levelsFre, main="FREIGHT")

dev.off()

