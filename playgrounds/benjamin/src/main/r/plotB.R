rm(list = ls())		# Clear all variables  
graphics.off()		# Close graphics windows

#vector of emissions which will be displayed
emissions <- c("CO2_TOTAL","NMHC","NOX","PM","SO2")
emissioncolors<- c("black", "mediumblue", "limegreen", "yellow", "red")

#relative paths
inputDir <- commandArgs()[3]
outputDir <- commandArgs()[4]
baseFile <- file.path(inputDir,"emissionInformation_baseCase_ctd_newCode.txt")
z30File <- file.path(inputDir, "emissionInformation_policyCase_zone30.txt")
priFile <- file.path(inputDir, "emissionInformation_policyCase_pricing_newCode.txt")
outFile <- file.path(outputDir, "PlotB.pdf")

#read files
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

sumbasecase<- matrix(nrow=1, ncol=length(emissions))
colnames(sumbasecase) <- emissions
sumZ30 <- matrix(nrow=1, ncol=length(emissions))
colnames(sumZ30) <- emissions
sumPri <- matrix(nrow=1, ncol=length(emissions))
colnames(sumPri) <- emissions

#for all entries of the basecase matrix
for(i in emissions){
	sumbasecase[1,i] <- sum(as.numeric(basecase.mat[,i]))
	sumZ30[1,i] <- sum(as.numeric(policycasez30.mat[,i]))
	sumPri[1,i] <- sum(as.numeric(policycasePri.mat[,i]))
}

#calculate relative changes (in percent)
relZ30<- matrix(nrow=1, ncol=length(emissions))
colnames(relZ30) <- emissions
relPri <- matrix(nrow=1, ncol=length(emissions))
colnames(relPri) <- emissions

for(i in emissions){
	relZ30[1,i] <- (sumZ30[1,i]-sumbasecase[1,i])/sumbasecase[1,i]*100
	relPri[1,i] <- (sumPri[1,i]-sumbasecase[1,i])/sumbasecase[1,i]*100
}

#graphic parameters
#dev.new(width=14, height=7)
pdf(outFile, width=15, height=7)
layout(matrix(c(1,1,1,1,2,2,2,2,3),1,9))
par(xpd=T, cex=1.7, oma=c(0,4,0,0), mar=c(2,0,1,0), las=2)

#ylimits for the plot depending on matrix entries
#this works fine if there is at least on value greater 1 or less than -1
yminimum<-floor(min(relPri,relZ30)) #rounded down minimum
ymaximum<-ceiling(max(relPri, relZ30)) #rounded up maximum
ylimits<-c(yminimum,ymaximum)

#plots
barplot(t(relZ30), legend=F, col=emissioncolors, ylim=ylimits, axes=F, main="Zone 30",beside=T, cex.names=1.2)
axis(2, at=c(-3,-2.5,-2,-1.5,-1,-0.5,0,0.5), labels=c("-3.00%", "-2.50%", "-2.00%", "-1.50%","-1.00%", "-0.50%","0.00%", "0.50%"), tick=TRUE)
barplot(t(relPri), legend=F, col=emissioncolors, ylim=ylimits, axes=F, main="Internalization", beside=T, cex.names=1.2) #use main="title" to add a title here 
plot.new()
emissions<-sub("_TOTAL","", emissions, fixed=T)
legend(-0.1,0.8, emissions, fill = emissioncolors, cex=1, bty="n", y.intersp=2)

dev.off()
