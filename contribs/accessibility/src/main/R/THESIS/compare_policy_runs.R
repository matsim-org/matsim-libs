# ==========================================================
# Policy run v6.3 vs. policy run v6.4
#
# Compares the policy (autofrei) results of two RunPersonBasedAccBerlin
# experiments that use different versions of the Berlin scenario.
#
# The two scenario versions have different synthetic populations, so the
# same person does not exist in both runs and a person-by-person "delta"
# like in analysis.R is not possible. Instead, the runs are compared on
# the group level: whole population, income groups, districts, and a
# 1 km grid for the maps. All differences are "v6.4 - v6.3".
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

# The two runs to compare. The names are used as labels in tables and plots.
# The first entry is the reference, differences are second - first.
run_dirs <- list(
  "v6.3" = file.path(base_dir, "2026-11-18-v6.3-spa-hosp-jobcenter", "2026-11-19-policy"),
  "v6.4" = file.path(base_dir, "2026-11-18-v6.4-spa-hosp-jobcenter", "2026-11-19-policy")
)

scenario      <- "policy"   # which scenario's files the two runs produced (file name suffix)
opportunities <- c("hospital", "spa", "jobCenter")   # folders without results are skipped

output_root <- file.path(base_dir, "output", "policy-v6.3-vs-v6.4")

# Berlin district shapefile and S-Bahn ring (kept in the repo)
shape_dir <- path.expand(
  "~/git/matsim-libs/contribs/accessibility/src/main/R/THESIS/input/Berlin shapefiles"
)

beta <- 1

# Grid for the maps: cell size in metres, and the minimum number of persons a
# cell needs in BOTH runs to be shown in the difference map
grid_cell_size   <- 1000
grid_min_persons <- 5

# Same nine income brackets as analysis.R, labelled with their upper bound
income_breaks <- c(-Inf, 500, 900, 1500, 2000, 2600, 3000, 3600, 4600, Inf)
income_labels <- c("500", "900", "1500", "2000", "2600", "3000", "3600", "4600", "5600")

district_levels <- c(
  "Mitte", "Friedrichshain-Kreuzberg", "Pankow", "Charlottenburg-Wilmersdorf",
  "Spandau", "Steglitz-Zehlendorf", "Tempelhof-Schöneberg", "Neukölln",
  "Treptow-Köpenick", "Marzahn-Hellersdorf", "Lichtenberg", "Reinickendorf"
)

# Modes as written by RunPersonBasedAccBerlin, and how they are labelled here
modes <- c(teleportedWalk = "Walk", pt = "PT", car = "Car")
measure_levels <- c("Multimodal", unname(modes))

poi_labels <- c(hospital = "Hospital", spa = "Spa", baeder = "Public bath", jobCenter = "Job center")

run_names <- names(run_dirs)
ref_run   <- run_names[1]
cmp_run   <- run_names[2]
delta_label <- paste0(cmp_run, " - ", ref_run)

missing_run_dirs <- Filter(function(d) !dir.exists(d), run_dirs)
if (length(missing_run_dirs) > 0) {
  stop("Run folder(s) not found: ", paste(unlist(missing_run_dirs), collapse = ", "))
}

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
berlin_sf <- st_transform(berlin_sf, 25832)
berlin_bbox <- st_bbox(berlin_sf)

ring_sf <- st_read(file.path(shape_dir, "Berlin_sbahn_ring.geojson"), quiet = TRUE)
if (is.na(st_crs(ring_sf))) st_crs(ring_sf) <- 4326
ring_sf <- st_transform(ring_sf, 25832)

# 1 km grid over Berlin, cut to the city boundary
grid_sf <- st_make_grid(berlin_sf, cellsize = grid_cell_size, square = TRUE) %>%
  st_sf(geometry = .) %>%
  mutate(cell_id = row_number())
grid_sf <- grid_sf[lengths(st_intersects(grid_sf, berlin_sf)) > 0, ]

# ==========================================================
# Helpers
# ==========================================================

