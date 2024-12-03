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

diff <- diff_out %>%
  select(contains("Diff"), segment) %>%
  pivot_longer(cols = contains("Diff"), names_to = "Species", values_to = "Values")%>%
  filter(`Species` != "CO-Diff" & Species != "CO2-Diff")

factors <- diff_out %>%
  select(contains("Factor"), segment) %>%
  pivot_longer(cols = contains("Factor"), names_to = "Diff. Factor", values_to = "Factor") %>%
  filter(`Diff. Factor` != "CO-Factor")

ggplot(factors, aes(x = segment, y = Factor, color = `Diff. Factor`)) +
  geom_line() +
  geom_point() +
  ggtitle("Rel. Diff MATSim / Sumo")+
  theme_light()

ggplot(diff, aes(x = segment, y = `Values`, color = Species)) +
  geom_line() +
  geom_point() +
  ggtitle("Abs. Diff MATSim - Sumo") +
  theme_light()


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




#------------------- Tinker with hbefa data
hbefa_per_tech_avg <- read_delim(
  "D:/Projects/VSP/MATSim/PHEM/EFA_HOT_Concept_2020_detailed_perTechAverage.csv", delim = ";")

hbefa_filtered <- hbefa_per_tech_avg %>%
  filter(VehCat == "pass. car" & Component == "NOx" & EmConcept == "petrol (4S)")

hbefa_NOX_max <- max(hbefa_filtered$EFA)
hbefa_NOX_min <- min(hbefa_filtered$EFA)

lengths <- tibble(
  segment = c(0,1,2,3),
  length = c(3095, 4756, 7158, 8254)
)

diff_out_NOx <- diff_out %>%
  select(segment, "NOx-SUMO", "NOx-MATSIM") %>%
  pivot_longer(cols = c("NOx-SUMO", "NOx-MATSIM"), names_to="model", values_to="value") %>%
  left_join(lengths, by="segment") %>%
  mutate(value = value/(length/1000))

diff_out_NOx_t <- tibble(
  segment = c(0,1,2,3,0,1,2,3),
  model = c("NOX_min","NOX_min","NOX_min","NOX_min","NOX_max","NOX_max","NOX_max","NOX_max"),
  value = c(hbefa_NOX_min,hbefa_NOX_min,hbefa_NOX_min,hbefa_NOX_min,hbefa_NOX_max,hbefa_NOX_max,hbefa_NOX_max,hbefa_NOX_max),
  )

a <- diff_out_NOx %>%
  bind_rows(diff_out_NOx_t)

ggplot(a, aes(x=segment, y=value, color=model)) +
  geom_line() +
  geom_point()
