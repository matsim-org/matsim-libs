# Network-Specification for railsim

## Introduction

As trains interact differently with links than regular cars, the typical attributes like `capacity`, `lanes` and even
`freespeed` might not be suitable for describing rail infrastructure.

railsim uses custom link and node attributes to describe the essential parts of the rail infrastructure.
This document specifies these custom attributes.

railsim supports the microscopic modelling of tracks, where each link represents a single track, and
a mesoscopic level of modelling, where a link may represent multiple tracks.

## Specification

### Link Attributes

#### grade

TODO

#### trainCapacity

The number of trains that can be on this link at the same time.
If the attribute is not provided, a default of 1 is used.

#### trainOppositeDirectionLink

The id of a link leading in the opposite direction of this link.

MATSim uses uni-directional links. While on a road, cars might usually be able to pass each other
even on small roads by going very slow and near the edge, but trains cannot.
In order to correctly simulate the simulation where a train blocks a bidirectional track

#### maxSpeed

TODO

#### minimumTime

The minimum time ("minimum train headway time") for the switch at the end of the link (toNode).
If no link attribute is provided, a default of 0 is used.

#### vehicle type

The vehicle-specific freespeed on this link.
Please note that the actual vehicle-type must be used as attribute name, see example.

==Should there be a prefix, like `railsimspeed_`? we might want to add more vehicle-dependent attributes later.==

Example:

```xml

<link id="A_B" from="A" to="B" length="200.0" freespeed="40" capacity="3600.0" permlanes="1" oneway="1" modes="rail">
    <attributes>
        <attribute name="ic2000" class="java.lang.Integer">44.444</attribute>
        <attribute name="fvdosto" class="java.lang.Integer">50.0</attribute>
    </attributes>
</link>

```

==Note:== The class `RailsimUtils` has additional getters, e.g. to get the freespeed depending on transit line and
route.
Do we keep these? If yes, we should document these as well, and a prefix would make even more sense in this case.

(ik, mu): We don't think we have to keep this feature, speed limits per vehicle type should be sufficient.

### Node Attributes

Currently none.

(ik, mu): We assume that blocked nodes are somehow identified by the currently blocked links? Otherwise, we need node
states to avoid collisions. Do we really want that?

## Examples

### Single track with contraflow

```xml

<links>
    <link id="A_B" from="A" to="B" length="200.0" freespeed="50" capacity="3600.0" permlanes="1" oneway="1"
          modes="rail">
        <attributes>
            <attribute name="trainCapacity" class="java.lang.Integer">1</attribute>
            <attribute name="trainOppositeDirectionLink" class="java.lang.String">B_A</attribute>
        </attributes>
    </link>
    <link id="B_A" from="B" to="A" length="200.0" freespeed="50" capacity="3600.0" permlanes="1" oneway="1"
          modes="rail">
        <attributes>
            <attribute name="trainCapacity" class="java.lang.Integer">1</attribute>
            <attribute name="trainOppositeDirectionLink" class="java.lang.String">A_B</attribute>
        </attributes>
    </link>
</links>
```

(ik, mu): General remark: If we use a prefix for attributes, should we just go with `railsim` everywhere.

### Two tracks, each with a single direction

TODO

### Three tracks, with contraflow in the middle track

(ik, mu): Default case, all tracks are open for both directions. For more complex track layout we would tend to use the
microscopic modelling approach.

### Two tracks that intersect each other

If two tracks cross each other, e.g. like in the form of the letter `X` or a plus `+`, a train driving in one direction
effectively also blocks the intersecting tracks, even if they only share a common node, but not a common link.

TODO

(ik, mu): See discussion about node states.
