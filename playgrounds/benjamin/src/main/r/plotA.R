#in: emissioninformation base case
#out: barplot: for each group (urban, commuter, inv_commuter, Freight)
# 	one bar with segments for each (chosen) emission

rm(list = ls())		# Clear all variables  
graphics.off()		# Close graphics windows

#vector of emissions which will be displayed
emissions<-c("CO2_TOTAL","NMHC","NOX","PM","SO2")
emissioncolors<- c("black","mediumblue","limegreen","yellow","red")

inputDir <- commandArgs()[3]
outputDir <- commandArgs()[4]
baseFile <- file.path(inputDir, "emissionInformation_baseCase_ctd_newCode.txt")
outFile <- file.path(outputDir, "PlotA.pdf")
basecase <- read.table(file=baseFile, header = T, sep = "\t")

basecase.mat <- as.matrix(basecase)[,2:ncol(basecase)]
rownames(basecase.mat) <- basecase$user.group
numberCol <- ncol(basecase.mat)

#scaling: multiply with 10^x or 0.1^x
#new values should be between -200 and +200
#at least one value outside [-20, 20]
#remember scale factors in 'relativmatrix'
relativmatrix<-matrix(1, 1, length(colnames(basecase.mat)))
colnames(relativmatrix)<-colnames(basecase.mat)

for(i in 1:numberCol){
	#if column maximum > 200 
	#or if column minimum < -200
	while (abs(max(as.numeric(basecase.mat[,i])))>200){
		basecase.mat[,i]<-0.1*as.numeric(basecase.mat[,i])
		actualEmission<-colnames(basecase.mat)[i]
		relativmatrix[1,actualEmission]<-relativmatrix[1,actualEmission]*10
		}

	#if column maximum <100
	#or if column minimum >-100
	while (abs(max(as.numeric(basecase.mat[,i])))<20 && 
		(abs(min(as.numeric(basecase.mat[,i]))))>0){ #string to numeric
		basecase.mat[,i]<-10*as.numeric(basecase.mat[,i])
		actualEmission<-colnames(basecase.mat)[i]
		relativmatrix[1,actualEmission]<-relativmatrix[1,actualEmission]/10
		}
	
}

#delete unwanted emissions
basecase.mat<-basecase.mat[,colnames(basecase.mat) %in% emissions]

emissionsLegend <- sub("_TOTAL","", emissions, fixed=T)

#graphic parameters
pdf(outFile, width=15, height=7)
layout(matrix(c(1,2,2,2,2,3),1,6))
par(xpd=T, cex=1.7, oma=c(0,0,1,0), mar=c(4,0,0,0), las=1) #left margin different to other plots (4,4,1,0)

#write legend with relative factors
for(i in 1: length(emissions)){
	#example: SO2 [g x 1000]
	#
	print(emissions[i])
	emissionsLegend[i]<-paste(emissionsLegend[i]," [ g x ",relativmatrix[1,emissions[i]], "]") #this works now
}

plot.new()
par(srt=90)
text(0.1,0, paste(emissionsLegend[1],emissionsLegend[2]), cex=1, adj=0)
text(0.25,0,paste(emissionsLegend[3],emissionsLegend[4]), cex=1, adj=0)
text(0.40,0,paste(emissionsLegend[5]), cex=1, adj=0)
par(srt=0)
barplot(t(basecase.mat), legend=F, col = emissioncolors, cex.axis=1, cex.names=1)
plot.new()
par(las=0)
legend(-0.0,0.8, sub("_TOTAL","", emissions, fixed=T), fill = emissioncolors, cex=1, bty="n", y.intersp=2)

dev.off()
