#
# 
# Author: aagarwal
###############################################################################

runCase = commandArgs()[3]


df1=read.delim(paste(runCase,"_queue_false.txt",sep=""),header=T,sep="\t")
df2=read.delim(paste(runCase,"_queue_true.txt",sep=""),header=T,sep="\t")
df3=read.delim(paste(runCase,"_withHoles_false.txt",sep=""),header=T,sep="\t")
df4=read.delim(paste(runCase,"_withHoles_true.txt",sep=""),header=T,sep="\t")

#remove initial 4 data points to get only qsim computational time.
df1=df1[-c(1,2,3,4),]
df2=df2[-c(1,2,3,4),]
df3=df3[-c(1,2,3,4),]
df4=df4[-c(1,2,3,4),]

png(paste(runCase,"_simulationTimePlots.png",sep=""),width=600,height=600)

means = c(mean(df1$SimulationTime),mean(df2$SimulationTime),mean(df3$SimulationTime),mean(df4$SimulationTime))
sds = c(sd(df1$SimulationTime),sd(df2$SimulationTime),sd(df3$SimulationTime),sd(df4$SimulationTime))

boxplot(df1$SimulationTime,df2$SimulationTime,df3$SimulationTime,df4$SimulationTime, names = c("q_slow","q_fast","holes_slow","holes_fast"), xlab=paste(runCase," simulation runs",sep=""), ylab="Simulation time [sec]",cex.lab=1.5,cex.axis=1.5)

points(means,col="red",pch=19)

legend("top","mean",col="red",pch=19,cex=1.5)

dev.off()
