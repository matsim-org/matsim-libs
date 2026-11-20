# ==========================================================
# PT accessibility with vs. without monetary costs
# (daily monetary constant, --daily-monetary-constant)
#
# Compares two runs of RunPersonBasedAccBerlin that differ only in the
# daily monetary constant, person by person. Same analyses and maps as
# analysis.R, but for PT only and "with - without" instead of
# "policy - base".
# ==========================================================

suppressPackageStartupMessages({
  library(dplyr)
  library(readr)
  library(sf)
  library(tidyr)
  library(ggplot2)
})

# ==========================================================
# Settings
# ==========================================================

base_dir <- "/home/brendan/Documents/arbeit/abmtrans_paper"

# The two runs to compare: without and with the daily monetary constant
run_dirs <- list(
  without = file.path(base_dir, "2026-11-18-v6.3-spa-jobcenter-noMonetaryCostsTest"),
  with    = file.path(base_dir, "2026-11-18-v6.3-spa-jobcenter-monetaryCostsTest")
)

opportunities <- c("spa", "jobCenter")   # folders without results are skipped
mode          <- "pt"
scenario      <- "policy"                # which scenario's files the two runs produced

output_root <- file.path(base_dir, "output", "monetaryCostsTest")

# Berlin district shapefile and S-Bahn ring (kept in the repo)
shape_dir <- path.expand(
  "~/git/matsim-libs/contribs/accessibility/src/main/R/THESIS/input/Berlin shapefiles"
)

beta <- 1

# Same nine income brackets as analysis.R, labelled with their upper bound
income_breaks <- c(-Inf, 500, 900, 1500, 2000, 2600, 3000, 3600, 4600, Inf)
income_labels <- c("500", "900", "1500", "2000", "2600", "3000", "3600", "4600", "5600")

# POI metadata for the locations map (same as analysis.R)
poi_meta <- list(
  hospital  = list(type = "hospital",  label = "Hospital",    color = "#1f78b4", shape = 16, label_points = FALSE),
  spa       = list(type = "spa",       label = "Spa",         color = "#e31a1c", shape = 17, label_points = TRUE),
  baeder    = list(type = "baeder",    label = "Public bath", color = "#33a02c", shape = 15, label_points = TRUE),
  jobCenter = list(type = "jobcenter", label = "Job center",  color = "#ff7f00", shape = 18, label_points = TRUE)
)
poi_types  <- vapply(poi_meta, `[[`, character(1), "type")
poi_labels <- vapply(poi_meta, `[[`, character(1), "label")
poi_colors <- vapply(poi_meta, `[[`, character(1), "color")
poi_shapes <- vapply(poi_meta, `[[`, numeric(1), "shape")

theme_set(
  theme_gray() +
    theme(
      axis.title.x = element_text(face = "bold"),
      axis.title.y = element_text(face = "bold")
    )
)

# ==========================================================
# Shapes (read once)
# ==========================================================

berlin_sf <- st_read(file.path(shape_dir, "Berlin_Bezirke.shp"), quiet = TRUE)
if (is.na(st_crs(berlin_sf))) st_crs(berlin_sf) <- 25832
if (st_crs(berlin_sf)$epsg != 25832) {
  berlin_sf <- st_transform(berlin_sf, 25832)
}
berlin_bbox <- st_bbox(berlin_sf)

ring_sf <- st_read(file.path(shape_dir, "Berlin_sbahn_ring.geojson"), quiet = TRUE)
if (is.na(st_crs(ring_sf))) st_crs(ring_sf) <- 4326
ring_sf <- st_transform(ring_sf, 25832)

district_levels <- c(
  "Mitte", "Friedrichshain-Kreuzberg", "Pankow", "Charlottenburg-Wilmersdorf",
  "Spandau", "Steglitz-Zehlendorf", "Tempelhof-Schöneberg", "Neukölln",
  "Treptow-Köpenick", "Marzahn-Hellersdorf", "Lichtenberg", "Reinickendorf"
)

# ==========================================================
# Helpers
# ==========================================================

