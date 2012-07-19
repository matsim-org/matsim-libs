#IN: emissionInformation_baseCase_ctd.txt
#IN: emissionInformation_policycase_pricing_ctd.txt
#IN: emissionInformation_policycase_zone30_ctd.txt

#TODO out schreiben

rm(list = ls())		# Clear all variables  
graphics.off()		# Close graphics windows

#TODO comment
#TODO farben sinnvoll waehlen, rausfinden ob die abhaengig vom wert sein koennen: +rot, - gruen
groupOrder <- c("URBAN", "COMMUTER","REV_COMMUTER","FREIGHT") #TODO erklaeren
groupColors<- c("yellow","mediumblue","red")
emissions <- c("CO","CO2_TOTAL","FC","HC","NMHC","NO2","NOX","PM","SO2")

#input
directory <- commandArgs()[3]

baseFile <- file.path(directory, "emissionInformation_baseCase_ctd.txt")
priFile <- file.path(directory, "emissionInformation_policyCase_pricing.txt")
z30File <- file.path(directory, "emissionInformation_policyCase_zone30.txt")

outFile <- file.path(commandArgs()[4], "PlotF.pdf")

basecase <- read.table(file=baseFile, header = T, sep = "\t", comment.char="")
pricase <- read.table(file=priFile, header = T, sep = "\t", comment.char="")
z30case <- read.table(file=z30File, header = T, sep = "\t", comment.char="")

#TODO werte raussuchen
#CO	CO2_TOTAL	FC	HC	NMHC	NO2	NOX	PM	SO2
emissionc<- matrix(nrow=1, ncol=(ncol(basecase)-1))
colnames(emissionc)<- colnames(basecase[,2:ncol(basecase)])
emissionc[1, "CO"]<-10
emissionc[1,"CO2_TOTAL"]<-10
emissionc[1,"FC"]<-10
emissionc[1,"HC"]<-10
emissionc[1,"NMHC"]<-10
emissionc[1,"NO2"]<-10
emissionc[1,"NOX"]<-10
emissionc[1,"PM"]<-10
emissionc[1,"SO2"]<-10

print(emissionc)

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

print(basecase)
for(i in groupOrder){
	pritemp<-0
	z30temp<-0
	for(j in emissions){
		#+= kosten der aktuellen emission * menge der aktuellen emission
		pritemp<-pritemp+ emissionc[1,j]*(pricase[i, j]-basecase[i,j])
		z30temp<-z30temp+ emissionc[1,j]*(z30case[i, j]-basecase[i,j])
		}
		pridata[i,"change"]<- pritemp #produkt aus emissionen und kosten pricase[i, "user.logsum..EUR."]-basecase[i, "user.logsum..EUR."]
		z30data[i,"change"]<- z30temp #produkt aus emissionen und kosten z30case[i, "user.logsum..EUR."]- basecase[i, "user.logsum..EUR."]

}

print(pridata)
print(z30data)
#TODO berechnungen ueberpruefen/nachvollziehen

#ylimits
yminimum<-floor(min(z30data,pridata)) #rounded down minimum
ymaximum<-ceiling(max(z30data, pridata)) #rounded up maximum
ylimits<-c(yminimum-5,ymaximum+5)

pdf(outFile, width=20, height=7)
#grafic parameters
par(mfrow=c(1,2), xpd=T, cex=1, oma=c(2.1,3.1,2.1,0), mar=c(2,2,2,2)) #three figures side by side
#plots and legend
barplot(t(z30data), beside=T, ylim=ylimits, names.arg= groupOrder, main="Policy case Zone 30", col=groupColors, ylab="EUR")
barplot(t(pridata), beside=T, ylim=ylimits, names.arg= groupOrder, main="Policy case Pricing", col=groupColors, axes =F)

dev.off()
