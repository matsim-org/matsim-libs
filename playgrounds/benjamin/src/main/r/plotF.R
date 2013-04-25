#IN: emissionInformation_baseCase_ctd.txt
#IN: emissionInformation_policycase_pricing_ctd.txt
#IN: emissionInformation_policycase_zone30_ctd.txt

#OUT: two plots side by side, differences in costs compared to base case

rm(list = ls())		# Clear all variables  
graphics.off()		# Close graphics windows

groupOrder <- c("URBAN", "COMMUTER","REV_COMMUTER","FREIGHT") #order of traffic groups
plotColors<- c("green","red") #1. color used if difference to base case is negative, 2. else
emissions <- c("CO","CO2_TOTAL","FC","HC","NMHC","NO2","NOX","PM","SO2")

#input
inputDir <- commandArgs()[3]
outputDir <- commandArgs()[4]

baseFile <- file.path(inputDir, "emissionInformation_baseCase_ctd_newCode.txt")
priFile <- file.path(inputDir, "emissionInformation_policyCase_pricing_newCode.txt")
z30File <- file.path(inputDir, "emissionInformation_policyCase_zone30.txt")

outFile <- file.path(outputDir, "PlotF.pdf")

basecase <- read.table(file=baseFile, header = T, sep = "\t", comment.char="")
pricase <- read.table(file=priFile, header = T, sep = "\t", comment.char="")
z30case <- read.table(file=z30File, header = T, sep = "\t", comment.char="")

#CO	CO2_TOTAL	FC	HC	NMHC	NO2	NOX	PM	SO2
emissionc<- matrix(nrow=1, ncol=(ncol(basecase)-1))
colnames(emissionc)<- colnames(basecase[,2:ncol(basecase)])
emissionc[1, "CO"]<-0.0 
emissionc[1,"CO2_TOTAL"]<-70/1000/1000
emissionc[1,"FC"]<-0.0
emissionc[1,"HC"]<-0.0
emissionc[1,"NMHC"]<-1700/1000/1000
emissionc[1,"NO2"]<-0.0
emissionc[1,"NOX"]<-9600/1000/1000
emissionc[1,"PM"]<-384500/1000/1000
emissionc[1,"SO2"]<-11000/1000/1000

#rownames
rownames(basecase)<-basecase$user.group
rownames(pricase)<-pricase$user.group
rownames(z30case)<-z30case$user.group

#sort all matrices by user groups
basecase$user.group <- ordered(basecase$user.group, levels = groupOrder)
pricase$user.group <- ordered(pricase$user.group, levels = groupOrder)
z30case$user.group <- ordered(z30case$user.group, levels = groupOrder)

pridata <- matrix(ncol=1, nrow=length(groupOrder))
colnames(pridata)<-c("change")
rownames(pridata)<-groupOrder
z30data <- matrix(ncol=1, nrow=length(groupOrder))
colnames(z30data)<-c("change")
rownames(z30data)<-groupOrder

for(i in groupOrder){
	pritemp<-0
	z30temp<-0
	for(j in emissions){
		#+= costs of actual emission * amount of actual emission
		pritemp<-pritemp+ emissionc[1,j]*(pricase[i, j]-basecase[i,j])
		z30temp<-z30temp+ emissionc[1,j]*(z30case[i, j]-basecase[i,j])
		}
		pridata[i,"change"]<- pritemp
		z30data[i,"change"]<- z30temp 

}

#ylimits
yminimum<-floor(min(z30data,pridata)) #rounded down minimum
ymaximum<-ceiling(max(z30data, pridata)) #rounded up maximum
ylimits<-c(yminimum-5,ymaximum+5)

#colors depending on values
z30colors<-rep(plotColors[1], length(groupOrder))
pricolors<-rep(plotColors[1], length(groupOrder))
for(i in 1 : length(groupOrder)){
	if(pridata[(groupOrder[i]), "change"]>0){
		pricolors[i]<-"red"
	}
	if(z30data[groupOrder[i],"change"]>0){
		z30colors[i]<-"red"
	}
}

#grafic parameters
pdf(outFile, width=15, height=7)
layout(matrix(c(1,1,1,1,2,2,2,2),1,8))
par(xpd=T, cex=1.7, oma=c(0,4,0,0), mar=c(0,0,9,0), las=2)

#plots and legend
barL<-barplot(t(z30data), beside=T, ylim=ylimits, names.arg= groupOrder, col=z30colors, axes=F, space=c(1,0.2)) #first argument of 'space' unused in this plot
par(srt=90)
text(x=barL, y=60, label=groupOrder, pos=4)
par(srt=0, font=2)
text(x=2.5, y=230, label="zone 30")
par(font=1)
axis(2, at=seq(-150,50,by=20), labels=seq(-150,50,by=20), tick=TRUE)
mtext("EUR", outer=F, side=2, at= 70, cex=1.7, adj=1)
barR<-barplot(t(pridata), beside=T, ylim=ylimits, names.arg= groupOrder, col=pricolors, axes =F, space=c(1,0.2))
par(srt=90)
text(x=barR, y=60, label=groupOrder, pos=4)
par(srt=0, font=2)
text(x=2.5, y=230, label="internalization")
par(font=1)
dev.off()
