# Events-Specification

railsim introduces additional, custom events. This document describes these event types.

All the additional events use the prefix `railsim`.

## Event Types

### railsimLinkStateChangeEvent

Instead of `trainPathEntersLinkEvent`, we could have a generic `railsimLinkStateChangeEvent`
that could include information about the new state of the link (or even the track of a multi-track link).

Attributes:

- `state`: `free`, `reserved`, or `blocked`
- `vehicleId`: if `state=reserved|blocked`, the id of the vehicle blocking or reserving this link
- `track`: a number (0-based or 1-based?) if the link has multiple tracks

### railsimTrainLeavesLinkEvent

Similar to the existing `trainLeavesLinkEvent`.
One could argue that setting the link state to `free` would imply the same. I (mr) would still
say it makes sense to have it separate, because depending on the implementation, a link could
remain blocked for a longer time even if the train has already passed (e.g. minimum headway time).

There is **no** railsimTrainEntersLinkEvent. The regular `LinkEnterEvent` is used to provide the highest
compatibility with existing analysis and visualization tools.

### railsimTrainStateEvent

This event is emitted every time there is a position update for a train. 
This event contains detailed information about the trains position on a single link.

### railsimDetourEvent

This event is emitted when a train is re-routed and contains parts of the routes that have changed.