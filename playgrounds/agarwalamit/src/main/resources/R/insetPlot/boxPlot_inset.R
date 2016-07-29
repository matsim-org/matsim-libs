# TODO: Add comment
# 
# Author: aagarwal
###############################################################################

# the data sheet is in PCU this time.

df = read.table(commandArgs()[3],header=FALSE,sep="\t")
png(commandArgs()[4],height=600,width=1000)

q = t(data.frame(df[,2:11]))
qmax = max(q)

print(qmax)

q = q/qmax

k=df[,1]
k=round(k/135,2)
par.default <- par(no.readonly=TRUE)

# create subplot (inset plot) first
par(mar=c(5,6,1,1))
bp <- boxplot(q, outline=FALSE, xaxt="n", ann=FALSE, border="black",col="red",cex.axis=2.5,xlab="Density", ylab="Flow",cex.axis=2.5,cex.lab=2.5)
axis(1, at=bp$names, labels=k, cex.axis = 2.5)
rect(21,-2/qmax,26,qmax/qmax+0.02,col="#FF003322",border="red",lty=3,lwd=2)


#set margins 
par(new=TRUE) # overlay existing plot
par(mar=c(0,0,0,0)) # strip out the margins for the inset plot
par(fig=c(0.45,0.79,0.55,0.85)) # fig shrinks and places relative to figure region


#create plot now.
bp <- boxplot(q, outline=FALSE, xaxt="n", ann=FALSE, border="black",col="red", xlab="Density", ylab="Flow",cex.axis=2.5, cex.lab=2.5, xlim = c(20,25), ylim = c(1600/qmax,1850/qmax))
axis(1, at=bp$names, labels=k, cex.axis = 2.5 )
#
box()

par(par.default)


dev.off()
