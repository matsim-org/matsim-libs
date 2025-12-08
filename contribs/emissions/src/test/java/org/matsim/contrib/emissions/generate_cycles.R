{
  library(tidyverse)
  library(glue)
  library(patchwork)

  # ==== Paths to ressources ====
  sumo_path <- "/Users/aleksander/Documents/VSP/PHEMTest/sumo"
  style_path <- "/Users/aleksander/Documents/VSP/PHEMTest/style"
}

# Generate the sinusodial driving cycles
{

  all <- list()
  groups <- list()

  i <- 0
  for (vel in seq(0, 144, by=0.36)){
    for (acc in seq(0.1, 2, by = 0.01)){
      V_amp <- (12.5*acc)/sin(pi/2)

      if (V_amp > vel) {
        # print(c(vel, acc))
        next
      }

      groups[[length(groups) + 1]] <- tibble(group=i, vel=vel, acc=acc)
      i <- i + 1

      cycle <- data.frame(
        tibble(
          time = seq(0, 99),
          vel = (V_amp)*sin(4*pi*time/100) + vel,
          acc = (4*pi*V_amp * cos((4*pi*time)/100))/100
        )
      ) #%>%
         #mutate(acc = coalesce(lead(vel)-vel, 0))

      #ggplot(cycle) +
      #  geom_line(aes(x=time, y=vel)) +
      #  geom_line(aes(x=time, y=acc), color="red")

      # print(acc)
      # print(mean(cycle$acc[cycle$acc >= 0]))

      # write_delim(cycle, glue("{style_path}/cycles/cycle_v{vel}_a{acc}.csv"), delim=";", col_names = FALSE)
      all[[length(all) + 1]] <- cycle
    }
  }

  combined <- bind_rows(all) %>%
    mutate(time = row_number()-1)

  groups <- bind_rows(groups)

  write_delim(combined, glue("{style_path}/combined_cycle.csv"), delim=";", col_names = FALSE)
}

# Driving Cycles characteristics
{
  WLTP <- read_delim("/Users/aleksander/Documents/VSP/PHEMTest/sumo/sumo_input.csv", delim=";", col_names=c("time", "vel", "acc"), col_types=c(col_integer(), col_double(), col_double()))
  WLTP_inv <- read_delim("/Users/aleksander/Documents/VSP/PHEMTest/sumo/sumo_input_inverted_time.csv", delim=";", col_names=c("time", "vel", "acc"), col_types=c(col_integer(), col_double(), col_double()))
  CADC <- read_delim("/Users/aleksander/Documents/VSP/PHEMTest/CADC/CADC.csv", delim=";", col_names=c("time", "vel", "acc"), col_types=c(col_integer(), col_double(), col_double()))

  WLTP <- WLTP %>%
    mutate(vel = vel/3.6) %>%
    mutate(acc = lead(vel) - vel) %>%
    mutate(acc = ifelse(is.na(acc), 0, acc))

  WLTP_inv <- WLTP_inv %>%
    mutate(vel = vel/3.6) %>%
    mutate(acc = lead(vel) - vel) %>%
    mutate(acc = ifelse(is.na(acc), 0, acc))

  CADC <- CADC %>%
    mutate(vel = vel/3.6) %>%
    mutate(acc = lead(vel) - vel) %>%
    mutate(acc = ifelse(is.na(acc), 0, acc))

  # WLTP
  WLTP.low <- WLTP[WLTP$time < 589, ]
  WLTP.medium <- WLTP[WLTP$time > 589 & WLTP$time < 1022, ]
  WLTP.high <- WLTP[WLTP$time > 1022 & WLTP$time < 1477, ]
  WLTP.extra_high <- WLTP[WLTP$time > 1477, ]

  WLTP.mean_acc <- mean(WLTP$acc[WLTP$acc > 0])
  WLTP.mean_vel <- mean(WLTP$vel)

  WLTP.low.mean_acc <- mean(WLTP.low$acc[WLTP.low$acc > 0])
  WLTP.medium.mean_acc <- mean(WLTP.medium$acc[WLTP.medium$acc > 0])
  WLTP.high.mean_acc <- mean(WLTP.high$acc[WLTP.high$acc > 0])
  WLTP.extra_high.mean_acc <- mean(WLTP.extra_high$acc[WLTP.extra_high$acc > 0])

  WLTP.low.mean_vel <- mean(WLTP.low$vel)
  WLTP.medium.mean_vel <- mean(WLTP.medium$vel)
  WLTP.high.mean_vel <- mean(WLTP.high$vel)
  WLTP.extra_high.mean_vel <- mean(WLTP.extra_high$vel)

  # WLTP inv
  WLTP_inv.mean_acc <- mean(WLTP_inv$acc[WLTP_inv$acc > 0])
  WLTP_inv.mean_vel <- mean(WLTP_inv$vel)

  # CADC
  CADC.mean_acc <- mean(CADC$acc[CADC$acc > 0])
  CADC.mean_vel <- mean(CADC$vel)
}

