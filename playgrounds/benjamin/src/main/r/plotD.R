#in: welfareTollinformation_baseCase_ctd.txt
#x: baseCase
#Titel Base case: user benefits (logsum) by subpopulation
#y user benefit [million EUR]
#barplot urban, com, rev.comm, freight

rm(list = ls())		# Clear all variables  
graphics.off()		# Close graphics windows

#vector of emissions which will be displayed
groupOrder <- c("URBAN", "COMMUTER","REV_COMMUTER","FREIGHT") #one plot for each grop
groupColors<- c("mediumblue","mediumblue","mediumblue","mediumblue")

inputDir <- commandArgs()[3]
outputDir <- commandArgs()[4]
baseFile <- file.path(inputDir, "welfareTollInformation_baseCase_ctd_newCode.txt")
outFile <- file.path(outputDir, "PlotD.pdf")
basecase <- read.table(file=baseFile, header = T, sep = "\t", comment.char="")

#sort basecase by user group 
basecase$user.group <- ordered(basecase$user.group, levels = groupOrder)

pdf(outFile, width=15, height=7)
par(xpd=T, cex=1.7, mar=c(4,4,1,0), las=1)

barplot((basecase$user.logsum..EUR./1000000), names.arg = groupOrder, beside=T, xlab= "base case", ylab="user benefit [million EUR]", col=groupColors)
dev.off()