read_run <- function(run, opportunity) {
  path <- file.path(run_dirs[[run]], opportunity,
                    paste0("pbAcc_", mode, "_", scenario, "_rawSum.csv"))
  if (!file.exists(path)) {
    return(NULL)
  }
  read_csv(path, show_col_types = FALSE) %>%
    mutate(personId = as.character(personId),
           income = as.numeric(income),
           accessibility = as.numeric(accessibility)) %>%
    select(personId, age, sex, income, homeX, homeY, accessibility)
}

base_map <- function() {
  ggplot() +
    geom_sf(data = berlin_sf, fill = "grey95", color = "grey60", linewidth = 0.3)
}

zoom_to_berlin <- function() {
  coord_sf(
    xlim = c(berlin_bbox["xmin"], berlin_bbox["xmax"]),
    ylim = c(berlin_bbox["ymin"], berlin_bbox["ymax"])
  )
}

# POI locations as an overlay on maps: white points with a black outline, so
# they stay visible on any colour scale. Returns NULL (no layer) without data.
poi_overlay <- function(locations) {
  if (is.null(locations) || nrow(locations) == 0) return(NULL)
  geom_sf(data = locations, shape = 21, fill = "white", colour = "black",
          size = 2.4, stroke = 0.7, inherit.aes = FALSE)
}

save_plot <- function(plot, dir, name, width = 8, height = 5, bg = NULL) {
  ggsave(file.path(dir, name), plot, dpi = 300, width = width, height = height, bg = bg)
}

# ==========================================================
# Per-opportunity comparison
# ==========================================================

