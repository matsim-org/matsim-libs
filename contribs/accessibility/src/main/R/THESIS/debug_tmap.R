# ==========================================================
# Interactive debug map (tmap view mode)
# ==========================================================
# Run analysis.R first, then source this file once:
#   source("debug_tmap.R")
# and call view_persons() as often as you like, e.g.
#   view_persons()                     # multimodal, last opportunity of the loop
#   view_persons(car_persons)          # car only
#   view_persons(sample_n = 20000)     # faster with many points
#   view_persons(shape_by = "economic_status")   # symbol shape per group
#   view_persons(breaks = NULL)                  # continuous colours
#   view_persons(breaks = c(-Inf, seq(-1, 0, 0.2), Inf))  # finer bins
# Click a point to see the person's values.

suppressPackageStartupMessages({
  library(dplyr)
  library(readr)
  library(sf)
  library(tmap)
})
tmap_mode("view")

# Person attributes: a small extract of the run's output_persons.csv made with
# DuckDB (columns: person, economic_status). Set the path to where you saved it.
# Comma- and semicolon-separated files both work; cached per attribute.
persons_attr_file <- path.expand(
  "~/git/matsim-libs/contribs/accessibility/src/main/R/THESIS/input/persons_econ.csv"
)
persons_attr_cache <- list()

persons_attr_path <- function() {
  f <- persons_attr_file
  if (!file.exists(f) && file.exists(sub("\\.gz$", "", f))) f <- sub("\\.gz$", "", f)
  if (!file.exists(f)) stop("Person attributes file not found: ", persons_attr_file)
  f
}

# DuckDB's COPY writes commas, MATSim writes semicolons: check the header line
persons_attr_delim <- function() {
  header <- readLines(persons_attr_path(), n = 1)
  if (grepl(";", header, fixed = TRUE)) ";" else ","
}

# column names only (reads the header line, nothing else)
persons_attr_columns <- function() {
  names(read_delim(persons_attr_path(), delim = persons_attr_delim(), n_max = 0,
                   show_col_types = FALSE, progress = FALSE))
}

read_persons_attr <- function(column) {
  key <- column
  if (is.null(persons_attr_cache[[key]])) {
    available <- persons_attr_columns()
    if (!column %in% available) {
      stop("'", column, "' is not a column of ", basename(persons_attr_file),
           ". Available: ", paste(available, collapse = ", "))
    }
    persons_attr_cache[[key]] <<- read_delim(
      persons_attr_path(), delim = persons_attr_delim(),
      col_select = c("person", all_of(column)),
      col_types = cols(.default = col_character()),
      show_col_types = FALSE, progress = FALSE
    ) %>%
      rename(personId = person)
  }
  persons_attr_cache[[key]]
}

# Income level from the nine income brackets used in the bar/box plots
# (df_acc$income from analysis.R): three brackets per level.
income_levels <- list(
  Low    = c("500", "900"),
  # Medium = c("1500", "2000", "2600", "3000", "3600"),
  High   = c("4600", "5600")
)

add_income_level <- function(persons) {
  if (!exists("df_acc")) stop("df_acc not found: run analysis.R first.")
  lookup <- df_acc %>%
    transmute(
      personId = as.character(personId),
      income_level = factor(
        case_when(
          as.character(income) %in% income_levels$Low    ~ "Low",
          as.character(income) %in% income_levels$Medium ~ "Medium",
          as.character(income) %in% income_levels$High   ~ "High"
        ),
        # legend order: High, Medium, Low
        levels = rev(names(income_levels))
      )
    ) %>%
    distinct(personId, .keep_all = TRUE)
  persons %>%
    mutate(personId = as.character(personId)) %>%
    select(-any_of("income_level")) %>%
    left_join(lookup, by = "personId")
}

