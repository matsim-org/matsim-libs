{
  library(tidyverse)
  library(glue)

  # ==== Paths to ressources ====
  data_path <- "/Users/aleksander/Documents/VSP/PHEMTest/Pretoria/PAPER/"
}

# Plots Old Model (S&G Petrol)
{
  # Load data from MATSim
  # diff_out <- read_csv("contribs/emissions/test/input/org/matsim/contrib/emissions/PHEMTest/diff_petrol_ref.csv")
  r <- read_matsim(glue("{data_path}/ExplorativeAnalysis/OldModelS&GResults/diff_WLTP_petrol_output_useFirstDuplicate_fromLinkAttributes_0.csv"), "")
  data.MATSIM <- r[[1]]
  intervals <- r[[2]]

  # Load data from SUMO with PHEMLight5 and summarize for each interval
  data.SUMO_PHEMLight5 <- read_sumo(glue("{data_path}/ExplorativeAnalysis/sumo_petrol_a_output_pl5.csv"), intervals, "PHEMLight5") %>%
    mutate(model = "PHEMLightV5")

  # recalc: gram -> gram per kilometer
  data <- rbind(data.MATSIM, data.SUMO_PHEMLight5) %>%
    merge(intervals, by="segment") %>%
    mutate(gPkm = value/lengths)

  # Generate colors
  colors <- c("#d21717", "#17d2a4")

  # Bar-Plot
  ggplot(data) +
    geom_bar(aes(x=segment, y=gPkm, fill=model), stat="identity", position="dodge") +
    scale_fill_manual(values=colors) +
    facet_wrap(~component, scales="free") +
    ylab("emissions in g/km") +
    theme_minimal() +
    ggtitle("Comparison across WLTP-cycle for petrol") +
    theme(text = element_text(size=12))

  ggsave("/Users/aleksander/Documents/VSP/PHEMTest/Pretoria/PAPER/ExplorativeAnalysis/OldModelS&GResultsPetrol.png",
         width = 16,
         height = 9,
         dpi = 300)
}

# Plots Old Model (S&G Diesel)
{
  # Load data from MATSim
  # diff_out <- read_csv("contribs/emissions/test/input/org/matsim/contrib/emissions/PHEMTest/diff_petrol_ref.csv")
  r <- read_matsim(glue("{data_path}/ExplorativeAnalysis/OldModelS&GResults/diff_WLTP_diesel_output_useFirstDuplicate_fromLinkAttributes_0.csv"), "")
  data.MATSIM <- r[[1]]
  intervals <- r[[2]]

  # Load data from SUMO with PHEMLight5 and summarize for each interval
  data.SUMO_PHEMLight5 <- read_sumo(glue("{data_path}/ExplorativeAnalysis/sumo_diesel_a_output_pl5.csv"), intervals, "") %>%
    mutate(model = "PHEMLightV5")

  # recalc: gram -> gram per kilometer
  data <- rbind(data.MATSIM, data.SUMO_PHEMLight5) %>%
    merge(intervals, by="segment") %>%
    mutate(gPkm = value/lengths)

  # Generate colors
  colors <- c("#d21717", "#17d2a4")

  # Bar-Plot
  ggplot(data) +
    geom_bar(aes(x=segment, y=gPkm, fill=model), stat="identity", position="dodge") +
    scale_fill_manual(values=colors) +
    facet_wrap(~component, scales="free") +
    ylab("emissions in g/km") +
    theme_minimal() +
    ggtitle("Comparison across WLTP-cycle for diesel") +
    theme(text = element_text(size=12))

  ggsave("/Users/aleksander/Documents/VSP/PHEMTest/Pretoria/PAPER/ExplorativeAnalysis/OldModelS&GResultsDiesel.png",
         width = 16,
         height = 9,
         dpi = 300)
}

# Plots New Model (Int. Petrol)
{
  # Load data from MATSim
  # diff_out <- read_csv("contribs/emissions/test/input/org/matsim/contrib/emissions/PHEMTest/diff_petrol_ref.csv")
  r <- read_matsim(glue("{data_path}/ExplorativeAnalysis/ImprovedModelResults/diff_WLTP_petrol_output_useFirstDuplicate_fromLinkAttributes_0.csv"), "")
  data.MATSIM <- r[[1]]
  intervals <- r[[2]]

  # Load data from SUMO with PHEMLight5 and summarize for each interval
  data.SUMO_PHEMLight5 <- read_sumo(glue("{data_path}/ExplorativeAnalysis/sumo_petrol_a_output_pl5.csv"), intervals, "PHEMLight5") %>%
    mutate(model = "PHEMLightV5")

  # recalc: gram -> gram per kilometer
  data <- rbind(data.MATSIM, data.SUMO_PHEMLight5) %>%
    merge(intervals, by="segment") %>%
    mutate(gPkm = value/lengths)

  # Generate colors
  colors <- c("#d21717", "#17d2a4")

  # Bar-Plot
  ggplot(data) +
    geom_bar(aes(x=segment, y=gPkm, fill=model), stat="identity", position="dodge") +
    scale_fill_manual(values=colors) +
    facet_wrap(~component, scales="free") +
    ylab("emissions in g/km") +
    theme_minimal() +
    ggtitle("Comparison across WLTP-cycle for petrol") +
    theme(text = element_text(size=12))

  ggsave("/Users/aleksander/Documents/VSP/PHEMTest/Pretoria/PAPER/ExplorativeAnalysis/ImprovedModelResultsPetrol.png",
         width = 16,
         height = 9,
         dpi = 300)
}

