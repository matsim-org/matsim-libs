
# Pseudo simulation

An approach to speed up simulation times.  A pseudo-simulation engine, *psim*, uses travel time information from the preceding qsim iteration to estimate how well an agent day plan might perform, allowing multiple iterations of mutation and evaluation between qsim iterations to more rapidly explore the agents' solution space, producing better performing plans in a shorter time.

The method is described in Fourie, Illenberger and Nagel, *Increased Convergence Rates in Multiagent
Transport Simulations with Pseudosimulation*, Transportation Research Record 2343, 2013.

## Running

`org.matsim.contrib.pseudosimulation.RunPSim` takes a config file and alternates between the queue
simulation and the pseudo-simulation. The number of iterations in a cycle is set through the `psim`
config group:

```xml
<module name="psim">
	<param name="iterationsPerCycle" value="25" />
	<param name="fullTransitPerformanceTransmission" value="true" />
</module>
```

`iterationsPerCycle` of 25 means one queue simulation iteration followed by 24 pseudo-simulation
iterations. The first and the last iteration of a run are always queue simulations.

## Transit

When the scenario simulates transit (`transit.useTransit`) and
`psim.fullTransitPerformanceTransmission` is on, `RunPSim` records transit performance during every
queue simulation iteration and replays it during the following pseudo-simulation iterations. Waiting
and in-vehicle times then come from what vehicles actually did on the network, including the effect
of congestion on buses.

Turning either switch off substitutes `NoTransitEmulator`, which gives every transit leg **no travel
time at all**. That is only appropriate for scenarios whose transit is teleported anyway; on a
scenario that runs transit on the network it makes transit free and instantaneous.

Transit lookups return infinity when nothing was recorded for the requested line, route, stop and
time - most visibly in the first pseudo-simulation iteration of an unrelaxed scenario, where the
network is still congested enough that few vehicles complete their runs. Agents whose plans cannot
be emulated are marked as stuck for that iteration, and the share recovers as the scenario relaxes.

## Interaction with the core analysis listeners

A pseudo-simulation iteration emits events only for the agents whose plans were replanned in that
iteration. Everything MATSim derives from experienced plans therefore describes the replanned subset
rather than the population. `RunPSim` switches `controller.writeTripsInterval` off for this reason:
`TripsAndLegsWriter` fails outright on the partial plans.

For the same reason, the score MATSim reports in `scorestats.csv` for a pseudo-simulation iteration
is **not** comparable with the one it reports for a queue simulation iteration. Read score
trajectories on queue simulation iterations only, or compute the mean over the population's selected
plans yourself, as `PSimConvergenceExperiment` does.

## Experiment driver

`PSimConvergenceExperiment` (in `src/test/java`) reproduces the paper's comparison on the
Sioux Falls 2014 scenario: a 100-iteration queue-simulation baseline at a 30% replanning rate
against a 1:24 pseudo-simulation run at 10%. It writes a per-iteration `progress.csv` plus
departure, link volume and travel time snapshots for comparing simulation state.

## Quality harness

See [QUALITY.md](QUALITY.md).