view_persons <- function(persons = mm_persons,
                         measure = "delta",
                         sample_n = NULL,
                         show_berlin = TRUE,
                         show_ring = TRUE,
                         show_pois = TRUE,
                         shape_by = NULL,
                         breaks = c(-Inf, seq(-3, 3, by = 0.5), Inf),
                         bbox = NULL) {
  if (is.null(persons)) stop("No persons data: run analysis.R first.")

  # optional: one symbol shape per group of a person attribute.
  # "income_level" comes from df_acc (Low/Medium/High); any other name is read
  # from persons_attr_file.
  if (identical(shape_by, "income_level")) {
    persons <- add_income_level(persons)
  } else if (!is.null(shape_by)) {
    attrs <- read_persons_attr(shape_by)
    persons <- persons %>%
      mutate(personId = as.character(personId)) %>%
      select(-any_of(shape_by)) %>%
      left_join(attrs %>% select(personId, all_of(shape_by)), by = "personId") %>%
      mutate(across(all_of(shape_by), ~ factor(.x)))
    # economic_status has five classes in the MATSim persons file; group them
    # into three, listed High, Medium, Low in the legend
    if (identical(shape_by, "economic_status")) {
      persons <- persons %>%
        mutate(economic_status = factor(
          case_when(
            economic_status %in% c("very_high", "high") ~ "High",
            # economic_status == "medium"                 ~ "Medium",
            economic_status %in% c("low", "very_low")   ~ "Low"
          ),
          # levels = c("High", "Medium", "Low")
          levels = c("High", "Low")
        ))
    }
    matched <- mean(persons$personId %in% attrs$personId) * 100
    message(round(matched, 1), "% of mapped persons found in ",
            basename(persons_attr_file))
  }

  persons_sf <- persons %>%
    filter(!is.na(.data[[measure]]), !is.na(homeX), !is.na(homeY)) %>%
    # persons without a group (e.g. the commented-out Medium) are not drawn
    filter(is.null(shape_by) | !is.na(.data[[shape_by %||% measure]])) %>%
    st_as_sf(coords = c("homeX", "homeY"), crs = 25832, remove = FALSE)

  if (!is.null(sample_n) && nrow(persons_sf) > sample_n) {
    persons_sf <- slice_sample(persons_sf, n = sample_n)
  }

  popup <- intersect(
    c("personId", measure, shape_by, "homeX", "homeY", "logSum_base", "logSum_policy",
      "accessibility_base", "accessibility_policy"),
    names(persons_sf)
  )

  # breaks = NULL: continuous colour gradient (shows small differences);
  # otherwise binned at the given breaks, e.g. breaks = seq(-1, 1, 0.25)
  delta_scale <- if (is.null(breaks)) {
    tm_scale_continuous(midpoint = 0, values = "brewer.rd_bu")
  } else {
    tm_scale_intervals(breaks = breaks, midpoint = 0, values = "brewer.rd_bu")
  }

  m <- if (is.null(shape_by)) {
    tm_shape(persons_sf, bbox = bbox) +
      tm_dots(
        fill = measure,
        size = 0.9,
        fill.scale = delta_scale,
        popup.vars = popup
      )
  } else {
    # filled shapes (circle, square, diamond, triangle up/down) keep the colour
    tm_shape(persons_sf, bbox = bbox) +
      tm_symbols(
        fill = measure,
        fill.scale = delta_scale,
        shape = shape_by,
        # shape.scale = tm_scale_categorical(values = c(21, 22, 24, 23, 25)),
        # same shapes per group as before (High = up, Medium = circle, Low = down),
        # listed in the legend order High, Medium, Low
        # shape.scale = tm_scale_categorical(values = c(24, 21, 25)),
        shape.scale = tm_scale_categorical(values = c(24, 25)),
        # delta legend drawn with circles, not the first group's shape
        fill.legend = tm_legend(shape = 21),
        size = 0.9,
        popup.vars = popup
      )
  }

  if (show_berlin) {
    m <- m + tm_shape(berlin_sf) + tm_borders(col = "grey30", lwd = 1.5)
  }
  if (show_ring && exists("ring_sf")) {
    # the ring file may hold a polygon or a line; draw its outline either way
    ring_is_polygon <- all(st_geometry_type(ring_sf) %in% c("POLYGON", "MULTIPOLYGON"))
    m <- m + tm_shape(ring_sf) +
      if (ring_is_polygon) {
        tm_borders(col = "#6a3d9a", lwd = 3, lty = "dashed")
      } else {
        tm_lines(col = "#6a3d9a", lwd = 3, lty = "dashed")
      }
  }
  if (show_pois && exists("locations_sf") && nrow(locations_sf) > 0) {
    m <- m + tm_shape(locations_sf) +
      tm_dots(fill = "black", size = 0.9, popup.vars = intersect("name", names(locations_sf)))
  }

  # returned (not just printed) so you can keep it: m <- view_persons()
  m
}