# Reads the three modes of one run and returns one row per person with the
# logsum per mode and the multimodal logsum (same formula as analysis.R).
read_run <- function(run, opportunity) {
  files <- file.path(run_dirs[[run]], opportunity,
                     paste0("pbAcc_", names(modes), "_", scenario, "_rawSum.csv"))
  names(files) <- names(modes)

  if (!all(file.exists(files))) {
    message("  ", run, ": missing ", paste(basename(files[!file.exists(files)]), collapse = ", "))
    return(NULL)
  }

  per_mode <- lapply(names(modes), function(m) {
    read_csv(files[[m]], show_col_types = FALSE) %>%
      mutate(personId = as.character(personId)) %>%
      select(personId, age, sex, income, homeX, homeY, raw = accessibility) %>%
      rename_with(~ paste0("raw_", m), raw)
  })

  persons <- per_mode[[1]]
  for (i in seq_along(per_mode)[-1]) {
    persons <- inner_join(persons, per_mode[[i]] %>% select(personId, starts_with("raw_")),
                          by = "personId")
  }

  persons %>%
    mutate(
      run        = run,
      income_raw = as.numeric(income),
      income     = cut(income_raw, breaks = income_breaks, labels = income_labels,
                       right = TRUE, ordered_result = TRUE),
      Walk       = log(raw_teleportedWalk) / beta,
      PT         = log(raw_pt) / beta,
      Car        = log(raw_car) / beta,
      Multimodal = log(raw_teleportedWalk + raw_pt + raw_car) / beta
    )
}

# Median etc. of every measure, grouped by run plus the given columns
summarise_measures <- function(df_long, ...) {
  df_long %>%
    group_by(run, measure, ...) %>%
    summarise(
      persons = n(),
      mean    = mean(logSum, na.rm = TRUE),
      median  = median(logSum, na.rm = TRUE),
      p10     = unname(quantile(logSum, 0.10, na.rm = TRUE)),
      p90     = unname(quantile(logSum, 0.90, na.rm = TRUE)),
      .groups = "drop"
    )
}

