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


