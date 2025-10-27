{
  library(tidyverse)
  library(glue)

  # ==== Paths to ressources ====
  sumo_path <- "/Users/aleksander/Documents/VSP/PHEMTest/sumo"
  diff_path <- "/Users/aleksander/Documents/VSP/PHEMTest/diff"
  hbefa_path <- "/Users/aleksander/Documents/VSP/PHEMTest/hbefa"

  # ==== Helper functions ====

  # Reads in the sv from matsim at the given path and returns the datasteeht and intervals from this sheet
  # model_suffix: optional,  adds a suffix to the 'model'-column which is just "MATSIM" by default
  read_matsim <- function(path, model_suffix = ""){
    # Load data from MATSim csv
    diff_out <- read_csv(path)

    # Create summarized data frame from MATSim results
    dataframe <- diff_out %>%
      select(segment, "CO-MATSIM", "CO2(total)-MATSIM", "HC-MATSIM", "PM-MATSIM", "NOx-MATSIM") %>%
      rename("CO2-MATSIM" = "CO2(total)-MATSIM", "PMx-MATSIM" = "PM-MATSIM") %>%
      pivot_longer(cols = c("CO-MATSIM",
                            "CO2-MATSIM",
                            "HC-MATSIM",
                            "PMx-MATSIM",
                            "NOx-MATSIM"), names_to="model", values_to="value") %>%
      separate(model, c("component", "model"), "-") %>%
      mutate(segment = as.integer(segment), model = paste(model, model_suffix, sep="_"))

    # Extract the interval times from the matsim-test-file
    intervals <- diff_out %>%
      mutate(endTime = startTime+travelTime) %>%
      select(segment, startTime, endTime, travelTime, lengths) %>%
      mutate(across(
        .cols = everything(),
        .fns = ~ as.integer(.x)
      ))

    return (list(dataframe, intervals))
  }
}

# ==== Plotting of raw SUMO NOx-emissions ====
{
  sumo_output <- read_csv2("contribs/emissions/test/input/org/matsim/contrib/emissions/PHEMTest/sumo_petrol_output.csv",
                           col_names = c("time", "velocity", "acceleration", "slope", "CO", "CO2", "HC", "PMx", "NOx", "fuel", "electricity"))

  bla <- sumo_output %>%
    mutate(NOx = as.numeric(NOx), velocity = as.numeric(velocity)) %>%
    mutate(avg_nox = (NOx / 1000) / (velocity / 3600)) %>%
    filter(!is.infinite(avg_nox))

  mean(bla$avg_nox)

  ggplot(bla, aes(x = as.numeric(time), y = avg_nox)) +
    geom_line() +
    theme_light()
}

# ==== Absolute and relative difference of MATSim / SUMO results ====
{
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
}

# ==== NOx Plot in g/km ====
{
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
}

# ==== Plot (all components) in g/km ====
{
  #Load data
  diff_out <- read_csv("contribs/emissions/test/input/org/matsim/contrib/emissions/PHEMTest/diff_petrol_ref.csv")

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
}