# Plots New Model (Int. Diesel)
{
  # Load data from MATSim
  # diff_out <- read_csv("contribs/emissions/test/input/org/matsim/contrib/emissions/PHEMTest/diff_petrol_ref.csv")
  r <- read_matsim(glue("{data_path}/ExplorativeAnalysis/ImprovedModelResults/diff_WLTP_diesel_output_useFirstDuplicate_fromLinkAttributes_0.csv"), "")
  data.MATSIM <- r[[1]]
  intervals <- r[[2]]

  # Load data from SUMO with PHEMLight5 and summarize for each interval
  data.SUMO_PHEMLight5 <- read_sumo(glue("{data_path}/ExplorativeAnalysis/sumo_diesel_a_output_pl5.csv"), intervals, "") %>%
    mutate(model = "PHEMLightV5")

  # recalc: gram -> gram per kilometer
  data <- rbind(data.MATSIM, data.SUMO_PHEMLight5) %>%
    merge(intervals, by="segment") %>%
    mutate(gPkm = value/lengths)

  # Generate colors
  colors <- c("#d21717", "#17d2a4")

  # Bar-Plot
  ggplot(data) +
    geom_bar(aes(x=segment, y=gPkm, fill=model), stat="identity", position="dodge") +
    scale_fill_manual(values=colors) +
    facet_wrap(~component, scales="free") +
    ylab("emissions in g/km") +
    theme_minimal() +
    ggtitle("Comparison across WLTP-cycle for diesel") +
    theme(text = element_text(size=12))

  ggsave("/Users/aleksander/Documents/VSP/PHEMTest/Pretoria/PAPER/ExplorativeAnalysis/ImprovedModelResultsDiesel.png",
         width = 16,
         height = 9,
         dpi = 300)
}

# Plot CO emission keys and values
{
  hbefa_det <- read_delim("/Users/aleksander/Documents/VSP/PHEMTest/hbefa/EFA_HOT_Subsegm_detailed_Car_Aleks_filtered.csv", delim = ";")

  hbefa_det_split <- hbefa_det %>%
    separate_wider_delim(TrafficSit, "/", names=c("Region", "RoadType", "Freespeed", "TrafficSituation"))

  tech <- "diesel"
  concept <- "PC D Euro-4"
  component <- "CO"

  d <- hbefa_det_split %>%
    mutate(Freespeed = as.numeric(Freespeed)) %>%
    filter(VehCat == "pass. car" &
             Technology == tech &
             EmConcept == concept &
             Component == component &
             # Freespeed < 75 &
             Subsegment == "PC diesel Euro-4 (DPF)")

  colors <- c("#17d2a4", "#70aef4", "#88e72f", "#f1e843", "#eb4949", "#eb49ad")

  ggplot(d) +
    geom_line(aes(x=as.numeric(Freespeed), y=EFA, color=TrafficSituation)) +
    # geom_point(aes(x=as.numeric(Freespeed), y=EFA, color=TrafficSituation)) +
    facet_wrap(Region~RoadType, scales = "free") +
    theme_minimal() +
    theme(text = element_text(size=12)) +
    scale_color_manual(values=colors) +
    # scale_x_continuous(breaks = seq(0, max(as.numeric(d$Freespeed)), by = 10)) +
    geom_hline(aes(yintercept = 0.042, color=" PLV5"), linetype="dashed") +
    labs(title=glue("Emissions for all HBEFA keys with: {tech}, {concept}, {component}")) +
    xlab("Speed (km/h)") +
    ylab("Emissions (g/km)")

  ggsave("/Users/aleksander/Documents/VSP/PHEMTest/Pretoria/PAPER/HBEFA_Diesel_EURO4_CO.png",
         width = 11,
         height = 10,
         dpi = 300)
}