# Display the results in a heatmap (cycles)
{
  fuel <- "petrol"

  # MATSim
  r <- read_matsim(glue("{style_path}/combined_matsim_{fuel}_output.csv"))
  matsim_file <- r[[1]] %>%
    mutate(group = floor(segment/10)) %>%
    group_by(component, group) %>%
    summarise(value = sum(value)) %>%
    merge(groups, by="group")
  intervals <- r[[2]]

  # Sumo-PL5
  sumo_file <- read_sumo(glue("{style_path}/combined_sumo_{fuel}_output.csv"), intervals) %>%
    mutate(group = floor(segment/10)) %>%
    group_by(component, group) %>%
    summarise(value = sum(value)) %>%
    merge(groups, by="group")

  difference <- merge(matsim_file, sumo_file, by=c("group", "component")) %>%
    mutate(delta = value.x-value.y, percent = 100*(value.x/value.y)-100) # TODO percent works okay-ish, but try to find a better solution

  # Single plot
  selected_component <- "PMx"
  difference_sel <- difference %>%
    filter(component==selected_component) %>%
    filter(vel.x < 60)

  ggplot() +
    geom_tile(data=difference_sel, aes(x = vel.x, y = acc.x, fill = delta, color = delta)) +
    geom_contour(data=difference_sel, aes(x = vel.x, y = acc.x, z = delta), color = "black") +
    labs(fill = "Emission", title = glue("Simulation Heatmap for {selected_component}")) +
    #scale_fill_viridis_c(trans="log") +
    # facet_wrap(~component, scales="free")
    scale_fill_gradient2(low="red", mid="white", high="green", midpoint = 0) +
    scale_color_gradient2(low="red", mid="white", high="green", midpoint = 0) +
    geom_point(aes(x=WLTP.mean_vel, y=WLTP.mean_acc), color="black") +
    geom_label(aes(x=WLTP.mean_vel, y=WLTP.mean_acc), color="black", label="WLTP", nudge_y = -0.02) +
    geom_point(aes(x=WLTP_inv.mean_vel, y=WLTP_inv.mean_acc), color="red") +
    geom_label(aes(x=WLTP_inv.mean_vel, y=WLTP_inv.mean_acc), color="red", label="inv.WLTP", nudge_y = 0.02) +
    geom_point(aes(x=CADC.mean_vel, y=CADC.mean_acc), color="blue") +
    geom_label(aes(x=CADC.mean_vel, y=CADC.mean_acc), color="blue", label="CADC", nudge_y = 0.02) +
    theme_minimal() +
    theme( panel.background = element_rect(fill = "grey90", color = NA), # gray panel behind tiles
      plot.background = element_rect(fill = "grey90", color = NA), # gray margins
      panel.grid.major = element_line(color = "grey40"), # major grid lines dark gray
      panel.grid.minor = element_line(color = "grey60", linetype = "dashed"), # optional minor grid
      axis.text = element_text(color = "black"), # axis labels
      axis.title = element_text(color = "black"),
      axis.ticks = element_line(color = "black") # axis ticks
   )

  # 5-plot
  plots <- difference %>%
    split(.$component) %>%
    map(~{
      df <- .x
      ggplot(df, aes(x = vel.x, y = acc.x, fill = delta, color=percent)) +
        geom_tile() +
        geom_contour(aes(x = vel.x, y = acc.x, z = delta), color = "black") +
        geom_contour(aes(x = vel.x, y = acc.x, z = delta), breaks = 0, color = "orange", linewidth = 1.1) +
        geom_point(aes(x=WLTP.mean_vel, y=WLTP.mean_acc), color="black") +
        geom_point(aes(x=WLTP.low.mean_vel, y=WLTP.low.mean_acc), color="gray20") +
        geom_point(aes(x=WLTP.medium.mean_vel, y=WLTP.medium.mean_acc), color="gray40") +
        geom_point(aes(x=WLTP.high.mean_vel, y=WLTP.high.mean_acc), color="gray60") +
        geom_point(aes(x=WLTP.extra_high.mean_vel, y=WLTP.extra_high.mean_acc), color="gray80") +
        # geom_text(aes(x=WLTP.mean_vel, y=WLTP.mean_acc), color="black", label="WLTP", nudge_y = -0.02) +
        geom_point(aes(x=WLTP_inv.mean_vel, y=WLTP_inv.mean_acc), color="red") +
        # geom_text(aes(x=WLTP_inv.mean_vel, y=WLTP_inv.mean_acc), color="red", label="inv.WLTP", nudge_y = 0.02) +
        geom_point(aes(x=CADC.mean_vel, y=CADC.mean_acc), color="blue") +
        # geom_text(aes(x=CADC.mean_vel, y=CADC.mean_acc), color="blue", label="CADC", nudge_y = 0.02) +
        coord_fixed(ratio = (diff(range(difference$vel.x)) / diff(range(difference$acc.x)))) +
        scale_fill_gradient2(low="red", mid="white", high="green", midpoint = 0) +
        scale_color_gradient2(low="red", mid="white", high="green", midpoint = 0) +
        labs(title = df$component[1], fill = "Emission") +
        theme_minimal() +
        theme(
          panel.background = element_rect(fill = "grey90", color = NA),
          plot.background  = element_rect(fill = "grey90", color = NA),
          panel.grid.major = element_line(color = "grey40"),
          panel.grid.minor = element_line(color = "grey60", linetype = "dashed"),
          axis.text        = element_text(color = "black"),
          axis.title       = element_text(color = "black"),
          axis.ticks       = element_line(color = "black")
        )
    })

  wrap_plots(plots, ncol = 2) +
    plot_annotation(title= glue("DeltaHeatMap for {fuel}"))
}

