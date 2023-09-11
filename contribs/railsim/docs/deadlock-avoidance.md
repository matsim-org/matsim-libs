# Deadlock-Avoidance

Deadlocks can occur on two scales in the mesoscopic approach of railsim:

- Locally within a route section with constant capacity: *Deadlock in a route section with bidirectionally shared
  constant capacity.*
- Globally due to conflicting route selections in the network: *Deadlock due to sequence of multiple route sections with
  different bidirectionally shared capacities (bottleneck).*

## Local case

The local case seems easier to solve. A possible approach is described below:

Before a train travels on a section with constant capacity (successive links with constant capacity, where the speed
limit may vary, hereafter called segment), it must check that not all tracks are already occupied from the opposite
direction. If this is the case, the train must wait in front of the segment before entering. If at least one track from
the opposite direction is free, then the train must check whether tracks in its direction are already in use. If this is
the case, it will be checked that the incoming train is not slowed down by the preceding train if there is sufficient
capacity, so that the faster train can overtake the slower train. If the incoming train is slower than the previous
train anyway, it should enter the same track to keep as much capacity as possible free for the opposite direction.

The tracks of a segment could be represented as an array of free-again times (maximum of the exit times of the trains on
a track) or zero if the track is free. When a train enters a track it overwrites the free-again time, if its exit time
is after the exit time of the preceding train.

## Global case

The global case is more complex to solve, and the problem is described below. The description is based on the segments
defined in the local case, even though they could possibly be omitted for this case, depending on the chosen solution
approach.

A train on its route reserves segments ahead as far as possible (see *full path reservation*) or alternatively until the
next station with the assumption that there are enough tracks available in the station to pass each other (see *station
to station path reservation*).
If two reserved paths running in opposite directions meet in a segment and the maximum capacity in the segment is used,
then both trains should travel as far as it is still possible to pass each other. So they have to stop before the
bottleneck, where the capacity still allows for a crossing. The train that arrives first at the bottleneck continues its
path, while the other train remains stationary until it no longer sees the opposite train in its reserved path. Then
the waiting train continues its path into the bottleneck.

This raises a few questions:

- What happens when several (not just two) reserved paths overlap? No matter how many trains are oncoming, the segment
  must allow **one** free track in the opposite direction.
- In situations with multiple overlaps, how do we find the positions where the trains should wait? Last places on the
  path where the equivalent capacity is at least 1?
- How do we solve the problem computationally efficiently?

### Full path reservation

In case of reservation of the entire route of a train line, i.e. from the origin station to the destination station of
the route, deadlocks are no longer possible on the route. If we have vehicle circuits where a train is waiting at the
destination station for the next departure (and thus blocks a track), again a deadlock would be possible when another
train tries to enter the destination station and no track is available.

==To Discuss:== this case is probably rather rare and negligible for the time being? If we model the stations as
segments too, then this case should be automatically solved?

**Key idea**

We have agents who want to navigate through resources. In the context of railsim, agents are individual vehicles,
resources are `RailLink`s (?), and the full path of an agent is the transit route it takes from the origin to the
destination station. A vehicle can have several transit routes on a simulation day.
Assuming that the current simulation is in a deadlock-free state, the goal is to ensure that the simulation will also be
deadlock-free after a next simulation step. This requires a deadlock avoidance algorithm. The algorithm shall prevent
deadlock state from occurring.

**Algorithm**

1. Setup:
    - Create a constant 1D-array with total train capacity per resource **CAP** (dim: 1 x n_resources)
    - Create a binary matrix for global positions **POS** (dim: n_agents x n_resources) and initialize it with zero.
    - Create a binary matrix for all path masks **PATH** (dim: n_agents x n_resources) and calculate for each
      agent **a_i** the binary mask for the full path with resources needed to reach the destination. Write mask into
      the paths mask matrix **PATH[a_i, ]**.
    - Create a set with moving agents **MOV** and a set with agents on a decision point resource **DEC**. Add all agents
      to both sets.
