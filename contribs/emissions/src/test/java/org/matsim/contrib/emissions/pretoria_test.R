{
  library(tidyverse)
  library(patchwork)
  library(scales)
  library(glue)

  # ==== Paths to ressources ====
  pretoria_path <- "/Users/aleksander/Documents/VSP/PHEMTest/Pretoria"
}

# ==== General Pretoria Analysis ====
{
  vehicle <- "FIGO_TECHAVG"
  method <- "InterpolationFraction"

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

  vehicle <- "FIGO_TECHAVG"
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

  vehicle <- "FIGO_TECHAVG"
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

# ==== Fix HBEFA V1 ====
{
  hbefa <- read_csv2("/Users/aleksander/Documents/VSP/PHEMTest/hbefa/EFA_HOT_Concept_Aleks_V1.csv") %>%
    filter(grepl("PC|HGV", EmConcept)) %>%
    mutate(Technology = word(EmConcept, 2)) %>%
    filter(Technology == "D" | Technology == "P") %>%
    mutate(Technology = ifelse(Technology == "D", "diesel", Technology)) %>%
    mutate(Technology = ifelse(Technology == "P", "petrol (4S)", Technology)) %>%
    mutate(SizeClasse = "average")

  hbefa.tavg <- hbefa %>%
    mutate(w = `%OfEmConcept`) %>%
    group_by(Case, VehCat, Year, TrafficScenario, Component, RoadCat, TrafficSit, Gradient, Technology) %>%
    summarize(EmConcept = "average", SizeClasse = "average", V = weighted.mean(V, w), EFA = weighted.mean(EFA, w))

  hbefa <- hbefa %>%
    select(Case, VehCat, Year, TrafficScenario, Component, RoadCat, TrafficSit, Gradient, Technology, EmConcept, SizeClasse, V, EFA )

  hbefa <- rbind(hbefa, hbefa.tavg)

  write_delim(hbefa, "/Users/aleksander/Documents/VSP/PHEMTest/hbefa/EFA_HOT_Concept_Aleks_V1.1.csv", delim = ";")
}

# ==== Gradient Problem analysis ====
{
  # Background: After implementing slopes into the emission model, it turned out that S&G method delivers better results
  # than InterpolationFraction. However, the results make no sense, as InterpolationFraction overestimates in highways.
  # InterpolationFraction was introduced to NOT overestimate on highway, therefore there has to be a more fundamental
  # problem.

  # Error Histograms
  {
    vehicle <- "FIGO_TECHAVG"

    pretoria_output.SG <- read_csv(glue("{pretoria_path}/output_{vehicle}_StopAndGoFraction.csv")) %>%
      filter(linkId != 6555 & linkId != "cold") %>%
      mutate(ERR_CO = CO_MATSim - CO_pems, ERR_CO2 = CO2_MATSim - CO2_pems, ERR_NOx = NOx_MATSim - NOx_pems) %>%
      pivot_longer(c("ERR_CO", "ERR_CO2", "ERR_NOx"), names_to = "component", values_to = "error") %>%
      mutate(method = "StopAndGoFraction")

    pretoria_output.Int <- read_csv(glue("{pretoria_path}/output_{vehicle}_InterpolationFraction.csv")) %>%
      filter(linkId != 6555 & linkId != "cold") %>%
      mutate(ERR_CO = CO_MATSim - CO_pems, ERR_CO2 = CO2_MATSim - CO2_pems, ERR_NOx = NOx_MATSim - NOx_pems) %>%
      pivot_longer(c("ERR_CO", "ERR_CO2", "ERR_NOx"), names_to = "component", values_to = "error") %>%
      mutate(method = "InterpolationFraction")

    network_information <- read_csv("/Users/aleksander/Documents/VSP/PHEMTest/Pretoria/networkInformation.csv") %>%
      separate(roadType, c("Region", "RoadType", "VClass"), sep="/")

    d <- pretoria_output.SG %>%
      rbind(pretoria_output.Int) %>%
      inner_join(network_information, by = "linkId")

    p1 <- ggplot(d) +
      stat_summary_bin(aes(x=gradient, y=error, color=method), fun = mean, binwidth=0.05, geom="line") +
      facet_wrap(~component, scales="free") +
      ggtitle("Error and distribution of error per link by gradient")

    p2 <- ggplot(d) +
      geom_histogram(aes(x=gradient), binwidth = 0.05) +
      facet_wrap(~component, scales="free")

    p1 / p2

    p3 <- ggplot(d) +
      stat_summary_bin(aes(x=freespeed, y=error, color=method), fun = mean, binwidth=1, geom="line") +
      facet_wrap(~component, scales="free") +
      theme_minimal() +
      theme(text = element_text(size=18)) +
      ggtitle("Absolute error and distribution of error per link by average speed")

    p4 <- ggplot(d) +
      geom_histogram(aes(x=freespeed), binwidth = 1) +
      facet_wrap(~component, scales="free") +
      theme_minimal() +
      theme(text = element_text(size=18))

    p3 / p4

    ggsave(glue("/Users/aleksander/Documents/VSP/PHEMTest/Pretoria/PAPER/freespeed_err.png"),
           width = 30,
           height = 20,
           dpi = 300)
  }

  {
    pretoria_output <- read_csv(glue("{pretoria_path}/output_{vehicle}_{method}.csv"))

    network_information <- read_csv("/Users/aleksander/Documents/VSP/PHEMTest/Pretoria/networkInformation.csv") %>%
      separate(roadType, c("Region", "RoadType", "VClass"), sep="/")

    hbefa_det <- read_delim("/Users/aleksander/Documents/VSP/PHEMTest/hbefa/EFA_HOT_Concept_Aleks_Average_V1.1.csv", delim = ";")

    d <- hbefa_det %>%
      separate_wider_delim(TrafficSit, "/", names=c("Region", "RoadType", "VClass", "TrafficSituation")) %>%
      filter(VehCat == "pass. car" &
                 Technology == "petrol (4S)" &
                 EmConcept == "average" &
                 Component == "CO" &
                 Region == "URB" &
               ( VClass == 50 | VClass == 60 | VClass == 80 |VClass == 100 | VClass == 120) &
               ( RoadType == "MW-Nat." | RoadType == "Distr" | RoadType == "MW_City" | RoadType == "Local")) %>%
      filter(!startsWith(Gradient, "+/-")) %>%
      mutate(Gradient = Gradient %>% str_remove("\\+/-") %>% str_remove("%") %>% as.numeric)

    d_ribbon <- d %>%
      filter(TrafficSituation %in% c("St+Go", "Freeflow")) %>%
      select(Gradient, EFA, TrafficSituation, RoadType, VClass) %>%
      pivot_wider(names_from = TrafficSituation, values_from = EFA)

    colors <- c("#17d2a4", "#70aef4", "#88e72f", "#f1e843", "#eb4949", "#eb49ad")

    ggplot(d) +
      geom_line(aes(x=as.numeric(Gradient), y=EFA, color=TrafficSituation)) +
      geom_ribbon(
        data = d_ribbon,
        aes(
          x = Gradient,
          ymin = Freeflow,
          ymax = `St+Go`
        ),
        fill = "red",
        alpha = 0.2
      ) +
      # geom_point(aes(x=as.numeric(Freespeed), y=EFA, color=TrafficSituation)) +
      facet_wrap(RoadType~as.numeric(VClass), scales = "free") +
      theme_minimal() +
      theme(text = element_text(size=12)) +
      scale_color_manual(values=colors) +
      # scale_x_continuous(breaks = seq(0, max(as.numeric(d$Freespeed)), by = 10)) +
      # labs(title=glue("Emissions for all HBEFA keys with: {tech}, {concept}, {component}")) +
      ggtitle("Emission development over gradient for CO") +
      xlab("Gradient (%)") +
      ylab("Emissions (g/km)")

    ggsave(glue("/Users/aleksander/Documents/VSP/PHEMTest/Pretoria/PAPER/gradient_development.png"),
           width = 10,
           height = 10,
           dpi = 300)
  }

}