library(tidyverse)

sumo_output <- read_csv2("contribs/emissions/test/input/org/matsim/contrib/emissions/PHEMTest/sumo_output.csv",
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

####

diff_out <- read_csv("contribs/emissions/test/output/org/matsim/contrib/emissions/PHEMTest/test/diff_out.csv")

ggplot(diff_out) +
  geom_line(aes(x = segment, y = `CO-Factor`, color = "CO")) +
  geom_line(aes(x = segment, y = `CO2-Factor`, color = "CO2")) +
  geom_line(aes(x = segment, y = `HC-Factor`, color = "HC")) +
  geom_line(aes(x = segment, y = `PMx-Factor`, color = "PMx")) +
  geom_line(aes(x = segment, y = `NOx-Factor`, color = "NOx")) +
  scale_color_manual(values = c("CO" = "red",
                                "CO2" = "blue",
                                "HC" = "green",
                                "PMx" = "yellow",
                                "NOx" = "violet")) +
  labs(y="Factor (MATSIM/SUMO)", color = "Emission")