2. Start railsim simulation, in each iteration:
    1. Handle moving set agents:
        - For each agent in the moving set **MOV** enter the currently occupied resource into the global position
          mask **POS[a_i, ]**.
    2. Handle decision set agents:
        - Mask the path of each agent in the decision set **DEC** with the global position mask and identify all
          potentially conflicting agents.
        - Create a new 1D-array (dim: 1 x n_resources) with the sum of paths of all conflicting agents per agent (zero
          out non-conflicting agents in **PATH** and sum the columns).
        - Subtract the summed path array from the constant available capacity layer **CAP** for each agent.
        - If the next resource an agent wants to allocate has at least one free capacity, the agent is added to the
          moving set **MOV**, otherwise the agent is removed from the moving set and has to wait on the current resource
          for this iteration.
    3. The steps 1 and 2 are repeated for **the situation where the next step is already moved virtually** of all agents
       in the moving agent set. If there is a conflict, randomly chose one agent of the conflicting pair and remove it
       from the moving set (has to wait in this interation). Do not save the actual movement from this step!
    4. For all agents in the moving set **MOV**, set the current resource in the path matrix to zero
       **PATH[a_i, r_c]=0**. and check if the next resource they occupy is a decision point. If it is a decision point,
       add the agent to the decision set *DEC*.
    5. Continue railsim iteration (here the actual movement of the agent happens) and repeat.

**Key benefits**

The algorithm described above can be fully parallelized. The computation time depends only on the number of agents (n)
and number of resources (m). The computation complexity is O(n2 * m) in the worst case.

**References**

This algorithm was designed and implemented by Adrian Egli (SBB) as part of the Flatland project.

**Railsim context**

- Resources have to be smaller entities (e.g. `RailLink`) than segments, since otherwise two agents are not allowed to
  travel in the same direction.
- Not all resources should trigger the deadlock avoidance algorithm for the agent. For better performance, only when an
  agent is on a decision point (decision set of agents), the algorithm is triggered (e.g. first / entering `RailLink` of
  a segment with constant capacity).
- The former two points could be combined:
    - Each segment consists of the following **directed** `RailLink`s for both directions (2x): An *entering* `RailLink`
      , *intermediate* `RailLink`s for each change in VMax (with same capacity), a *leaving* `RailLink`.
    - Every `RailLink` has an opposite direction `RailLink`, where the entering link is the leaving link and vice versa.
    - The algorithm is only triggered on the **leaving** `RailLink`s.
    - Stations could also be modelled as segments (entering, intermediate, leaving links).
- I (mu) am not sure if we still need the combination with the local deadlock case, but I suspect that this is necessary
  if the capacity of a segment is greater than 1. We then have to determine which train is on which track to see if the
  train can overtake or has to brake.

### Station to station path reservation

In the case where the trains only reserve the path to the next station, a deadlock is again possible when entering the
station. It is not guaranteed that the train can enter the station, and a train in the station (travelling in the
opposite direction), could be waiting for the incoming train to free its track.

==To Discuss:== do we want to see this deadlock or should the simulation should never block in general?

If the simulation never blocks, a lack of capacity in a station should be visible in the delays of the trains. It is
probably not easy to find the initial cause of the delay, as a delay potentially propagates and affects other trains.
Additional events may be needed to signal and document a train's decision to wait at a particular point and thus deviate
from the timetable. This allows to detect the origins and the course of the delays in an event analysis after the
simulation.

## References

The deadlock problem has already been solved in the Flatland project and could be used as a starting point or serve as
inspiration:

- [flatland_solver_policy/policy/heuristic_policy/shortest_path_deadlock_avoidance_policy/deadlock_avoidance_policy.py](https://github.com/aiAdrian/flatland_solver_policy/blob/main/policy/heuristic_policy/shortest_path_deadlock_avoidance_policy/deadlock_avoidance_policy.py)
- [flatland/flatland/contrib/utils/deadlock_checker.py](https://gitlab.aicrowd.com/flatland/flatland/-/blob/master/flatland/contrib/utils/deadlock_checker.py)