# ==== Plot with MATSim/PHEMLight/HBEFA3
{
  fuel <- "petrol"

  # Load data from MATSim
  # diff_out <- read_csv("contribs/emissions/test/input/org/matsim/contrib/emissions/PHEMTest/diff_petrol_ref.csv")
  diff_out <- read_csv(glue("/Users/aleksander/Documents/VSP/PHEMTest/diff/diff_{fuel}_output_fixedIntervalLength_60.csv"))

  # Create summarized data fram from MATSim results
  data.MATSIM <- diff_out %>%
    select(segment, "CO-MATSIM", "CO2(total)-MATSIM", "HC-MATSIM", "PM-MATSIM", "NOx-MATSIM") %>%
    rename("CO2-MATSIM" = "CO2(total)-MATSIM", "PMx-MATSIM" = "PM-MATSIM") %>%
    pivot_longer(cols = c("CO-MATSIM",
                          "CO2-MATSIM",
                          "HC-MATSIM",
                          "PMx-MATSIM",
                          "NOx-MATSIM"), names_to="model", values_to="value") %>%
    separate(model, c("component", "model"), "-") %>%
    mutate(segment = as.integer(segment))

  # Extract the interval times from the matsim-test-file
  intervals <- diff_out %>%
    mutate(endTime = startTime+travelTime) %>%
    select(segment, startTime, endTime, travelTime, lengths) %>%
    mutate(across(
      .cols = everything(),
      .fns = ~ as.integer(.x)
    ))

  # Load data from SUMO with PHEMLight and summarize for each interval
  data.SUMO_PHEMLight <- read_delim(glue("contribs/emissions/test/input/org/matsim/contrib/emissions/PHEMTest/sumo_{fuel}_output.csv"),
                           delim = ";",
                           col_names = c("time", "velocity", "acceleration", "slope", "CO", "CO2", "HC", "PMx", "NOx", "fuel", "electricity"),
                           col_types = cols(
                             time = col_integer(),
                             velocity = col_double(),
                             acceleration = col_double(),
                             slope = col_double(),
                             CO = col_double(),
                             CO2 = col_double(),
                             HC = col_double(),
                             PMx = col_double(),
                             NOx = col_double(),
                             fuel = col_double(),
                             electricity = col_double())) %>%
    pivot_longer(cols = c("CO", "CO2", "HC", "PMx", "NOx"), names_to = "component", values_to="value") %>%
    mutate(segment = cut(time, breaks = c(0, intervals$endTime), labels = FALSE, right = FALSE, include.lowest = TRUE)-as.integer(1)) %>%
    group_by(segment, component) %>%
    summarize(value = sum(value)) %>%
    mutate(model = "SUMO_PHEMLight", value=value/1000)

  # Load data from SUMO with PHEMLight5 and summarize for each interval
  data.SUMO_PHEMLight5 <- read_delim(glue("/Users/aleksander/Documents/VSP/PHEMTest/sumo/sumo_{fuel}_output_pl5.csv"),
                                    delim = ";",
                                    col_names = c("time", "velocity", "acceleration", "slope", "CO", "CO2", "HC", "PMx", "NOx", "fuel", "electricity"),
                                    col_types = cols(
                                      time = col_integer(),
                                      velocity = col_double(),
                                      acceleration = col_double(),
                                      slope = col_double(),
                                      CO = col_double(),
                                      CO2 = col_double(),
                                      HC = col_double(),
                                      PMx = col_double(),
                                      NOx = col_double(),
                                      fuel = col_double(),
                                      electricity = col_double())) %>%
    pivot_longer(cols = c("CO", "CO2", "HC", "PMx", "NOx"), names_to = "component", values_to="value") %>%
    mutate(segment = cut(time, breaks = c(0, intervals$endTime), labels = FALSE, right = FALSE, include.lowest = TRUE)-as.integer(1)) %>%
    group_by(segment, component) %>%
    summarize(value = sum(value)) %>%
    mutate(model = "SUMO_PHEMLight5", value=value/1000)

  # TODO make sure, that this actually is a petrol car! It is just called "default"
  # Load data from SUMO with HBEFA3 and summarize for each interval
  # data.SUMO_HBEFA3 <- read_delim("/Users/aleksander/Documents/VSP/PHEMTest/sumo/sumo_average_hbefa3_output.csv",
  #                                delim = ";",
  #                                col_names = c("time", "velocity", "acceleration", "slope", "CO", "CO2", "HC", "PMx", "NOx", "fuel", "electricity"),
  #                                col_types = cols(
  #                                  time = col_integer(),
  #                                  velocity = col_double(),
  #                                  acceleration = col_double(),
  #                                  slope = col_double(),
  #                                  CO = col_double(),
  #                                  CO2 = col_double(),
  #                                  HC = col_double(),
  #                                  PMx = col_double(),
  #                                  NOx = col_double(),
  #                                  fuel = col_double(),
  #                                  electricity = col_double())) %>%
  #   pivot_longer(cols = c("CO", "CO2", "HC", "PMx", "NOx"), names_to = "component", values_to="value") %>%
  #   mutate(segment = cut(time, breaks = c(0, intervals$endTime), labels = FALSE, right = FALSE, include.lowest = TRUE)-as.integer(1)) %>%
  #   group_by(segment, component) %>%
  #   summarize(value = sum(value)) %>%
  #   mutate(model = "SUMO_HBEFA3", value=value/1000)

  # Append all datasets together
  data_list <- mget(ls(pattern = "^data\\."), envir = .GlobalEnv)

  # recalc: gram -> gram per kilometer
  data <- do.call(rbind, data_list) %>%
    merge(intervals, by="segment") %>%
    mutate(gPkm = value/lengths)

  # Bar-Plot
  ggplot(data) +
    geom_bar(aes(x=segment, y=gPkm, fill=model), stat="identity", position="dodge") +
    scale_fill_manual(values=c("#d21717", "#17d2a4", "#7d23cc")) +
    facet_wrap(~component, scales="free") +
    ylab("emissions in g/km") +
    theme(text = element_text(size=18)) +
    #geom_rect(data=min_max_vals_used, aes(xmin=0, xmax=3, ymin=min, ymax=max, fill=table), alpha=0.2) +
    ggtitle(glue("Comparison across WLTP-cycle for {fuel}")) +
    theme_minimal()

  # Line-Plot (for scenarios with more links)
  ggplot(data) +
    geom_line(aes(x=startTime, y=gPkm, color=model), size=12/nrow(intervals)) +
    geom_point(aes(x=startTime, y=gPkm, color=model), size=6/nrow(intervals)) +
    scale_color_manual(values=c("#d21717", "#17d2a4", "#7d23cc")) +
    facet_wrap(~component, scales="free") +
    ylab("emissions in g/km") +
    theme(text = element_text(size=18)) +
    ggtitle(glue("Comparison across WLTP-cycle for {fuel}")) +
    theme_minimal()

}

# ==== Plot with different Freespeed-factors
{
  # TODO Do this for all MATSim diffs
  r <- read_matsim("/Users/aleksander/Documents/VSP/PHEMTest/diff/diff_petrol_fixedIntervalLength_60_1.2_out.csv", "1.2")
  data.MATSIM_1_2 <- r[[1]]
  intervals <- r[[2]]

  r <- read_matsim("/Users/aleksander/Documents/VSP/PHEMTest/diff/diff_petrol_fixedIntervalLength_60_1.1_out.csv", "1.1")
  data.MATSIM_1_1 <- r[[1]]

  r <- read_matsim("/Users/aleksander/Documents/VSP/PHEMTest/diff/diff_petrol_fixedIntervalLength_60_1.0_out.csv", "1.0")
  data.MATSIM_1_0 <- r[[1]]

  r <- read_matsim("/Users/aleksander/Documents/VSP/PHEMTest/diff/diff_petrol_fixedIntervalLength_60_1.5_out.csv", "1.5")
  data.MATSIM_1_5 <- r[[1]]

  r <- read_matsim("/Users/aleksander/Documents/VSP/PHEMTest/diff/diff_petrol_fixedIntervalLength_60_2.0_out.csv", "2.0")
  data.MATSIM_2_0 <- r[[1]]

  # Load data from SUMO with PHEMLight and summarize for each interval
  data.SUMO_PHEMLight <- read_delim("contribs/emissions/test/input/org/matsim/contrib/emissions/PHEMTest/sumo_petrol_output.csv",
                                    delim = ";",
                                    col_names = c("time", "velocity", "acceleration", "slope", "CO", "CO2", "HC", "PMx", "NOx", "fuel", "electricity"),
                                    col_types = cols(
                                      time = col_integer(),
                                      velocity = col_double(),
                                      acceleration = col_double(),
                                      slope = col_double(),
                                      CO = col_double(),
                                      CO2 = col_double(),
                                      HC = col_double(),
                                      PMx = col_double(),
                                      NOx = col_double(),
                                      fuel = col_double(),
                                      electricity = col_double())) %>%
    pivot_longer(cols = c("CO", "CO2", "HC", "PMx", "NOx"), names_to = "component", values_to="value") %>%
    mutate(segment = cut(time, breaks = c(0, intervals$endTime), labels = FALSE, right = FALSE, include.lowest = TRUE)-as.integer(1)) %>%
    group_by(segment, component) %>%
    summarize(value = sum(value)) %>%
    mutate(model = "SUMO_PHEMLight", value=value/1000)


  # Append all datasets together
  data_list <- mget(ls(pattern = "^data\\."), envir = .GlobalEnv)

  # recalc: gram -> gram per kilometer
  data <- do.call(rbind, data_list) %>%
    merge(intervals, by="segment") %>%
    mutate(gPkm = value/lengths)

  # Line-Plot (for scenarios with more links)
  ggplot(data) +
    geom_line(aes(x=startTime, y=gPkm, color=model), size=16/nrow(intervals)) +
    geom_point(aes(x=startTime, y=gPkm, color=model), size=8/nrow(intervals)) +
    #scale_color_manual(values=c("#d21717", "#bfbf00", "#17d2a4", "#7d23cc")) +
    facet_wrap(~component, scales="free") +
    ylab("emissions in g/km") +
    theme(text = element_text(size=18))
}