for (opportunity in opportunities) {
  without_df <- read_run("without", opportunity)
  with_df    <- read_run("with", opportunity)

  if (is.null(without_df) || is.null(with_df)) {
    message("Skipping '", opportunity, "': results missing in one of the two runs.")
    next
  }

  message("\n===== ", opportunity, " / ", mode, " / ", scenario, " =====")
  poi <- poi_meta[[opportunity]]

  output_dir <- file.path(output_root, opportunity)
  map_dir    <- file.path(output_root, "maps", opportunity)
  dir.create(output_dir, recursive = TRUE, showWarnings = FALSE)
  dir.create(map_dir, recursive = TRUE, showWarnings = FALSE)

  # --------------------------------------------------------
  # Merge, accessibility calculations, district
  # --------------------------------------------------------

  df <- inner_join(
    without_df %>% rename(acc_without = accessibility),
    with_df    %>% select(personId, acc_with = accessibility),
    by = "personId"
  )

  # Both runs must contain the same persons, otherwise the comparison is invalid
  message("Persons: without = ", nrow(without_df), ", with = ", nrow(with_df),
          ", in both = ", nrow(df))
  if (nrow(df) < 0.99 * max(nrow(without_df), nrow(with_df))) {
    warning("The two runs contain different persons for '", opportunity,
            "' - check that both used the same --sample and --sample-seed.")
  }

  df_acc <- df %>%
    mutate(
      logSum_without = log(acc_without) / beta,
      logSum_with    = log(acc_with) / beta,
      delta          = logSum_with - logSum_without,
      outcome = case_when(
        delta > 0 ~ "Improved",
        delta < 0 ~ "Worsened",
        TRUE      ~ "No change"
      ),
      income = cut(income, breaks = income_breaks, labels = income_labels,
                   right = TRUE, ordered_result = TRUE)
    ) %>%
    st_as_sf(coords = c("homeX", "homeY"), crs = 25832, remove = FALSE) %>%
    st_join(berlin_sf[, "Gemeinde_n", drop = FALSE]) %>%
    st_drop_geometry() %>%
    rename(district = Gemeinde_n) %>%
    mutate(district = factor(district, levels = district_levels, ordered = TRUE))

  write_csv(df_acc, file.path(output_dir, paste0("acc_", mode, "_monetary_costs.csv")))

  # --------------------------------------------------------
  # Summaries
  # --------------------------------------------------------

  summary_accessibility <- df_acc %>%
    summarise(
      persons               = n(),
      median_logSum_without = median(logSum_without, na.rm = TRUE),
      median_logSum_with    = median(logSum_with, na.rm = TRUE),
      mean_delta            = mean(delta, na.rm = TRUE),
      median_delta          = median(delta, na.rm = TRUE),
      min_delta             = min(delta, na.rm = TRUE),
      max_delta             = max(delta, na.rm = TRUE)
    ) %>%
    mutate(across(where(is.numeric), ~ round(.x, 2)))
  print(summary_accessibility)
  write_csv(summary_accessibility, file.path(output_dir, "summary_accessibility.csv"))

  summary_by_income <- df_acc %>%
    group_by(income) %>%
    summarise(
      persons               = n(),
      median_logSum_without = median(logSum_without, na.rm = TRUE),
      median_logSum_with    = median(logSum_with, na.rm = TRUE),
      mean_delta            = mean(delta, na.rm = TRUE),
      median_delta          = median(delta, na.rm = TRUE),
      sd_delta              = sd(delta, na.rm = TRUE),
      .groups = "drop"
    ) %>%
    mutate(across(where(is.numeric), ~ round(.x, 2)))
  print(summary_by_income)
  write_csv(summary_by_income, file.path(output_dir, "summary_accessibility_by_income.csv"))

  overall_outcome <- df_acc %>%
    count(outcome) %>%
    mutate(percent = round(100 * n / sum(n), 1))
  print(overall_outcome)
  write_csv(overall_outcome, file.path(output_dir, "summary_outcome_overall.csv"))

  summary_outcome_income <- df_acc %>%
    group_by(income) %>%
    summarise(
      Population      = n(),
      `Improved (n)`  = sum(delta > 0, na.rm = TRUE),
      `Worsened (n)`  = sum(delta < 0, na.rm = TRUE),
      `No change (n)` = sum(delta == 0, na.rm = TRUE),
      `Improved (%)`  = 100 * `Improved (n)` / Population,
      `Worsened (%)`  = 100 * `Worsened (n)` / Population,
      `No change (%)` = 100 * `No change (n)` / Population,
      .groups = "drop"
    ) %>%
    mutate(across(where(is.numeric), ~ round(.x, 2)))
  print(summary_outcome_income)
  write_csv(summary_outcome_income, file.path(output_dir, "summary_outcome_by_income.csv"))

  # --------------------------------------------------------
  # Plots
  # --------------------------------------------------------

  # scatter with vs without
  scatter <- ggplot(df_acc, aes(logSum_without, logSum_with)) +
    geom_point(alpha = 0.3) +
    geom_abline(slope = 1, intercept = 0, color = "red", linewidth = 1) +
    labs(
      x = "PT accessibility without monetary costs (utils)",
      y = "PT accessibility with monetary costs (utils)"
    )
  save_plot(scatter, output_dir, "scatter_pt_acc_monetary_costs.png")

  # bar median accessibility by income, with vs without
  bar <- summary_by_income %>%
    select(income, without = median_logSum_without, with = median_logSum_with) %>%
    pivot_longer(c(without, with), names_to = "monetary_costs", values_to = "median_logSum") %>%
    mutate(monetary_costs = factor(monetary_costs, levels = c("without", "with"))) %>%
    ggplot(aes(income, median_logSum, fill = monetary_costs)) +
    geom_col(position = "dodge") +
    labs(
      x = "Income group",
      y = "Median PT accessibility (utils)",
      fill = "Monetary costs"
    )
  save_plot(bar, output_dir, "bar_medianacc_pt_by_income_monetary_costs.png")

  # boxplot Δ by income
  boxplot_income <- ggplot(df_acc, aes(income, delta)) +
    geom_boxplot(outlier.alpha = 0.3, na.rm = TRUE) +
    labs(
      x = "Income group",
      y = "PT accessibility change (utils)"
    )
  save_plot(boxplot_income, output_dir, "boxplot_pt_deltaacc_by_income.png")

  # boxplot Δ by district
  boxplot_district <- ggplot(df_acc, aes(district, delta)) +
    geom_boxplot(outlier.alpha = 0.3, na.rm = TRUE) +
    labs(
      x = "District",
      y = "PT accessibility change (utils)"
    ) +
    theme(axis.text.x = element_text(angle = 45, hjust = 1))
  save_plot(boxplot_district, output_dir, "boxplot_pt_deltaacc_by_district.png", width = 10, height = 6)

  # --------------------------------------------------------
  # Map of POI locations
  # --------------------------------------------------------

  locations_sf <- NULL
  locations_csv <- file.path(run_dirs$with, opportunity, "locations.csv")
  if (!file.exists(locations_csv)) {
    locations_csv <- file.path(run_dirs$without, opportunity, "locations.csv")
  }
  if (file.exists(locations_csv)) {
    locations_sf <- read_csv(locations_csv, show_col_types = FALSE) %>%
      filter(!is.na(x), !is.na(y)) %>%
      mutate(type = factor(type, levels = unname(poi_types), labels = unname(poi_labels))) %>%
      st_as_sf(coords = c("x", "y"), crs = 25832)

    map_locations <- base_map() +
      geom_sf(data = ring_sf, fill = NA, color = "#6a3d9a", linetype = "dashed", linewidth = 0.6) +
      geom_sf(data = locations_sf, aes(color = type, shape = type), size = 2.2, alpha = 0.85) +
      scale_color_manual(values = stats::setNames(unname(poi_colors), unname(poi_labels)), drop = TRUE) +
      scale_shape_manual(values = stats::setNames(unname(poi_shapes), unname(poi_labels)), drop = TRUE) +
      zoom_to_berlin() +
      labs(title = paste0(poi$label, " locations in Berlin"), color = NULL, shape = NULL) +
      theme_void() +
      theme(legend.position = "bottom", plot.title = element_text(face = "bold", hjust = 0.5))

    if (poi$label_points) {
      map_locations <- map_locations +
        geom_sf_text(data = locations_sf, aes(label = name), size = 3, vjust = -1,
                     check_overlap = TRUE, color = "grey20")
    }
    save_plot(map_locations, map_dir, paste0("map_", opportunity, "_locations.png"),
              height = 6.5, bg = "white")
  } else {
    message("No locations.csv for '", opportunity, "', skipping locations map.")
  }

  # --------------------------------------------------------
  # Person-level map: with - without
  # --------------------------------------------------------
  # One point per person at their home location, coloured by the change.
  # The colour scale is capped at the 95th percentile of |change| (analysis.R
  # uses 1, which would make every point here the same colour); values beyond
  # the cap get the end colours.

  persons_sf <- df_acc %>%
    filter(!is.na(delta), !is.na(homeX), !is.na(homeY)) %>%
    st_as_sf(coords = c("homeX", "homeY"), crs = 25832)

  colour_limit <- max(1, unname(quantile(abs(persons_sf$delta), 0.95, na.rm = TRUE)))
  # colour_limit <- 1

  map_delta <- base_map() +
    geom_sf(data = ring_sf, fill = NA, color = "grey40", linetype = "dashed", linewidth = 0.4) +
    geom_sf(data = persons_sf, aes(color = delta), size = 0.6, alpha = 0.8) +
    poi_overlay(locations_sf) +
    scale_color_gradient2(
      low = "#b2182b", mid = "grey92", high = "#2166ac", midpoint = 0,
      limits = c(-colour_limit, colour_limit), oob = scales::squish,
      name = "Δ accessibility\n(with - without, utils)"
    ) +
    zoom_to_berlin() +
    labs(title = "Public transport accessibility change: with - without monetary costs") +
    theme_void() +
    theme(
      legend.position = "right",
      legend.key.height = unit(1.5, "cm"),
      plot.title = element_text(face = "bold", hjust = 0.5)
    )
  save_plot(map_delta, map_dir, "map_persons_pt_with_minus_without_monetary_costs.png",
            height = 6.5, bg = "white")

  message("Results written to ", output_dir, " and ", map_dir)
}
