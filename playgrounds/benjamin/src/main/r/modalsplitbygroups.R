#in: avgTTInformation_baseCase_ctd

rm(list=ls())
graphics.off()

colors<-c(1:6) #colors for modes - will be repeated if necessary

#read files and set directories
directory <- commandArgs()[3]

BaCFile <- file.path(directory,"avgTTInformation_baseCase_ctd.txt")
PriFile <- file.path(directory,"avgTTInformation_policyCase_pricing.txt")
Z30File <- file.path(directory,"avgTTInformation_policyCase_zone30.txt")

BaC <- read.table(file = BaCFile, header=T, sep = "\t", comment.char="")
Z30 <- read.table(file = Z30File, header=T, sep = "\t", comment.char="")
Pri <- read.table(file = PriFile, header=T, sep = "\t", comment.char="")

outputFile <- file.path(commandArgs()[4], "ModalSplitByGroups.pdf")
rownames(BaC)<-paste(BaC[,"mode"], BaC[,"user.group"])
rownames(Pri)<-paste(Pri[,"mode"], Pri[,"user.group"])
rownames(Z30)<-paste(Z30[,"mode"], Z30[,"user.group"])

BaCmodes<-tapply(BaC[,"departures"],BaC[,"mode"],sum)
Primodes<-tapply(Pri[,"departures"],Pri[,"mode"],sum)
Z30modes<-tapply(Z30[,"departures"],Z30[,"mode"],sum)

#generate new matrix, columns "case" "user group" 

pdf(outputFile, height=12, width=20)
par(mar=c(2,0,2,0), oma=c(2,2,2,2))
m<-matrix(c(1,2,2,2,2,14,1,3,3,3,3,14,1,4,4,4,4,14,
1,5,5,5,5,14,1,6,6,6,6,14,1,7,7,7,7,14,
1,8,8,8,8,14,1,9,9,9,9,14,1,10,10,10,10,14,
1,11,11,11,11,14,1,12,12,12,12,14,1,13,13,13,13,14)
,6,12)
layout(m)

#par(mfrow=c(1,12), mar=c(2,0,0,0), oma=c(4,4,4,4))
basUrb<-cbind(subset(BaC[,"departures"], BaC$user.group=="URBAN"))
priUrb<-cbind(subset(Pri[,"departures"], Pri$user.group=="URBAN"))
z30Urb<-cbind(subset(Z30[,"departures"], Z30$user.group=="URBAN"))
basCom<-cbind(subset(BaC[,"departures"], BaC$user.group=="COMMUTER"))
priCom<-cbind(subset(Pri[,"departures"], Pri$user.group=="COMMUTER"))
z30Com<-cbind(subset(Z30[,"departures"], Z30$user.group=="COMMUTER"))
basRev<-cbind(subset(BaC[,"departures"], BaC$user.group=="REV_COMMUTER"))
priRev<-cbind(subset(Pri[,"departures"], Pri$user.group=="REV_COMMUTER"))
z30Rev<-cbind(subset(Z30[,"departures"], Z30$user.group=="REV_COMMUTER"))
basFre<-cbind(subset(BaC[,"departures"], BaC$user.group=="FREIGHT"))
priFre<-cbind(subset(Pri[,"departures"], Pri$user.group=="FREIGHT"))
z30Fre<-cbind(subset(Z30[,"departures"], Z30$user.group=="FREIGHT"))


basUrb[,1]<-basUrb[,1]*100/sum(basUrb)
priUrb[,1]<-priUrb[,1]*100/sum(priUrb)
z30Urb[,1]<-z30Urb[,1]*100/sum(z30Urb)
basCom[,1]<-basCom[,1]*100/sum(basCom)
priCom[,1]<-priCom[,1]*100/sum(priCom)
z30Com[,1]<-z30Com[,1]*100/sum(z30Com)
basRev[,1]<-basRev[,1]*100/sum(basRev)
priRev[,1]<-priRev[,1]*100/sum(priRev)
z30Rev[,1]<-z30Rev[,1]*100/sum(z30Rev)
basFre[,1]<-basFre[,1]*100/sum(basFre)
priFre[,1]<-priFre[,1]*100/sum(priFre)
z30Fre[,1]<-z30Fre[,1]*100/sum(z30Fre)


plot.new();text(0.5,0.5,"Modal split by groups", cex=4)

barplot(basUrb, beside=F, axisnames=T, names.arg= c("Base case"), col=colors)
barplot(priUrb, beside=F, axes=F, axisnames=T, names.arg= c("Pricing"), col=colors)
barplot(z30Urb, beside=F, axes=F, axisnames=T, names.arg= c("Zone 30"), col=colors)
barplot(basCom, beside=F, axes=F, axisnames=T, names.arg= c("Base case"), col=colors)
barplot(priCom, beside=F, axes=F, axisnames=T, names.arg= c("Pricing"), col=colors)
barplot(z30Com, beside=F, axes=F, axisnames=T, names.arg= c("Zone 30"), col=colors)
barplot(basCom, beside=F, axes=F, axisnames=T, names.arg= c("Base case"), col=colors)
barplot(priCom, beside=F, axes=F, axisnames=T, names.arg= c("Pricing"), col=colors)
barplot(z30Com, beside=F, axes=F, axisnames=T, names.arg= c("Zone 30"), col=colors)
barplot(basCom, beside=F, axes=F, axisnames=T, names.arg= c("Base case"), col=colors)
barplot(priCom, beside=F, axes=F, axisnames=T, names.arg= c("Pricing"), col=colors)
barplot(z30Com, beside=F, axes=F, axisnames=T, names.arg= c("Zone 30"), col=colors)

plot.new()
text(0.08,0.6, "URBAN", cex=2)
text(0.37,0.6, "COMMUTER", cex=2)
text(0.64,0.6, "REV_COMMUTER", cex=2)
text(0.92,0.6, "FREIGHT", cex=2)