# Puts the two runs side by side and adds the difference of mean and median
compare_runs <- function(summary_df, ...) {
  summary_df %>%
    select(run, measure, ..., persons, mean, median) %>%
    pivot_wider(names_from = run, values_from = c(persons, mean, median), names_sep = "_") %>%
    mutate(
      delta_mean   = .data[[paste0("mean_", cmp_run)]]   - .data[[paste0("mean_", ref_run)]],
      delta_median = .data[[paste0("median_", cmp_run)]] - .data[[paste0("median_", ref_run)]]
    ) %>%
    arrange(measure, ...) %>%
    mutate(across(where(is.numeric) & !starts_with("persons"), ~ round(.x, 3)))
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

map_theme <- function() {
  theme_void() +
    theme(
      legend.position = "right",
      legend.key.height = unit(1.2, "cm"),
      plot.title = element_text(face = "bold", hjust = 0.5),
      strip.text = element_text(face = "bold", size = 11)
    )
}

# ==========================================================
# Per-opportunity comparison
# ==========================================================

for (opportunity in opportunities) {
  message("\n===== ", opportunity, ": ", delta_label, " (", scenario, ") =====")

  runs <- lapply(run_names, read_run, opportunity = opportunity)
  names(runs) <- run_names
  if (any(vapply(runs, is.null, logical(1)))) {
    message("Skipping '", opportunity, "': results missing in one of the two runs.")
    next
  }

  poi_label  <- poi_labels[[opportunity]]
  output_dir <- file.path(output_root, opportunity)
  map_dir    <- file.path(output_root, "maps", opportunity)
  dir.create(output_dir, recursive = TRUE, showWarnings = FALSE)
  dir.create(map_dir, recursive = TRUE, showWarnings = FALSE)

  # --------------------------------------------------------
  # Population check
  # --------------------------------------------------------

  in_both <- length(intersect(runs[[ref_run]]$personId, runs[[cmp_run]]$personId))
  message("Persons: ", ref_run, " = ", nrow(runs[[ref_run]]), ", ",
          cmp_run, " = ", nrow(runs[[cmp_run]]), ", in both = ", in_both,
          " -> comparing groups, not persons")

  # Did the POI locations change between the runs?
  loc_files <- file.path(unlist(run_dirs), opportunity, "locations.csv")
  locations_sf <- NULL
  if (any(file.exists(loc_files))) {
    locations_sf <- read_csv(loc_files[file.exists(loc_files)][1], show_col_types = FALSE) %>%
      filter(!is.na(x), !is.na(y)) %>%
      st_as_sf(coords = c("x", "y"), crs = 25832)
  }
  if (all(file.exists(loc_files))) {
    locs <- lapply(loc_files, function(f) read_csv(f, show_col_types = FALSE) %>% arrange(across(everything())))
    if (!isTRUE(all.equal(as.data.frame(locs[[1]]), as.data.frame(locs[[2]]), check.attributes = FALSE))) {
      warning("locations.csv differs between the two runs for '", opportunity, "'.")
    }
  }

  # --------------------------------------------------------
  # Combine, district, long format
  # --------------------------------------------------------

  df_acc <- bind_rows(runs) %>%
    mutate(run = factor(run, levels = run_names)) %>%
    st_as_sf(coords = c("homeX", "homeY"), crs = 25832, remove = FALSE) %>%
    st_join(berlin_sf[, "Gemeinde_n", drop = FALSE]) %>%
    st_join(grid_sf[, "cell_id"]) %>%
    st_drop_geometry() %>%
    rename(district = Gemeinde_n) %>%
    mutate(district = factor(district, levels = district_levels, ordered = TRUE))

  write_csv(df_acc, file.path(output_dir, "acc_both_runs.csv"))

  df_long <- df_acc %>%
    pivot_longer(all_of(measure_levels), names_to = "measure", values_to = "logSum") %>%
    mutate(measure = factor(measure, levels = measure_levels))

  # --------------------------------------------------------
  # Summaries
  # --------------------------------------------------------

  summary_overall <- summarise_measures(df_long)
  write_csv(summary_overall %>% mutate(across(where(is.double), ~ round(.x, 3))),
            file.path(output_dir, "summary_by_run.csv"))

  comparison_overall <- compare_runs(summary_overall)
  print(comparison_overall)
  write_csv(comparison_overall, file.path(output_dir, "comparison_overall.csv"))

  comparison_income <- compare_runs(summarise_measures(df_long, income), income)
  write_csv(comparison_income, file.path(output_dir, "comparison_by_income.csv"))

  comparison_district <- compare_runs(
    summarise_measures(df_long %>% filter(!is.na(district)), district), district
  )
  write_csv(comparison_district, file.path(output_dir, "comparison_by_district.csv"))

  # Persons per income group, to check that the two populations are comparable
  population_income <- df_acc %>%
    count(run, income) %>%
    group_by(run) %>%
    mutate(percent = round(100 * n / sum(n), 1)) %>%
    ungroup() %>%
    pivot_wider(names_from = run, values_from = c(n, percent))
  write_csv(population_income, file.path(output_dir, "population_by_income.csv"))

  # --------------------------------------------------------
  # Plots
  # --------------------------------------------------------

  # Distribution of accessibility per measure, both runs overlaid
  density_plot <- ggplot(df_long, aes(logSum, color = run, fill = run)) +
    geom_density(alpha = 0.15, na.rm = TRUE) +
    facet_wrap(~ measure, scales = "free") +
    labs(
      title = paste0(poi_label, ": accessibility distribution"),
      x = "Accessibility (utils)", y = "Density", color = "Run", fill = "Run"
    )
  save_plot(density_plot, output_dir, "density_acc_by_measure.png", width = 10, height = 7)

  # Median accessibility by income group, both runs side by side
  bar_income <- summarise_measures(df_long, income) %>%
    ggplot(aes(income, median, fill = run)) +
    geom_col(position = "dodge") +
    facet_wrap(~ measure, scales = "free_y") +
    labs(
      title = paste0(poi_label, ": median accessibility by income group"),
      x = "Income group", y = "Median accessibility (utils)", fill = "Run"
    ) +
    theme(axis.text.x = element_text(angle = 45, hjust = 1))
  save_plot(bar_income, output_dir, "bar_medianacc_by_income.png", width = 10, height = 7)

  # Difference of the medians by income group
  bar_income_delta <- comparison_income %>%
    ggplot(aes(income, delta_median)) +
    geom_col(fill = "#2166ac") +
    geom_hline(yintercept = 0, linewidth = 0.3) +
    facet_wrap(~ measure, scales = "free_y") +
    labs(
      title = paste0(poi_label, ": change of median accessibility (", delta_label, ")"),
      x = "Income group", y = paste0("Δ median accessibility (", delta_label, ", utils)")
    ) +
    theme(axis.text.x = element_text(angle = 45, hjust = 1))
  save_plot(bar_income_delta, output_dir, "bar_delta_medianacc_by_income.png", width = 10, height = 7)

  # Difference of the medians by district
  bar_district_delta <- comparison_district %>%
    ggplot(aes(district, delta_median)) +
    geom_col(fill = "#2166ac") +
    geom_hline(yintercept = 0, linewidth = 0.3) +
    facet_wrap(~ measure, scales = "free_y") +
    labs(
      title = paste0(poi_label, ": change of median accessibility by district (", delta_label, ")"),
      x = "District", y = paste0("Δ median accessibility (", delta_label, ", utils)")
    ) +
    theme(axis.text.x = element_text(angle = 45, hjust = 1))
  save_plot(bar_district_delta, output_dir, "bar_delta_medianacc_by_district.png", width = 11, height = 8)

  # Boxplot of multimodal accessibility by district, both runs
  boxplot_district <- df_acc %>%
    filter(!is.na(district)) %>%
    ggplot(aes(district, Multimodal, fill = run)) +
    geom_boxplot(outlier.alpha = 0.2, na.rm = TRUE) +
    labs(
      title = paste0(poi_label, ": multimodal accessibility by district"),
      x = "District", y = "Multimodal accessibility (utils)", fill = "Run"
    ) +
    theme(axis.text.x = element_text(angle = 45, hjust = 1))
  save_plot(boxplot_district, output_dir, "boxplot_mmacc_by_district.png", width = 10, height = 6)

  # --------------------------------------------------------
  # Maps on a 1 km grid
  # --------------------------------------------------------
  # Median multimodal accessibility per grid cell and run, then the difference
  # of the cell medians. Cells with fewer than grid_min_persons persons in
  # either run are left out of the difference map.

  grid_medians <- df_acc %>%
    filter(!is.na(cell_id)) %>%
    group_by(cell_id, run) %>%
    summarise(persons = n(), median_mm = median(Multimodal, na.rm = TRUE), .groups = "drop")

  grid_runs_sf <- grid_sf %>%
    inner_join(grid_medians, by = "cell_id")

  map_runs <- base_map() +
    geom_sf(data = grid_runs_sf, aes(fill = median_mm), color = NA) +
    geom_sf(data = ring_sf, fill = NA, color = "grey20", linetype = "dashed", linewidth = 0.4) +
    poi_overlay(locations_sf) +
    scale_fill_viridis_c(name = "Median multimodal\naccessibility (utils)") +
    facet_wrap(~ run) +
    zoom_to_berlin() +
    labs(title = paste0(poi_label, ": median multimodal accessibility per 1 km cell")) +
    map_theme()
  save_plot(map_runs, map_dir, "map_grid_mmacc_by_run.png", width = 12, height = 5.5, bg = "white")

  grid_delta_sf <- grid_medians %>%
    pivot_wider(names_from = run, values_from = c(persons, median_mm)) %>%
    filter(.data[[paste0("persons_", ref_run)]] >= grid_min_persons,
           .data[[paste0("persons_", cmp_run)]] >= grid_min_persons) %>%
    mutate(delta = .data[[paste0("median_mm_", cmp_run)]] - .data[[paste0("median_mm_", ref_run)]]) %>%
    { inner_join(grid_sf, ., by = "cell_id") }

  write_csv(st_drop_geometry(grid_delta_sf), file.path(output_dir, "grid_delta_mmacc.csv"))

  # Colour scale capped at the 95th percentile of |change|, values beyond get the end colours
  colour_limit <- unname(quantile(abs(grid_delta_sf$delta), 0.95, na.rm = TRUE))
  if (!is.finite(colour_limit) || colour_limit == 0) colour_limit <- 1

  map_delta <- base_map() +
    geom_sf(data = grid_delta_sf, aes(fill = delta), color = NA) +
    geom_sf(data = ring_sf, fill = NA, color = "grey20", linetype = "dashed", linewidth = 0.4) +
    poi_overlay(locations_sf) +
    scale_fill_gradient2(
      low = "#b2182b", mid = "grey92", high = "#2166ac", midpoint = 0,
      limits = c(-colour_limit, colour_limit), oob = scales::squish,
      name = paste0("Δ median multimodal\naccessibility\n(", delta_label, ", utils)")
    ) +
    zoom_to_berlin() +
    labs(title = paste0(poi_label, ": change of multimodal accessibility (", delta_label, ")"),
         subtitle = paste0("1 km cells with at least ", grid_min_persons, " persons in both runs")) +
    map_theme() +
    theme(plot.subtitle = element_text(hjust = 0.5))
  save_plot(map_delta, map_dir, "map_grid_mmacc_delta.png", height = 6.5, bg = "white")

  message("Results written to ", output_dir, " and ", map_dir)
}