# Plot NOx emission keys and values
{
  hbefa_det <- read_delim("/Users/aleksander/Documents/VSP/PHEMTest/hbefa/EFA_HOT_Subsegm_detailed_Car_Aleks_filtered.csv", delim = ";")

  hbefa_det_split <- hbefa_det %>%
    separate_wider_delim(TrafficSit, "/", names=c("Region", "RoadType", "Freespeed", "TrafficSituation"))

  tech <- "petrol (4S)"
  concept <- "PC P Euro-4"
  component <- "NOx"

  d <- hbefa_det_split %>%
    mutate(Freespeed = as.numeric(Freespeed)) %>%
    filter(VehCat == "pass. car" &
             Technology == tech &
             EmConcept == concept &
             Component == component)
             # Freespeed < 75 &)

  colors <- c("#17d2a4", "#177bd2", "#1720d2", "#8146e5", "#70aef4", "#88e72f", "#f1e843", "#eb4949", "#eb49ad")

  ggplot(d) +
    geom_line(aes(x=as.numeric(Freespeed), y=EFA, color=TrafficSituation)) +
    # geom_point(aes(x=as.numeric(Freespeed), y=EFA, color=TrafficSituation)) +
    facet_wrap(Region~RoadType, scales = "free") +
    theme_minimal() +
    theme(text = element_text(size=12)) +
    scale_color_manual(values=colors) +
    # scale_x_continuous(breaks = seq(0, max(as.numeric(d$Freespeed)), by = 10)) +
    geom_hline(aes(yintercept = 0.057 , color=" PLV5 (Low)"), linetype="dashed") +
    geom_hline(aes(yintercept = 0.049, color=" PLV5 (Medium)"), linetype="dashed") +
    geom_hline(aes(yintercept = 0.044, color=" PLV5 (High)"), linetype="dashed") +
    geom_hline(aes(yintercept = 0.070, color=" PLV5 (Extra High)"), linetype="dashed") +
    labs(title=glue("Emissions for all HBEFA keys with: {tech}, {concept}, {component}")) +
    xlab("Speed (km/h)") +
    ylab("Emissions (g/km)")

  ggsave("/Users/aleksander/Documents/VSP/PHEMTest/Pretoria/PAPER/HBEFA_Petrol_EURO4_NOx.png",
         width = 11,
         height = 10,
         dpi = 300)
}

# Plot interpolation curves for methods
curves <- function(
  trafficSit = "RUR/MW/>130",
  emConcept = "PC P Euro-4",
  curves = c("AverageSpeed", "StopAndGoFraction", "InterpolationFraction", "BilinearInterpolationFraction")){
  trafficSit.u <- gsub( "/", "_", trafficSit)
  emConcept.u <- gsub( " ", "_", emConcept)

  hbefa_det <- read_delim("/Users/aleksander/Documents/VSP/PHEMTest/hbefa/EFA_HOT_Subsegm_detailed_Car_Aleks_filtered.csv", delim = ";") %>%
    filter(Component == "CO" | Component == "CO2(total)" | Component == "NOx") %>%
    filter(EmConcept == emConcept) %>%
    filter(startsWith(TrafficSit, trafficSit)) %>%
    mutate(component = ifelse(Component == "CO2(total)", "CO2", Component))

  curves <- read_csv(glue("/Users/aleksander/Documents/VSP/PHEMTest/Pretoria/PAPER/InterpolationCurves/{trafficSit.u}_{emConcept.u}.csv")) %>%
    pivot_longer(cols = c(
      "CO_StopAndGoFraction", "CO2_StopAndGoFraction", "NOx_StopAndGoFraction",
      "CO_AverageSpeed", "CO2_AverageSpeed", "NOx_AverageSpeed",
      "CO_InterpolationFraction", "CO2_InterpolationFraction", "NOx_InterpolationFraction",
      "CO_BilinearInterpolationFraction", "CO2_BilinearInterpolationFraction", "NOx_BilinearInterpolationFraction"),
           names_to = "method", values_to = "value") %>%
    separate("method", c("component", "method"), "_") %>%
    filter(method %in% curves)

  ggplot() +
    geom_line(data=curves, aes(x=vel, y=value, color=method)) +
    geom_point(data=hbefa_det, aes(x=V, y=EFA)) +
    facet_wrap(~component, scales = "free") +
    theme_minimal() +
    ggtitle(glue("Comparison of emission development for different methods ({trafficSit}, {emConcept})")) +
    theme(text = element_text(size=12)) +
    xlab("Average velocity (km/h)") +
    ylab("Emissions (g/km)")

  ggsave(glue("/Users/aleksander/Documents/VSP/PHEMTest/Pretoria/PAPER/{trafficSit.u}_{emConcept.u}.png"),
         width = 11,
         height = 10,
         dpi = 300)
}

# Curve plots
{
  curves(curves = c("AverageSpeed", "StopAndGoFraction"))
}
