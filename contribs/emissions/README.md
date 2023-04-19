
# Emissions (with third computation method ~ rgraebe)

This package provides a tool for exhaust emission calculation based on the "Handbook on Emission Factors for Road Transport" (HBEFA), version 3.1 and 4.1 (see <http://www.hbefa.net>). 
When publishing papers where this emission package is used, please make sure to cite the following two articles: 
 1. Kickhöfer, B. (2016). *Emission Modeling*. In:  Horni, A.;  Nagel, K.; Axhausen, K. W.. *The Multi-Agent Transport Simulation MATSim.* Ubiquity, London. Chapter 36. DOI: <https://doi.org/10.5334/baw>.
 2. Hülsmann, F.; Gerike, R.; Kickhöfer, B.; Nagel, K. & Luz, R. (2011). *Towards a multi-agent based modeling approach for air pollutants in urban regions.* Proceedings of the Conference on ``Luftqualität an Straßen'', FGSV Verlag GmbH, pp. 144-166. ISBN: 978-3-941790-77-3.
 
The probably most detailed documentation of this package can be found in Benjamin's dissertation, available [here](http://www.nbn-resolving.org/urn:nbn:de:kobv:83-opus4-53489).
For more information see the org.matsim.contrib.emissions package above. 

## *** UPDATED BRANCH *** 
Refer to the article:
Gräbe, R. (2022). *Are we getting vehicle emissions estimation right?*. Transportation Research Part D: Transport and Environment,
Vol 112, ISSN 1361-9209. DOI: <https://doi.org/10.1016/j.trd.2022.103477>

From Gräbe's work we see that the emissions contrib can cause underestimates in emissions estimation. 
This branch aims to solve this problem one step at a time.
It starts by introducing a new emissions computation method.
This method builds on the logic introduced by the `EmissionsConfigGroup`'s `EmissionsComputationMethod`, 'StopAndGoFraction'.
Here, instead of using the 'Stop&Go' traffic situation to calculate the "congested" part of the link's emissions, the 'Stop&Go2' traffic situation is used.
This results in (slightly) higher emissions especially during congested periods.
The "switch" enables the user to specify a third computation method: now we have 'AverageSpeed', 'StopAndGoFraction' AND 'StopAndGo2Fraction'.
This not tested on a larger scenario yet, but small scale experiments show that emissions do increase when switching between 'StopAndGoFraction' and 'StopAndGo2Fraction'.

Along with Tim Kirschbaum's work on road grades in the emissions contrib, this extra emissions calculation method aims to bring emissions estimates closer to reality.

For more information, contact Ruan Gräbe (ruan.graebe@gmail.com).

#### Example files:
You can find some example files in the matsim-examples.
There is also a free to use file for _cold_ emission factors (all set to 0.0) for those vehicle types which do not have values in the database. 


  
