# Events-Specification

railsim introduces additional, custom events. This document describes these event types.

==Note:== I'm not sure we really need this. I was inspired by the fact that the prototyp 
has custom events like `trainPathEntersLinkEvent`, `trainEntersLinkEvent` and `trainLeavesLinkEvent`.
Thus the following event types are currently **to be discussed**.

==To Discuss:== should all event types start with `railsim`?

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
remain blocked for a longer time even if the train has already passed.


