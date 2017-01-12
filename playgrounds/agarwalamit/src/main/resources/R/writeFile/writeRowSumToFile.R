#
# 
# Author: aagarwal
###############################################################################

df1 = read.table(commandArgs()[3],header=TRUE,sep="\t")
df2 = read.table(commandArgs()[4],header=TRUE,sep="\t")
df3 = read.table(commandArgs()[5],header=TRUE,sep="\t")

print("Subtracting pariwise x and y columns of data frames to check \n if the data is in the same order or not.")

sum(df1$x-df2$x)==0
sum(df2$x-df3$x)==0
sum(df1$y-df2$y)==0
sum(df2$y-df3$y)==0

print("If all True, everything is fine, go ahead.")

print("Adding weights of the files.")
df = df1 
df$weight = df1$weight + df2$weight + df3$weight

print("writing data to the file.")

write.table(df,file=commandArgs()[6],sep="\t")