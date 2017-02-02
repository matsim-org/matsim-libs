#
# 
# Author: aagarwal
###############################################################################

df1 = read.table(commandArgs()[3],header=TRUE,sep="\t")
df2 = read.table(commandArgs()[4],header=TRUE,sep="\t")

df3 = read.table(commandArgs()[5],header=TRUE,sep="\t")

print("summary of files in the same order -- ")
summary(df1)
summary(df2)
summary(df3)

df = df1$weight + df2$weight - df3$weight

print("summary of file1$weight + file2$weight - file3$weight")
summary(df)

print("Sum should be zero but sum of output is = ") 
sum(df)

a=table(df)
print("Number of zero rows are = ") 
a[names(a)==0]
print(" and number of zero rows are = ") 
length(df) - a[names(a)==0]