# ==== Filter out pass.veh ===
{
  path_in <- "/Users/aleksander/Documents/VSP/PHEMTest/hbefa/EFA_HOT_Subsegm_detailed_Car_Aleks.csv"
  path_out <- "/Users/aleksander/Documents/VSP/PHEMTest/hbefa/EFA_HOT_Subsegm_detailed_Car_Aleks_filtered.csv"

  table <- read_delim(path_in, delim=";")
  table <- table %>%
    filter(VehCat == "pass. car") %>%
    filter(Technology == "petrol (4S)" | Technology == "diesel")

  write_delim(table, path_out, delim=";")
}

# ==== SUMO Plot ====
{
  sumo_output <- read_delim("contribs/emissions/test/input/org/matsim/contrib/emissions/PHEMTest/sumo_petrol_output.csv", delim = ";",
                           col_names = c("time", "velocity", "acceleration", "slope", "CO", "CO2", "HC", "PMx", "NOx", "fuel", "electricity"))

  #Create helper vars
  lengths <- tibble(
    segment = c(0,1,2,3),
    time = c(589, 433, 455, 323),
    length = c(3095, 4756, 7158, 8254)
  )

  sumo_output <- sumo_output %>%
    mutate(CO2_m = ifelse(velocity <= 1, 0, CO2/velocity))

  ggplot(sumo_output, aes(x=time)) +
    geom_line(aes(y=as.numeric(CO2)/100), color="red") +
    # geom_line(aes(y=CO2_m/100), color="blue") +
    geom_line(aes(y=velocity), color="black")
    # geom_line(aes(y=acceleration*10), color="orange")

  # Absolute emissionen pro segment und dann mit java code vergleichen
  segments <- c(sum(sumo_output$CO[1:589]), sum(sumo_output$CO[590:1022]), sum(sumo_output$CO[1023:1477]), sum(sumo_output$CO[1478:1800]))
  segments <- segments/1000
}

# ==== HBEFA NOx > 0,08g discussion ====
{
  hbefa_detailed.EU4_NOx <- read_delim("D:/Projects/VSP/MATSim/PHEM/hbefa/EFA_HOT_Subsegm_detailed_Car_Aleks_filtered.csv") %>%
    filter(Component=="NOx" & EmConcept=="PC P Euro-4")

  ggplot(data=hbefa_detailed.EU4_NOx) +
    geom_histogram(aes(x=EFA, fill = EFA > 0.08), binwidth = 0.002, boundary = 0) +
    scale_fill_manual(values = c("blue", "red"))

  ggplot(data=hbefa_detailed.EU4_NOx) +
    geom_point(aes(x=V, y=EFA, color = EFA > 0.08)) +
    scale_color_manual(values = c("blue", "red"))
}

# ==== Regression curve ====
{
  #Load data
  diff_out <- read_csv("contribs/emissions/test/output/org/matsim/contrib/emissions/PHEMTest/test/diff_out.csv")

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
}

# ==== Avg Factor Deviation ====
{
  #Load data
  diff_out <- read_csv("contribs/emissions/test/output/org/matsim/contrib/emissions/PHEMTest/test/diff_petrol_out.csv")

  #Compute avg Factors
  diff_out_factors <- diff_out %>%
    select(segment, "CO-Factor", "CO2(total)-Factor", "HC-Factor", "PM-Factor", "NOx-Factor") %>%
  summarize(co_avg=sum(`CO-Factor`)/n()-1, co2_avg=sum(`CO2(total)-Factor`)/n()-1, hc_avg=sum(`HC-Factor`)/n()-1, pm_avg=sum(`PM-Factor`)/n()-1, nox_avg=sum(`NOx-Factor`)/n()-1)
}

