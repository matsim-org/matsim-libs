library(tidyverse)

sample <- read_csv("/Users/aleksander/Documents/VSP/Cutout/gartenfeld/out/sample.csv") %>%
  group_by(linkId) %>%
  summarize(cutoutVolume = sum(cutoutVolume), totalValue = sum(totalVolume))

sample