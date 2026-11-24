# ==========================================================
# Traffic congestion difference map: autofrei policy - base
# Uses SimWrapper's analysis/traffic/traffic_stats_by_link_daily.csv
# from both runs and the run's output network for link geometry.
# ==========================================================

suppressPackageStartupMessages({
  library(dplyr)
  library(readr)
  library(sf)
  library(ggplot2)
  library(xml2)
})

# ==========================================================
# Directories
# ==========================================================

runs_dir <- path.expand(
  "~/public-svn/matsim/scenarios/countries/de/berlin/projects/autofrei/1pct-v6.4"
)
run_dirs <- list(
  base   = file.path(runs_dir, "berlin-autofrei-v6.4-baseCtd"),
  policy = file.path(runs_dir, "berlin-autofrei-v6.4-policy")
)

shape_dir <- path.expand(
  "~/git/matsim-libs/contribs/accessibility/src/main/R/THESIS/input/Berlin shapefiles"
)
output_dir <- path.expand(
  "~/public-svn/matsim/scenarios/countries/de/berlin/projects/autofrei/_dev/2026-11-19-v6.4-spa-uni-jobcenter-NoMonConstant/congestion"
)
dir.create(output_dir, recursive = TRUE, showWarnings = FALSE)

# Measure to compare. congestion_index is SimWrapper's daily index
# (1 = free flow all day, lower = more congested). Other columns in the file
# such as avg_speed or road_capacity_utilization work the same way.
measure <- "simulated_traffic_volume"

# Colour scale is capped at +/- this value; larger changes get the end colours.
colour_limit <- 50

# Only draw links that carry at least this many vehicles per day in either run
# (1pct sample, so small numbers). Set to 0 to keep every link.
min_volume <- 1

# Changes smaller than this (in either direction) count as unchanged: those
# links are drawn thin and grey, all others thick and coloured.
unchanged_threshold <- 0.01

# Line widths for unchanged and changed links.
width_unchanged <- 0.15
width_changed   <- 0.9

# Traffic volume: every link drawn at the same width, no unchanged/changed split.
is_volume <- measure == "simulated_traffic_volume"
if (is_volume) {
  unchanged_threshold <- 0
  width_unchanged <- 0.35
  width_changed   <- 0.35
}

# Colours: for volume (and capacity utilisation) an increase is bad, so
# red = increase, blue = decrease. For congestion index and speed a decrease
# is bad, so red = decrease, blue = increase.
increase_is_bad <- measure %in% c("simulated_traffic_volume", "road_capacity_utilization")
colour_low  <- if (increase_is_bad) "#2166ac" else "#b2182b"
colour_high <- if (increase_is_bad) "#b2182b" else "#2166ac"

# ==========================================================
# Read traffic stats
# ==========================================================

read_traffic <- function(run_dir) {
  path <- file.path(run_dir, "analysis", "traffic", "traffic_stats_by_link_daily.csv")
  if (!file.exists(path)) stop("Not found: ", path)
  df <- read_csv(path, show_col_types = FALSE, col_types = cols(link_id = col_character()))
  needed <- c("link_id", measure, "simulated_traffic_volume")
  missing <- setdiff(needed, names(df))
  if (length(missing) > 0) {
    stop("Column(s) missing in ", path, ": ", paste(missing, collapse = ", "),
         ". Available: ", paste(names(df), collapse = ", "))
  }
  df
}

traffic_base   <- read_traffic(run_dirs$base)
traffic_policy <- read_traffic(run_dirs$policy)

traffic_diff <- full_join(
  traffic_base %>%
    transmute(link_id, value_base = .data[[measure]], volume_base = simulated_traffic_volume),
  traffic_policy %>%
    transmute(link_id, value_policy = .data[[measure]], volume_policy = simulated_traffic_volume),
  by = "link_id"
) %>%
  mutate(
    volume_base   = coalesce(volume_base, 0),
    volume_policy = coalesce(volume_policy, 0),
    # a link missing from one run has zero traffic there
    value_base    = if (is_volume) volume_base else value_base,
    value_policy  = if (is_volume) volume_policy else value_policy,
    delta         = value_policy - value_base,
    delta_volume  = volume_policy - volume_base
  )

write_csv(traffic_diff, file.path(output_dir, paste0(measure, "_policy_minus_base.csv")))

# ==========================================================
# Link geometry from the output network
# ==========================================================
# Takes the base run's *output_network.xml.gz (EPSG:25832). Link ids are the
# same in both runs; only allowed modes differ.

network_file <- list.files(run_dirs$base, pattern = "output_network\\.xml\\.gz$", full.names = TRUE)[1]
if (is.na(network_file)) stop("No *output_network.xml.gz in ", run_dirs$base)
message("Reading network: ", network_file)

net   <- read_xml(network_file)
nodes <- xml_find_all(net, "/network/nodes/node")
node_xy <- tibble(
  id = xml_attr(nodes, "id"),
  x  = as.numeric(xml_attr(nodes, "x")),
  y  = as.numeric(xml_attr(nodes, "y"))
)
links <- xml_find_all(net, "/network/links/link")
link_df <- tibble(
  link_id = xml_attr(links, "id"),
  from    = xml_attr(links, "from"),
  to      = xml_attr(links, "to")
) %>%
  filter(link_id %in% traffic_diff$link_id) %>%
  left_join(node_xy %>% rename(from = id, fx = x, fy = y), by = "from") %>%
  left_join(node_xy %>% rename(to = id, tx = x, ty = y), by = "to")
