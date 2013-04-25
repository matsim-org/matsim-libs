#IN: welfareTollinformation_baseCase_ctd.txt
#IN: welfareTollinformation_policycase_pricing_ctd.txt
#IN: welfareTollinformation_policycase_zone30_ctd.txt

#OUT: Absollute changes in user benefits (logsum), redistributed toll payments and sum by subpopulation

rm(list = ls())		# Clear all variables  
graphics.off()		# Close graphics windows

#vector of emissions which will be displayed
groupOrder <- c("URBAN", "COMMUTER","REV_COMMUTER","FREIGHT") #order of user groups
groupColors<- c("yellow","mediumblue","red")

#input
inputDir <- commandArgs()[3]
outputDir <- commandArgs()[4]
baseFile <- file.path(inputDir, "welfareTollInformation_baseCase_ctd_newCode.txt")
priFile <- file.path(inputDir, "welfareTollInformation_policyCase_pricing_newCode.txt")
z30File <- file.path(inputDir, "welfareTollInformation_policyCase_zone30.txt")

outFile <- file.path(outputDir, "PlotE.pdf")
basecase <- read.table(file=baseFile, header = T, sep = "\t", comment.char="")
pricase <- read.table(file=priFile, header = T, sep = "\t", comment.char="")
z30case <- read.table(file=z30File, header = T, sep = "\t", comment.char="")

#rownames
rownames(basecase)<-basecase$user.group
rownames(pricase)<-pricase$user.group
rownames(z30case)<-z30case$user.group

#sort all matrices by user groups
basecase$user.group <- ordered(basecase$user.group, levels = groupOrder)
pricase$user.group <- ordered(pricase$user.group, levels = groupOrder)
z30case$user.group <- ordered(z30case$user.group, levels = groupOrder)

pridata <- matrix(ncol=3, nrow=length(groupOrder))
colnames(pridata)<-c("changeinuserlogsum","changeintollpayments","sum")
rownames(pridata)<-groupOrder
z30data <- matrix(ncol=3, nrow=length(groupOrder))
colnames(z30data)<-c("changeinuserlogsum","changeintollpayments","sum")
rownames(z30data)<-groupOrder

for(i in groupOrder){

		pridata[i,"changeinuserlogsum"]<- pricase[i, "user.logsum..EUR."]-basecase[i, "user.logsum..EUR."]
		pridata[i,"changeintollpayments"]<-pricase[i, "toll.payments..EUR."]-basecase[i, "toll.payments..EUR."]
		pridata[i,"sum"]<- pridata[i,"changeinuserlogsum"]+pridata[i,"changeintollpayments"]
		z30data[i,"changeinuserlogsum"]<- z30case[i, "user.logsum..EUR."]- basecase[i, "user.logsum..EUR."]
		z30data[i,"changeintollpayments"]<-z30case[i, "toll.payments..EUR."]-basecase[i, "toll.payments..EUR."]
		z30data[i,"sum"]<- z30data[i,"changeinuserlogsum"]+z30data[i,"changeintollpayments"]


}

#ylimits
yminimum<-floor(min(z30data,pridata)) #rounded down minimum
ymaximum<-ceiling(max(z30data, pridata)) #rounded up maximum
ylimits<-c(yminimum-5000,ymaximum+5000)

#grafic parameters
pdf(outFile, width=15, height=11)
layout(matrix(c(1,1,1,1,1,1,3,2,2,2,2,2,2,3),7,2))
par(xpd=T, cex=1.7, oma=c(0,4,0,0), mar=c(0,0,10,0), las=2)

#labels for the plot
glabels<- rep("", times=ncol(pridata)*length(groupOrder))
glabels[2]<-groupOrder[1]
glabels[5]<-groupOrder[2]
glabels[8]<-groupOrder[3]
glabels[11]<-groupOrder[4]

#plots and legend
barL<-barplot(t(z30data), beside=T, ylim=ylimits, col=groupColors, axes=F, names.arg=c("","","",""))
par(srt=90)
text(x=barL, y=27000, label=glabels, pos=4)
par(srt=0, font=2)
text(x=7, y=60000, label="zone 30")
par(font=1)
axis(2, at=seq(-25000,25000,by=10000), labels=seq(-25000,25000,by=10000), tick=TRUE)
mtext("EUR", outer=F, side=2, at= -29000, cex=1.7, adj=1)
barR<-barplot(t(pridata), beside=T, ylim=ylimits, col=groupColors, axes =F, names.arg=c("","","",""))
par(srt=90)
text(x=barR, y=27000, label=glabels, pos=4)
par(srt=0, font=2)
text(x=7, y=60000, label="internalization")
par(font=1)
par(mar=c(0,0,0,0));plot.new(); 
legend(0.0,0.8, c("change in user logsum","change in toll payments","sum"), fill=groupColors, cex=1.2, bty="n", y.intersp=2, horiz =TRUE)
dev.off()

###############

