# Pseudo random errors

This example introduces determinstic random error terms that are generated and added to the score. The error terms represent a Gumbel distribution when looked at over the entirety of all persons and trips in a simulation run, but they are determinstic for a combination of (person, trip index, mode). This way, variability in the preferences for modes can be introduced into MATSim. The whole approach is described in 

Hörl, S., 2021. Integrating discrete choice models with MATSim scoring. Procedia Computer Science 184, 704–711. [https://doi.org/10.1016/j.procs.2021.03.088](https://doi.org/10.1016/j.procs.2021.03.088)

## Experiments

The same experiements as performed in the paper can be run again using the `Analysis.ipynb` notebook, which automatically performs the necessary runs given a precompiled code examples JAR.
