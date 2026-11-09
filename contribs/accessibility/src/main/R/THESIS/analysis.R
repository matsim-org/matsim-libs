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

# ==========================================================
# Directories
# ==========================================================

base_dir     <- "."
input_dir    <- file.path(base_dir, "input")
hospital_dir <- file.path(input_dir, "hospital")
output_dir   <- file.path(base_dir, "output")

beta <- 1

# ==========================================================
# Read input data
# ==========================================================

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
      income = case_when(
        income %in% c(499, 500) ~ "500",
        income == 900  ~ "900",
        income == 1500 ~ "1500",
        income == 2000 ~ "2000",
        income == 2600 ~ "2600",
        income == 3000 ~ "3000",
        income == 3600 ~ "3600",
        income == 4600 ~ "4600",
        income == 5600 ~ "5600",
        TRUE ~ NA_character_
      ),
      income = factor(
        income,
        levels = c(
          "500",
          "900",
          "1500",
          "2000",
          "2600",
          "3000",
          "3600",
          "4600",
          "5600"
        ),
        ordered = TRUE
      )
    )
}

read_acc <- function(file){
  read_csv(file, show_col_types = FALSE) %>% 
    clean_df()
}

# Hospital-based accessibility results (current run)
twalk_df <- read_acc(file.path(hospital_dir, "pbAcc_teleportedWalk_base_rawSum.csv"))
pt_df <- read_acc(file.path(hospital_dir, "pbAcc_pt_base_rawSum.csv"))
car_base_df <- read_acc(file.path(hospital_dir, "pbAcc_car_base_rawSum.csv"))
car_policy_df <- read_acc(file.path(hospital_dir, "pbAcc_car_policy_rawSum.csv"))
car_base_no_income_df <- read_acc(file.path(hospital_dir, "pbAcc_car_no_income.csv"))

# Spa-based reference run (kept for later refactoring)
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
  ) %>%
  left_join(
    car_base_no_income_df %>% 
      select(personId, car_base_logSum_no_income = accessibility),
    by = "personId"
  ) %>% 
  relocate(
    twalk_rawSum,
    pt_rawSum,
    car_base_rawSum,
    car_policy_rawSum,
    car_base_logSum_no_income,
    .after = homeY
  )

# ==========================================================
# Add district
# ==========================================================

berlin_sf <- st_read("input/Berlin shapefiles/Berlin_Bezirke.shp", quiet = TRUE)

if (is.na(st_crs(berlin_sf))) st_crs(berlin_sf) <- 25832
if (st_crs(berlin_sf)$epsg != 25832) {
  berlin_sf <- st_transform(berlin_sf, 25832)
}

df_acc <- df %>%
  st_as_sf(
    coords = c("homeX", "homeY"),
    crs = 25832,
    remove = FALSE
  ) %>%
  st_join(
    berlin_sf[, "Gemeinde_n", drop = FALSE]
  ) %>%
  st_drop_geometry() %>%
  rename(
    district = Gemeinde_n
  )

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

# income integration

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
  labs(
    x = "Income group",
    y = "Car accessibility change (utils)"
  )
ggsave(file.path(output_dir, "boxplot_base_car_deltaacc_by_income.png"), boxplot_base_car_deltaacc_by_income, dpi = 300, width = 8, height = 5)

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
  labs(
    x = "Income group",
    y = "Car accessibility change (utils)"
  )
ggsave(file.path(output_dir, "boxplot_car_deltaacc_by_income.png"), boxplot_car_deltaacc_by_income, dpi = 300, width = 8, height = 5)

# boxplot Δ multimodal accessibility by income
boxplot_mm_deltaacc_by_income <- ggplot(df_acc, aes(income, mm_delta)) +
  geom_boxplot(outlier.alpha = 0.3) +
  labs(
    x = "Income group",
    y = "Multimodal accessibility change (utils)"
  )
ggsave(file.path(output_dir, "boxplot_mm_deltaacc_by_income.png"), boxplot_mm_deltaacc_by_income, dpi = 300, width = 8, height = 5)

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


