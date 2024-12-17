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

diff_out <- read_csv("contribs/emissions/test/output/org/matsim/contrib/emissions/PHEMTest/test/diff_out.csv")

hbefa_avg <- read_delim("D:/Projects/VSP/MATSim/PHEM/EFA_HOT_Vehcat_2020_Average.csv")
hbefa_det <- read_delim("D:/Projects/VSP/MATSim/PHEM/EFA_HOT_Concept_2020_detailed_perTechAverage.csv", delim = ";")

hbefa_filtered_det <- hbefa_det %>%
  filter(VehCat == "pass. car" & Component == "NOx")

hbefa_filtered_avg <- hbefa_avg %>%
  filter(VehCat == "pass. car" & Component == "NOx")

hbefa_NOX_max_det <- max(hbefa_filtered_det$EFA)
hbefa_NOX_min_det <- min(hbefa_filtered_det$EFA)

hbefa_NOX_max_avg <- max(hbefa_filtered_avg$EFA_weighted, na.rm = T)
hbefa_NOX_min_avg <- min(hbefa_filtered_avg$EFA_weighted, na.rm = T)

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
  model = c("NOX_min_det","NOX_min_det","NOX_min_det","NOX_min_det","NOX_max_det","NOX_max_det","NOX_max_det","NOX_max_det",
            "NOX_min_avg","NOX_min_avg","NOX_min_avg","NOX_min_avg","NOX_max_avg","NOX_max_avg","NOX_max_avg","NOX_max_avg"),
  value = c(hbefa_NOX_min_det, hbefa_NOX_min_det, hbefa_NOX_min_det, hbefa_NOX_min_det, hbefa_NOX_max_det, hbefa_NOX_max_det, hbefa_NOX_max_det, hbefa_NOX_max_det),
  )

a <- diff_out_NOx %>%
  bind_rows(diff_out_NOx_t)

ggplot(a, aes(x=segment, y=value, color=model)) +
  geom_line() +
  geom_point()

min_max_vals <- tibble(
  table = c("avg", "det"),
  min = c(hbefa_NOX_min_avg, hbefa_NOX_min_det),
  max = c(hbefa_NOX_max_avg, hbefa_NOX_max_det),
)

ggplot(diff_out_NOx) +
  geom_line(aes(x=segment, y=value, color=model)) +
  geom_point(aes(x=segment, y=value, color=model)) +
  geom_rect(data=min_max_vals, aes(xmin=0, xmax=3, ymin=min, ymax=max, fill=table), alpha=0.2)

