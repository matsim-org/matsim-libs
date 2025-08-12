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
  index <- 9
  fuel <- "petrol"

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
    ggtitle(glue("Original acceleration vs. Derivated acceleration vs. SUMO acceleration for {fuel} cars ({index})"))

}

