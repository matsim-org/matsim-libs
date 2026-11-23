# ==========================================================
# Multimodal person-based accessibility
# Accessibility analysis script
# ==========================================================

suppressPackageStartupMessages({
  # install.packages("sf")
  library(dplyr)
  library(readr)
  library(sf)
  library(tidyr)
  library(ggplot2)
})

berlin_only <- TRUE  # TRUE keeps only people whose homes are inside Berlin.

# ==========================================================
# Directories
# ==========================================================

base_dir     <- "../public-svn/matsim/scenarios/countries/de/berlin/projects/autofrei/_dev/2026-11-19-v6.4-spa-uni-jobcenter-NoMonConstant"
input_dir    <- base_dir

# Berlin district shapefile and S-Bahn ring: static files kept in the repo,
# independent of where the run folders are.
shape_dir <- path.expand(
  "~/git/matsim-libs/contribs/accessibility/src/main/R/THESIS/input/Berlin shapefiles"
)

# Run folders from the cluster, one per scenario, as written by
# run-person-based-acc-berlin-<scenario>.sh: <date>[a-z]-<scenario>/<opportunity>/...
# Change the folder names here when you use results from another day or rerun.
run_dirs <- list(
  base   = file.path(input_dir, "2026-11-19-base"),
  policy = file.path(input_dir, "2026-11-19-policy")
)

# Results go to output/<date>-output/, named after the policy run folder,
# e.g. input 2026-11-16-policy -> output/2026-11-16-output/<opportunity>/
run_label   <- sub("-policy$", "", basename(run_dirs$policy))
output_root <- file.path(base_dir, "output", paste0(run_label, "-output-berlinfiltered"))

missing_run_dirs <- Filter(function(d) !dir.exists(d), run_dirs)
if (length(missing_run_dirs) > 0) {
  stop("Run folder(s) not found: ", paste(unlist(missing_run_dirs), collapse = ", "))
}

# Opportunities to analyse in one go. The whole analysis below runs once per
# entry. Any of: "hospital", "spa", "baeder", "jobCenter".
# Use a single entry, e.g. c("baeder"), to run just one.
opportunities <- c("university", "spa", "jobCenter")

# Metadata for every POI type written by RunPersonBasedAccBerlin.
# The list names are the input/output folder names; `type` is the value in
# locations.csv (the Java OpportunityType activity type).
poi_meta <- list(
  hospital = list(
    type = "hospital", label = "Hospital",
    color = "#1f78b4", shape = 16, label_points = FALSE
  ),
  spa = list(
    type = "spa", label = "Spa",
    color = "#e31a1c", shape = 17, label_points = TRUE
  ),
  university = list(
    type = "university", label = "Public University",
    color = "#33a02c", shape = 15, label_points = TRUE
  ),
  jobCenter = list(
    type = "jobcenter", label = "Job center",
    color = "#ff7f00", shape = 18, label_points = TRUE
  )
)

unknown_opportunities <- setdiff(opportunities, names(poi_meta))
if (length(unknown_opportunities) > 0) {
  stop(
    "Unknown opportunity '", paste(unknown_opportunities, collapse = "', '"),
    "'. Choose from: ", paste(names(poi_meta), collapse = ", ")
  )
}

poi_types  <- vapply(poi_meta, `[[`, character(1), "type")
poi_labels <- vapply(poi_meta, `[[`, character(1), "label")
poi_colors <- vapply(poi_meta, `[[`, character(1), "color")
poi_shapes <- vapply(poi_meta, `[[`, numeric(1), "shape")

beta <- 1

# ==========================================================
# Read input data
# ==========================================================

# Income is a continuous value per person. It is grouped into the same nine
# brackets used in the earlier analysis. Each bracket is labelled with its
# upper bound: "500" = up to 500, "900" = 501-900, ..., "4600" = 3601-4600,
# and "5600" = everything above 4600.
income_breaks <- c(-Inf, 500, 900, 1500, 2000, 2600, 3000, 3600, 4600, Inf)
income_labels <- c("500", "900", "1500", "2000", "2600", "3000", "3600", "4600", "5600")

income_group <- function(income) {
  cut(income, breaks = income_breaks, labels = income_labels,
      right = TRUE, ordered_result = TRUE)
}  


clean_df <- function(df){
  df %>%
    mutate(
      personId = as.character(personId),
      age = as.numeric(age),
      sex = factor(
        sex,
        levels = c("f", "m")
      ),
      accessibility = as.numeric(accessibility),
      income_raw = as.numeric(income),
      income = income_group(income_raw)
    )
}

berlin_sf <- st_read(file.path(shape_dir, "Berlin_Bezirke.shp"), quiet = TRUE)

if (is.na(st_crs(berlin_sf))) st_crs(berlin_sf) <- 25832
if (st_crs(berlin_sf)$epsg != 25832) {
  berlin_sf <- st_transform(berlin_sf, 25832)
}