# Histogram over the datapoint distribution TODO WIP
{

  # WLTP ALL
  v12.9_a0.42 <- combined %>%
    filter(time >= 547900 & time < 548000)

  ggplot(WLTP, aes(x=vel, y=acc)) +
    geom_point(alpha = 0.3) +
    geom_density_2d(color = "black") +

  ggplot(v12.9_a0.42, aes(x=vel, y=acc)) +
    geom_point(alpha = 0.3, color="orange") +
    geom_density_2d(color="orange") +

  ggplot() +
    geom_point(data = WLTP, aes(x=vel, y=acc), alpha = 0.3) +
    geom_density_2d(data = WLTP, aes(x=vel, y=acc), color="black") +

    geom_point(data = v12.9_a0.42, aes(x=vel, y=acc), color="orange", alpha = 0.3) +
    geom_density_2d(data = v12.9_a0.42, aes(x=vel, y=acc), color="orange")

  # WLTP High

  v <- groups[abs(groups$vel - round(WLTP.high.mean_vel, digits=1)) < 1e-6 & abs(groups$acc - round(WLTP.high.mean_acc, digits = 2)) < 1e-6, ]
  v.WLTP.high <- combined %>%
    filter(time >= v$group*100 & time < v$group*100+100)

  ggplot(WLTP.high, aes(x=vel, y=acc)) +
    geom_point(alpha = 0.3) +
    geom_density_2d(color = "black") +

    ggplot(v.WLTP.high, aes(x=vel, y=acc)) +
    geom_point(alpha = 0.3, color="orange") +
    geom_density_2d(color="orange") +

    ggplot() +
    geom_point(data = WLTP.high, aes(x=vel, y=acc), alpha = 0.3) +
    geom_density_2d(data = WLTP.high, aes(x=vel, y=acc), color="black") +

    geom_point(data = v.WLTP.high, aes(x=vel, y=acc), color="orange", alpha = 0.3) +
    geom_density_2d(data = v.WLTP.high, aes(x=vel, y=acc), color="orange")
}

# =====

# Display the results in a heatmap (dots) TODO remove
{
  fuel <- "petrol"
  selected_component <- "NOx"
  velocity_low <- 0
  velocity_high <- 130
  acceleration_low <- -4
  acceleration_high <- 4

  sumo_output <- read_delim(glue("{sumo_path}/sumo_{fuel}_output_heatmap.csv"),
                            delim = ";",
                            col_names = c("time", "velocity", "acceleration", "slope", "CO", "CO2", "HC", "PMx", "NOx", "fuel", "electricity"),
                            col_types = cols(
                              time = col_integer(),
                              velocity = col_double(),
                              acceleration = col_double(),
                              slope = col_double(),
                              CO = col_double(),
                              CO2 = col_double(),
                              HC = col_double(),
                              PMx = col_double(),
                              NOx = col_double(),
                              fuel = col_double(),
                              electricity = col_double())) %>%
    pivot_longer(cols = c("CO", "CO2", "HC", "PMx", "NOx"), names_to = "component", values_to="value") %>%
    filter(component == selected_component, velocity_low < velocity & velocity < velocity_high & acceleration_low < acceleration & acceleration < acceleration_high) %>%
    mutate(model = "SUMO_0", value=value/1000)

  ggplot() +
    geom_tile(data=sumo_output, aes(x = velocity, y = acceleration, fill = value, color = value)) +
    labs(fill = "Emission", title = glue("Simulation Heatmap for {selected_component}")) +
    #scale_fill_viridis_c(trans="log") +
    scale_fill_viridis_c() +
    scale_color_viridis_c() +
    geom_point(aes(x=WLTP.mean_vel, y=WLTP.mean_acc), color="black") +
    geom_point(aes(x=WLTP_inv.mean_vel, y=WLTP_inv.mean_acc), color="red") +
    geom_point(aes(x=CADC.mean_vel, y=CADC.mean_acc), color="blue") +
    # geom_path(data = sumo_input, aes(x = velocity/3.6, y = acceleration)) +
    # geom_path(data = sumo_input_inverted_time, aes(x = velocity/3.6, y = acceleration), color="red") +
    theme_minimal()
}
