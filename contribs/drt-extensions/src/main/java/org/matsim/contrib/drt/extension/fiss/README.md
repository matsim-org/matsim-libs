
# Flow-inflated Selective Sampling (FISS, working title).


Scaling down agent populations by simulating only a fraction of all agents is a frequently
used option to reduce computational burdens. However, recent studies have pointed out the 
difficulty of scaling ride-pooling simulations as these rely heavily on demand density and 
do not scale linearly. FISS introduces a simple yet effective methodology for simulating 
full-scale dynamic ride-sharing services. 
The aim is to only assign a specified fraction of vehicular agents and teleport the rest, i.e., 
only trips of private car transport are sampled, while public transport as well as ride-sharing 
vehicles are fully represented. 
The capacity consumption of the cars is scaled up to obtain realistic traffic flows.
Most key performance indicators of ride-sharing services remain stable and mostly 
unbiased. Mode choice decisions based on this approach also remain stable. 


It is aimed to preserve mass conservation by also ensuring that the vehicle of the car agent
teleported along.

For more documentation, please see the article [here](https://doi.org/10.3929/ethz-b-000569127
). Here, the run-times of the actual assignment (i.e., QSim) can be almost halved.



  