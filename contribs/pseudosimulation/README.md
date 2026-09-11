
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

## Choosing a replanning rate and a cycle length

**These two settings are not independent, and neither can be carried over unchanged from a
queue-simulation-only run.** The replanning rate applies to every iteration, pseudo-simulation
iterations included, so what the population actually experiences between two reality checks is

```
perturbation per cycle = replanning rate x iterationsPerCycle
```

Changing `iterationsPerCycle` therefore changes how hard the population is perturbed even if the
rate is untouched. Going from 25 to 10 at a fixed 5% rate drops the budget from 125% to 50% per
cycle and the run will under-converge; going from 10 to 25 raises it from 50% to 125% and it may
gridlock. Tune them together, or hold their product fixed when you change one.

### Why it matters

Rerouting is a best-response module evaluated against a surrogate with no agent interaction. Every
rerouted agent is sent down whatever corridor was fastest in the preceding queue simulation, and
nothing in a pseudo-simulation iteration notices that thousands of other agents just received the
same advice. Repeat that for a whole cycle and the population concentrates onto a few links; the
next queue simulation then cannot serve the result.

The failure is easy to recognise once you know the signature. A queue simulation iteration executes
far fewer trips than its neighbours, the mean score collapses, and the following cycle slowly digs
back out. On Sioux Falls a gridlocked iteration executed 98,870 car departures where a healthy one
executed 133,556 - about a quarter of the day's trips never happened - and the score fell from 19.3
to -29.6. It is not a crash and nothing in the logs flags it, so it is easy to mistake for noise.

### What the trade-off looks like

Measured on Sioux Falls 2014, 84,110 agents, `iterationsPerCycle` 25, against a 100-iteration
queue-simulation baseline at a 30% rate. The multiple is of that baseline's per-iteration rate:

| rate per iteration | per cycle | QSim iterations to the baseline's final score | wall clock | gridlock episodes |
| ---: | ---: | ---: | ---: | ---: |
| baseline, no PSim | - | 84 | 774 s | 0 |
| 1.2% | 30% (1x) | never reached | - | 0 |
| 2.4% | 60% (2x) | never reached | - | 0 |
| **5.0%** | **125% (4x)** | **26** | **495 s** | **0** |
| 10.0% | 250% (8x) | 16 | 337 s | 2 |

Both ends of that range are bad in different ways. Too low and the run is perfectly stable but never
reaches the score the baseline reaches, because too little innovation ever gets to the population -
at 30% per cycle it is slower in wall clock than not using pseudo-simulation at all. Too high and it
converges fastest of all, then periodically destroys what it built.

At 125% per cycle the run reached the baseline's final score in 26 queue simulations against 84,
finished slightly higher (19.313 against 19.268), and showed no gridlock in 38 post-convergence
queue simulations.

### How to tune it

The gridlock threshold depends on how congested the network is and on which replanning modules are
in the strategy, so these numbers are a starting point, not a constant to copy.

1. Fix `iterationsPerCycle` first, from how much queue simulation time you are trying to avoid.
2. Set the rate so that `rate x iterationsPerCycle` is roughly the per-iteration rate an equivalent
   queue-simulation-only run would use. This is the safe, stable starting point.
3. Raise the rate until the score trace shows a collapsing queue simulation iteration, then step
   back. Look at queue simulation iterations only.
4. Re-check after any change to `iterationsPerCycle`, since step 2's product has moved.

Watch queue simulation iterations specifically. The score MATSim writes for a pseudo-simulation
iteration covers only the agents that were replanned, so it is not comparable with the score of a
queue simulation iteration and the raw series in `scorestats.csv` will look discontinuous.

`PSimConvergenceExperiment` in the test sources runs this scan; it takes an explicit per-iteration
rate as its fourth argument.

## Transit

`psim.transitEmulation` selects how a pseudo-simulation iteration works out what a transit leg costs
an agent. It supersedes `psim.fullTransitPerformanceTransmission`, which could only say whether to
emulate transit at all. A config that sets the old flag alone keeps its meaning - `false` maps to
`none` and `true` to `fullTransitPerformance` - and logs a warning naming the replacement. Setting
both is accepted while they agree on whether transit is emulated and rejected when they do not. A scenario that does not simulate transit (`transit.useTransit`) always gets `none`,
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