rm(net, nodes, links)

link_geom <- st_sfc(
  lapply(seq_len(nrow(link_df)), function(i) {
    st_linestring(matrix(c(link_df$fx[i], link_df$tx[i],
                           link_df$fy[i], link_df$ty[i]), ncol = 2))
  }),
  crs = 25832
)
links_sf <- st_sf(link_id = link_df$link_id, geometry = link_geom) %>%
  inner_join(traffic_diff, by = "link_id")

# ==========================================================
# Berlin boundary and S-Bahn ring
# ==========================================================

berlin_sf <- st_read(file.path(shape_dir, "Berlin_Bezirke.shp"), quiet = TRUE) %>%
  st_transform(25832)
ring_sf <- st_read(file.path(shape_dir, "Berlin_sbahn_ring.geojson"), quiet = TRUE)
if (is.na(st_crs(ring_sf))) st_crs(ring_sf) <- 4326
ring_sf <- st_transform(ring_sf, 25832)
berlin_bbox <- st_bbox(berlin_sf)

# ==========================================================
# Map
# ==========================================================

links_used <- links_sf %>%
  filter(pmax(volume_base, volume_policy) >= min_volume)

# Only links inside Berlin (touching the district shapes) get a colour; links
# outside the city are drawn in plain light grey for context.
berlin_union <- st_union(berlin_sf)
links_used$in_berlin <- lengths(st_intersects(links_used, berlin_union)) > 0
links_outside <- links_used %>% filter(!in_berlin)
links_used    <- links_used %>% filter(in_berlin)

# Links with no car traffic left in the policy run (e.g. inside the ring) have
# no measure value there; they are drawn in dark grey instead of a colour.
links_changed <- links_used %>%
  filter(!is.na(delta), abs(delta) >= unchanged_threshold) %>%
  arrange(abs(delta))
links_unchanged <- links_used %>%
  filter(!is.na(delta), abs(delta) < unchanged_threshold)
links_no_cars <- links_used %>% filter(is.na(value_policy), !is.na(value_base))

measure_label <- switch(
  measure,
  congestion_index = "congestion index",
  avg_speed = "average speed",
  road_capacity_utilization = "capacity utilisation",
  simulated_traffic_volume = "car volume",
  measure
)

map_congestion_diff <- ggplot() +
  geom_sf(data = berlin_sf, fill = "grey90", color = "grey70", linewidth = 0.3) +
  geom_sf(data = links_outside, color = "grey85", linewidth = width_unchanged) +
  geom_sf(data = links_unchanged, color = "grey80", linewidth = width_unchanged) +
  geom_sf(data = links_no_cars, color = "grey45", linewidth = width_unchanged * 2) +
  geom_sf(data = links_changed, aes(color = delta), linewidth = width_changed) +
  geom_sf(data = ring_sf, fill = NA, color = "grey20", linetype = "dashed", linewidth = 0.5) +
  scale_color_gradient2(
    low = colour_low, mid = "grey88", high = colour_high, midpoint = 0,
    limits = c(-colour_limit, colour_limit), oob = scales::squish,
    name = paste0("Δ ", measure_label, "\n(policy - base)")
  ) +
  coord_sf(
    xlim = c(berlin_bbox["xmin"], berlin_bbox["xmax"]),
    ylim = c(berlin_bbox["ymin"], berlin_bbox["ymax"])
  ) +
  labs(
    title = paste0("Traffic ", measure_label, " change: policy - base"),
    subtitle = if (is_volume)
      "Red = more cars per day under the policy, blue = fewer (1pct sample)"
    else if (measure == "congestion_index")
      paste0("Red = more congested under the policy, blue = less congested; light grey: change < ", unchanged_threshold, "; dark grey: no car traffic in policy")
  ) +
  theme_void() +
  theme(
    legend.position = "right",
    legend.key.height = unit(1.5, "cm"),
    plot.title = element_text(face = "bold", hjust = 0.5),
    plot.subtitle = element_text(hjust = 0.5, size = 8)
  )

ggsave(
  file.path(output_dir, paste0("map_", measure, "_policy_minus_base.png")),
  map_congestion_diff, dpi = 300, width = 7, height = 5, bg = "white"
)

# Short summary of the change, for the text
summary_congestion <- links_used %>%
  st_drop_geometry() %>%
  summarise(
    links = n(),
    links_without_cars_in_policy = sum(is.na(value_policy) & !is.na(value_base)),
    mean_delta   = mean(delta, na.rm = TRUE),
    median_delta = median(delta, na.rm = TRUE),
    share_more_congested = mean(delta < 0, na.rm = TRUE) * 100,
    share_less_congested = mean(delta > 0, na.rm = TRUE) * 100
  ) %>%
  mutate(across(where(is.numeric), ~ round(.x, 3)))
print(summary_congestion)
write_csv(summary_congestion, file.path(output_dir, paste0("summary_", measure, "_policy_minus_base.csv")))
