{
  library(tidyverse)
  library(glue)
  library(kableExtra)

  # ==== Paths to ressources ====
  matsim_output_path <- "/Users/aleksander/Documents/VSP/PHEMTest/MatsimOutput"
  sumo_path <- "/Users/aleksander/Documents/VSP/PHEMTest/sumo2"
  hbefa_path <- "/Users/aleksander/Documents/VSP/PHEMTest/hbefa"
  plots_path <- "/Users/aleksander/Documents/VSP/PHEMTest/plots2"
}

# Plots Old Model (S&G Petrol)
{
  # Load data from MATSim
  # diff_out <- read_csv("contribs/emissions/test/input/org/matsim/contrib/emissions/PHEMTest/diff_petrol_ref.csv")
  r <- read_matsim(glue("{matsim_output_path}/PHEMTest/diff_WLTP_petrol_output_oldEmissionModule.csv"), "")
  data.MATSIM <- r[[1]]
  intervals <- r[[2]]

  # Load data from SUMO with PHEMLight5 and summarize for each interval
  data.SUMO_PHEMLight5 <- read_sumo(glue("{sumo_path}/sumo_petrol_a_output_pl5.csv"), intervals, "PHEMLight5") %>%
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
    # ggtitle("Comparison across WLTP-cycle for petrol") +
    labs(caption = "Fig XX: Comparison of MATSim and PHEMLightV4 across WLTP-cycle for a petrol vehicle") +
    theme(text = element_text(size=12), plot.caption = element_text(size = 12, hjust = 0.5, margin = margin(t=20)))

  ggsave(glue("{plots_path}/OldModelS&GResultsPetrol.png"),
         width = 16,
         height = 9,
         dpi = 300)

  # Export values to latex table
  # (Tables here are not final, but the served as basis for the journal tables)
  export <- data %>%
    select(segment, component, model, gPkm) %>%
    arrange(segment, component, model) %>%
    group_by(component) %>%
    mutate(
      exponent = floor(log10(max(abs(gPkm), na.rm = TRUE))),
      mantissa = round(gPkm / 10^exponent, digits = 3)
    ) %>%
  ungroup()

  tab <- export %>%
    select(segment, model, component, mantissa) %>%
    pivot_wider(names_from = component,
                values_from = mantissa) %>%
    ungroup()

  exp_row <- export %>%
    distinct(component, exponent) %>%
    arrange(component)

  tab %>%
    kbl(
      format = "latex",
      escape = FALSE,
      booktabs = TRUE
    ) %>%
    add_header_above(
      c(" " = 2, "Emissions [g/km]" = ncol(tab) - 2)
    ) %>%
    add_header_above(
      c(
        " " = 2,
        setNames(
          rep(1, nrow(exp_row)),
          paste0("$10^{", exp_row$exponent, "}$")
        )
      ),
      escape = FALSE
    ) %>%
    kable_styling()

}

# Plots Old Model (S&G Diesel)
{
  # Load data from MATSim
  # diff_out <- read_csv("contribs/emissions/test/input/org/matsim/contrib/emissions/PHEMTest/diff_petrol_ref.csv")
  r <- read_matsim(glue("{matsim_output_path}/PHEMTest/diff_WLTP_diesel_output_oldEmissionModule.csv"), "")
  data.MATSIM <- r[[1]]
  intervals <- r[[2]]

  # Load data from SUMO with PHEMLight5 and summarize for each interval
  data.SUMO_PHEMLight5 <- read_sumo(glue("{sumo_path}/sumo_diesel_a_output_pl5.csv"), intervals, "") %>%
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
    # ggtitle("Comparison across WLTP-cycle for diesel") +
    labs(caption = "Fig XX: Comparison of MATSim and PHEMLightV4 across WLTP-cycle for a diesel vehicle") +
    theme(text = element_text(size=12), plot.caption = element_text(size = 12, hjust = 0.5, margin = margin(t=20)))

  ggsave(glue("{plots_path}/OldModelS&GResultsDiesel.png"),
         width = 16,
         height = 9,
         dpi = 300)

  # Export values to latex table
  # (Tables here are not final, but the served as basis for the journal tables)
  export <- data %>%
    select(segment, component, model, gPkm) %>%
    arrange(segment, component, model) %>%
    group_by(component) %>%
    mutate(
      exponent = floor(log10(max(abs(gPkm), na.rm = TRUE))),
      mantissa = round(gPkm / 10^exponent, digits = 3)
    ) %>%
    ungroup()

  tab <- export %>%
    select(segment, model, component, mantissa) %>%
    pivot_wider(names_from = component,
                values_from = mantissa) %>%
    ungroup()

  exp_row <- export %>%
    distinct(component, exponent) %>%
    arrange(component)

  tab %>%
    kbl(
      format = "latex",
      escape = FALSE,
      booktabs = TRUE
    ) %>%
    add_header_above(
      c(" " = 2, "Emissions [g/km]" = ncol(tab) - 2)
    ) %>%
    add_header_above(
      c(
        " " = 2,
        setNames(
          rep(1, nrow(exp_row)),
          paste0("$10^{", exp_row$exponent, "}$")
        )
      ),
      escape = FALSE
    ) %>%
    kable_styling()
}

