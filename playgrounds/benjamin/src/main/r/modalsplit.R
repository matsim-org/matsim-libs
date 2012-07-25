#in: avgTTInformation_baseCase_ctd
#in....

#out: modalsplit, V1: barplot eleven groups

rm(list=ls())
graphics.off()

colors<-c(1:11) #colors for eleven groups - will be repeated if necessary

#read files and set directories
directory <- commandArgs()[3]

BaCFile <- file.path(directory,"avgTTInformation_baseCase_ctd.txt")
PriFile <- file.path(directory,"avgTTInformation_policyCase_pricing.txt")
Z30File <- file.path(directory,"avgTTInformation_policyCase_zone30.txt")

BaC <- read.table(file = BaCFile, header=T, sep = "\t", comment.char="")
Z30 <- read.table(file = Z30File, header=T, sep = "\t", comment.char="")
Pri <- read.table(file = PriFile, header=T, sep = "\t", comment.char="")

outputFile <- file.path(commandArgs()[4], "ModalSplit.pdf")
rownames(BaC)<-paste(BaC[,"mode"], BaC[,"user.group"])
rownames(Pri)<-paste(Pri[,"mode"], Pri[,"user.group"])
rownames(Z30)<-paste(Z30[,"mode"], Z30[,"user.group"])

BaCmodes<-tapply(BaC[,"departures"],BaC[,"mode"],sum)
Primodes<-tapply(Pri[,"departures"],Pri[,"mode"],sum)
Z30modes<-tapply(Z30[,"departures"],Z30[,"mode"],sum)

pdf(outputFile)
par(mar=c(12,5,5,2)) #bottom,left,top,right

#V1: barplot eleven groups
barplot(BaC[,"departures"], names.arg=rownames(BaC), las=2, cex.lab=0.5, main="Modal split per trips base case", col=colors )
barplot(Pri[,"departures"], names.arg=rownames(Pri), las=2, cex.lab=0.5, main="Modal split per trips policy case pricing", col=colors )
barplot(Z30[,"departures"], names.arg=rownames(Z30), las=2, cex.lab=0.5, main="Modal split per trips policy case zone 30", col=colors )

#comparative plots
barplot(Z30[,"departures"]-BaC[,"departures"], names.arg=rownames(Z30), las=2, cex.lab=0.5, main="Modal split: Diffence zone 30 to base case", col=colors )
barplot(Pri[,"departures"]-BaC[,"departures"], names.arg=rownames(Pri), las=2, cex.lab=0.5, main="Modal split: Diffence pricing to base case", col=colors )
barplot(Pri[,"departures"]-Z30[,"departures"], names.arg=rownames(Pri), las=2, cex.lab=0.5, main="Modal split: Diffence pricing to zone 30", col=colors )

#set new margins
par(mar=c(0,2,2,7))

pie.data <- BaC[,"departures"]
names(pie.data)<- rownames(BaC)
pie(pie.data, main="Base case") #rotate with  'init.angle=45' #set colors with 'col=colors'

par(mfrow=c(2,2), oma=c(0,0,0,0), mar=c(2,2,4,2))
pie(BaCmodes, main="Base case")
pie(Primodes, main="Pricing")
pie(Z30modes, main="Zone 30")

dev.off()

