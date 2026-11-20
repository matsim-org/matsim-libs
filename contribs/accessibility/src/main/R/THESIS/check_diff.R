# install.packages("waldo")
library(waldo)

df1 <- read.csv("/home/brendan/Downloads/spa_base_newBranch/pbAcc_car_base_rawSum.csv")
df2 <- read.csv("/home/brendan/git/matsim-libs/contribs/accessibility/src/main/R/THESIS/input/spa/pbAcc_car_base_rawSum.csv")

names(df1); names(df2)

# Sort both by the same key columns (replace with your actual ID/key columns)
key <- c("personId")   # e.g. c("x", "y") or c("link_id", "time")
df1s <- df1[do.call(order, df1[key]), ]
df2s <- df2[do.call(order, df2[key]), ]

# Reset row names, otherwise waldo reports them as differences
rownames(df1s) <- NULL
rownames(df2s) <- NULL

comparison_result <- compare(df1s, df2s)
print(comparison_result)





