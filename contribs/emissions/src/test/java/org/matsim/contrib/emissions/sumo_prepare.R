library("tidyverse")
library("glue")

# ==== Paths to ressources ====
sumo_path <- "/Users/aleksander/Documents/VSP/PHEMTest/sumo"
diff_path <- "/Users/aleksander/Documents/VSP/PHEMTest/diff"
hbefa_path <- "/Users/aleksander/Documents/VSP/PHEMTest/hbefa"

# Generate time inverted smo input file
{
  sumo_input <- read_delim("/Users/aleksander/Documents/VSP/PHEMTest/sumo/sumo_input.csv", delim=";", col_names=c("time", "velocity", "acceleration"))

  # Invert along time-axis
  sumo_input_inverted_time <- sumo_input %>%
    mutate(time = (-1) * (time-max(sumo_input$time))) %>%
    mutate(acceleration = (-1) * acceleration) %>%
    # This procedure can create "-0" entries which cause problems. Fixed by this check:
    mutate(time = if_else(time == 0, 0, time),
           acceleration = if_else(acceleration == 0, 0, acceleration)) %>%
    arrange(time)

  # Test plot for visual check
  ggplot() +
    geom_line(data=sumo_input, aes(x=time, y=velocity), color="red") +
    geom_line(data=sumo_input_inverted_time, aes(x=time, y=velocity), color="blue")

  # Write out inverted sumo input
  write_delim(sumo_input_inverted_time, "/Users/aleksander/Documents/VSP/PHEMTest/sumo/sumo_input_inverted_time.csv", delim=";", col_names=FALSE)
}

{
  sumo_output <- read_delim("/Users/aleksander/Documents/VSP/PHEMTest/sumo/sumo_output.csv", delim=";",
                            col_names = c("time", "velocity", "acceleration", "slope", "CO", "CO2", "HC", "PMx", "NOx", "fuel", "electricity"))
  sumo_output_inverted_time <- read_delim("/Users/aleksander/Documents/VSP/PHEMTest/sumo/sumo_output_inverted_time.csv", delim=";",
                                          col_names = c("time", "velocity", "acceleration", "slope", "CO", "CO2", "HC", "PMx", "NOx", "fuel", "electricity"))

  ggplot() +
    geom_line(data=sumo_output, aes(x=time, y=HC), color="red") +
    geom_line(data=sumo_output_inverted_time, aes(x=time, y=HC), color="blue")

}

# Generate input file with numerically derivated accelerations
{
  sumo_input <- read_delim("/Users/aleksander/Documents/VSP/PHEMTest/sumo/sumo_input.csv", delim=";", col_names=c("time", "velocity", "acceleration"))

  sumo_input_derivated_acc <- sumo_input %>%
    mutate(acceleration = replace_na(lead(velocity), 0) - velocity)

  ggplot() +
    geom_line(data=sumo_input, aes(x=time, y=acceleration), color="red") +
    geom_line(data=sumo_input_inverted_time, aes(x=time, y=acceleration), color="blue")

  write_delim(sumo_input_derivated_acc, "/Users/aleksander/Documents/VSP/PHEMTest/sumo/sumo_input_derivated_acc.csv", delim=";", col_names=FALSE)
}

# Generate input file with a WLTP data point
{
  # Read in the default wltp-cycle
  sumo_input <- read_delim("/Users/aleksander/Documents/VSP/PHEMTest/sumo/sumo_input.csv", delim=";", col_names=c("time", "velocity", "acceleration"))

  set.seed(42)
  samples <- 10
  selected_points <- list()

  for (x in 0:(samples-1)){
    # Select a random data point from the WLTP Cycle
    while (TRUE){
      r <- sample.int(nrow(sumo_input), 1)
      point <- sumo_input[r,]
      selected_points[x] <- r
      if(abs(point$acceleration) > 0.01){
        break
      }
    }

    needed_time <- round(130/(point$acceleration*3.6))

    if(point$acceleration < 0){
      i <- 0:abs(needed_time)
      sumo_output <- data.frame(
        time = i,
        vel = 130 + (i*point$acceleration*3.6),
        acc = point$acceleration
      )
    } else {
      i <- 0:needed_time
      sumo_output <- data.frame(
        time = i,
        vel = (i*point$acceleration)*3.6,
        acc = point$acceleration
      )
    }

    write_delim(sumo_output, glue("/Users/aleksander/Documents/VSP/PHEMTest/sumo/sumo_input_gen_{x}.csv"), delim=";", col_names=FALSE)
    print(point)
  }
}