ring_sf <- st_read(
  file.path(shape_dir, "Berlin_sbahn_ring.geojson"),
  quiet = TRUE
)
if (is.na(st_crs(ring_sf))) st_crs(ring_sf) <- 4326
ring_sf <- st_transform(ring_sf, 25832)

read_person_data <- function(path) {
  people <- read_csv(path, show_col_types = FALSE)
  homes <- st_as_sf(people, coords = c("homeX", "homeY"), crs = 25832, remove = FALSE)
  districts <- st_intersects(homes, berlin_sf)
  people$district <- berlin_sf$Gemeinde_n[vapply(districts, function(matches) {
    matches[1L]
  }, integer(1))]
  people$within <- if_else(lengths(st_intersects(homes, ring_sf)) > 0,
                           "Within Ring", "Outside Ring")
  if (berlin_only) {
    people <- people[lengths(districts) > 0, ]
  }
  people
}

read_acc <- function(file){
  read_person_data(file) %>%
    clean_df()
}

# ==========================================================
# Per-opportunity analysis
# ==========================================================
# Everything from here to the end of the file runs once per opportunity.
# If one opportunity fails (e.g. an input file is missing), the error is
# reported and the loop continues with the next one.
# To step through a single opportunity by hand: set e.g. opportunity <- "baeder"
# and run the lines inside the loop.

failed_opportunities <- character(0)

