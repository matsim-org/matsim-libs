==================================================================
Enriched Sioux Falls Scenario with Dynamic and Disaggregate Demand
==================================================================


File list
----------------
   config_default.xml				MATSim config file, which was used for runs in Chakirov and Fourie (2014) 
					 	(Case 1, Seed 1).  
   Siouxfalls_facilities.xml.gz		 	MATSim facilities file. Contains list of all facilities including opening times 
						(opening times are not used in the moment, as activity rectriciotns are defined in the config file).
   Siouxfalls_network_PT.xml			Network file
   Siouxfalls_population.xml.gz		 	Initial population file
   Siouxfalls_transitSchedule.xml	 	Transit schedule file containing information on 
					 	bus stop locations, bus lines and bus schedule
   Siouxfalls_vehicles.xml		 	Vehicle file containing information on type and capacity of buses
   

   
Additional scenario data, as well as simulations input and output, can be obtained by following the instructions at www.matsim.org/scenario/sioux-falls and on the Transportation Test Problems page of the Ben-Gurion University of the Negev, maintained by Hillel Bar-gera (http://www.bgu.ac.il/~bargera/tntp/).


Corresponding publication 
--------------------------

Chakirov, A. and P. Fourie (2014) Enriched Sioux Falls Scenario with Dynamic and Disaggregate Demand, Working paper, Future Cities Laboratory, Singapore - ETH Centre (SEC), Singapore.

Abstract
This paper presents an enriched, agent-based small scale scenario with dynamic demand and an integrated public transport system based on the commonly used Sioux Falls road network. The scenario aims to provide a realistic, fully dynamic demand with heterogeneous socio-demographic users and a high degree of spatial resolution. Real world survey and land-use data is used to generate a diverse synthetic population and accurate activity locations. The socio-demographic characteristics include age and sex on individual and income on household levels. The assignment of home and work locations employs land-use and building information, census data from the City of Sioux Falls, South Dakota as well as commonly used static OD-matrices from LeBlanc et al., 1975.
This enriched Sioux Falls scenario can serve as a convenient test-case for the study of different transportation policies as well as a test bed for the extension and development of agent-based simulation frameworks. It is important to note that the scenario does not aim to replicate the real City of Sioux Falls, SD, and remains a fictitious test-case scenario. In this work, we use Multi-agent Transport Simulation (MATSim) in order to evaluate the stochastic user-equilibrium of the compiled supply and demand.