# Plots New Model (Int. Petrol)
{
  # Load data from MATSim
  # diff_out <- read_csv("contribs/emissions/test/input/org/matsim/contrib/emissions/PHEMTest/diff_petrol_ref.csv")
  r <- read_matsim(glue("{matsim_output_path}/PHEMTest/diff_WLTP_petrol_output_useFirstDuplicate_InterpolationFraction_fromLinkAttributes_0.csv"), "")
  data.MATSIM <- r[[1]]
  intervals <- r[[2]]

  # Load data from SUMO with PHEMLight5 and summarize for each interval
  data.SUMO_PHEMLight5 <- read_sumo(glue("{sumo_path}/sumo_petrol_a_output_pl5.csv"), intervals, "PHEMLight5") %>%
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
    labs(caption = "Fig XX: Comparison of MATSim and PHEMLightV4 across WLTP-cycle for a petrol vehicle with the improved emission model") +
    theme(text = element_text(size=12), plot.caption = element_text(size = 12, hjust = 0.5, margin = margin(t=20)))

  ggsave(glue("{plots_path}/ImprovedModelResultsPetrol.png"),
         width = 16,
         height = 9,
         dpi = 300)
}

# Plots New Model (Int. Diesel)
{
  # Load data from MATSim
  # diff_out <- read_csv("contribs/emissions/test/input/org/matsim/contrib/emissions/PHEMTest/diff_petrol_ref.csv")
  r <- read_matsim(glue("{matsim_output_path}/PHEMTest/diff_WLTP_diesel_output_useFirstDuplicate_InterpolationFraction_fromLinkAttributes_0.csv"), "")
  data.MATSIM <- r[[1]]
  intervals <- r[[2]]

  # Load data from SUMO with PHEMLight5 and summarize for each interval
  data.SUMO_PHEMLight5 <- read_sumo(glue("{sumo_path}/sumo_diesel_a_output_pl5.csv"), intervals, "") %>%
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
    labs(caption = "Fig XX: Comparison of MATSim and PHEMLightV4 across WLTP-cycle for a diesel vehicle with the improved emission model") +
    theme(text = element_text(size=12), plot.caption = element_text(size = 12, hjust = 0.5, margin = margin(t=20)))

  ggsave(glue("{plots_path}/ImprovedModelResultsDiesel.png"),
         width = 16,
         height = 9,
         dpi = 300)
}

# Plot CO emission keys and values
{
  hbefa_det <- read_delim(glue("{hbefa_path}/EFA_HOT_Subsegm_detailed_Car_Aleks_filtered.csv"), delim = ";")

  hbefa_det_split <- hbefa_det %>%
    separate_wider_delim(TrafficSit, "/", names=c("Region", "RoadType", "VClass", "TrafficSituation"))

  tech <- "diesel"
  concept <- "PC D Euro-4"
  component <- "CO"

  d <- hbefa_det_split %>%
    mutate(VClass = as.numeric(VClass)) %>%
    filter(VehCat == "pass. car" &
             Technology == tech &
             EmConcept == concept &
             Component == component &
             # Freespeed < 75 &
             Subsegment == "PC diesel Euro-4 (DPF)")

  colors <- c("#17d2a4", "#70aef4", "#88e72f", "#f1e843", "#eb4949", "#eb49ad")

  ggplot(d) +
    geom_line(aes(x=as.numeric(VClass), y=EFA, color=TrafficSituation)) +
    # geom_point(aes(x=as.numeric(Freespeed), y=EFA, color=TrafficSituation)) +
    facet_wrap(Region~RoadType, scales = "free") +
    theme_minimal() +
    scale_color_manual(values=colors) +
    # scale_x_continuous(breaks = seq(0, max(as.numeric(VClass)), by = 10)) +
    geom_hline(aes(yintercept = 0.042, color="PLV5 (Low)"), linetype="dashed") +
    labs(caption=glue("Fig XX: Emissions for all HBEFA keys with: {tech}, {concept}, {component}. \n PLV5 reference value is average value for the Low segment of the WLTP trajectory.")) +
    theme(text = element_text(size=12), plot.caption = element_text(size = 12, hjust = 0.5, margin = margin(t=20))) +
    xlab("Speed (km/h)") +
    ylab("Emissions (g/km)")

  ggsave(glue("{plots_path}/HBEFA_Diesel_EURO4_CO.png"),
         width = 11,
         height = 10,
         dpi = 300)
}