# ==== Stop&Go-Fraction ====
{
  hbefa_det <- read_delim("D:/Projects/VSP/MATSim/PHEM/hbefa/EFA_HOT_Subsegm_detailed_Car_Aleks_filtered.csv", delim = ";")

  hbefa_det_split <- hbefa_det %>%
    separate_wider_delim(TrafficSit, "/", names=c("Region", "RoadType", "Freespeed", "TrafficSituation"))

  tech <- "petrol (4S)"
  concept <- "PC P Euro-4"
  component <- "CO"
  reg <- "RUR"
  roadtype <- "MW"

  d <- hbefa_det_split %>%
    filter(VehCat == "pass. car" &
             Technology == tech &
             EmConcept == concept &
             Component == component &
             Region == reg &
             RoadType == roadtype &
             Freespeed != ">130")
             #(TrafficSituation == "Freeflow" | TrafficSituation == "St+Go"))
             #&Subsegment == "PC diesel Euro-4")

  ggplot(d) +
    geom_line(aes(x=as.numeric(Freespeed), y=EFA, color=TrafficSituation)) +
    #geom_line(data=e, aes(x=speed, y=EFA)) +
    #geom_function(fun = function(v) e_ff(v), alpha=0.3, color="green", xlim = c(80, 130)) +
    #geom_hline(aes(yintercept = 0.688245, color="SUMO-Wert"), linetype="dashed") +
    #geom_function(fun = function(v) 0.0022*v^2 - 0.4093*v + 18.7549, alpha=0.3, xlim = c(80, 130)) +
    labs(title=paste(tech, concept, reg, roadtype, component, sep=", ")) +
    xlab("Geschwindigkeit (km/h)") +
    ylab("Emissionen (g/km)")

  # Print out diffs:
  d2 <- d %>%
    filter(TrafficSituation == "Freeflow")
  diff(d2$EFA)

  #geom_function(fun = function(v) (1-r(v))*e_ff(v)+r(v)*0.403, color="black", xlim = c(80, 130)) +
  #geom_function(fun = function(v) r(v), alpha = 0.3, color="black", xlim = c(80, 130)) +

  #e_ff <- function(v) {
  #  #return (exp(-6.93254926906049018243+0.06375004168334660881*v))
  #  return (0.00000193333*v^4 - 0.000745315*v^3 + 0.107818*v^2 - 6.91413*v + 165.726)
  #}

  r <- function(v) {
    return ((17.9/(0.7*v))*((0.3*v)/(v-17.9)))
  }

  e <- tibble(
    speed = c(80,90,100,110,120,130),
    EFA = c(0.347, 0.317, 0.546, 0.784, 1.537, 3.524)
  )
}

# ==== Quantitative analysis ====
{
  # Load data from MATSim
  # diff_out <- read_csv("contribs/emissions/test/input/org/matsim/contrib/emissions/PHEMTest/diff_petrol_ref.csv")
  diff_out <- read_csv("/Users/aleksander/Documents/VSP/PHEMTest/diff/diff_diesel_fixedIntervalLength_60_out.csv")

  # Compute the average difference for each component
  print(paste("CO:", (mean(diff_out$`CO-Factor`)-1)*100, "%"))
  print(paste("CO2:", (mean(diff_out$`CO2(total)-Factor`)-1)*100, "%"))
  print(paste("HC:", (mean(diff_out$`HC-Factor`)-1)*100, "%"))
  print(paste("NOx:", (mean(diff_out$`NOx-Factor`)-1)*100, "%"))
  print(paste("PM:", (mean(diff_out$`PM-Factor`)-1)*100, "%"))
}

# ==== Inverted Time axis ====
{
  fuel <- "diesel"

  # Clear old data
  rm(list = ls(pattern = "^data\\."))

  r <- read_matsim(glue("{diff_path}/diff_{fuel}_output.csv"), "original")
  data.MATSIM_original <- r[[1]]
  intervals <- r[[2]]

  r <- read_matsim(glue("{diff_path}/diff_{fuel}_output_inverted_time.csv"), "inverted")
  data.MATSIM_inverted_time <- r[[1]]

  # Load data from SUMO with original times and summarize for each interval
  data.SUMO_original <- read_delim(glue("{sumo_path}/sumo_{fuel}_output.csv"),
                                   delim = ";",
                                   col_names = c("time", "velocity", "acceleration", "slope", "CO", "CO2", "HC", "PMx", "NOx", "fuel", "electricity"),
                                   col_types = cols(
                                     time = col_integer(),
                                     velocity = col_double(),
                                     acceleration = col_double(),
                                     slope = col_double(),
                                     CO = col_double(),
                                     CO2 = col_double(),
                                     HC = col_double(),
                                     PMx = col_double(),
                                     NOx = col_double(),
                                     fuel = col_double(),
                                     electricity = col_double())) %>%
    pivot_longer(cols = c("CO", "CO2", "HC", "PMx", "NOx"), names_to = "component", values_to="value") %>%
    mutate(segment = cut(time, breaks = c(0, intervals$endTime), labels = FALSE, right = FALSE, include.lowest = TRUE)-as.integer(1)) %>%
    group_by(segment, component) %>%
    summarize(value = sum(value)) %>%
    mutate(model = "SUMO_original", value=value/1000)


  # Load data from SUMO with inverted times and summarize for each interval
  data.SUMO_inverted_time <- read_delim(glue("{sumo_path}/sumo_{fuel}_output_inverted_time.csv"),
                                    delim = ";",
                                    col_names = c("time", "velocity", "acceleration", "slope", "CO", "CO2", "HC", "PMx", "NOx", "fuel", "electricity"),
                                    col_types = cols(
                                      time = col_integer(),
                                      velocity = col_double(),
                                      acceleration = col_double(),
                                      slope = col_double(),
                                      CO = col_double(),
                                      CO2 = col_double(),
                                      HC = col_double(),
                                      PMx = col_double(),
                                      NOx = col_double(),
                                      fuel = col_double(),
                                      electricity = col_double())) %>%
    pivot_longer(cols = c("CO", "CO2", "HC", "PMx", "NOx"), names_to = "component", values_to="value") %>%
    mutate(segment = cut(time, breaks = c(0, intervals$endTime), labels = FALSE, right = FALSE, include.lowest = TRUE)-as.integer(1)) %>%
    group_by(segment, component) %>%
    summarize(value = sum(value)) %>%
    mutate(model = "SUMO_inverted", value=value/1000)

  # Append all datasets together
  data_list <- mget(ls(pattern = "^data\\."), envir = .GlobalEnv)

  # recalc: gram -> gram per kilometer
  # separate: "MODEL_SCENARIO" -> "MODEL" | "SCENARIO"
  data <- do.call(rbind, data_list) %>%
    merge(intervals, by="segment") %>%
    mutate(gPkm = value/lengths) %>%
    separate(model, into=c("model", "scenario"), sep="_")

  # Line-Plot (for scenarios with more links)
  ggplot(data) +
    geom_line(aes(x=startTime, y=gPkm, color=model), size=12/nrow(intervals)) +
    geom_point(aes(x=startTime, y=gPkm, color=model), size=6/nrow(intervals)) +
    scale_color_manual(values=c("#d21717", "#17d2a4", "#7d23cc")) +
    facet_wrap(component ~ scenario, scales="free") +
    ylab("emissions in g/km") +
    theme(text = element_text(size=22)) +
    ggtitle(glue("Original WLTP vs. Inverted WLTP for {fuel} cars"))
}

