library(tidyverse)

sumo_output <- read_csv2("/Users/janek/projects/matsim-libs/contribs/emissions/test/input/org/matsim/contrib/emissions/PHEMTest/sumo_output.csv",
                        col_names = c("time", "velocity", "acceleration", "slope", "CO", "CO2", "HC", "PMx", "NOx", "fuel", "electricity"))
typeof(sumo_output$NOx)

bla <- sumo_output %>%
  mutate(NOx = as.numeric(NOx), velocity = as.numeric(velocity)) %>%
  mutate(avg_nox = (NOx / 1000) / (velocity / 3600)) %>%
  filter(!is.infinite(avg_nox))

mean(bla$avg_nox)

ggplot(bla, aes(x = as.numeric(time), y = avg_nox)) +
  geom_line() +
  theme_light()