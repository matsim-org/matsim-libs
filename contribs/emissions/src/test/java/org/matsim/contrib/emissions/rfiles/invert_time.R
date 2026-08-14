# This is a pretty old file. It was only used once, I just uploaded it for documentation purposes.

library("tidyverse")

# Generate time inverted smo input file
{
  sumo_input <- read_delim("/Users/aleksander/Documents/VSP/PHEMTest/sumo/sumo_input.csv", delim=";")

  # Invert along time-axis
  sumo_input_inverted_time <- sumo_input %>%
    mutate(`0` = (-1) * (`0`-max(sumo_input$`0`)-1)) %>%
    mutate(`0.0...3` = (-1) * `0.0...3`) %>%
    arrange(`0`)

  # Test plot for visual check
  ggplot() +
    geom_line(data=sumo_input, aes(x=`0`, y=`0.0...2`), color="red") +
    geom_line(data=sumo_input_inverted_time, aes(x=`0`, y=`0.0...2`), color="blue")

  # Write out inverted sumo input
  write_delim(sumo_input_inverted_time, "/Users/aleksander/Documents/VSP/PHEMTest/sumo/sumo_input_inverted_time.csv", delim=";", col_names=FALSE)
}

# Rough plot
{
  sumo_output <- read_delim("/Users/aleksander/Documents/VSP/PHEMTest/sumo/sumo_output.csv", delim=";",
                           col_names = c("time", "velocity", "acceleration", "slope", "CO", "CO2", "HC", "PMx", "NOx", "fuel", "electricity"))
  sumo_output_inverted_time <- read_delim("/Users/aleksander/Documents/VSP/PHEMTest/sumo/sumo_output_inverted_time.csv", delim=";",
                           col_names = c("time", "velocity", "acceleration", "slope", "CO", "CO2", "HC", "PMx", "NOx", "fuel", "electricity"))

  ggplot() +
    geom_line(data=sumo_output, aes(x=time, y=HC), color="red") +
    geom_line(data=sumo_output_inverted_time, aes(x=time, y=HC), color="blue")

}