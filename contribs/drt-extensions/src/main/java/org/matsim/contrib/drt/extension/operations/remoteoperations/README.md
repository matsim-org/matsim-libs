# Remote Operations (Remote Guidance)

Simulates remote operators supervising an autonomous DRT fleet at a ratio of 1:N. An operator shift is a
`DrtShift` of a dedicated type, so the module builds on the driver-shift infrastructure of the parent
`operations` package instead of replacing it. Operators are not bound to individual vehicles. They provide
aggregate supervision capacity, and they resolve incidents that a vehicle cannot handle on its own.

The module is opt-in. Without `RemoteGuidanceParams`, shift behaviour is unchanged.

Initially developed for MOIA GmbH.

## References

If used, please cite the working paper that describes the module and applies it to a full-day case study:

> Pfeil, L., N. Kuehnel and A. Loder (2026) Smooth Operator: Tracing the Capital-Labor Production Frontier
> of Remote Operations in Autonomous Mobility-on-Demand, Working paper, Professorship of Mobility Policy,
> Technical University of Munich, Munich.
> https://mediatum.ub.tum.de/doc/1859375/document.pdf

That study uses empirical MOIA trip demand for one full day in Hamburg, Germany, and runs the model across a
range of operator staffing levels and automated driving system (ADS) maturities. Maturity enters through the
incident hazard: a more mature ADS has a lower hazard per driven metre, so capital does work that would
otherwise fall to operator labour. The resulting output observations form an operational production frontier,
from which the study derives the elasticity of substitution between automated driving capability and remote
operator labour. Section 3.2 documents the model components implemented here, and Figure 3 shows how they sit
next to QSim and the DRT dispatcher.

The implementation itself is presented at the **MATSim User Meeting 2026 in Paris**:

> Kuehnel, N., L. Pfeil and M. Frawley (2026) A remote-operator shift module for MATSim: simulating
> teleoperation of autonomous ride-pooling fleets, MATSim User Meeting 2026, Paris.

The shift and break architecture this module extends is described in:

> Zwick, F., N. Kuehnel and S. Hoerl (2022) Shifts in perspective: Operational aspects in (non-)autonomous
> ride-pooling simulations, Transportation Research Part A, 165, 300-320.
> https://doi.org/10.1016/j.tra.2022.09.001

## Three mechanisms

### 1. Capacity gating

Each operator shift carries a supervision capacity (kappa). The number of vehicles allowed in service at time
t is bounded by the capacity on duty:

```
nActive(t) <= sum over on-duty operators of kappa
```

`RemoteGuidanceScheduler` (a `ShiftScheduler`) puts vehicles into service by emitting transient virtual
shifts for vehicles idling at a hub. `RemoteGuidanceShiftEndLogic` (a `ShiftEndLogic`) recalls vehicles when
capacity drops or when a supervised vehicle stays idle in service for longer than `idleTimeout`. Both margins
read one reconciled fleet-sizing target, so activation and recall cannot disagree and the low-demand band does
not churn.

How many vehicles to keep supervised is decided by pluggable `ActivationTrigger`s, combined by
`ActivationReconciler`:

| Trigger | Effect |
|---|---|
| `MinFleetActivation` | hard floor on the number of supervised vehicles (`minActiveFleet`) |
| `IdleBufferActivation` | keeps `readyBufferSize` vehicles idle in service to absorb incoming requests, optionally smoothed over `busyWindowSize` |
| `RejectionRateActivation` | demand-driven, engages when the rejection rate over `windowSize` exceeds `rejectionRateThreshold` |
| `GreedyIdleActivation` | activates every idle vehicle up to the capacity ceiling (`activationPolicy = greedy`) |

Operators defer their shift end while they still supervise vehicles, which keeps coverage intact during a
hand-over. `recallLeadTime` pulls the recall forward so vehicles reach a hub before capacity actually drops.

### 2. Incidents

Every metre driven carries a hazard that the vehicle cannot resolve on its own, one independent Poisson
process per severity class over vehicle kilometres travelled (occupied and empty). Vehicle kilometres are
accrued from `LinkLeave` events, so the incident rate is endogenous to the operation.

When an incident occurs, `IncidentDispatcher` stops the vehicle in place by mid-drive diversion and inserts an
`IncidentHoldTask`, rendered by `IncidentHoldActivity` with its own activity type for event and visualisation
purposes. The vehicle then continues on a re-routed drive. A held vehicle is out of service for passengers,
which is what propagates the operator constraint to waiting time and rejections.

Each severity class defines its own hazard rate and its own duration distribution, `LOGNORMAL` (default),
`EXPONENTIAL` or `DETERMINISTIC`, with `durationMu` and `durationSigma` in log space. A mixture of classes
reproduces the usual empirical pattern: many short confirmations, fewer long takeovers.

