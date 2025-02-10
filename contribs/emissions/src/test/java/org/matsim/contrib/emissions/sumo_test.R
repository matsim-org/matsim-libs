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

hbefa_hot_avg <- read_delim("D:/Projects/VSP/MATSim/PHEM/hbefa/EFA_HOT_Vehcat_2020_Average.csv")
hbefa_hot_det <- read_delim("D:/Projects/VSP/MATSim/PHEM/hbefa/EFA_HOT_Concept_2020_detailed_perTechAverage.csv", delim = ";")

hbefa_filtered_det <- hbefa_hot_det %>%
  filter(VehCat == "pass. car" & Component == "NOx" & Technology == "petrol (4S)")

hbefa_filtered_avg <- hbefa_hot_avg %>%
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

min_max_vals <- tibble(
  table = c("avg", "det"),
  min = c(hbefa_NOX_min_avg, hbefa_NOX_min_det),
  max = c(hbefa_NOX_max_avg, hbefa_NOX_max_det),
)

ggplot(diff_out_NOx) +
  geom_line(aes(x=segment, y=value, color=model), size=1.5) +
  geom_point(aes(x=segment, y=value, color=model), size=2.5) +
  geom_rect(data=min_max_vals, aes(xmin=0, xmax=3, ymin=min, ymax=max, fill=table), alpha=0.2) +
  scale_color_manual(values=c("#d21717", "#17d2a4")) +
  scale_fill_manual(values=c("#00f6ff", "#ff004c"))

#------------------- Create plot for all components

#Load data
diff_out <- read_csv("contribs/emissions/test/output/org/matsim/contrib/emissions/PHEMTest/test/diff_out.csv")

hbefa_avg <- read_delim("D:/Projects/VSP/MATSim/PHEM/hbefa/EFA_HOT_Concept_2020_detailed_perTechAverage.csv")
hbefa_det <- read_delim("D:/Projects/VSP/MATSim/PHEM/hbefa/EFA_HOT_Subsegm_detailed_Car_Aleks_filtered.csv", delim = ";")

#Create helper vars
lengths <- tibble(
  segment = c(0,1,2,3),
  length = c(3095, 4756, 7158, 8254)
)

components_avg <- unique(hbefa_avg$Component)
components_det <- unique(hbefa_det$Component)
components <- intersect(components_avg, components_det)

# TODO: Check, that components = components_avg = components_det

diff_out_cleaned <- diff_out %>%
  select(segment, "CO-SUMO", "CO-MATSIM", "CO2(total)-SUMO", "CO2(total)-MATSIM", "HC-SUMO", "HC-MATSIM", "PM-SUMO", "PM-MATSIM", "NOx-SUMO", "NOx-MATSIM") %>%
  pivot_longer(cols = c("CO-SUMO", "CO-MATSIM",
                        "CO2(total)-SUMO", "CO2(total)-MATSIM",
                        "HC-SUMO", "HC-MATSIM",
                        "PM-SUMO", "PM-MATSIM",
                        "NOx-SUMO", "NOx-MATSIM"), names_to="model", values_to="value") %>%
  separate(model, c("component", "model"), "-") %>%
  left_join(lengths, by="segment") %>%
  mutate(gPkm=value/(length/1000))

hbefa_filtered_avg <- hbefa_avg %>%
  filter(VehCat == "pass. car" & Technology == "petrol (4S)")
hbefa_filtered_det <- hbefa_det %>%
  filter(VehCat == "pass. car" & Technology == "petrol (4S)" & EmConcept == "PC P Euro-4")

hbefa_avg_max <- lapply(components, function(component) {
  hbefa_filtered_avg %>%
    filter(Component == component) %>%
    .$EFA_weighted %>%
    max(na.rm = TRUE)
})
names(hbefa_avg_max) <- components

hbefa_det_max <- lapply(components, function(component) {
  hbefa_filtered_det %>%
    filter(Component == component) %>%
    .$EFA %>%
    max(na.rm = TRUE)
})
names(hbefa_det_max) <- components

hbefa_avg_min <- lapply(components, function(component) {
  hbefa_filtered_avg %>%
    filter(Component == component) %>%
    .$EFA_weighted %>%
    min(na.rm = TRUE)
})
names(hbefa_avg_min) <- components

hbefa_det_min <- lapply(components, function(component) {
  hbefa_filtered_det %>%
    filter(Component == component) %>%
    .$EFA %>%
    min(na.rm = TRUE)
})
names(hbefa_det_min) <- components

min_max_vals <- tibble(
  component = unlist(lapply(components, function(c) {c(c, c)})),
  table = unlist(lapply(components, function(c) {c("avg", "EURO-4")})),
  min = unlist(lapply(components, function(c) {
    c(hbefa_avg_min[[c]], hbefa_det_min[[c]])
  })),
  max = unlist(lapply(components, function(c) {
   c(hbefa_avg_max[[c]], hbefa_det_max[[c]])
  }))
)

min_max_vals_used <- min_max_vals %>%
  filter(component %in% diff_out_cleaned$component)

ggplot(diff_out_cleaned) +
  geom_line(aes(x=segment, y=gPkm, color=model), size=1.5) +
  geom_point(aes(x=segment, y=gPkm, color=model), size=2.5) +
  scale_color_manual(values=c("#d21717", "#17d2a4")) +
  scale_fill_manual(values=c("#00f6ff", "#ff004c")) +
  facet_wrap(~component, scales="free") +
  geom_rect(data=min_max_vals_used, aes(xmin=0, xmax=3, ymin=min, ymax=max, fill=table), alpha=0.2)

