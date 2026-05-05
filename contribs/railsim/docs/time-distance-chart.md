# Time-Distance Chart

## Overview

Time-distance charts track the position of vehicles over time during simulation.
Each row records a vehicle's position (distance along its route) at a specific time point, with support for target (planned) and simulated (realized) trajectories.

## Format

The time-distance chart is provided as a tabular format with the following columns:

## Column Specifications

### vehicle_id
- **Type**: String
- **Description**: Unique identifier for the rail vehicle/train being tracked

### line_id
- **Type**: String
- **Description**: Identifier for the transit line this vehicle is operating on

### route_id
- **Type**: String
- **Description**: Identifier for the specific route variant within the line

### departure_id
- **Type**: String
- **Description**: Identifier for the specific departure instance of this route

### time
- **Type**: Double (seconds)
- **Description**: Simulation time in seconds when this position was recorded

### distance
- **Type**: Double (meters)
- **Description**: Distance along the route in meters from the route start

### type
- **Type**: String (enum)
- **Description**: Indicates whether this is a planned/target position or actual/realized position
- **Values**: `target` | `simulated`
  - `target`: Planned/targeted position according to schedule
  - `simulated`: Actual realized position during simulation

### link_id
- **Type**: String
- **Description**: Identifier of the network link the vehicle is currently on

### stop_id
- **Type**: String / empty
- **Description**: Identifier of the transit stop/facility, if the vehicle is at a station
- **Note**: Should be empty/null if vehicle is not at a stop

## Example Data

| vehicle_id | line_id | route_id | departure_id | time  | distance | type      | link_id | stop_id |
|------------|---------|----------|--------------|-------|----------|-----------|---------|---------|
| 1          | 1       | 1        | 1            | 0.0   | 0.0      | target    | link_1  | stop_A  |
| 1          | 1       | 1        | 1            | 0.0   | 0.0      | simulated | link_1  | stop_A  |
| 1          | 1       | 1        | 1            | 30.0  | 150.0    | target    | link_1  |         |
| 1          | 1       | 1        | 1            | 30.0  | 148.5    | simulated | link_1  |         |
| 1          | 1       | 1        | 1            | 120.0 | 750.0    | target    | link_2  |         |
| 1          | 1       | 1        | 1            | 121.5 | 745.2    | simulated | link_2  |         |
| 1          | 1       | 1        | 1            | 180.0 | 1000.0   | target    | link_3  | stop_B  |
| 1          | 1       | 1        | 1            | 182.3 | 1000.0   | simulated | link_3  | stop_B  |
| 2          | 2       | 3        | 2            | 0.0   | 0.0      | target    | link_10 | stop_X  |
| 2          | 2       | 3        | 2            | 0.0   | 0.0      | simulated | link_10 | stop_X  |

## Notes

- Each row represents a single position record at a specific time point
- For visualization, target and simulated trajectories can be plotted separately to compare planned vs. realized vehicle positions
- The `distance` field represents cumulative distance along the route from the start
- Both `target` and `simulated` records may exist for the same distance, but can also deviate in case the vehicle performed a re-routing