# ==== Derivated Acceleration ====
{
  fuel <- "petrol"

  # Clear old data
  rm(list = ls(pattern = "^data\\."))

  r <- read_matsim(glue("{diff_path}/diff_{fuel}_output.csv"), "original")
  data.MATSIM_original <- r[[1]]
  intervals <- r[[2]]

  # Load data from SUMO with original times and summarize for each interval
  data.SUMO_original <- read_delim(glue("{sumo_path}/sumo_{fuel}_output.csv"),
                                delim = ";",
                                col_names = c("time", "velocity", "acceleration", "slope", "CO", "CO2", "HC", "PMx", "NOx", "fuel", "electricity"),
                                col_types = cols(
                                  time = col_integer(),
                                  velocity = col_double(),
                                  acceleration = col_double(),
                                  slope = col_double(),
                                  CO = col_double(),
                                  CO2 = col_double(),
                                  HC = col_double(),
                                  PMx = col_double(),
                                  NOx = col_double(),
                                  fuel = col_double(),
                                  electricity = col_double())) %>%
    pivot_longer(cols = c("CO", "CO2", "HC", "PMx", "NOx"), names_to = "component", values_to="value") %>%
    mutate(segment = cut(time, breaks = c(0, intervals$endTime), labels = FALSE, right = FALSE, include.lowest = TRUE)-as.integer(1)) %>%
    group_by(segment, component) %>%
    summarize(value = sum(value)) %>%
    mutate(model = "SUMO_original", value=value/1000)

  # Load data from SUMO with derivated accelerations and summarize for each interval
  data.SUMO_derivated_acc <- read_delim(glue("{sumo_path}/sumo_{fuel}_output_derivated_acc.csv"),
                                   delim = ";",
                                   col_names = c("time", "velocity", "acceleration", "slope", "CO", "CO2", "HC", "PMx", "NOx", "fuel", "electricity"),
                                   col_types = cols(
                                     time = col_integer(),
                                     velocity = col_double(),
                                     acceleration = col_double(),
                                     slope = col_double(),
                                     CO = col_double(),
                                     CO2 = col_double(),
                                     HC = col_double(),
                                     PMx = col_double(),
                                     NOx = col_double(),
                                     fuel = col_double(),
                                     electricity = col_double())) %>%
    pivot_longer(cols = c("CO", "CO2", "HC", "PMx", "NOx"), names_to = "component", values_to="value") %>%
    mutate(segment = cut(time, breaks = c(0, intervals$endTime), labels = FALSE, right = FALSE, include.lowest = TRUE)-as.integer(1)) %>%
    group_by(segment, component) %>%
    summarize(value = sum(value)) %>%
    mutate(model = "SUMO_derivated_acc", value=value/1000)

  # Load data from SUMO with SUMO computed accelerations and summarize for each interval
  data.SUMO_sumo_acc <- read_delim(glue("{sumo_path}/sumo_{fuel}_output_sumo_acc.csv"),
                                        delim = ";",
                                        col_names = c("time", "velocity", "acceleration", "slope", "CO", "CO2", "HC", "PMx", "NOx", "fuel", "electricity"),
                                        col_types = cols(
                                          time = col_integer(),
                                          velocity = col_double(),
                                          acceleration = col_double(),
                                          slope = col_double(),
                                          CO = col_double(),
                                          CO2 = col_double(),
                                          HC = col_double(),
                                          PMx = col_double(),
                                          NOx = col_double(),
                                          fuel = col_double(),
                                          electricity = col_double())) %>%
    pivot_longer(cols = c("CO", "CO2", "HC", "PMx", "NOx"), names_to = "component", values_to="value") %>%
    mutate(segment = cut(time, breaks = c(0, intervals$endTime), labels = FALSE, right = FALSE, include.lowest = TRUE)-as.integer(1)) %>%
    group_by(segment, component) %>%
    summarize(value = sum(value)) %>%
    mutate(model = "SUMO_sumo_acc", value=value/1000)

  # Append all datasets together
  data_list <- mget(ls(pattern = "^data\\."), envir = .GlobalEnv)

  # recalc: gram -> gram per kilometer
  # separate: "MODEL_SCENARIO" -> "MODEL" | "SCENARIO"
  data <- do.call(rbind, data_list) %>%
    merge(intervals, by="segment") %>%
    mutate(gPkm = value/lengths)

  # Line-Plot (for scenarios with more links)
  ggplot(data) +
    geom_line(aes(x=startTime, y=gPkm, color=model), size=18/nrow(intervals)) +
    geom_point(aes(x=startTime, y=gPkm, color=model), size=6/nrow(intervals)) +
    scale_color_manual(values=c("#d21717", "#17d2a4", "#7d23cc", "#228b22")) +
    facet_wrap(~component, scales="free") +
    ylab("emissions in g/km") +
    theme(text = element_text(size=22)) +
    ggtitle(glue("Original acceleration vs. Derivated acceleration vs. SUMO acceleration for {fuel} cars"))
}

