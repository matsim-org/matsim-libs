#in: emissioninformation base case
#out: barplot: for each group (urban, commuter, inv_commuter, Freight)
# 	one bar with segments for each (chosen) emission

rm(list = ls())		# Clear all variables  
graphics.off()		# Close graphics windows

#vector of emissions which will be displayed
emissions<-c("CO2_TOTAL","NMHC","NOX","PM","SO2")
emissioncolors<- c("black","mediumblue","limegreen","yellow","red")

directory <- commandArgs()[3]
baseFile <- file.path(directory, "emissionInformation_baseCase_ctd.txt")
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
	while (abs(max(as.numeric(basecase.mat[,i])))>1000){
		basecase.mat[,i]<-0.1*as.numeric(basecase.mat[,i])
		actualEmission<-colnames(basecase.mat)[i]
		relativmatrix[1,actualEmission]<-relativmatrix[1,actualEmission]*10
		}

	#if column maximum <100
	#or if column minimum >-100
	while (abs(max(as.numeric(basecase.mat[,i])))<100 && 
		(abs(min(as.numeric(basecase.mat[,i]))))>0){ #string to numeric
		basecase.mat[,i]<-10*as.numeric(basecase.mat[,i])
		actualEmission<-colnames(basecase.mat)[i]
		relativmatrix[1,actualEmission]<-relativmatrix[1,actualEmission]/10
		}
	
}

#delete unwanted emissions
basecase.mat<-basecase.mat[,colnames(basecase.mat) %in% emissions]

#number of colors needs to equal number of emissions

pdf(outFile, width=10)
par(xpd=T, mar=par()$mar+c(0,0,0,10))
barplot(t(basecase.mat), legend=F, col = emissioncolors)
emissionsLegend <- emissions

#write legend with relative factors
for(i in 1: length(emissions)){
	#example: SO2 [g x 1000]
	emissionsLegend[i]<-paste(emissionsLegend[i]," [ g x ",relativmatrix[1,i], "]")
}

legend(5,300, emissionsLegend, fill = emissioncolors, title = "Emission", cex=0.8)
dev.off()