# Plot NOx emission keys and values
{
  hbefa_det <- read_delim(glue("{hbefa_path}/EFA_HOT_Subsegm_detailed_Car_Aleks_filtered.csv"), delim = ";")

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
    # scale_x_continuous(breaks = seq(0, max(as.numeric(d$Freespeed)), by = 10)) +
    geom_hline(aes(yintercept = 0.057, color="PLV5 (Low)"), linetype="dashed") +
    geom_hline(aes(yintercept = 0.049, color="PLV5 (Medium)"), linetype="dashed") +
    geom_hline(aes(yintercept = 0.044, color="PLV5 (High)"), linetype="dashed") +
    geom_hline(aes(yintercept = 0.070, color="PLV5 (Extra High)"), linetype="dashed") +

    scale_color_manual(
      values = c(
        "PLV5 (Low)" = colors[1],
        "PLV5 (Medium)" = colors[2],
        "PLV5 (High)" = colors[3],
        "PLV5 (Extra High)" = colors[4],

        "Freeflow" = colors[5],
        "Heavy"    = colors[6],
        "Satur."   = colors[7],
        "St+Go"    = colors[8],
        "St+Go2"   = colors[9]
      ),

      breaks = c(
        # line legend first
        "PLV5 (Low)", "PLV5 (Medium)", "PLV5 (High)", "PLV5 (Extra High)",

        # hline legend second
        "Freeflow", "Heavy", "Satur.", "St+Go", "St+Go2"
      )
    ) +

    labs(caption=glue("Fig XX: Emissions for all HBEFA keys with: {tech}, {concept}, {component}. \n PLV5 reference values are the averages for respective segment")) +
    theme(text = element_text(size=12), plot.caption = element_text(size = 12, hjust = 0.5, margin = margin(t=20))) +
    xlab("Speed (km/h)") +
    ylab("Emissions (g/km)")

  ggsave(glue("{plots_path}/HBEFA_Petrol_EURO4_NOx.png"),
         width = 11,
         height = 10,
         dpi = 300)
}