# ==== Characteristic values of driving cycles ====
{
  # sumo_input <- read_delim("/Users/aleksander/Documents/VSP/PHEMTest/sumo/sumo_input.csv", delim=";", col_names=c("time", "velocity", "acceleration"))
  WLTP <- read_delim("/Users/aleksander/Documents/VSP/PHEMTest/sumo/sumo_input.csv", delim=";", col_names=c("time", "vel", "acc"), col_types=c(col_integer(), col_double(), col_double()))
  WLTP_inv <- read_delim("/Users/aleksander/Documents/VSP/PHEMTest/sumo/sumo_input_inverted_time.csv", delim=";", col_names=c("time", "vel", "acc"), col_types=c(col_integer(), col_double(), col_double()))
  CADC <- read_delim("/Users/aleksander/Documents/VSP/PHEMTest/CADC/CADC.csv", delim=";", col_names=c("time", "vel", "acc"), col_types=c(col_integer(), col_double(), col_double()))

  # Make sure, that the accelerations of all datasets were computed in the same way

  WLTP <- WLTP %>%
    mutate(acc = (lead(vel) - lag(vel)) / (2*3.6)) %>%
    mutate(acc = ifelse(is.na(acc), 0, acc))

  WLTP_inv <- WLTP_inv %>%
  mutate(acc = (lead(vel) - lag(vel)) / (2*3.6)) %>%
  mutate(acc = ifelse(is.na(acc), 0, acc))

  CADC <- CADC %>%
    mutate(acc = (lead(vel) - lag(vel)) / (2*3.6)) %>%
    mutate(acc = ifelse(is.na(acc), 0, acc))


  # Sections (WLTP):
  # Low: [0:589]
  # Medium: [590:1022]
  # High: [1023:1477]
  # Extra High: [1478:1800]

  # Sections (CADC):
  # Low [1:993]
  # Medium [994:2075]
  # High [2076:3143]

  WLTP.st_dev <- sd(WLTP$acc)
  WLTP_inv.st_dev <- sd(WLTP_inv$acc)
  CADC.st_dev <- sd(CADC$acc)

  WLTP.st_dev_pos_acc <- sd(WLTP$acc[WLTP$acc > 0])
  WLTP_inv.st_dev_pos_acc <- sd(WLTP_inv$acc[WLTP_inv$acc > 0])
  CADC.st_dev_pos_acc <- sd(CADC$acc[CADC$acc > 0])

  WLTP.mean_acc <- mean(WLTP$acc[WLTP$acc > 0])
  WLTP.low.mean_acc <- mean(WLTP$acc[1:589][WLTP$acc[1:589] > 0])
  WLTP.medium.mean_acc <- mean(WLTP$acc[590:1022][WLTP$acc[590:1022] > 0])
  WLTP.high.mean_acc <- mean(WLTP$acc[1023:1477][WLTP$acc[1023:1477] > 0])
  WLTP.extra_high.mean_acc <- mean(WLTP$acc[1478:1800][WLTP$acc[1478:1800] > 0])

  WLTP.mean_vel <- mean(WLTP$vel)
  WLTP.low.mean_vel <- mean(WLTP$vel[1:589])
  WLTP.medium.mean_vel <- mean(WLTP$vel[590:1022])
  WLTP.high.mean_vel <- mean(WLTP$vel[1023:1477])
  WLTP.extra_high.mean_vel <- mean(WLTP$vel[1478:1800])

  WLTP_inv.mean_acc <- mean(WLTP_inv$acc[WLTP_inv$acc > 0])
  WLTP_inv.low.mean_acc <- mean(WLTP_inv$acc[1211:1800][WLTP_inv$acc[1211:1800] > 0])
  WLTP_inv.medium.mean_acc <- mean(WLTP_inv$acc[778:1210][WLTP_inv$acc[778:1210] > 0])
  WLTP_inv.high.mean_acc <- mean(WLTP_inv$acc[323:777][WLTP_inv$acc[323:777] > 0])
  WLTP_inv.extra_high.mean_acc <- mean(WLTP_inv$acc[1:322][WLTP_inv$acc[1:322] > 0])

  WLTP_inv.mean_vel <- mean(WLTP_inv$vel)
  WLTP_inv.low.mean_vel <- mean(WLTP_inv$vel[1211:1800])
  WLTP_inv.medium.mean_vel <- mean(WLTP_inv$vel[778:1210])
  WLTP_inv.high.mean_vel <- mean(WLTP_inv$vel[323:777])
  WLTP_inv.extra_high.mean_vel <- mean(WLTP_inv$vel[1:322])

  CADC.mean_acc <- mean(CADC$acc[CADC$acc > 0])
  CADC.low.mean_acc <- mean(CADC$acc[1:993][CADC$acc[1:993] > 0])
  CADC.medium.mean_acc <- mean(CADC$acc[994:2075][CADC$acc[994:2075] > 0])
  CADC.high.mean_acc <- mean(CADC$acc[2076:3143][CADC$acc[2076:3143] > 0])

  CADC.mean_vel <- mean(CADC$vel)
  CADC.low.mean_vel <- mean(CADC$vel[1:993])
  CADC.medium.mean_vel <- mean(CADC$vel[994:2075])
  CADC.high.mean_vel <- mean(CADC$vel[2076:3143])

  print(glue("WLTP, St.dev. : {WLTP.st_dev}; Mean vel: {WLTP.mean_vel}; Mean acc: {WLTP.mean_acc}"))
  print(glue("CADC, St.dev.: {CADC.st_dev}; Mean vel: {CADC.mean_vel}; Mean acc: {WLTP.mean_acc};"))
  # print(glue("WLTP_inv, St.dev. : {WLTP_inv.st_dev}; Mean vel: {WLTP_inv.mean_vel}; Mean acc: {WLTP.mean_acc}"))

  print(glue("WLTP: Low mean vel: {WLTP.low.mean_vel}; Medium mean vel: {WLTP.medium.mean_vel}; High mean vel: {WLTP.high.mean_vel}; Extra high mean vel: {WLTP.extra_high.mean_vel}"))
  print(glue("CADC: Low mean vel: {CADC.low.mean_vel}; Medium mean acc: --.--------------; High mean vel: {CADC.medium.mean_vel}; Extra high mean vel: {CADC.high.mean_vel}"))
  print(glue("WLTP_inv: Low mean vel: {WLTP_inv.low.mean_vel}; Medium mean vel: {WLTP_inv.medium.mean_vel}; High mean vel: {WLTP_inv.high.mean_vel}; Extra high mean vel: {WLTP_inv.extra_high.mean_vel}"))

  print(glue("WLTP: Low mean acc: {WLTP.low.mean_acc}; Medium mean acc: {WLTP.medium.mean_acc}; High mean acc: {WLTP.high.mean_acc}; Extra high mean acc: {WLTP.extra_high.mean_acc}"))
  print(glue("CADC: Low mean acc: {CADC.low.mean_acc}; Medium mean acc: --.--------------; High mean acc: {CADC.medium.mean_acc}; Extra high mean acc: {CADC.high.mean_acc}"))
  print(glue("WLTP_inv: Low mean acc: {WLTP_inv.low.mean_acc}; Medium mean acc: {WLTP_inv.medium.mean_acc}; High mean acc: {WLTP_inv.high.mean_acc}; Extra high mean acc: {WLTP_inv.extra_high.mean_acc}"))

  # ggplot() +
  #   geom_line(data=CADC, aes(x=time, y=vel), color="red") +
  #   geom_line(data=WLTP, aes(x=time, y=vel), color="blue")
}

