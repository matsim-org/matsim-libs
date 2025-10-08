{
  library(tidyverse)
  library(glue)

  # ==== Paths to ressources ====
  sumo_path <- "/Users/aleksander/Documents/VSP/PHEMTest/sumo"
  diff_path <- "/Users/aleksander/Documents/VSP/PHEMTest/diff"
  hbefa_path <- "/Users/aleksander/Documents/VSP/PHEMTest/hbefa"

}

# ==== Helper functions ====

# Reads in the csv from matsim at the given path and returns the datasheet and intervals from this sheet
# model_suffix: optional, adds a suffix to the 'model'-column which is just "MATSIM" by default
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
    mutate(segment = as.integer(segment), model = glue("{model}_{model_suffix}"))

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

# Reads in the csv format from SUMO at the given paths and returns the tidied datasheet
# model_suffix: optional, adds a suffix to the 'model'-column which is just "MATSIM" by default
read_sumo <- function(path, intervals, model_suffix = ""){
  d <- read_delim(path,
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
    mutate(model = glue("SUMO_{model_suffix}"), value=value/1000)

  return (d)
}

# Creates the default plot with all components
# Additional datasets, can be attached (however, the function assumes, that it is formatted correctly)
# Example: plot_main(extra_notes = "Test", data.TEST)
plot_main <- function(..., fuel = "petrol", segment_method = "fixedIntervalLength_60", extra_notes = ""){
  # Load data from MATSim
  # diff_out <- read_csv("contribs/emissions/test/input/org/matsim/contrib/emissions/PHEMTest/diff_petrol_ref.csv")
  r <- read_matsim(glue("{diff_path}/diff_{fuel}_output_{segment_method}.csv"), "")
  data.MATSIM <- r[[1]]
  intervals <- r[[2]]

  # Load data from SUMO with PHEMLight and summarize for each interval
  data.SUMO_PHEMLight <- read_sumo(glue("{sumo_path}/sumo_{fuel}_output.csv"), intervals, "PHEMLight")

  # Load data from SUMO with PHEMLight5 and summarize for each interval
  data.SUMO_PHEMLight5 <- read_sumo(glue("{sumo_path}/sumo_{fuel}_output_pl5.csv"), intervals, "PHEMLight5")

  # Append all datasets together
  data_list <- list(data.MATSIM, data.SUMO_PHEMLight, data.SUMO_PHEMLight5, ...)

  # recalc: gram -> gram per kilometer
  data <- do.call(rbind, data_list) %>%
    merge(intervals, by="segment") %>%
    mutate(gPkm = value/lengths)

  # Generate colors (first 3 always the same)
  colors <- c("#d21717", "#17d2a4", "#7d23cc")
  extra <- max(0, length(data_list)-3)
  all_colors <- c(colors, hcl.colors(extra, palette = "viridis"))
  names(all_colors) <- c("MATSIM_", "SUMO_PHEMLight", "SUMO_PHEMLight5", unlist(lapply(list(...), function(df) unique(df$model))))

  # Bar-Plot
  ggplot(data) +
  geom_bar(aes(x=segment, y=gPkm, fill=model), stat="identity", position="dodge") +
  scale_fill_manual(values=all_colors) +
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
  scale_color_manual(values=all_colors) +
  facet_wrap(~component, scales="free") +
  ylab("emissions in g/km") +
  theme(text = element_text(size=18)) +
  ggtitle(glue("Comparison across WLTP-cycle for {fuel}"),
          subtitle=glue("{segment_method} {extra_notes}")) +
  theme_minimal()

}

# Takes a MATSim and SUMO output and compares the total
compute_absolute_emission_difference <- function(path){
  # Load
}

# Takes a MATSim and SUMO output and compares for each component:
# 1. the average difference per segment (%r)
# 2. the difference of the sum over all segments (%a)
# Essentially, %r is a relative measure where all segments are weighted equally,
# while %a is the difference between the integrals.
# Example: compute_emission_difference(glue("{diff_path}/diff_petrol_output_fixedIntervalLength_60.csv"), glue("{sumo_path}/sumo_petrol_output.csv") )
compute_emission_difference <- function(path_matsim, path_sumo){
  # Load data from MATSim
  r <- read_matsim(path_matsim, "")
  data.MATSIM <- r[[1]]
  intervals <- r[[2]]

  # Load data from SUMO
  data.SUMO_PHEMLight <- read_sumo(path_sumo, intervals, "PHEMLight")

  cat(sep = "\n")

  relative <- merge(data.MATSIM, data.SUMO_PHEMLight, by = c("segment", "component")) %>%
    mutate(relative = ((value.x/value.y)-1)*100) %>%
    group_by(component) %>%
    summarize(percent_r = mean(relative)) %>%

    mutate(line = glue("{component} {percent_r} %r")) %>%
      pull(line) %>%
      cat(sep = "\n")

  cat(sep = "\n")

  absolute <- merge(data.MATSIM, data.SUMO_PHEMLight, by = c("segment", "component")) %>%
    group_by(component) %>%
    summarize(absolute.x = sum(value.x), absolute.y = sum(value.y)) %>%
    mutate(percent_a = absolute.x/absolute.y) %>%

    mutate(line = glue("{component} {percent_a} %a")) %>%
    pull(line) %>%
    cat(sep = "\n")

}