for (opportunity in opportunities) {
message("\n===== Opportunity: ", opportunity, " =====")
opportunity_ok <- tryCatch({

# input folder of this opportunity, per scenario
scenario_dirs <- lapply(run_dirs, function(d) file.path(d, opportunity))
output_dir   <- file.path(output_root, opportunity)
dir.create(output_dir, recursive = TRUE, showWarnings = FALSE)
poi <- poi_meta[[opportunity]]

# Accessibility results for the selected POI (current run)
twalk_df <- read_acc(file.path(scenario_dirs$base, "pbAcc_teleportedWalk_base_rawSum.csv"))
pt_df <- read_acc(file.path(scenario_dirs$base, "pbAcc_pt_base_rawSum.csv"))
car_base_df <- read_acc(file.path(scenario_dirs$base, "pbAcc_car_base_rawSum.csv"))
car_policy_df <- read_acc(file.path(scenario_dirs$policy, "pbAcc_car_policy_rawSum.csv"))

# Income range and number of persons per quintile, for reporting
income_quintiles <- twalk_df %>%
  group_by(income) %>%
  summarise(
    min_income = min(income_raw),
    max_income = max(income_raw),
    persons = n(),
    .groups = "drop"
  )
write_csv(income_quintiles, file.path(output_dir, "income_quintiles.csv"))

# Optional: base car run without income integration. If the file is missing,
# the income-integration table and plots are skipped for this opportunity.
no_income_file <- file.path(scenario_dirs$base, "pbAcc_car_no_income.csv")
has_no_income  <- file.exists(no_income_file)
if (has_no_income) {
  car_base_no_income_df <- read_acc(no_income_file)
} else {
  message("No pbAcc_car_no_income.csv for '", opportunity, "': skipping income-integration outputs.")
}

# Legacy spa-based reference run (kept for later refactoring)
# twalk_df <- read_acc(file.path(input_dir, "pbAcc_teleportedWalk_base_rawSum.csv"))
# pt_df <- read_acc(file.path(input_dir, "pbAcc_pt_base_rawSum.csv"))
# car_base_df <- read_acc(file.path(input_dir, "pbAcc_car_base_rawSum.csv"))
# car_policy_df <- read_acc(file.path(input_dir, "pbAcc_car_policy_rawSum.csv"))
# car_base_no_income_df <- read_acc(file.path(input_dir, "pbAcc_car_no_income.csv"))

# ==========================================================
# Merge accessibility datasets
# ==========================================================

df <- twalk_df %>%
  rename(twalk_rawSum = accessibility) %>%
  left_join(
    pt_df %>%
      select(personId, pt_rawSum = accessibility),
    by = "personId"
  ) %>% 
  left_join(
    car_base_df %>%
      select(personId, car_base_rawSum = accessibility),
    by = "personId"
  ) %>%
  left_join(
    car_policy_df %>%
      select(personId, car_policy_rawSum = accessibility),
    by = "personId"
  )

if (has_no_income) {
  df <- df %>%
    left_join(
      car_base_no_income_df %>%
        select(personId, car_base_logSum_no_income = accessibility),
      by = "personId"
    )
} else {
  # keep the column so the rest of the script runs unchanged (values are NA)
  df$car_base_logSum_no_income <- NA_real_
}

df <- df %>%
  relocate(
    twalk_rawSum,
    pt_rawSum,
    car_base_rawSum,
    car_policy_rawSum,
    car_base_logSum_no_income,
    .after = homeY
  )

# ==========================================================
# Order Berlin districts
# ==========================================================

df_acc <- df

df_acc$district <- factor(
  df_acc$district,
  levels = c(
    "Mitte",
    "Friedrichshain-Kreuzberg",
    "Pankow",
    "Charlottenburg-Wilmersdorf",
    "Spandau",
    "Steglitz-Zehlendorf",
    "Tempelhof-Schöneberg",
    "Neukölln",
    "Treptow-Köpenick",
    "Marzahn-Hellersdorf",
    "Lichtenberg",
    "Reinickendorf"
  ),
  ordered = TRUE
)



# ==========================================================
# Accessibility calculations
# ==========================================================

df_acc <- df_acc %>%
  mutate(
    
    mm_logSum_base = log(
      twalk_rawSum +
        pt_rawSum +
        car_base_rawSum
    ) / beta,
    
    mm_logSum_policy = log(
      twalk_rawSum +
        pt_rawSum +
        car_policy_rawSum
    ) / beta,
    
    mm_delta =
      mm_logSum_policy -
      mm_logSum_base,
    
    twalk_logSum =
      log(twalk_rawSum) / beta,
    
    pt_logSum =
      log(pt_rawSum) / beta,
    
    car_logSum_base =
      log(car_base_rawSum) / beta,
    
    car_logSum_policy =
      log(car_policy_rawSum) / beta,
    
    car_delta =
      car_logSum_policy -
      car_logSum_base,
    
    base_car_delta_incomeFactor =
      car_logSum_base - car_base_logSum_no_income,
    
    outcome = case_when(
      mm_delta > 0  ~ "Winner",
      mm_delta < 0  ~ "Loser",
      TRUE          ~ "No change"
    )
  )

# ==========================================================
# Final column order
# ==========================================================

df_acc <- df_acc %>%
  relocate(
    district,
    .after = homeY
  ) %>%
  select(
    personId,
    age,
    sex,
    income,
    
    homeX,
    homeY,
    district,
    within,
    
    twalk_rawSum,
    pt_rawSum,
    car_base_rawSum,
    car_policy_rawSum,

    twalk_logSum,
    pt_logSum,
    car_logSum_base,
    car_logSum_policy,

    car_delta,
    car_base_logSum_no_income,
    base_car_delta_incomeFactor,
    
    mm_logSum_base,
    mm_logSum_policy,
    mm_delta,
    
    outcome
  )

# ==========================================================
# Export
# ==========================================================

write_csv(
  df_acc,
  file.path(output_dir, "acc.csv")
)

# ==========================================================
# Plots
# ==========================================================

# uniform plot formatting
theme_set(
  theme_gray() +
    theme(
      axis.title.x = element_text(face = "bold"),
      axis.title.y = element_text(face = "bold")
    )
)

# income integration (only if the no-income run is available)
if (has_no_income) {

# summary table income integration
table_summary_income_integration <- df_acc %>%
  group_by(income) %>%
  summarise(
    mean_no_income = round(mean(car_base_logSum_no_income, na.rm = TRUE), 2),
    mean_income   = round(mean(car_logSum_base, na.rm = TRUE), 2),
    mean_delta    = round(mean(base_car_delta_incomeFactor, na.rm = TRUE), 2),
    median_no_income = round(median(car_base_logSum_no_income, na.rm = TRUE), 2),
    median_income   = round(median(car_logSum_base, na.rm = TRUE), 2),
    median_delta  = round(median(base_car_delta_incomeFactor, na.rm = TRUE), 2),
    sd_delta      = round(sd(base_car_delta_incomeFactor, na.rm = TRUE), 2),
    .groups = "drop"
  )
table_summary_income_integration
write_csv(table_summary_income_integration, file.path(output_dir,"table_summary_income_integration.csv"))

# scatter base car with/without income
scatter_car_acc_income <- ggplot (df_acc, aes(car_base_logSum_no_income, car_logSum_base)) +
  geom_point(alpha = 0.3) +
  geom_abline(slope = 1, intercept = 0, color = "red", linewidth = 1) +
  labs(
    y = "Car accessibility with income integration (utils)",
    x = "Car accessibility without income integration (utils)"
  ) #
ggsave(file.path(output_dir, "scatter_car_acc_income.png"), scatter_car_acc_income, dpi = 300, width = 8, height = 5)

# boxplot Δ base car accessibility by income (income integration)
boxplot_base_car_deltaacc_by_income <- ggplot(df_acc, aes(income, base_car_delta_incomeFactor)) +
  geom_boxplot(outlier.alpha = 0.3) +
  stat_summary(fun = mean, geom = "point", shape = 23, fill = "white", size = 2.5, na.rm = TRUE) +
  labs(
    x = "Income group",
    y = "Car accessibility change (utils)"
  )
ggsave(file.path(output_dir, "boxplot_base_car_deltaacc_by_income.png"), boxplot_base_car_deltaacc_by_income, dpi = 300, width = 8, height = 5)

} # end income integration

# scatter base car & mm accessibility
scatter_car_mm_acc_base <- ggplot(df_acc, aes(car_logSum_base, mm_logSum_base)) +
  geom_point(alpha = 0.3) +
  geom_abline(slope = 1, intercept = 0, color = "red", linewidth = 1) +
  labs(
    x = "Car accessibility (utils)",
    y = "Multimodal accessibility (utils)"
  )

ggsave(file.path(output_dir, "scatter_car_mm_acc_base.png"), scatter_car_mm_acc_base, dpi = 300, width = 8, height = 5)

# Overall accessibility summary
summary_accessibility <- tibble(
  `Accessibility measure` = c(
    "Walking",
    "Public transport",
    "Car",
    "Multimodal"),
  
  `Base mean` = c(
    mean(df_acc$twalk_logSum, na.rm = TRUE),
    mean(df_acc$pt_logSum, na.rm = TRUE),
    mean(df_acc$car_logSum_base, na.rm = TRUE),
    mean(df_acc$mm_logSum_base, na.rm = TRUE)),
  
  `Policy mean` = c(
    NA_real_,
    NA_real_,
    mean(df_acc$car_logSum_policy, na.rm = TRUE),
    mean(df_acc$mm_logSum_policy, na.rm = TRUE)),
  
  `Mean Δ` = c(
    NA_real_,
    NA_real_,
    mean(df_acc$car_delta, na.rm = TRUE),
    mean(df_acc$mm_delta, na.rm = TRUE)),
  
  `Base median` = c(
    median(df_acc$twalk_logSum, na.rm = TRUE),
    median(df_acc$pt_logSum, na.rm = TRUE),
    median(df_acc$car_logSum_base, na.rm = TRUE),
    median(df_acc$mm_logSum_base, na.rm = TRUE)),
  
  `Policy median` = c(
    NA_real_,
    NA_real_,
    median(df_acc$car_logSum_policy, na.rm = TRUE),
    median(df_acc$mm_logSum_policy, na.rm = TRUE)),
  
  `Median Δ` = c(
    NA_real_,
    NA_real_,
    median(df_acc$car_delta, na.rm = TRUE),
    median(df_acc$mm_delta, na.rm = TRUE)),
  
  `SD of Δ` = c(
    NA_real_,
    NA_real_,
    sd(df_acc$car_delta, na.rm = TRUE),
    sd(df_acc$mm_delta, na.rm = TRUE)),
  
  `Improved (%)` = c(
    NA_real_,
    NA_real_,
    mean(df_acc$car_delta > 0, na.rm = TRUE) * 100,
    mean(df_acc$mm_delta > 0, na.rm = TRUE) * 100),
  
  `Worsened (%)` = c(
    NA_real_,
    NA_real_,
    mean(df_acc$car_delta < 0, na.rm = TRUE) * 100,
    mean(df_acc$mm_delta < 0, na.rm = TRUE) * 100),
  
  `No change (%)` = c(
    100,
    100,
    mean(df_acc$car_delta == 0, na.rm = TRUE) * 100,
    mean(df_acc$mm_delta == 0, na.rm = TRUE) * 100)) %>%
  mutate(
    across(where(is.numeric), ~ round(.x, 2))
  )
summary_accessibility
write_csv(
  summary_accessibility,
  file.path(output_dir, "summary_accessibility.csv")
)

# Accessibility summary by income group
summary_accessibility_income <- df_acc %>%
  group_by(income) %>%
  summarise(
    Population = n(),
    
    `Base mean` = mean(mm_logSum_base, na.rm = TRUE),
    `Policy mean` = mean(mm_logSum_policy, na.rm = TRUE),
    `Mean Δ` = mean(mm_delta, na.rm = TRUE),
    
    `Base median` = median(mm_logSum_base, na.rm = TRUE),
    `Policy median` = median(mm_logSum_policy, na.rm = TRUE),
    `Median Δ` = median(mm_delta, na.rm = TRUE),
    
    `SD of Δ` = sd(mm_delta, na.rm = TRUE),
    
    .groups = "drop"
  ) %>%
  mutate(
    across(where(is.numeric), ~ round(.x, 2))
  )
summary_accessibility_income
# Export
write_csv(
  summary_accessibility_income,
  file.path(output_dir, "summary_accessibility_by_income.csv")
)

# bar median accessibility by income, mode & scenario
medianacc_by_income_mode_scenario <- df_acc %>%
  select(
    personId, income,
    twalk_logSum,
    pt_logSum,
    car_logSum_base,
    car_logSum_policy,
    mm_logSum_base,
    mm_logSum_policy
  ) %>%
  pivot_longer(
    cols = c(
      twalk_logSum,
      pt_logSum,
      car_logSum_base,
      car_logSum_policy,
      mm_logSum_base,
      mm_logSum_policy
    ),
    names_to = "accessibility_type",
    values_to = "accessibility"
  ) %>%
  mutate(
    scenario = if_else(
      accessibility_type %in% c("car_logSum_policy", "mm_logSum_policy"),
      "Policy", "Base"
    ),
    mode = case_when(
      accessibility_type == "twalk_logSum" ~ "Walking",
      accessibility_type == "pt_logSum" ~ "Public transport",
      grepl("car_logSum", accessibility_type) ~ "Car",
      grepl("mm_logSum", accessibility_type) ~ "Multimodal"
    ),
    scenario = factor(scenario, levels = c("Base", "Policy")),
    mode = factor(mode, levels = c("Walking", "Public transport", "Car", "Multimodal"))
  ) %>%
  filter(!is.na(income), !is.na(mode), !is.na(accessibility)) %>%
  # Duplicate Walk and PT for Policy scenario (since they're unchanged)
  bind_rows(
    filter(., scenario == "Base", mode %in% c("Walking", "Public transport")) %>%
      mutate(scenario = factor("Policy", levels = c("Base", "Policy")))
  ) %>%
  group_by(scenario, income, mode) %>%
  summarise(
    median_accessibility = median(accessibility, na.rm = TRUE),
    .groups = "drop"
  )
write_csv(medianacc_by_income_mode_scenario, file.path(output_dir, "median_accessibility_by_income_mode_scenario.csv"))

bar_medianacc_by_income_mode_scenario <- medianacc_by_income_mode_scenario %>%
  ggplot(aes(x = income, y = median_accessibility, fill = mode)) +
  geom_col(position = position_dodge(width = 0.8), width = 0.7) +
  facet_wrap(~ scenario, ncol = 1) +
  labs(
    x = "Income group",
    y = "Median accessibility (utils)",
    fill = "Mode"
  )
ggsave(file.path(output_dir, "bar_medianacc_by_income_mode_scenario.png"), bar_medianacc_by_income_mode_scenario, dpi = 300, width = 8, height = 5)

# boxplot Δ car accessibility by income
boxplot_car_deltaacc_by_income <- ggplot(df_acc, aes(income, car_delta)) +
  geom_boxplot(outlier.alpha = 0.3) +
  stat_summary(fun = mean, geom = "point", shape = 23, fill = "white", size = 2.5, na.rm = TRUE) +
  labs(
    x = "Income group",
    y = "Car accessibility change (utils)"
  )
ggsave(file.path(output_dir, "boxplot_car_deltaacc_by_income.png"), boxplot_car_deltaacc_by_income, dpi = 300, width = 8, height = 5)



# library(tmap)
# tmap_mode('view')
# boxplot Δ multimodal accessibility by income
boxplot_mm_deltaacc_by_income <- ggplot(df_acc, aes(income, mm_delta)) +
  geom_boxplot(outlier.alpha = 0.3) +
  stat_summary(fun = mean, geom = "point", shape = 23, fill = "white", size = 2.5, na.rm = TRUE) +
  labs(
    x = "Income group",
    y = "Multimodal accessibility change (utils)"
  )
ggsave(file.path(output_dir, "boxplot_mm_deltaacc_by_income.png"), boxplot_mm_deltaacc_by_income, dpi = 300, width = 8, height = 5)

# faceted by w/in or outside of ring
boxplot_mm_deltaacc_by_income_ring <- ggplot(df_acc, aes(income, mm_delta)) +
  geom_boxplot(outlier.alpha = 0.3) +
  stat_summary(fun = mean, geom = "point", shape = 23, fill = "white", size = 2.5, na.rm = TRUE) +
  labs(
    x = "Income group",
    y = "Multimodal accessibility change (utils)"
  ) +
  facet_grid(~within)
boxplot_mm_deltaacc_by_income_ring
ggsave(file.path(output_dir, "boxplot_mm_deltaacc_by_income_ring.png"), boxplot_mm_deltaacc_by_income_ring, dpi = 300, width = 8, height = 5)


# boxplot Δ car accessibility by district
boxplot_car_acc_by_district <- ggplot(df_acc, aes(x = district,
                                                  y = car_delta)) +
  geom_boxplot(outlier.alpha = 0.3, na.rm = TRUE) +
  labs(
    x = "District",
    y = "Car accessibility change (utils)"
  ) +
  theme(axis.text.x = element_text(angle = 45, hjust = 1))
ggsave(file.path(output_dir, "boxplot_car_acc_by_district.png"), boxplot_car_acc_by_district, dpi = 300, width = 10, height = 6)

# boxplot Δ multimodal accessibility by district
boxplot_mm_acc_by_district <- ggplot(df_acc, aes(x = district,
                                                 y = mm_delta)) +
  geom_boxplot(outlier.alpha = 0.3, na.rm = TRUE) +
  labs(
    x = "District",
    y = "Multimodal accessibility change (utils)"
  ) +
  theme(axis.text.x = element_text(angle = 45, hjust = 1))
ggsave(file.path(output_dir, "boxplot_mm_acc_by_district.png"), boxplot_mm_acc_by_district, dpi = 300, width = 10, height = 6)

# overall winner/losers
overall_outcome <- df_acc %>%
  count(outcome) %>%
  mutate(
    percent = round(100 * n / sum(n), 1)
  )
overall_outcome
write_csv(
  overall_outcome,
  file.path(output_dir, "summary_outcome_overall.csv")
)

# Winners / Losers / No change by income
summary_outcome_income <- df_acc %>%
  group_by(income) %>%
  summarise(
    Population = n(),
    `Improved (n)` = sum(mm_delta > 0, na.rm = TRUE),
    `Worsened (n)` = sum(mm_delta < 0, na.rm = TRUE),
    `No change (n)` = sum(mm_delta == 0, na.rm = TRUE),
    `Improved (%)` = 100 * `Improved (n)` / Population,
    `Worsened (%)` = 100 * `Worsened (n)` / Population,
    `No change (%)` = 100 * `No change (n)` / Population,
    .groups = "drop"
  ) %>%
  mutate(
    across(where(is.numeric), ~ round(.x, 2))
  )
summary_outcome_income
write_csv(
  summary_outcome_income,
  file.path(output_dir, "summary_outcome_by_income.csv")
)

# ==========================================================
# Map of POI locations
# ==========================================================
# locations.csv is written by RunPersonBasedAccBerlin (EPSG:25832),
# one row per location with type = "hospital", "spa", "baeder" or "jobcenter".

# Both scenarios write the same locations.csv; take whichever exists.
locations_csv <- file.path(scenario_dirs$policy, "locations.csv")
if (!file.exists(locations_csv)) {
  locations_csv <- file.path(scenario_dirs$base, "locations.csv")
}
if (!file.exists(locations_csv)) {
  stop(
    "locations.csv not found for opportunity '", opportunity, "' at ",
    locations_csv, ". Run RunPersonBasedAccBerlin for this opportunity first."
  )
}
map_dir <- file.path(output_root, "maps", opportunity)
dir.create(map_dir, recursive = TRUE, showWarnings = FALSE)

locations_sf <- read_csv(locations_csv, show_col_types = FALSE) %>%
  filter(!is.na(x), !is.na(y)) %>%
  mutate(
    type = factor(
      type,
      levels = unname(poi_types),
      labels = unname(poi_labels)
    )
  ) %>%
  st_as_sf(
    coords = c("x", "y"),
    crs = 25832
  )

# Berlin extent, so the map is not zoomed out by locations outside the city
# (e.g. the spa in Ludwigsfelde).
berlin_bbox <- st_bbox(berlin_sf)

plot_locations_map <- function(locations,
                               title = paste0(poi$label, " locations in Berlin"),
                               label_points = poi$label_points,
                               show_ring = TRUE) {
  p <- ggplot() +
    geom_sf(
      data = berlin_sf,
      fill = "grey95",
      color = "grey60",
      linewidth = 0.3
    )

  if (show_ring) {
    p <- p +
      geom_sf(
        data = ring_sf,
        fill = NA,
        color = "#6a3d9a",
        linetype = "dashed",
        linewidth = 0.6
      )
  }

  p <- p +
    geom_sf(
      data = locations,
      aes(color = type, shape = type),
      size = 2.2,
      alpha = 0.85
    ) +
    scale_color_manual(
      values = stats::setNames(unname(poi_colors), unname(poi_labels)),
      drop = TRUE
    ) +
    scale_shape_manual(
      values = stats::setNames(unname(poi_shapes), unname(poi_labels)),
      drop = TRUE
    ) +
    coord_sf(
      xlim = c(berlin_bbox["xmin"], berlin_bbox["xmax"]),
      ylim = c(berlin_bbox["ymin"], berlin_bbox["ymax"])
    ) +
    labs(
      title = title,
      color = NULL,
      shape = NULL
    ) +
    theme_void() +
    theme(
      legend.position = "bottom",
      plot.title = element_text(face = "bold", hjust = 0.5)
    )

  if (label_points) {
    p <- p +
      geom_sf_text(
        data = locations,
        aes(label = name),
        size = 3,
        vjust = -1,
        check_overlap = TRUE,
        color = "grey20"
      )
  }

  p
}

# Locations of the selected POI
map_locations <- plot_locations_map(locations_sf)
ggsave(
  file.path(map_dir, paste0("map_", opportunity, "_locations.png")),
  map_locations,
  dpi = 300,
  width = 8,
  height = 6.5,
  bg = "white"
)

# ==========================================================
# Person-level base vs policy comparison
# ==========================================================
# persons_<mode>_<scenario>.csv are written by RunPersonBasedAccBerlin:
# personId, homeX, homeY, accessibility (rawSum, EPSG:25832).
# One file per run, so base and policy are joined on personId.

read_persons <- function(mode, scenario) {
  path <- file.path(
    scenario_dirs[[scenario]],
    paste0("persons_", mode, "_", scenario, ".csv")
  )

  if (!file.exists(path)) {
    message("persons file not found, skipping: ", path)
    return(NULL)
  }

  read_person_data(path)
}

# Reads one mode's accessibility for a scenario: the persons file if it exists,
# otherwise the matching pbAcc file, from that scenario's run folder
# (run_dirs$<scenario>/<opportunity>/). Modes that are unchanged by the
# policy reuse their base file when the policy file is missing.
policy_unchanged_modes <- c("pt", "teleportedWalk")

read_mode_accessibility <- function(mode, scenario) {
  candidates <- c(
    file.path(scenario_dirs[[scenario]], paste0("persons_", mode, "_", scenario, ".csv")),
    file.path(scenario_dirs[[scenario]], paste0("pbAcc_", mode, "_", scenario, "_rawSum.csv"))
  )

  path <- Find(file.exists, candidates)

  if (!is.null(path)) {
    return(suppressWarnings(read_person_data(path)))
  }

  if (scenario != "base" && mode %in% policy_unchanged_modes) {
    message(
      "No ", mode, " ", scenario, " data; using base (",
      mode, " is unchanged by the policy)"
    )
    return(read_mode_accessibility(mode, "base"))
  }

  message("No accessibility data for mode '", mode, "', scenario '", scenario, "'.")
  NULL
}

# Multimodal accessibility = sum of the three modes' raw sums per person,
# the same definition as mm_logSum in the accessibility calculations above.
mm_modes <- c("teleportedWalk", "pt", "car")

read_persons_mm <- function(scenario) {
  first <- read_mode_accessibility(mm_modes[1], scenario)
  if (is.null(first)) {
    return(NULL)
  }

  combined <- first %>% select(personId, homeX, homeY, accessibility)
  names(combined)[4] <- mm_modes[1]

  for (m in mm_modes[-1]) {
    d <- read_mode_accessibility(m, scenario)
    if (is.null(d)) {
      return(NULL)
    }
    d <- d %>% select(personId, accessibility)
    names(d)[2] <- m
    combined <- inner_join(combined, d, by = "personId")
  }

  combined %>%
    mutate(
      accessibility = .data[[mm_modes[1]]] +
        .data[[mm_modes[2]]] +
        .data[[mm_modes[3]]]
    ) %>%
    select(personId, homeX, homeY, accessibility)
}

join_base_policy <- function(base_df, policy_df) {
  inner_join(
    base_df %>%
      select(personId, homeX, homeY, accessibility_base = accessibility),
    policy_df %>%
      select(personId, accessibility_policy = accessibility),
    by = "personId"
  ) %>%
    mutate(
      logSum_base = if_else(
        accessibility_base > 0,
        log(accessibility_base) / beta,
        NA_real_
      ),
      logSum_policy = if_else(
        accessibility_policy > 0,
        log(accessibility_policy) / beta,
        NA_real_
      ),
      delta = logSum_policy - logSum_base,
      delta_raw = accessibility_policy - accessibility_base,
      outcome = case_when(
        delta > 0 ~ "Winner",
        delta < 0 ~ "Loser",
        delta == 0 ~ "No change",
        TRUE ~ NA_character_
      )
    )
}

join_persons <- function(mode, base = "base", policy = "policy") {
  base_df <- read_mode_accessibility(mode, base)
  policy_df <- read_mode_accessibility(mode, policy)

  if (is.null(base_df) || is.null(policy_df)) {
    return(NULL)
  }

  join_base_policy(base_df, policy_df)
}

join_persons_mm <- function(base = "base", policy = "policy") {
  base_df <- read_persons_mm(base)
  policy_df <- read_persons_mm(policy)

  if (is.null(base_df) || is.null(policy_df)) {
    return(NULL)
  }

  join_base_policy(base_df, policy_df)
}

# Car base vs policy (same function works for "pt" and "teleportedWalk")
car_persons <- join_persons("car")

if (!is.null(car_persons)) {
  car_persons
  write_csv(
    car_persons,
    file.path(output_dir, "persons_car_base_vs_policy.csv")
  )
}

# ==========================================================
# Person-level maps: policy - base accessibility change
# ==========================================================
# One point per person at their home location, coloured by the signed
# accessibility change between the policy and base scenario (log scale,
# i.e. delta from join_persons()). Works for any mode with both person files.

# POI locations as an overlay on maps: white points with a black outline, so
# they stay visible on any colour scale. Returns NULL (no layer) without data.
poi_overlay <- function(locations) {
  if (is.null(locations) || nrow(locations) == 0) return(NULL)
  geom_sf(data = locations, shape = 21, fill = "white", colour = "black",
          size = 2.4, stroke = 0.7, inherit.aes = FALSE)
}

plot_person_delta_map <- function(mode,
                                  base = "base",
                                  policy = "policy",
                                  measure = c("delta", "delta_raw")) {
  measure <- match.arg(measure)
  persons <- if (identical(mode, "mm")) {
    join_persons_mm(base, policy)
  } else {
    join_persons(mode, base, policy)
  }

  if (is.null(persons)) {
    message("No persons data for mode '", mode, "', skipping map.")
    return(NULL)
  }

  persons_sf <- persons %>%
    filter(!is.na(.data[[measure]]), !is.na(homeX), !is.na(homeY)) %>%
    st_as_sf(
      coords = c("homeX", "homeY"),
      crs = 25832
    )

  # capped colour scale: most deltas are small and negative, so a wide range
  # would hide them; values beyond the cap are squished to the end colours
  colour_limit <- 1

  legend_title <- if (measure == "delta") {
    "Δ accessibility\n(policy - base, utils)"
  } else {
    "Δ accessibility\n(policy - base, rawSum)"
  }

  ggplot() +
    geom_sf(
      data = berlin_sf,
      fill = "grey95",
      color = "grey60",
      linewidth = 0.3
    ) +
    geom_sf(
      data = ring_sf,
      fill = NA,
      color = "grey40",
      linetype = "dashed",
      linewidth = 0.4
    ) +
    geom_sf(
      data = persons_sf,
      aes(color = .data[[measure]]),
      size = 0.4,
      alpha = 0.7
    ) +
    poi_overlay(locations_sf) +
    scale_color_gradient2(
      low = "#b2182b",
      mid = "grey92",
      high = "#2166ac",
      midpoint = 0,
      limits = c(-colour_limit, colour_limit),
      oob = scales::squish,
      name = legend_title
    ) +
    coord_sf(
      xlim = c(berlin_bbox["xmin"], berlin_bbox["xmax"]),
      ylim = c(berlin_bbox["ymin"], berlin_bbox["ymax"])
    ) +
    labs(
      title = paste0(
        switch(
          mode,
          mm = "Multimodal",
          pt = "Public transport",
          teleportedWalk = "Walking",
          tools::toTitleCase(mode)
        ),
        " accessibility change: policy - base"
      )
    ) +
    theme_void() +
    theme(
      legend.position = "right",
      legend.key.height = unit(1.5, "cm"),
      plot.title = element_text(face = "bold", hjust = 0.5)
    )
}

for (mode in c("car", "pt", "teleportedWalk", "mm")) {
  person_delta_map <- plot_person_delta_map(mode)

  if (!is.null(person_delta_map)) {
    ggsave(
      file.path(map_dir, paste0("map_persons_", mode, "_policy_minus_base.png")),
      person_delta_map,
      dpi = 300,
      width = 8,
      height = 6.5,
      bg = "white"
    )
  }
}

# ==========================================================
# ECDF of multimodal accessibility: base vs policy
# ==========================================================

plot_mm_ecdf <- function(persons) {
  # join_persons_mm() returns raw sums; build the log sums if they're not there yet
  if (!all(c("logSum_base", "logSum_policy") %in% names(persons))) {
    persons <- persons %>%
      mutate(
        logSum_base   = log(accessibility_base) / beta,
        logSum_policy = log(accessibility_policy) / beta
      )
  }
  
  ecdf_data <- persons %>%
    filter(is.finite(logSum_base), is.finite(logSum_policy)) %>%
    select(Base = logSum_base, Policy = logSum_policy) %>%
    pivot_longer(everything(), names_to = "scenario", values_to = "accessibility") %>%
    mutate(scenario = factor(scenario, levels = c("Base", "Policy")))
  x_min <- quantile(ecdf_data$accessibility, 0.01, names = FALSE)
  
  ggplot(ecdf_data, aes(accessibility, colour = scenario, linetype = scenario)) +
    geom_hline(yintercept = c(0.1, 0.5, 0.9), colour = "grey80", linetype = "dotted") +
    geom_vline(xintercept = c(-6, -3), colour = "grey65", linetype = "dashed") +
    stat_ecdf(geom = "step", linewidth = 1) +
    scale_colour_manual(values = c(Base = "#434343", Policy = "#2166ac")) +
    scale_y_continuous(labels = scales::label_percent(), limits = c(0, 1)) +
    coord_cartesian(xlim = c(x_min, NA), expand = FALSE) +
    labs(
      x = "Multimodal accessibility (utils)",
      y = "Cumulative share of persons",
      colour = NULL, linetype = NULL,
      title = poi$label, subtitle = "Walk + PT + car; departure at 08:00"
    ) +
    theme_minimal(base_size = 14) +
    theme(legend.position = "bottom")
}

mm_persons <- join_persons_mm()
if (!is.null(mm_persons)) {
  ecdf_mm_base_vs_policy <- plot_mm_ecdf(mm_persons)
  ggsave(
    file.path(output_dir, "ecdf_mm_base_vs_policy.png"),
    ecdf_mm_base_vs_policy,
    dpi = 300, width = 8, height = 6
  )
  print(ecdf_mm_base_vs_policy)
}

# ---- end of per-opportunity analysis ----
TRUE
}, error = function(e) {
  message("FAILED for '", opportunity, "': ", conditionMessage(e))
  FALSE
})
if (!opportunity_ok) failed_opportunities <- c(failed_opportunities, opportunity)
}

message(
  "\nDone. Succeeded: ",
  paste(setdiff(opportunities, failed_opportunities), collapse = ", "),
  if (length(failed_opportunities) > 0)
    paste0(" | Failed: ", paste(failed_opportunities, collapse = ", "))
)