# ==== CADC Test (using phem_lib)
{
  #TODO init phem_lib
  fuel <- "petrol"

  # WLTP
  plot_main()
  compute_emission_difference(glue("{diff_path}/diff_{fuel}_output_fixedIntervalLength_60.csv"), glue("{sumo_path}/sumo_{fuel}_output_pl5.csv") )
  compute_emission_difference(glue("{diff_path}/diff_{fuel}_output_fixedIntervalLength_60.csv"), glue("{sumo_path}/sumo_{fuel}_output_pl5.csv") )

  # CADC
  plot_main(title = glue("Comparison across CADC-cycle for {fuel}"), pl_data=glue("/Users/aleksander/Documents/VSP/PHEMTest/CADC/sumo_{fuel}_output.csv"), pl5_data=glue("/Users/aleksander/Documents/VSP/PHEMTest/CADC/sumo_{fuel}_output_pl5.csv"), fuel=fuel)
  compute_emission_difference(glue("{diff_path}/diff_{fuel}_output_fixedIntervalLength_60.csv"), glue("/Users/aleksander/Documents/VSP/PHEMTest/CADC/sumo_{fuel}_output_pl5.csv"))

  plot_main(title = glue("Comparison across CADC-cycle for {fuel}"), pl_data=glue("/Users/aleksander/Documents/VSP/PHEMTest/CADC/sumo_{fuel}_output.csv"), pl5_data=glue("/Users/aleksander/Documents/VSP/PHEMTest/CADC/sumo_{fuel}_output_pl5.csv"), fuel=fuel)
  compute_emission_difference(glue("{diff_path}/diff_{fuel}_output_fixedIntervalLength_60.csv"), glue("/Users/aleksander/Documents/VSP/PHEMTest/CADC/sumo_{fuel}_output_pl5.csv"))
}

# ==== Scale WLTP-cycle ====
{
  wltp <- read_delim(glue("{sumo_path}/sumo_input.csv"), col_names=c("time", "vel", "acc"), col_types=cols(time=col_integer(), vel=col_double(), acc=col_double()))

  scales <- c(1.0, 0.9, 0.8, 0.7, 0.6, 0.5)

  for (s in scales) {
    wltp_scaled <- wltp %>%
      mutate(time = s * time)

    t_target <- seq(1, floor(max(wltp_scaled$time)))

    vel_interp <- approx(wltp_scaled$time, wltp_scaled$vel, xout = t_target)$y

    assign(
      glue("wltp{s}"),
      tibble(
        time = t_target,
        vel = vel_interp,
        acc = 0
      )
    )

    write_delim(get(glue("wltp{s}")), glue("/Users/aleksander/Documents/VSP/PHEMTest/scaled/wltp{s}.csv"), delim=";", col_names = FALSE)
  }
}

# ==== Compare the scaled WLTP segments ====
{
  fuel <- "petrol"

  # Define scales
  scales <- c(1.0, 0.9, 0.8, 0.7, 0.6, 0.5)
  input_dir <- "/Users/aleksander/Documents/VSP/PHEMTest/scaled"

  # Create empty lists to store results
  avg_vel_list <- list()
  avg_acc_list <- list()
  emissions_list <- list()

  for (s in scales) {
    # Build file path
    file_path <- file.path(input_dir, paste0("sumo_petrol_", s, "_output.csv"))

    # Read the file
    sumo_output <- read_delim(
      file_path,
      delim = ";",
      col_names = c("time", "velocity", "acceleration", "slope", "CO", "CO2", "HC", "PMx", "NOx", "fuel", "electricity"),
      col_types = cols(
        time = col_integer(),
        velocity = col_double(),
        acceleration = col_double(),
        slope = col_double(),
        CO = col_double(),
        CO2 = col_double(),
        HC = col_double(),
        PMx = col_double(),
        NOx = col_double(),
        fuel = col_double(),
        electricity = col_double()
      )
    )

    avg_vel_list[[as.character(s)]] <- mean(sumo_output$velocity)
    avg_acc_list[[as.character(s)]] <- mean(sumo_output$acceleration[sumo_output$acceleration > 0])

    emissions_list[[as.character(s)]] <- sumo_output %>%
      pivot_longer(cols = c("CO", "CO2", "HC", "PMx", "NOx"), names_to = "component", values_to="value") %>%
      group_by(component) %>%
      summarize(value = sum(value)/1000) %>%
      mutate(avg_acc = avg_acc_list[[as.character(s)]], avg_vel = avg_vel_list[[as.character(s)]], scale = s, )
  }

  emissions_all <- bind_rows(emissions_list)

  # Compute the sum for MATSim values
  r <- read_matsim(glue("{diff_path}/WLTP/diff_{fuel}_output_fixedIntervalLength_60.csv"), "")
  emissions_matsim <- r[[1]] %>%
    group_by(component) %>%
    summarize(value = sum(value))
}