min_max_vals_used.EURO_4 <- min_max_vals_used %>%
  filter(table=="EURO-4")

ggplot(diff_out_cleaned) +
  geom_rect(data=min_max_vals_used.EURO_4, aes(xmin=-0.5, xmax=3.5, ymin=min, ymax=max, fill=table), alpha=0.2) +
  geom_bar(aes(x=segment, y=gPkm, fill=model), stat="identity", position="dodge") +
  scale_fill_manual(values=c("#ff004c", "#d21717", "#17d2a4")) +
  facet_wrap(~component, scales="free")

# Plot Euro0-Euro6 chart

hbefa_filtered_det_EU <- lapply(c(0,1,2,3,4,5,6), function(eu) {
  hbefa_det %>%
    filter(VehCat == "pass. car" & Technology == "petrol (4S)" & EmConcept == paste0("PC P Euro-", eu))
})
names(hbefa_filtered_det_EU) <- c(0,1,2,3,4,5,6)

min_max_vals_EU <- tibble(
  component = unlist(lapply(components, function(c) {c(c,c,c,c,c,c,c)})),
  concept = unlist(lapply(components, function(c) {c(0,1,2,3,4,5,6)})),
  min = unlist(lapply(components, function(c) {
    lapply(c(0,1,2,3,4,5,6), function(concept) {
      hbefa_filtered_det_EU[[as.character(concept)]] %>%
        filter(Component == c) %>%
        summarise(min = min(EFA, na.rm = TRUE)) %>%
        pull(min)
    })
  })),
  max = unlist(lapply(components, function(c) {
    lapply(c(0,1,2,3,4,5,6), function(concept) {
      hbefa_filtered_det_EU[[as.character(concept)]] %>%
        filter(Component == c) %>%
        summarise(max = max(EFA, na.rm = TRUE)) %>%
        pull(max)
    })
  }))
)

#------------------- Filter out pass.veh
path_in <- "D:/Projects/VSP/MATSim/PHEM/hbefa/EFA_HOT_Subsegm_detailed_Car_Aleks.csv"
path_out <- "D:/Projects/VSP/MATSim/PHEM/hbefa/EFA_HOT_Subsegm_detailed_Car_Aleks_filtered.csv"

table <- read_delim(path_in, delim=";")
table <- table %>%
  filter(VehCat == "pass. car") %>%
  filter(Technology == "petrol (4S)" | Technology == "diesel")

write_delim(table, path_out, delim=";")

#----- SUMO Plot

sumo_out <- read_delim("D:/Projects/VSP/MATSim/matsim-libs/contribs/emissions/test/input/org/matsim/contrib/emissions/PHEMTest/sumo_output.csv", delim = ';')
names(sumo_out) <- c("second", "velocity", "acceleration", "slope", "CO", "CO2", "HC", "PMx", "NOx", "fuel", "electricity")

#Create helper vars
lengths <- tibble(
  segment = c(0,1,2,3),
  time = c(589, 433, 455, 323),
  length = c(3095, 4756, 7158, 8254)
)

sumo_out <- sumo_out %>%
  mutate(CO2_m = ifelse(velocity <= 1, 0, CO2/velocity))

ggplot(sumo_out, aes(x=second)) +
  geom_line(aes(y=as.numeric(CO2)/100), color="red") +
  # geom_line(aes(y=CO2_m/100), color="blue") +
  geom_line(aes(y=velocity), color="black")
  # geom_line(aes(y=acceleration*10), color="orange")

# Absolute emissionen pro segment und dann mit java code vergleichen
segments <- c(sum(sumo_out$CO[1:589]), sum(sumo_out$CO[590:1022]), sum(sumo_out$CO[1023:1477]), sum(sumo_out$CO[1478:1800]))
segments <- segments/1000

# ---- HBEFA NOx > 0,08g discussion

hbefa_detailed.EU4_NOx <- read_delim("D:/Projects/VSP/MATSim/PHEM/hbefa/EFA_HOT_Subsegm_detailed_Car_Aleks_filtered.csv") %>%
  filter(Component=="NOx" & EmConcept=="PC P Euro-4")

ggplot(data=hbefa_detailed.EU4_NOx) +
  geom_histogram(aes(x=EFA, fill = EFA > 0.08), binwidth = 0.002, boundary = 0) +
  scale_fill_manual(values = c("blue", "red"))

ggplot(data=hbefa_detailed.EU4_NOx) +
  geom_point(aes(x=V, y=EFA, color = EFA > 0.08)) +
  scale_color_manual(values = c("blue", "red"))

# ----

segment_freespeeds <- tibble(
  segment = c(0,1,2,3),
  freespeed = c(50, 80, 100, 130),
)

diff_out_CO2 <- diff_out_cleaned %>%
  filter(component=="CO2(total)") %>%
  merge(segment_freespeeds)

ggplot() +
  geom_function(fun = function(x) 0.0165 * x^2 - 2.3481 * x + 211.68, xlim = c(80, 200)) +
  geom_function(fun = function(x) 0.0928 * x * x - 9.2601 * x + 358.7, xlim = c(0, 50)) +
  geom_function(fun = function(x) 130, xlim = c(50, 80)) +
  geom_line(data=diff_out_CO2, aes(x=freespeed, y=gPkm, color=model)) +
  geom_point(data=diff_out_CO2, aes(x=freespeed, y=gPkm, color=model))

