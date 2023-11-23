# railsim

A large-scale hybrid micro- and mesoscopic simulation approach for railway operation.

Trains behave and interact with links differently than cars: While usually multiple cars can be on one link,
for safety reasons there should be only one train on a track (segment or block) per time.
And while a car is typically located on one link only, a long train may occupy space on multiple links.
To capture these differences and offer a realistic simulation of train traffic within MATSim,
the *railsim* contrib provides a custom QSim-Engine to simulate trains:

- Trains are spatially expanded along several links. Additional events indicate when the end of the train leaves a link.
- Trains accelerate and decelerate based on predefined vehicle attributes (along a single link or along several links).
- The infrastructure ahead of each train is blocked (reserved train path) depending on the braking distance which is
  computed based on the vehicle-specific deceleration and the current speed.
- Capacity effects are modeled at the level of resources. A resource consists of one link or several links.
- Trains may deviate from the network route given in the schedule, e.g. to avoid a blocked track (dispatching,
  disposition).

## Configuration

See `config.RailsimConfigGroup.java`

## Enabling railsim in MATSim

See `RunRailsimExample.java`

## Specifications

- [network-specification](docs/network-specification.md)
- [train-specification](docs/train-specification.md)
- [events-specification](docs/events-specification.md)

## Acknowledgments

This contrib was initiated and initially funded by the [Swiss Federal Railways](https://www.sbb.ch) (SBB).