# Generated pinput file plot
{
  index <- 2
  fuel <- "diesel"

  # Get the point
  sumo_input <- read_delim("/Users/aleksander/Documents/VSP/PHEMTest/sumo/sumo_input.csv", delim=";", col_names=c("time", "velocity", "acceleration"))
  point <- sumo_input[selected_points[[index]],]

  # Clear old data
  rm(list = ls(pattern = "^data\\."))

  # Load data from SUMO with PHEMLight and summarize for each interval
  data.SUMO <- read_delim(glue("{sumo_path}/sumo_{fuel}_output_gen_{index}.csv"),
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
    mutate(model = glue("SUMO_{index}"), value=value/1000)

  # Append all datasets together
  data_list <- mget(ls(pattern = "^data\\."), envir = .GlobalEnv)

  # recalc: gram -> gram per kilometer
  data <- do.call(rbind, data_list)

  # Line-Plot (for scenarios with more links)
  ggplot(data) +
    geom_line(aes(x=velocity, y=value, color=model), size=2) +
    #geom_point(aes(x=velocity, y=value, color=model), size=1) +
    scale_color_manual(values=c("#d21717", "#17d2a4", "#7d23cc", "#228b22")) +
    geom_vline(xintercept = point$velocity/3.6, size=2, fill = "blue") +
    facet_wrap(~component, scales="free") +
    ylab("emissions in g/km") +
    theme(text = element_text(size=22)) +
    ggtitle(glue("Constant acceleration scenario with acc={point$acceleration} for {fuel} cars ({index})"))

}

# Memory test Trajectory
{
  sumo_input <- read_delim("/Users/aleksander/Documents/VSP/PHEMTest/sumo/sumo_input.csv", delim=";", col_names=c("time", "velocity", "acceleration"))

  wltp_slice <- sumo_input[250:500,]

  set.seed(42)

  for (x in 0:5){
    random_vel <- sample.int(130, 1)
    random_acc <- runif(1)

    part1 <- data.frame(
      time = 1:250,
      velocity = rep(random_vel, 250),
      acceleration = rep(random_acc, 250)
    )

    test_input <- rbind(part1, wltp_slice)

    write_delim(test_input, glue("/Users/aleksander/Documents/VSP/PHEMTest/sumo/sumo_input_test_{x}.csv"), delim=";", col_names=FALSE)
  }
}

# Memory test Plot
{
  fuel <- "petrol"

  # Clear old data
  rm(list = ls(pattern = "^data\\."))

  # Load data from SUMO with PHEMLight and summarize for each interval
  data.SUMO_0 <- read_delim(glue("{sumo_path}/sumo_{fuel}_output_test_0.csv"),
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
    mutate(model = "SUMO_0", value=value/1000)

  # Load data from SUMO with PHEMLight and summarize for each interval
  data.SUMO_1 <- read_delim(glue("{sumo_path}/sumo_{fuel}_output_test_1.csv"),
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
    mutate(model = "SUMO_1", value=value/1000)

  # Load data from SUMO with PHEMLight and summarize for each interval
  data.SUMO_2 <- read_delim(glue("{sumo_path}/sumo_{fuel}_output_test_2.csv"),
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
    mutate(model = "SUMO_2", value=value/1000)

  # Load data from SUMO with PHEMLight and summarize for each interval
  data.SUMO_3 <- read_delim(glue("{sumo_path}/sumo_{fuel}_output_test_3.csv"),
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
    mutate(model = "SUMO_3", value=value/1000)

  # Load data from SUMO with PHEMLight and summarize for each interval
  data.SUMO_4 <- read_delim(glue("{sumo_path}/sumo_{fuel}_output_test_4.csv"),
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
    mutate(model = "SUMO_4", value=value/1000)

    # Append all datasets together
    data_list <- mget(ls(pattern = "^data\\."), envir = .GlobalEnv)

    # recalc: gram -> gram per kilometer
    data <- do.call(rbind, data_list)

    # Line-Plot (for scenarios with more links)
    ggplot(data) +
      geom_line(aes(x=time, y=value, color=model), size=2) +
      #geom_point(aes(x=velocity, y=value, color=model), size=1) +
      facet_wrap(~component, scales="free") +
      ylab("emissions in g/km") +
      theme(text = element_text(size=22)) +
      ggtitle(glue("5 artifical trajecotires with WLTPsSlice beginning at second 250"))
}

# HeatMap Trajectory
{
  length <- 1000
  velocity_low <- 0
  velocity_high <- 40
  acceleration_low <- -1.5
  acceleration_high <- 2

  vels <- seq(from=velocity_low*3.6, to=velocity_high*3.6, length.out = length)
  accs <- seq(from=acceleration_low, to=acceleration_high, length.out = length)
  combinations <- expand.grid(velocity=vels, acceleration=accs)
  combinations$time <- 0:(length*length-1)
  combinations <- combinations %>% select(time, velocity, acceleration)

  write_delim(combinations, "/Users/aleksander/Documents/VSP/PHEMTest/sumo/sumo_input_heatmap.csv", delim=";", col_names=FALSE)
}

# HeatMap Plot
{
  fuel <- "petrol"
  selected_component <- "NOx"
  velocity_low <- 0
  velocity_high <- 130
  acceleration_low <- -4
  acceleration_high <- 4

  # Read in the default wltp-cycle
  sumo_input <- read_delim("/Users/aleksander/Documents/VSP/PHEMTest/sumo/sumo_input.csv", delim=";", col_names=c("time", "velocity", "acceleration")) %>%
    filter(velocity_low < velocity/3.6 & velocity/3.6 < velocity_high & acceleration_low < acceleration & acceleration < acceleration_high)
  sumo_input_inverted_time <- read_delim("/Users/aleksander/Documents/VSP/PHEMTest/sumo/sumo_input_inverted_time.csv", delim=";", col_names=c("time", "velocity", "acceleration")) %>%
    filter(velocity_low < velocity/3.6 & velocity/3.6 < velocity_high & acceleration_low < acceleration & acceleration < acceleration_high)

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
    geom_path(data = sumo_input, aes(x = velocity/3.6, y = acceleration)) +
    geom_path(data = sumo_input_inverted_time, aes(x = velocity/3.6, y = acceleration), color="red") +
    theme_minimal()

  # Compute gradient of the output
  sumo_grad <- sumo_output %>%
    arrange(velocity, acceleration) %>%
    group_by(acceleration) %>%
    mutate(dv = velocity - lag(velocity),
           df_dv = (value - lag(value)) / dv) %>%
    ungroup() %>%
    group_by(velocity) %>%
    mutate(da = acceleration - lag(acceleration),
           df_da = (value - lag(value)) / da) %>%
    ungroup() %>%
    #filter(df_da != NaN & df_dv != NaN) %>%
    mutate(grad_mag = sqrt(df_dv^2 + df_da^2),
           grad_dir = atan2(df_dv, df_da))

  # Second approach with gradient-length
  ggplot() +
    geom_tile(data=sumo_grad, aes(x = velocity, y = acceleration, fill = grad_mag, color = grad_mag)) +
    labs(fill = "Emission", title = glue("Simulation Heatmap with gradient magnitudes for {selected_component}")) +
    scale_fill_gradient(trans="log", low="black", high="white") +
    scale_color_gradient(trans="log", low="black", high="white") +
    geom_point(data = sumo_input, aes(x = velocity/3.6, y = acceleration), size=0.01) +
    geom_point(data = sumo_input_inverted_time, aes(x = velocity/3.6, y = acceleration), color="red", size=0.01) +
    theme_minimal()

  # Third approach with gradient directions?
  ggplot() +
    geom_tile(data=sumo_grad, aes(x = velocity, y = acceleration, fill = grad_dir, color = grad_dir)) +
    labs(fill = "Emission", title = glue("Simulation Heatmap with gradient directions for {selected_component}")) +
    #scale_fill_viridis_c(trans="log") +
    scale_fill_gradient(trans="log", low="black", high="white") +
    scale_color_gradient(trans="log", low="black", high="white") +
    geom_point(data = sumo_input, aes(x = velocity/3.6, y = acceleration), size=0.01) +
    geom_point(data = sumo_input_inverted_time[(nrow(sumo_input_inverted_time)-100):nrow(sumo_input_inverted_time),], aes(x = velocity/3.6, y = acceleration), color="red", size=0.01) +
    theme_minimal()
}

# Trend trajectory
{
  d <- 0.1

  sumo_input <- read_delim("/Users/aleksander/Documents/VSP/PHEMTest/sumo/sumo_input.csv", delim=";", col_names=c("time", "velocity", "acceleration")) %>%
    filter(velocity >= d*5)
  sumo_input_inverted_time <- read_delim("/Users/aleksander/Documents/VSP/PHEMTest/sumo/sumo_input_inverted_time.csv", delim=";", col_names=c("time", "velocity", "acceleration")) %>%
    filter(velocity >= d*5)

  offsets <- expand.grid(
    step = seq(-5, 5),
    da = c(0, d, -d),
    dv = c(0, d)
  )

  sumo_input_offsets <- sumo_input %>%
    left_join(offsets %>% mutate(), by = character()) %>%
    mutate(
      acceleration = acceleration + step*da,
      velocity = velocity + step*dv
    ) %>%
    mutate(i = time, time = row_number()-1, angle = ifelse(dv == 0 & da == 0, NaN, atan2(dv, da) * 180 / pi)) %>%
    filter(!is.nan(angle))
    #filter(!(is.nan(angle) & step != 1))

  sumo_input_inverted_time_offsets <- sumo_input_inverted_time %>%
    left_join(offsets %>% mutate(), by = character()) %>%
    mutate(
      acceleration = acceleration + step*da,
      velocity = velocity + step*dv
    ) %>%
    mutate(i = time, time = row_number()-1, angle = ifelse(dv == 0 & da == 0, NaN, atan2(dv, da) * 180 / pi)) %>%
    filter(!is.nan(angle))
    #filter(!(is.nan(angle) & step != 1))

  write_delim(sumo_input_offsets, "/Users/aleksander/Documents/VSP/PHEMTest/sumo/sumo_input_offsets.csv", delim=";", col_names=FALSE)
  write_delim(sumo_input_inverted_time_offsets, "/Users/aleksander/Documents/VSP/PHEMTest/sumo/sumo_input_inverted_time_offsets.csv", delim=";", col_names=FALSE)
}

# Trend analysis
{
  sumo_input_offsets <- read_delim("/Users/aleksander/Documents/VSP/PHEMTest/sumo/sumo_input_offsets.csv", delim=";", col_names=c("time", "velocity", "acceleration", "step", "da", "dv", "i", "angle"))
  sumo_input_inverted_time_offsets <- read_delim("/Users/aleksander/Documents/VSP/PHEMTest/sumo/sumo_input_inverted_time_offsets.csv", delim=";", col_names=c("time", "velocity", "acceleration", "step", "da", "dv", "i", "angle"))

  sumo_output_offsets <- read_delim(glue("{sumo_path}/sumo_{fuel}_output_offsets.csv"),
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
    mutate(model = "SUMO_0", value=value/1000) %>%
    inner_join(sumo_input_offsets, by = join_by(time, )) %>%
    group_by(component, angle, step) %>%
    summarize(mean = mean(value)) %>%
    filter(!is.na(angle))

  sumo_output_inverted_time_offsets <- read_delim(glue("{sumo_path}/sumo_{fuel}_output_inverted_time_offsets.csv"),
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
    mutate(model = "SUMO_0", value=value/1000) %>%
    inner_join(sumo_input_inverted_time_offsets, by = join_by(time, )) %>%
    group_by(component, angle, step) %>%
    summarize(mean = mean(value)) %>%
    filter(!is.na(angle))

  ggplot(sumo_output_offsets) +
    geom_line(aes(x=step, y=mean)) +
    facet_wrap(component~angle, scales="free")

  ggplot(sumo_output_inverted_time_offsets) +
    geom_line(aes(x=step, y=mean)) +
    facet_wrap(component~angle, scales="free")


}

# MATSim Driving Style Analysis # TODO Outdated, remove eventually
{
  fuel <- "petrol"
  selected_component <- "NOx"
  velocity_low <- 0
  velocity_high <- 130
  acceleration_low <- -4
  acceleration_high <- 4

  HBEFA <- read_csv2("/Users/aleksander/Documents/VSP/PHEMTest/hbefa/EFA_HOT_Subsegm_detailed_Car_Aleks_filtered.csv") %>%
    filter(VehCat == "pass. car") %>%
    filter(Technology == ifelse(fuel == "petrol", "petrol (4S)", "diesel")) %>%
    filter(endsWith(EmConcept, "Euro-4")) %>%
    filter(startsWith(TrafficSit, "URB/Local/50/Freeflow")) %>%
    filter(Component == selected_component)

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
    geom_tile(data=sumo_output, aes(x = velocity, y = acceleration, fill = value)) +
    labs(fill = "Emission", title = glue("Simulation Heatmap for {selected_component}")) +
    #scale_fill_viridis_c(trans="log") +
    scale_fill_viridis_c() +
    scale_color_viridis_c() +
    geom_contour(data = sumo_output, aes(x = velocity, y = acceleration, z = value), breaks = (HBEFA$EFA[[1]]), color = "black", linewidth = 1) +
    theme_minimal()

}