### 3. Guidance queue

Incidents queue for the operator pool and are served by whichever operator is free, either uniformly at random
(`RANDOM_FREE`) or by fewest incidents in progress (`LEAST_LOADED`). The pool therefore behaves as an M/G/m
system, with m the operators on duty. Setting one severity class with `EXPONENTIAL` durations and a constant
number of operators reduces the queue to M/M/m, which is useful for verification against the analytical result.

## Configuration

The parameter sets nest inside the DRT mode:

```xml
<module name="multiModeDrt">
  <parameterset type="drt">
    <parameterset type="drtOperations">
      <parameterset type="operationFacilities">
        <param name="operationFacilityInputFile" value="operationFacilities.xml"/>
      </parameterset>
      <parameterset type="shifts">
        <param name="shiftInputFile" value="operatorShifts.xml"/>
      </parameterset>
      <parameterset type="remoteGuidance">
        <param name="operatorShiftType" value="remoteGuidance"/>
        <param name="defaultOperatorCapacity" value="10"/>
        <param name="activationPolicy" value="buffered"/>
        <param name="minActiveFleet" value="0"/>
        <param name="readyBufferSize" value="1"/>
        <param name="busyWindowSize" value="0"/>
        <param name="idleTimeout" value="900"/>
        <param name="recallLeadTime" value="900"/>
        <param name="minRemainingShiftTimeForActivation" value="1800"/>
        <parameterset type="incidents">
          <param name="assignmentPolicy" value="RANDOM_FREE"/>
          <!-- one class per severity level; lambdaPerMeter 1e-6 is on average one incident every 1000 km -->
          <parameterset type="incidentSeverity">
            <param name="severityName" value="minor"/>
            <param name="lambdaPerMeter" value="1.0E-6"/>
            <param name="durationMu" value="5.7"/>       <!-- log(300), median 300 s -->
            <param name="durationSigma" value="0.5"/>
            <param name="durationDistribution" value="LOGNORMAL"/>
          </parameterset>
        </parameterset>
        <!-- optional, enables the demand-driven activation trigger -->
        <parameterset type="rejectionActivation">
          <param name="windowSize" value="900"/>
          <param name="rejectionRateThreshold" value="0.1"/>
        </parameterset>
      </parameterset>
    </parameterset>
  </parameterset>
</module>
```

Operator shifts are ordinary shifts marked by `type`, in the shift input file of the parent package:

```xml
<shifts>
    <shift id="operator_0" start="14400" end="50400" type="remoteGuidance"/>
    <shift id="operator_1" start="18000" end="45000" type="remoteGuidance"/>
</shifts>
```

The entry point is unchanged, `DrtOperationsControlerCreator` (or `EDrtOperationsControlerCreator` for
electric fleets). Adding the `remoteGuidance` parameter set switches the mode from driver shifts to operator
supervision.

## Events

`VehicleActivatedForRemoteGuidanceEvent`, `VehicleDeactivatedForRemoteGuidanceEvent` (with a deactivation
reason), `RemoteGuidanceOperatorStartedEvent`, `RemoteGuidanceOperatorEndedEvent`, `IncidentStartedEvent`,
`IncidentAssignedToOperatorEvent`, `IncidentResolvedEvent`. All analysis is driven from these events, so an
external analysis can reconstruct the full operator and incident history from the event file alone.

## Analysis output

`RemoteGuidanceAnalysisModeModule` writes one set of files per iteration, named
`drt_remoteGuidance_<prefix>_<mode>`:

| Prefix | Content |
|---|---|
| `incidents` | one row per incident, including queue delay and handling duration |
| `incidentStats` | per-iteration aggregates |
| `operatorUtilisation` | operators on duty, busy operators and utilisation over time (CSV and PNG) |
| `activeVehicles` | supervised, idle and held vehicles over time (CSV and PNG) |
| `deactivationReasons` | recall reasons by count |
| `operatorHours` | operator hours per iteration, for cost calculations |
| `production` | the per-iteration production tuple: operator hours as labour input against served rides and passenger kilometres as output |

At shutdown, the final iteration's incident hotspots are written to `drt_remoteGuidance_<mode>.gpkg`, layer
`incident_hotspots`: one point per link that saw an incident, placed at the link's to-node, carrying incident count
and mean queue delay. This requires the global coordinate system to be set.

## Example

`RunRemoteGuidanceDrtScenarioIT` runs the Holzkirchen example with four staggered operator shifts of capacity
five over a 20-vehicle fleet, using `examples/scenarios/holzkirchen/holzkirchenRemoteGuidanceShifts.xml`. It
asserts that vehicles are activated under operators and that the number of supervised vehicles never exceeds
the combined capacity of the operators on duty at that time.
