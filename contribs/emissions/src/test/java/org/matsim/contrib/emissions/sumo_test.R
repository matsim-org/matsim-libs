library(tidyverse)

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
  diff_out <- read_csv("contribs/emissions/test/output/org/matsim/contrib/emissions/PHEMTest/test/diff_petrol_out.csv")

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

# ==== Filter out pass.veh ===
{
  path_in <- "D:/Projects/VSP/MATSim/PHEM/hbefa/EFA_HOT_Subsegm_detailed_Car_Aleks.csv"
  path_out <- "D:/Projects/VSP/MATSim/PHEM/hbefa/EFA_HOT_Subsegm_detailed_Car_Aleks_filtered.csv"

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
