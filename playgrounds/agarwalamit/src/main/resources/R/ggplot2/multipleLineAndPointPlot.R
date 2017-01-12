#
# 
# Author: aagarwal
###############################################################################

df <- read.table( commandArgs() [3],header=TRUE,sep=",")
png( commandArgs() [4],width=1000,height=800)

library(reshape2)
data = melt(df)

labels = c("1","5","10","15","20","25")

data$UserGroup_factor =  factor(data$UserGroup,levels=c('Urban','Rev_commuter','Freight','Total'),labels=c('Urban','(Rev.) commuter','Freight','Total'))

library(ggplot2)
g=ggplot(data,aes(variable,value,group=UserGroup_factor)) + theme_bw() # start a plot

p=g+geom_point(aes(shape=UserGroup_factor),size=6)+geom_line(size=1.2)  

q = p+labs(y=expression("% change in CO"[2]),x="\n Emission cost multiplication factor") + 
scale_x_discrete(breaks=c("X1", "X5", "X10","X15","X20","X25"),labels=labels)+
theme(text=element_text(size=24),strip.text.x = element_text(size = 24),axis.text=element_text(size=20),axis.title=element_text(size=24),axis.title.y=element_text(vjust=0.2))

r = q + guides(shape=guide_legend(title="User group",keyheight = 2))




print(r) 

dev.off()
