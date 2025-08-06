library("tidyverse")

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