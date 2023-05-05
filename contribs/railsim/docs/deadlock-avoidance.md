# Deadlock avoidance

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

The global case is more complex to solve, therefore only the problem is described below and not yet a solution approach
provided. The description is based on the segments defined in the local case, even though they could possibly be omitted
for this case, depending on the chosen solution approach.

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
