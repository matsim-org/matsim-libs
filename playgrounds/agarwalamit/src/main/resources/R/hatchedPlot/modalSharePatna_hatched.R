# TODO: Add comment
# 
# Author: aagarwal
###############################################################################

df <- read.table( commandArgs() [3],header=TRUE,sep="\t")
#png( commandArgs() [4],width=1000,height=800)
pdf( commandArgs() [4],width=10,height=6,paper="special")

density = c(10,30,50)
angles = c(40,80,135)

cases=colnames(df[,-1])
modes=df[,1]

colnames(df) = NULL
data = df[-1]

values = as.numeric(unlist(data))

mat = matrix(values, nrow=3, ncol=3, byrow=T, dimnames=list(cases,modes))

tab = as.table((mat))

barplot(tab, beside=T, density=density,angle= angles,col="black",cex.names=1.5,cex.axis=1.5)

legend("top", cases, density=density, angle= angles, cex = 1.5, ncol=1, bty="n")

dev.off()
