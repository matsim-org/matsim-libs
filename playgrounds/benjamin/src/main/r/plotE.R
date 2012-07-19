#IN: welfareTollinformation_baseCase_ctd.txt
#IN: welfareTollinformation_policycase_pricing_ctd.txt
#IN: welfareTollinformation_policycase_zone30_ctd.txt

#TODO out schreiben

rm(list = ls())		# Clear all variables  
graphics.off()		# Close graphics windows

#vector of emissions which will be displayed
groupOrder <- c("URBAN", "COMMUTER","REV_COMMUTER","FREIGHT") #TODO erklaeren
groupColors<- c("yellow","mediumblue","red")

#input
directory <- commandArgs()[3]
baseFile <- file.path(directory, "welfareTollInformation_baseCase_ctd.txt")
priFile <- file.path(directory, "welfareTollInformation_policyCase_pricing.txt")
z30File <- file.path(directory, "welfareTollInformation_policyCase_zone30.txt")

outFile <- file.path(commandArgs()[4], "PlotE.pdf")
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
		z30data[i,"sum"]<- z30data[i,"changeinuserlogsum"]+pridata[i,"changeintollpayments"]


}

#TODO berechnungen ueberpruefen/nachvollziehen

#ylimits
yminimum<-floor(min(z30data,pridata)) #rounded down minimum
ymaximum<-ceiling(max(z30data, pridata)) #rounded up maximum
ylimits<-c(yminimum-5000,ymaximum+5000)

pdf(outFile, width=20, height=7)
#grafic parameters
par(mfrow=c(1,3), xpd=T, cex=1, oma=c(2.1,3.1,2.1,0), mar=c(2,2,2,2)) #three figures side by side
#plots and legend
barplot(t(z30data), beside=T, ylim=ylimits, names.arg= groupOrder, main="Policy case Zone 30", col=groupColors, ylab="EUR")
barplot(t(pridata), beside=T, ylim=ylimits, names.arg= groupOrder, main="Policy case Pricing", col=groupColors, axes =F)
plot.new()
legend(0.1,0.7, c("change in user logsum","change in toll payments","sum"), fill=groupColors)
dev.off()
