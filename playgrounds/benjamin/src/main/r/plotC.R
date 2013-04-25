rm(list = ls())		# Clear all variables  
graphics.off()		# Close graphics windows

emissions<-c("CO2_TOTAL","NMHC","NOX","PM","SO2")
emissioncolors <- c("black", "mediumblue", "limegreen", "yellow", "red")

#relative paths
#directory <- file.path(getwd(),"rFiles/plots/analyse")
inputDir <- commandArgs()[3]
outputDir <- commandArgs()[4]
baseFile <- file.path(inputDir,"emissionInformation_baseCase_ctd_newCode.txt")
z30File <- file.path(inputDir, "emissionInformation_policyCase_zone30.txt")
priFile <- file.path(inputDir, "emissionInformation_policyCase_pricing_newCode.txt")
outFile <- file.path(outputDir, "PlotC.pdf")

#read tables
basecase <- read.table(file=baseFile, header = T, sep = "\t", comment.char="")
policycasez30 <- read.table(file=z30File, header = T, sep = "\t", comment.char="")
policycasePri <- read.table(file=priFile, header = T, sep = "\t", comment.char="")

#initiate matrices with same row and column names 
basecase.mat <- as.matrix(basecase)[,2:10]
rownames(basecase.mat) <- basecase$user.group
colnames(basecase.mat) <- names(basecase)[2:10]

policycasez30.mat <- as.matrix(policycasez30)[,2:10]
rownames(policycasez30.mat) <- policycasez30$user.group
colnames(policycasez30.mat) <- names(policycasez30)[2:10]

policycasePri.mat <- as.matrix(policycasePri)[,2:10]
rownames(policycasePri.mat) <- policycasePri$user.group
colnames(policycasePri.mat) <- names(policycasePri)[2:10]

numberCol <- 9
numberRow <- 4

changematrixZ30 <- matrix(ncol=numberCol, nrow=numberRow)
rownames(changematrixZ30) <- basecase$user.group
colnames(changematrixZ30) <- names(basecase)[2:10]

changematrixPri <- matrix(ncol=numberCol, nrow=numberRow)
rownames(changematrixPri) <- basecase$user.group
colnames(changematrixPri) <- names(basecase)[2:10]

#for all entries of the basecase matrix
for(i in 1:numberRow){
	for(j in 1:numberCol){
		
		#relative differences to basecase for zone30 (in percent)
		#change data type to numeric
		basevalue <- as.numeric(basecase.mat[rownames(changematrixZ30)[i],colnames(changematrixZ30)[j]])
		policyvalueZ30 <- as.numeric(policycasez30.mat[rownames(changematrixZ30)[i],colnames(changematrixZ30)[j]])
		differenzZ30 <- as.numeric(policyvalueZ30-basevalue)/basevalue*100
		changematrixZ30[rownames(changematrixZ30)[i],colnames(changematrixZ30)[j]]<- differenzZ30

		#relative differences to basecase for pricing (in percent)
		#change data type to numeric
		basevalue <- as.numeric(basecase.mat[rownames(changematrixPri)[i],colnames(changematrixPri)[j]])
		policyvaluePri <- as.numeric(policycasePri.mat[rownames(changematrixPri)[i],colnames(changematrixPri)[j]])
		differenzPri <- (policyvaluePri-basevalue)/basevalue*100
		changematrixPri[rownames(changematrixPri)[i],colnames(changematrixPri)[j]]<- differenzPri
	}
}

#delete unwanted rows/columns in matrices
emissions<-c("CO2_TOTAL","NMHC","NOX","PM","SO2")
changematrixZ30 <- changematrixZ30[, colnames(changematrixZ30) %in% emissions]
changematrixPri <- changematrixPri[, colnames(changematrixPri) %in% emissions] 

#graphic parameters
pdf(outFile, width=15, height=10) #height was 7 as in plotB, plotF
layout(matrix(c(1,1,1,1,2,2,2,2,3),1,9))
par(xpd=T, cex=1.7, oma=c(0,4,0,0), mar=c(0,0,9,0), las=2)

#ylimits for the plot depending on matrix entries
#this works fine if there is at least on value greater 1 or less than -1
yminimum<-floor(min(changematrixPri,changematrixZ30)) #rounded down minimum
ymaximum<-ceiling(max(changematrixPri, changematrixZ30)) #rounded up maximum
ylimits<-c(yminimum-1,ymaximum+1)

#labels for the plot
glabels<- rep("", times=length(emissions)*nrow(basecase.mat))
glabels[2]<-rownames(changematrixZ30)[1]
glabels[7]<-rownames(changematrixZ30)[2]
glabels[12]<-rownames(changematrixZ30)[3]
glabels[17]<-rownames(changematrixZ30)[4]

#plots
barL<-barplot(t(changematrixZ30), legend=F, col=emissioncolors, ylim=ylimits, axes=F,beside=T, cex.names=1.2)
par(srt=90)
text(x=barL, y=1.5, label=glabels, pos=4)
par(srt=0, font=2)
text(x=13, y=6.5, label="Zone 30")
par(font=1)
axis(2, at=c(-7:2), labels=c("-7.00%", "-6.00%", "-5.00%", "-4.00%","-3.00%", "-2.00%","-1.00%", "0.00%","1.00%","2.00%"), tick=TRUE)

barR<-barplot(t(changematrixPri), legend=F, col=emissioncolors, ylim=ylimits, axes=F, beside=T)
par(srt=90)
text(x=barR, y=1.5, label=glabels, pos=4)
par(srt=0, font=2)
text(x=15, y=6.5, label="Internalization")
par(font=1)
plot.new()
emissions<-sub("_TOTAL","", emissions, fixed=T)
legend(-0.1,0.9, emissions, fill = emissioncolors, cex=1, bty="n", y.intersp=2)
dev.off()