# ==== Plot CADCs ====
{
  CADC_URBAN <- read_delim("/Users/aleksander/Documents/VSP/PHEMTest/CADC/CADC_URBAN.csv", col_names = c("time", "vel"), delim=";")
  CADC_ROAD <- read_delim("/Users/aleksander/Documents/VSP/PHEMTest/CADC/CADC_ROAD.csv", col_names = c("time", "vel"), delim=";")
  CADC_MOTORWAY <- read_delim("/Users/aleksander/Documents/VSP/PHEMTest/CADC/CADC_MOTORWAY.csv", col_names = c("time", "vel"), delim=";")

  ggplot(CADC_URBAN) +
    geom_line(aes(x=time, y=vel))
  ggplot(CADC_ROAD) +
    geom_line(aes(x=time, y=vel))
  ggplot(CADC_MOTORWAY) +
    geom_line(aes(x=time, y=vel))

}

# ==== Connect CADCs ====
{
  CADC_URBAN <- read_delim("/Users/aleksander/Documents/VSP/PHEMTest/CADC/CADC_URBAN.csv", col_names = c("time", "vel"), delim=";")
  CADC_ROAD <- read_delim("/Users/aleksander/Documents/VSP/PHEMTest/CADC/CADC_ROAD.csv", col_names = c("time", "vel"), delim=";")
  CADC_MOTORWAY <- read_delim("/Users/aleksander/Documents/VSP/PHEMTest/CADC/CADC_MOTORWAY.csv", col_names = c("time", "vel"), delim=";")

  # Connect the dataframes
  CADC_ROAD <- CADC_ROAD %>%
    mutate(time = time + nrow(CADC_URBAN))
  CADC_MOTORWAY <- CADC_MOTORWAY %>%
    mutate(time = time + nrow(CADC_URBAN) + nrow(CADC_ROAD))
  CADC <- CADC_URBAN
  CADC <- rbind(CADC, CADC_ROAD)
  CADC <- rbind(CADC, CADC_MOTORWAY)

  # Compute the acc (method 1)
  CADC <- CADC %>%
    mutate(acc = (lead(vel) - lag(vel)) / (2*3.6)) %>%
    mutate(acc = ifelse(is.na(acc), 0, acc))

  # Compute the acc (method 2)
  # CADC <- CADC %>%
  #   mutate(acc = lead(vel) - vel) %>%
  #   mutate(acc = ifelse(is.na(acc), 0, acc))

  write_delim(CADC, "/Users/aleksander/Documents/VSP/PHEMTest/CADC/CADC.csv", delim=";", col_names = FALSE)
}

# ==== Acceleration, unit&computation test ====
{
  # Sumo has a different computation method for the accelerations. I tested it, but has much worse results.

  compute_emission_difference(glue("{diff_path}/diff_petrol_output_fixedIntervalLength_60.csv"), glue("/Users/aleksander/Documents/VSP/PHEMTest/CADC/sumo_petrol_output_pl5.csv") )
  compute_emission_difference(glue("{diff_path}/diff_petrol_output_fixedIntervalLength_60.csv"), glue("/Users/aleksander/Documents/VSP/PHEMTest/CADC/sumo_petrol_output_a.csv") )

  manually_computed <- read_delim("/Users/aleksander/Documents/VSP/PHEMTest/CADC/sumo_petrol_output.csv",
                                  delim = ";",
                                  col_names = c("time", "velocity", "acceleration", "slope", "CO", "CO2", "HC", "PMx", "NOx", "fuel", "electricity"),
                                  col_types = cols(
                                    time = col_integer(),
                                    velocity = col_double(),
                                    acceleration = col_double(),
                                    slope = col_double(),
                                    CO = col_double(),
                                    CO2 = col_double(),
                                    HC = col_double(),
                                    PMx = col_double(),
                                    NOx = col_double(),
                                    fuel = col_double(),
                                    electricity = col_double()))%>%
    pivot_longer(cols = c("CO", "CO2", "HC", "PMx", "NOx"), names_to = "component", values_to="value")

  # SUMO computation removes first row, so we need to do this as well
  # manually_computed <- manually_computed[-1,]

  sumo_computed <- read_delim("/Users/aleksander/Documents/VSP/PHEMTest/CADC/sumo_petrol_output_a.csv",
                                  delim = ";",
                                  col_names = c("time", "velocity", "acceleration", "slope", "CO", "CO2", "HC", "PMx", "NOx", "fuel", "electricity"),
                                  col_types = cols(
                                    time = col_integer(),
                                    velocity = col_double(),
                                    acceleration = col_double(),
                                    slope = col_double(),
                                    CO = col_double(),
                                    CO2 = col_double(),
                                    HC = col_double(),
                                    PMx = col_double(),
                                    NOx = col_double(),
                                    fuel = col_double(),
                                    electricity = col_double()))%>%
    pivot_longer(cols = c("CO", "CO2", "HC", "PMx", "NOx"), names_to = "component", values_to="value")

  all <- inner_join(sumo_computed, manually_computed, by=join_by("time","component")) %>%
    group_by(component) %>%
    summarize(absolute.x = sum(value.x), absolute.y = sum(value.y)) %>%
    mutate(deviation = absolute.x/absolute.y)


  # Check how large the difference is
  manually_computed

}