# Plot interpolation curves for methods
speedCurves <- function(
  trafficSit = "RUR/MW/>130",
  emConcept = "PC P Euro-4",
  curves = c("AverageSpeed", "StopAndGoFraction", "InterpolationFraction", "BilinearInterpolationFraction"),
  components = c("CO", "CO2", "HC", "NOx"),
  caption = ""){
  trafficSit.u <- gsub( "/", "_", trafficSit)
  emConcept.u <- gsub( " ", "_", emConcept)
  components.u <- paste(components, collapse="-")

  hbefa_det <- read_delim(glue("{hbefa_path}/EFA_HOT_Subsegm_detailed_Car_Aleks_filtered.csv"), delim = ";") %>%
    filter(Component == "CO" | Component == "CO2(total)" | Component == "HC" | Component == "NOx") %>%
    filter(EmConcept == emConcept) %>%
    filter(startsWith(TrafficSit, trafficSit)) %>%
    mutate(component = ifelse(Component == "CO2(total)", "CO2", Component)) %>%
    filter(component %in% components)

  curves <- read_csv(glue("{matsim_output_path}/EmissionMethodComputationTest/{trafficSit.u}_{emConcept.u}.csv")) %>%
    pivot_longer(cols = c(
      "CO_StopAndGoFraction", "CO2_StopAndGoFraction", "HC_StopAndGoFraction", "NOx_StopAndGoFraction", "PMx_StopAndGoFraction",
      "CO_AverageSpeed", "CO2_AverageSpeed",  "HC_AverageSpeed", "NOx_AverageSpeed", "PMx_AverageSpeed",
      "CO_InterpolationFraction", "CO2_InterpolationFraction", "HC_InterpolationFraction", "NOx_InterpolationFraction", "PMx_InterpolationFraction",
      "CO_BilinearInterpolationFraction", "CO2_BilinearInterpolationFraction", "HC_BilinearInterpolationFraction", "NOx_BilinearInterpolationFraction", "PMx_BilinearInterpolationFraction"),
           names_to = "method", values_to = "value") %>%
    separate("method", c("component", "method"), "_") %>%
    filter(method %in% curves) %>%
    filter(component %in% components)

  colors <- c(
    "AverageSpeed"="#F54927",
    "StopAndGoFraction"="#27C2F5",
    "InterpolationFraction"="#F227F5",
    "BilinearInterpolationFraction"="#169C07"
  )

  p <- ggplot() +
    geom_line(data=curves, aes(x=vel, y=value, color=method)) +
    geom_point(data=hbefa_det, aes(x=V, y=EFA)) +
    facet_wrap(~component, scales = "free") +
    expand_limits(y = 0)+
    theme_minimal() +
    scale_color_manual(values=colors) +
    # ggtitle(glue("Comparison of emission development for different methods ({trafficSit}, {emConcept})")) +
    labs(caption = caption) +
    theme(text = element_text(size=12), plot.caption = element_text(size = 12, hjust = 0.5, margin = margin(t=20))) +
    xlab("Average velocity (km/h)") +
    ylab("Emissions (g/km)")

  layout <- ggplot_build(p)$layout$layout

  nrow <- layout$ROW
  ncol <- layout$COL

  ggsave(glue("{plots_path}/{trafficSit.u}_{emConcept.u}_{components.u}.png"),
         p,
         width = max(ncol)*10,
         height = max(nrow)*10,
         dpi = 300)
}

# Curve plots
{
  speedCurves(curves = c("AverageSpeed", "StopAndGoFraction"), caption="Fig XX: Development of emissions output for a petrol vehicle driving at various speeds on a motorway with speed limit of 130 km/h")
  speedCurves(curves = c("AverageSpeed", "StopAndGoFraction"), components="NOx")
  speedCurves(curves = c("AverageSpeed", "StopAndGoFraction"), components=c("CO", "CO2", "HC"))
  speedCurves(components=c("CO", "CO2", "HC"))
  speedCurves(trafficSit = "URB/Local/50", components=c("CO", "CO2"))
}

