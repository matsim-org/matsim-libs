{
  library(tidyverse)
  library(glue)

  # ==== Paths to ressources ====
  pretoria_path <- "/Users/aleksander/Documents/VSP/PHEMTest/Pretoria"
}

# ==== Pretoria Analysis ====
{
  vehicle <- "FIGO"

  pretoria_output <- read_csv(glue("/Users/aleksander/Documents/VSP/PHEMTest/Pretoria/output_{vehicle}.csv")) %>%
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
              NOx_MATSim = mean(NOx_MATSim), NOx_pems = mean(NOx_pems))

  # ggplot(pretoria_output) +
  #   geom_line(aes(x=n, y=CO_pems), color = "orange") +
  #   geom_line(aes(x=n, y=CO_MATSim), color = "red")

  100*sum(pretoria_output$CO_MATSim)/sum(pretoria_output$CO_pems)-100
  100*sum(pretoria_output$CO2_MATSim)/sum(pretoria_output$CO2_pems)-100
  100*sum(pretoria_output$NOx_MATSim)/sum(pretoria_output$NOx_pems)-100

}