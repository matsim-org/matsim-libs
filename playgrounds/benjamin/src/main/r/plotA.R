#in: emissioninformation base case
#out: barplot: for each group (urban, commuter, inv_commuter, Freight)
# 	one bar with segments for each (chosen) emission

rm(list = ls())		# Clear all variables  
graphics.off()		# Close graphics windows

#vector of emissions which will be displayed
emissions<-c("CO2_TOTAL","NMHC","NOX","PM","SO2")
emissioncolors<- c("black","mediumblue","limegreen","yellow","red")

directory <- commandArgs()[3]
baseFile <- file.path(directory, "emissionInformation_baseCase_ctd_newCode.txt")
outFile <- file.path(commandArgs()[4], "PlotA.pdf")
basecase <- read.table(file=baseFile, header = T, sep = "\t", comment.char="")

basecase.mat <- as.matrix(basecase)[,2:10]
rownames(basecase.mat) <- basecase$user.group
colnames(basecase.mat) <- names(basecase)[2:10]

numberCol <- 9
numberRow <- 4

#scaling: multiply with 10^x or 0.1^x
#new values should be betwenn -1000 and +1000
#at least one value outside [-100, 100]
#remember scale factors in 'relativmatrix'
relativmatrix<-matrix(1, 1, length(colnames(basecase.mat)))
colnames(relativmatrix)<-colnames(basecase.mat)

for(i in 1:numberCol){
	#if column maximum > 1000 
	#or if column minimum < -1000
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

#number of colors needs to equal number of emissions

#pdf(outFile, width=10)
#par(xpd=T, mar=par()$mar+c(0,0,0,10))
#barplot(t(basecase.mat), legend=F, col = emissioncolors)
emissionsLegend <- sub("_TOTAL","", emissions, fixed=T)

########
pdf(outFile, width=15, height=7)
layout(matrix(c(1,2,2,2,2,3),1,6))
par(xpd=T, cex=1.7, oma=c(0,0,1,0), mar=c(4,0,0,0), las=1) #left margin different to other plots (4,4,1,0)
emP<-""
#write legend with relative factors
for(i in 1: length(emissions)){
	emP<-paste(emP, emissionsLegend[i],"[ g x ",relativmatrix[1,i], "]")
	if(i%%3==0){emp<-paste(emP, "\n")}
	#example: SO2 [g x 1000]
	emissionsLegend[i]<-paste(emissionsLegend[i]," [ g x ",relativmatrix[1,i], "]")
	
}
print(emP)

#par(cex=0.9)
#ylab=emP
plot.new()
par(cex=1.7, srt=90)
text(0.1,0, paste(emissionsLegend[1],emissionsLegend[2]), outer=T, cex=1, adj=0,padj=0)
text(0.25,0,paste(emissionsLegend[3],emissionsLegend[4]), outer=T, cex=1, adj=0,padj=0)
text(0.40,0,paste(emissionsLegend[5]), outer=T, cex=1, adj=0,padj=0)
par(las=1, srt=0)
barplot(t(basecase.mat), legend=F, col = emissioncolors, cex.axis=1, cex.names=1, xlab= "base case")
par(cex=1.7)
#mtext(emP, outer=F, side=2, cex=1.7, adj=0,padj=0)
plot.new()
par(las=0)
#emissions<-sub("_TOTAL","", emissions, fixed=T)
legend(-0.0,0.8, sub("_TOTAL","", emissions, fixed=T), fill = emissioncolors, cex=1, bty="n", y.intersp=2)

dev.off()