# Binned Error Diagrams
errorDiagrams <- function(
  name = "freespeed",
  vehicle = "FIGO_TECHAVG",
  binwidth = 5,
  xlab = "Freespeed (km/h)",
  ylab = "Average error (g/m)",
  caption = "Fig XX: The absolute error distribution by freespeed. Upper plots show the error for the distinct components.
           Bounding boxes represent the range between the 25th and 75th error percentile for the respective method.
           Bottom plots show the distance driven with respective freespeed.",
  x = function(error) error$freespeed,
  y = function(error) error$error_gPkm
) {

  pretoria_output.SG <- read_csv(glue("{matsim_output_path}/PretoriaTest/output_{vehicle}_StopAndGoFraction.csv")) %>%
    filter(linkId != 6555 & linkId != "cold") %>%
    mutate(method = "StopAndGoFraction")

  pretoria_output.Int <- read_csv(glue("{matsim_output_path}/PretoriaTest/output_{vehicle}_InterpolationFraction.csv")) %>%
    filter(linkId != 6555 & linkId != "cold") %>%
    mutate(method = "InterpolationFraction")

  network_information <- read_csv(glue("{matsim_output_path}/PretoriaTest/networkInformation.csv")) %>%
    separate(roadType, c("Region", "RoadType", "VClass"), sep="/")

  pretoria_output <- pretoria_output.SG %>%
    rbind(pretoria_output.Int) %>%
    inner_join(network_information, by = "linkId") %>%
    mutate(freespeed = freespeed*3.6, averageVelocity = averageVelocity*3.6)

  value_avg <- pretoria_output %>%
    pivot_longer(cols = c("CO_MATSim", "CO_pems", "CO2_MATSim", "CO2_pems", "NOx_MATSim", "NOx_pems"), names_to="component", values_to="value") %>%
    separate(component, sep = "_", into = c("component", "model")) %>%
    group_by(component, method, model) %>%
    summarize(mean_value = mean(value, na.rm=TRUE))

  d_error <- pretoria_output %>%
    mutate(CO = CO_MATSim - CO_pems, CO2 = CO2_MATSim - CO2_pems, NOx = NOx_MATSim - NOx_pems) %>%
    select(-CO_MATSim, -CO_pems, -CO2_MATSim, -CO2_pems, -NOx_MATSim, -NOx_pems) %>%
    pivot_longer(c("CO", "CO2", "NOx"), names_to = "component", values_to = "error") %>%
    mutate(error_gPkm = error/length) %>%
    left_join(value_avg %>% filter(model == "MATSim"), by =c("component", "method")) %>%
    select(-model)

  colors <- c("#00a4f5", "#d21717")

  d <- d_error %>%
    mutate(bin = ceiling(x(d_error) / binwidth) * binwidth ) %>%
    group_by(component, method, bin) %>%
    summarize(
      mean_error = mean(y(cur_data()), na.rm = TRUE),
      q05 = quantile(y(pick(everything())), 0.05, na.rm = TRUE),
      q25 = quantile(y(pick(everything())), 0.25, na.rm = TRUE),
      q75 = quantile(y(pick(everything())), 0.75, na.rm = TRUE),
      q95 = quantile(y(pick(everything())), 0.95, na.rm = TRUE),
      n = n(),
      .groups = "drop"
    )

  xmin <- min(d$bin)
  xmax <- max(d$bin)

  p1 <- ggplot(d) +
    geom_ribbon(
      aes(x=bin, ymin = q25, ymax = q75, fill = method),
      alpha = 0.2,
    ) +
    geom_line(aes(x=bin, y = mean_error, color=method)) +
    geom_hline(yintercept=0) +
    coord_cartesian(xlim = c(xmin, xmax)) +
    theme_minimal() +
    # scale_y_continuous(labels = \(x) str_pad(round(x, 2), width = 6)) +
    scale_color_manual(values=colors) +
    scale_fill_manual(values=colors) +
    facet_wrap(~component, scales="free_y") +
    theme(text = element_text(size=18)) +
    xlab("") +
    ylab(ylab)

  p2 <- ggplot(d_error %>% mutate(x_value = x(d_error))) +
    geom_histogram(aes(x=x_value, fill=segment, weight=length/(2*length(unique(d_error$tripId)))/1000), binwidth = binwidth) +
    coord_cartesian(xlim = c(xmin, xmax)) +
    theme_minimal() +
    scale_y_continuous(labels = \(x) str_pad(round(x, 2), width = 6)) +
    scale_fill_manual(
      values = c(
        "A" = "#1f77b4",
        "B" = "#ff7f0e",
        "C" = "#2ca02c",
        "none" = "grey70"
      )
    ) +
    facet_wrap(~component, scales="free_y") +
    theme(text = element_text(size=18)) +
    xlab(xlab) +
    ylab("Driven distance per trip (km)") +
    labs(caption=
           "Fig XX: The absolute error distribution by freespeed. Upper plots show the error for the distinct components.
           Bounding boxes represent the range between the 25th and 75th error percentile for the respective method.
           Bottom plots show the distance driven with respective freespeed.") +
    theme(text = element_text(size=18), plot.caption = element_text(size = 18, hjust = 0.5, margin = margin(t=20)))

  p1 / p2

  ggsave(glue("{plots_path}/{name}_err.png"),
         width = 30,
         height = 20,
         dpi = 300)
}

errorDiagrams()
errorDiagrams(name="gradient", binwidth = 0.05, xlab="Gradient (%)", x = function(error) error$gradient)
errorDiagrams(name="freespeed_normalized", ylab="Normalized average error (g/m)", y = function(error) {
  error$error_gPkm / error$mean_value
})
errorDiagrams(name="gradient_normalized", binwidth = 0.05, xlab="Gradient (%)", ylab="Normalized average error (g/m)", x = function(error) error$gradient, y = function(error) {
  error$error_gPkm / error$mean_value
})

# PHEM Plots with all computation methods
{
  plot_main2()
  plot_main2(fuel = "diesel")
}