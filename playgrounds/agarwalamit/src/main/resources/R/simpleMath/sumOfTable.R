#
# 
# Author: aagarwal
###############################################################################

data1 <- read.table(commandArgs()[3], header=T)
data2 <- read.table(commandArgs()[4],header=T)

data1$rowsum=rowSums(data1,na.rm=TRUE)# na.rm will exclude all NaN if exits
totalSum1=sum(data1$rowsum,na.rm=TRUE)

print("Sum of first data sheet is ")
print(totalSum1)

data2$rowsum=rowSums(data2,na.rm=TRUE)
totalSum2=sum(data2$rowsum,na.rm=TRUE)

print("Sum of next data sheet is ")
print(totalSum2)

print("% difference between them is ")
change=(totalSum1-totalSum2)*100/totalSum1
print(change)