# ==========================================================
# Export a square map with the legends beside it
# ==========================================================
# Same arguments as view_persons() (data, shape_by, breaks, ...), plus the box:
#   save_persons_map("lichtenberg_uni.png", my_persons, shape_by = "economic_status")
# (my_persons = the data frame you pass to view_persons(); without one it uses
# mm_persons and crops it to the square)
# The map shows exactly the square centre +/- half_width (metres, EPSG:25832);
# the legends go to the right of it. width/height in pixels at the given dpi.

# Square study area as an sf polygon, aligned north-south. The centre is given
# in EPSG:25832; the square is built in Web Mercator (EPSG:3857, north = up),
# because a square that is straight in UTM 32 looks rotated by ~3 degrees in
# Berlin. half_width is in real metres (Mercator units are scaled by 1/cos(lat)).
area_box <- function(center, half_width, name = "area") {
  center_pt <- st_sfc(st_point(center), crs = 25832)
  lat <- st_coordinates(st_transform(center_pt, 4326))[2]
  c3857 <- st_coordinates(st_transform(center_pt, 3857))
  hw <- half_width / cos(lat * pi / 180)
  st_sf(
    name = name,
    geometry = st_as_sfc(st_bbox(
      c(xmin = c3857[1] - hw, ymin = c3857[2] - hw,
        xmax = c3857[1] + hw, ymax = c3857[2] + hw),
      crs = st_crs(3857)
    ))
  )
}

# Persons living inside that north-south square
in_area <- function(persons, center, half_width) {
  box <- st_transform(area_box(center, half_width), 25832)
  persons %>%
    filter(!is.na(homeX), !is.na(homeY)) %>%
    st_as_sf(coords = c("homeX", "homeY"), crs = 25832, remove = FALSE) %>%
    st_filter(box) %>%
    st_drop_geometry()
}

save_persons_map <- function(file, ...,
                             center = c(805074, 5827333),  # S Lichtenberg
                             half_width = 500,
                             basemap = "Esri.WorldTopoMap",
                             width = 2400, height = 1800, dpi = 300) {
  old_mode <- tmap_mode("plot")
  on.exit(tmap_mode(old_mode), add = TRUE)

  map_bbox <- st_bbox(area_box(center, half_width))

  m <- tm_basemap(basemap) +
    view_persons(..., bbox = map_bbox) +
    tm_layout(legend.outside = TRUE, legend.outside.position = "right", frame = TRUE)

  tmap_save(m, file, width = width, height = height, dpi = dpi)
  message("Saved ", normalizePath(file))
  invisible(m)
}

# ==========================================================
# Berlin overview map with the study area(s) outlined
# ==========================================================
# The citywide person map from analysis.R (plot_person_delta_map, same colours
# and bins) with one black box per study area, e.g.
#   save_berlin_map_with_boxes("berlin_citycenter_boxes.png",
#                              areas = list(Steglitz = c(793544, 5820541)),
#                              half_width = 1000)
# Run analysis.R first (for the opportunity you want, e.g. citycenter).

save_berlin_map_with_boxes <- function(file, areas, half_width = 1000, mode = "mm",
                                       width = 8, height = 6.5, dpi = 300) {
  if (!exists("plot_person_delta_map")) stop("Run analysis.R first.")
  boxes <- do.call(rbind, Map(area_box, areas, half_width, names(areas)))
  # drawn north-up (EPSG:3857) so the boxes are straight
  berlin_bbox_3857 <- st_bbox(st_transform(berlin_sf, 3857))
  p <- suppressMessages(
    plot_person_delta_map(mode) +
      geom_sf(data = boxes, fill = NA, colour = "black", linewidth = 0.9, inherit.aes = FALSE) +
      # geom_sf_text(data = boxes, aes(label = name), nudge_y = 1.6 * (half_width + 600),
      #              size = 3.5, fontface = "bold", inherit.aes = FALSE) +
      coord_sf(crs = 3857,
               xlim = berlin_bbox_3857[c("xmin", "xmax")],
               ylim = berlin_bbox_3857[c("ymin", "ymax")])
  )
  ggsave(file, p, width = width, height = height, dpi = dpi, bg = "white")
  message("Saved ", normalizePath(file))
  invisible(p)
}
