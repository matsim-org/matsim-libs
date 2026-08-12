
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
	<param name="transitEmulation" value="fullTransitPerformance" />
</module>
```

`iterationsPerCycle` of 25 means one queue simulation iteration followed by 24 pseudo-simulation
iterations. The first and the last iteration of a run are always queue simulations.

## Choosing a replanning rate

The replanning rate applies to every iteration, pseudo-simulation iterations included, so a cycle of
`iterationsPerCycle` iterations perturbs the population that many times before the queue simulation
next gets to check the result. Carrying a queue-simulation-only rate straight over is the most
common way to make a pseudo-simulation run worse than the run it replaces.

Rerouting is a best-response module evaluated against a surrogate with no agent interaction, so every
rerouted agent is sent down whatever corridor looked fastest in the preceding queue simulation. Left
unchecked for a whole cycle this concentrates the population onto a few links, and the following
queue simulation cannot serve the result: it gridlocks, roughly a quarter of the day's trips never
happen, and the mean score collapses before recovering over the next cycle.

What matters is the **cumulative** rate over a cycle, expressed as a multiple of the rate an
equivalent queue-simulation-only run would use per iteration. Measured on Sioux Falls 2014, 84,110
agents, `iterationsPerCycle` 25, against a 100-iteration queue-simulation baseline at a 30% rate:

| rate per iteration | per cycle | QSim iterations to reach the baseline's final score | wall clock | gridlock episodes |
| ---: | ---: | ---: | ---: | ---: |
| baseline, no PSim | - | 84 | 774 s | 0 |
| 1.2% | 30% (1x) | never reached | - | 0 |
| 2.4% | 60% (2x) | never reached | - | 0 |
| **5.0%** | **125% (4x)** | **26** | **495 s** | **0** |
| 10.0% | 250% (8x) | 16 | 337 s | 2 |

Around four times the baseline's per-iteration rate, spread across the cycle, converges more than
three times faster than the baseline in queue simulations and stays stable. Below that the run is
stable but too little innovation reaches the population to beat the baseline at all. Above it the
run is faster still, and gridlocks.

The onset of gridlock lies between 125% and 250% per cycle in this scenario, and there is no reason
to expect that boundary to transfer unchanged to another network. Treat the table as a method for
choosing the rate - scan upward until gridlock appears, then step back - rather than as values to
copy. `PSimConvergenceExperiment` in the test sources runs this scan; it takes an explicit
per-iteration rate as its fourth argument.

## Transit

`psim.transitEmulation` selects how a pseudo-simulation iteration works out what a transit leg costs
an agent. A scenario that does not simulate transit (`transit.useTransit`) always gets `none`,
whatever the setting says, because there is no transit performance to record.

| value | behaviour |
| --- | --- |
| `fullTransitPerformance` (default) | Records every vehicle's dwell events during the queue simulation, then answers each query by drawing a random one of the last few observed departures and applying a stochastic boarding model. Captures boarding denial on full vehicles. |
| `waitAndStopStopTimes` | Averages the observed wait at each stop and the observed time between each pair of consecutive stops, per time-of-day bin, and falls back to the timetable where nothing was observed. Deterministic. |
| `none` | Every transit leg takes **no time at all**. Only appropriate where transit is teleported anyway; on a scenario running transit on the network it makes transit free and instantaneous. |

The two are close in outcome on Sioux Falls: `waitAndStopStopTimes` was slower to converge (26 queue
simulations to the baseline's final score against 16) and marginally steadier afterwards. It is
worth preferring where reproducibility matters, since `fullTransitPerformance` answers the same
query differently on successive calls, which means a plan can be retained on the strength of a
favourable draw.

Both read only what the preceding queue simulation measured. Events emitted during a
pseudo-simulation iteration are ignored, because those times were themselves derived from these
structures.

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
