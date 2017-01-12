#
# 
# Author: aagarwal
###############################################################################

library(ggplot2)

library(data.table)

df = read.table(commandArgs()[3],sep="\t",header=T)

pdf (commandArgs()[4],width=8, height=6, paper="special")

g = ggplot(df,aes(y=simCount,x=realCount)) +geom_point(aes(color=mode))+scale_y_log10(limits = c(10, 100000)) + scale_x_log10(limits = c(10, 100000)) 

p=seq(10,100000,10)

df_p = data.frame(x=p,y=p)

df_q = data.frame(x=2*p,y=p)

df_r = data.frame(x=0.5*p,y=p)

h= g + geom_line(data=df_p,aes(x,y)) 
i = h + geom_line(data=df_q,aes(x,y)) 
 i + geom_line(data=df_r,aes(x,y))



dev.off()
