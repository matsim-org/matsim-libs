#in: avgTTInformation_baseCase_ctd
#R --slave $dataDir $dataDir < plotA.R
rm(list=ls())
graphics.off()

colors<-c(1:6) #colors for modes - will be repeated if necessary - use colours for a bigger variety
modeOrder<- c("ride", "car", "bike", "undefined", "pt", "walk")
groupOrder<- c("URBAN", "COMMUTER", "REV_COMMUTER", "FREIGHT")
caseOrder<- c("BaseCase", "Internalization", "Zone30")

#read files and set directories
inputDir <- commandArgs()[3]
outputDir <- commandArgs()[4]

BaCFile <- file.path(inputDir,"avgTTInformation_baseCase_ctd_newCode.txt")
PriFile <- file.path(inputDir,"avgTTInformation_policyCase_pricing_newCode.txt")
Z30File <- file.path(inputDir,"avgTTInformation_policyCase_zone30.txt")

BaC <- read.table(file = BaCFile, header=T, sep = "\t", comment.char="")
Z30 <- read.table(file = Z30File, header=T, sep = "\t", comment.char="")
Pri <- read.table(file = PriFile, header=T, sep = "\t", comment.char="")

outputFile <- file.path(outputDir, "ModalSplitByGroups.pdf")
rownames(BaC)<-paste(BaC[,"mode"], BaC[,"user.group"])
rownames(Pri)<-paste(Pri[,"mode"], Pri[,"user.group"])
rownames(Z30)<-paste(Z30[,"mode"], Z30[,"user.group"])

#organise needed data as array
data<- array(dim=c(length(modeOrder), length(caseOrder), length(groupOrder)), dimnames=list(modeOrder, caseOrder, groupOrder)) 

for (case in caseOrder){
	for (group in groupOrder){
		for(mode in modeOrder){
			# entry at array(mode, case, group) =
			# percentage of departures by group, mode and case compared to all departures by the same group and case
			if(case == "BaseCase"){
				summe<- sum(subset(BaC[,"departures"], BaC$user.group==group))
				data[mode, case, group]<- BaC[paste(mode, group), "departures"]/summe*100
			}
			if(case == "Internalization"){
				summe<- sum(subset(Pri[,"departures"], Pri$user.group==group))
				data[mode, case, group]<- Pri[paste(mode, group), "departures"]/summe*100
			}
			if(case == "Zone30"){
				summe<- sum(subset(Z30[,"departures"], Z30$user.group==group))
				data[mode, case, group]<- Z30[paste(mode, group), "departures"]/summe*100			
			}
			#set NA to 0 
			if(is.na(data[mode,case,group])){
				data[mode,case,group]<-0			
			}
		}
	}
}

#graphic parameters
pdf(outputFile, height=12, width=20)
par(mar=c(2,0,2,0), oma=c(2,4,2,2), las=1, cex.axis=2, cex.lab=2)

m<-t(matrix(c(1,1,1,1,1,1,1,1,1,1,1,1,
2,3,4,5,6,7,8,9,10,11,12,13,
2,3,4,5,6,7,8,9,10,11,12,13,
2,3,4,5,6,7,8,9,10,11,12,13,
2,3,4,5,6,7,8,9,10,11,12,13,
14,14,14,14,14,14,14,14,14,14,14,14),12,6))
layout(m)

##plots
#header
plot.new();text(0.5,0.5,"Modal split by groups", cex=4)
#barplots
axisF<-TRUE
for (group in groupOrder){
for (case in caseOrder){
		currData<-cbind(data[,case,group])
		barplot(currData, beside=F, axes=axisF, axisnames=T, names.arg= c(case), col=colors)
		axisF<-FALSE #axis only for first plot
	}
}

#legend
plot.new(); par(cex=2);
text(0.08,0.9, "URBAN"); text(0.37,0.9, "COMMUTER"); text(0.64,0.9, "REV_COMMUTER"); text(0.92,0.9, "FREIGHT")
legend(0.08,0.5, modeOrder, fill=colors, horiz=T, bty="n")

dev.off()
