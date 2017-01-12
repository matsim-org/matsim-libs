#
# 
# Author: aagarwal
###############################################################################

file1 = read.table(commandArgs()[3],header=TRUE,sep="\t")
outFile = png(commandArgs()[4],width=800,height=600)

data= file1[ which(file1$ITERATION<101),] #only first 100 iterations

xlim = c(0,100)
ylim = c(min(data$avg..BEST,data$avg..EXECUTED,data$avg..AVG,data$avg..WORST),max(data$avg..BEST,data$avg..EXECUTED,data$avg..AVG,data$avg..WORST))

plot(data$ITERATION,data$avg..BEST, col="blue",type="l",xlim=xlim,ylim=ylim,xlab="Iteration",ylab="Score",cex.lab=1.5,cex.axis=1.5)
lines(data$ITERATION,data$avg..EXECUTED,col="yellow")
lines(data$ITERATION,data$avg..AVG,col="green")
lines(data$ITERATION,data$avg..WORST,col="red")

legend("bottom",c("avg. worst score","avg. best score","avg. of plans' average score","avg. executed score"),col=c("red","blue","green","yellow"),lty=1,ncol=2,cex=1.5)


dev.off()
