{
  library(tidyverse)
  library(glue)

  # ==== Paths to ressources ====
  pretoria_path <- "/Users/aleksander/Documents/VSP/PHEMTest/Pretoria"
}

# ==== General Pretoria Analysis ====
{
  vehicle <- "RRV"
  method <- "StopAndGoFraction"

  pretoria_output <- read_csv(glue("{pretoria_path}/output_{vehicle}_{method}.csv")) %>%
    filter(linkId != 6555) %>%
    mutate(n = row_number())

  pretoria_avg <- pretoria_output %>%
    group_by(tripId, load) %>%
    summarize(CO_MATSim = sum(CO_MATSim), CO_pems = sum(CO_pems),
              CO2_MATSim = sum(CO2_MATSim), CO2_pems = sum(CO2_pems),
              NOx_MATSim = sum(NOx_MATSim), NOx_pems = sum(NOx_pems)) %>%
    group_by(load) %>%
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

# ==== Driving Style ====
{
  vehicle <- "FIGO"

  gps_input <- read_csv(glue("{pretoria_path}/data/public-{vehicle}.csv")) %>%
    mutate(gps_acc = lead(gps_speed) - gps_speed, veh_acc = lead(speed_vehicle) - speed_vehicle)

  gps_avg <- gps_input %>%
    group_by(driver) %>%
    summarize(veh_vel_avg = mean(speed_vehicle, na.rm = TRUE),
              veh_acc_avg = mean(veh_acc[veh_acc > 0], na.rm = TRUE),
              gps_vel_avg = mean(gps_speed, na.rm = TRUE),
              gps_acc_avg = mean(gps_acc[gps_acc > 0], na.rm = TRUE))

}

# == Driver Wise Pretoria Analysis ===
{
  # Three different drivers were participating:
  # 1: Matches the profile of an aggressive driver  (avg.vel.=40.53; avg.pos.acc.=2.59)
  # 2: Matches the profile of a moderate driver     (avg.vel.=40.19; avg.pos.acc.=2.38)
  # 3: Matches the profile of a passive driver      (avg.vel.=37.02; avg.pos.acc.=2.17)

  vehicle <- "FIGO"
  method <- "InterpolationFraction"

  pretoria_output <- read_csv(glue("{pretoria_path}/output_{vehicle}_{method}.csv")) %>%
    filter(linkId != 6555) %>%
    mutate(n = row_number())

  pretoria_driver_avg <- pretoria_output %>%
    group_by(tripId, load, driver) %>%
    summarize(CO_MATSim = sum(CO_MATSim), CO_pems = sum(CO_pems),
              CO2_MATSim = sum(CO2_MATSim), CO2_pems = sum(CO2_pems),
              NOx_MATSim = sum(NOx_MATSim), NOx_pems = sum(NOx_pems)) %>%
    group_by(load, driver) %>%
    summarize(CO_MATSim = mean(CO_MATSim), CO_pems = mean(CO_pems),
              CO2_MATSim = mean(CO2_MATSim), CO2_pems = mean(CO2_pems),
              NOx_MATSim = mean(NOx_MATSim), NOx_pems = mean(NOx_pems)) %>%
    mutate(CO_diff = 100*CO_MATSim/CO_pems-100,
           CO2_diff = 100*CO2_MATSim/CO2_pems-100,
           NOx_diff = 100*NOx_MATSim/NOx_pems-100)
}

# ==== Segment Wise Pretoria Analysis ====
{
  # In the JWJ Paper, 3 segments were defined
  # A: Urban (from 28948 to 14100)
  # B: Freeway (from 11614 to 28906)
  # C: Steep, suburban (from waterkloof4_waterkloof5 to 37156)

  vehicle <- "FIGO"
  method <- "InterpolationFraction"

  pretoria_output <- read_csv(glue("/Users/aleksander/Documents/VSP/PHEMTest/Pretoria/output_{vehicle}_{method}.csv")) %>%
    filter(linkId != 6555, segment != "none") %>%
    mutate(n = row_number())

  pretoria_segment_avg <- pretoria_output %>%
    group_by(tripId, load, segment) %>%
    summarize(CO_MATSim = sum(CO_MATSim), CO_pems = sum(CO_pems),
              CO2_MATSim = sum(CO2_MATSim), CO2_pems = sum(CO2_pems),
              NOx_MATSim = sum(NOx_MATSim), NOx_pems = sum(NOx_pems)) %>%
    group_by(load, segment) %>%
    summarize(CO_MATSim = mean(CO_MATSim), CO_pems = mean(CO_pems),
              CO2_MATSim = mean(CO2_MATSim), CO2_pems = mean(CO2_pems),
              NOx_MATSim = mean(NOx_MATSim), NOx_pems = mean(NOx_pems)) %>%
    mutate(CO_diff = 100*CO_MATSim/CO_pems-100,
           CO2_diff = 100*CO2_MATSim/CO2_pems-100,
           NOx_diff = 100*NOx_MATSim/NOx_pems-100)
}

# ==== Accumulative Plots ====
{
  #TODO
}

# ==== PHEM C-Route Test Preparation ====
{
  vehicle <- "FIGO"

  cRoute <- read_csv(glue("{pretoria_path}/data/public-{vehicle}.csv")) %>%
    mutate(acc = lead(speed_vehicle) - speed_vehicle, n = row_number()) %>%
    mutate(acc = ifelse(is.na(acc), 0, acc)) %>%
    select(n, speed_vehicle, acc, trip)

  write_delim(cRoute, glue("{pretoria_path}/phem-in/phem-in-{vehicle}.csv"), delim=";", col_names=FALSE)
}

# ==== PHEM C-Route Test Analysis ====
{
  vehicle <- "FIGO"

  phem_avg <- read_delim(glue("{pretoria_path}/phem-out/phem-out-{vehicle}.csv"), delim = ";", col_names = c("n", "velocity", "acceleration", "slope", "CO", "CO2", "HC", "PMx", "NOx", "fuel", "electricity")) %>%
    merge(cRoute, by="n") %>%
    group_by(trip) %>%
    summarize(CO = sum(CO)/1000, CO2 = sum(CO2)/1000, NOx = sum(NOx)/1000) %>%
    group_by() %>%
    summarize(CO_phem = mean(CO), CO2_phem = mean(CO2), NOx_phem = mean(NOx))


  pretoria_avg <- read_csv(glue("{pretoria_path}/output_{vehicle}.csv")) %>%
    filter(linkId != 6555) %>%
    group_by(tripId) %>%
    summarize(CO_MATSim = sum(CO_MATSim), CO_pems = sum(CO_pems),
              CO2_MATSim = sum(CO2_MATSim), CO2_pems = sum(CO2_pems),
              NOx_MATSim = sum(NOx_MATSim), NOx_pems = sum(NOx_pems)) %>%
    summarize(CO_MATSim = mean(CO_MATSim), CO_pems = mean(CO_pems),
              CO2_MATSim = mean(CO2_MATSim), CO2_pems = mean(CO2_pems),
              NOx_MATSim = mean(NOx_MATSim), NOx_pems = mean(NOx_pems))

  all_avg <- merge(phem_avg, pretoria_avg) %>%
    pivot_longer(cols = c("CO_phem",
                          "CO2_phem",
                          "NOx_phem",
                          "CO_MATSim",
                          "CO2_MATSim",
                          "NOx_MATSim",
                          "CO_pems",
                          "CO2_pems",
                          "NOx_pems"), names_to="model", values_to="value") %>%
    separate(model, c("component", "model"), "_")

  ggplot(all_avg) +
    geom_bar(aes(x=model, y=value, fill=model), stat="identity") +
    facet_wrap(~component, scales="free")

}

# ==== Kalman Filter Diagram ====
{
  a <- read_csv("/Users/aleksander/Documents/VSP/PHEMTest/Pretoria/kalman/outFIGO.csv")

  a_f <- a %>% filter(time > 800, time < 1000)

  ggplot(a_f) +
    geom_line(aes(x=time, y=accDist), color="red") +
    geom_line(aes(x=time, y=naiveAccDist), color="blue") +
    geom_line(aes(x=time, y=projectedAccDist), color="green")
}