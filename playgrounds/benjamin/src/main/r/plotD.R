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

directory <- commandArgs()[3]
baseFile <- file.path(directory, "welfareTollInformation_baseCase_ctd.txt")
outFile <- file.path(commandArgs()[4], "PlotD.pdf")
basecase <- read.table(file=baseFile, header = T, sep = "\t", comment.char="")

#sort basecase by user group 
basecase$user.group <- ordered(basecase$user.group, levels = groupOrder)

pdf(outFile)

barplot((basecase$user.logsum..EUR./1000000), names.arg = groupOrder, beside=T, main="Base case: user benefits (logsum) by subpopulation", xlab= "base case", ylab="user benefit [million EUR]", col=groupColors)

dev.off()
