{
  library(tidyverse)
  library(glue)

  # ==== Paths to ressources ====
  pretoria_path <- "/Users/aleksander/Documents/VSP/PHEMTest/Pretoria"
}

# ==== General Pretoria Analysis ====
{
  vehicle <- "FIGO"

  pretoria_output <- read_csv(glue("{pretoria_path}/output_{vehicle}.csv")) %>%
    filter(linkId != 6555) %>%
    mutate(n = row_number())

  pretoria_avg <- pretoria_output %>%
    group_by(tripId) %>%
    summarize(CO_MATSim = sum(CO_MATSim), CO_pems = sum(CO_pems),
              CO2_MATSim = sum(CO2_MATSim), CO2_pems = sum(CO2_pems),
              NOx_MATSim = sum(NOx_MATSim), NOx_pems = sum(NOx_pems)) %>%
    group_by() %>%
    summarize(CO_MATSim = mean(CO_MATSim), CO_pems = mean(CO_pems),
              CO2_MATSim = mean(CO2_MATSim), CO2_pems = mean(CO2_pems),
              NOx_MATSim = mean(NOx_MATSim), NOx_pems = mean(NOx_pems)) %>%
    mutate(CO_diff = 100*CO_MATSim/CO_pems-100,
           CO2_diff = 100*CO2_MATSim/CO2_pems-100,
           NOx_diff = 100*NOx_MATSim/NOx_pems-100)

  # ggplot(pretoria_output) +
  #   geom_line(aes(x=n, y=CO_pems), color = "orange") +
  #   geom_line(aes(x=n, y=CO_MATSim), color = "red")


}

# ==== Accumulative Plots ===
{
  #TODO
}

# ==== Segment Wise Pretoria Analysis ====
{
  # In the JWJ Paper, 3 segments were defined
  # A: Urban (from 28948 to 14100)
  # B: Freeway (from 11614 to 28906)
  # C: Steep, suburban (from waterkloof4_waterkloof5 to 37156)

  vehicle <- "FIGO"

  pretoria_output <- read_csv(glue("/Users/aleksander/Documents/VSP/PHEMTest/Pretoria/output_{vehicle}.csv")) %>%
    filter(linkId != 6555, segment != "none") %>%
    mutate(n = row_number())

  pretoria_segment_avg <- pretoria_output %>%
    group_by(tripId, segment) %>%
    summarize(CO_MATSim = sum(CO_MATSim), CO_pems = sum(CO_pems),
              CO2_MATSim = sum(CO2_MATSim), CO2_pems = sum(CO2_pems),
              NOx_MATSim = sum(NOx_MATSim), NOx_pems = sum(NOx_pems)) %>%
    group_by(segment) %>%
    summarize(CO_MATSim = mean(CO_MATSim), CO_pems = mean(CO_pems),
              CO2_MATSim = mean(CO2_MATSim), CO2_pems = mean(CO2_pems),
              NOx_MATSim = mean(NOx_MATSim), NOx_pems = mean(NOx_pems)) %>%
    mutate(CO_diff = 100*CO_MATSim/CO_pems-100,
           CO2_diff = 100*CO2_MATSim/CO2_pems-100,
           NOx_diff = 100*NOx_MATSim/NOx_pems-100)
}