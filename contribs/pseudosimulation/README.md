
# Pseudo simulation

An approach to speed up simulation times.  A pseudo-simulation engine, *psim*, uses travel time information from the preceding qsim iteration to estimate how well an agent day plan might perform, allowing multiple iterations of mutation and evaluation between qsim iterations to more rapidly explore the agents' solution space, producing better performing plans in a shorter time. 

Recently migrated to contribs; being re-hauled to work with the controller injection framework. 

Running **org.matsim.contrib.pseudosimulation.RunPSim** without any command line arguments will print a list of current options.   