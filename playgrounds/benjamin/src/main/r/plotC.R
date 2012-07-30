rm(list = ls())		# Clear all variables  
graphics.off()		# Close graphics windows

emissioncolors <- c("black", "mediumblue", "limegreen", "yellow", "red")

#relative paths
directory <- commandArgs()[3]
baseFile <- file.path(directory,"emissionInformation_baseCase_ctd.txt")
z30File <- file.path(directory, "emissionInformation_policyCase_zone30.txt")
priFile <- file.path(directory, "emissionInformation_policyCase_pricing.txt")
outFile <- file.path(commandArgs()[4], "plotC.pdf")

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
#dev.new(width=14, height=7) 
pdf(outFile, width=14, height=7)
layout(matrix(c(1,1,2,2,3),1,5))
#par(mfrow=c(1,3), xpd=T, cex=1, oma=c(2.1,3.1,2.1,0), mar=c(2,0,0,0)) #three figures side by side
par(xpd=T, cex=1, oma=c(0,2,2,0), mar=c(5,2,0,0))
#pdf(outFile, width=10, height=7)
#par(mfrow=c(1,3), xpd=T, cex=1.2) #three figures side by side

#ylimits for the plot depending on matrix entries
#this works fine if there is at least on value greater 1 or less than -1
yminimum<-floor(min(changematrixPri,changematrixZ30)) #rounded down minimum
ymaximum<-ceiling(max(changematrixPri, changematrixZ30)) #rounded up maximum
ylimits<-c(yminimum-1,ymaximum+1)

#plots
barplot(t(changematrixZ30), legend=F, col=emissioncolors, ylim=ylimits, xlab="Zone 30", beside=T, cex.names=0.6)

barplot(t(changematrixPri), legend=F, col=emissioncolors, ylim=ylimits, axes=F, xlab="Pricing", beside=T, cex.names=0.6)
plot.new()
emissionsLegend <- emissions
legend(0.1,0.9, emissionsLegend, fill = emissioncolors, title = "Emissions")
dev.off()
