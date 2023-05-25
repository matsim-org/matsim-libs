# Network-Specification for railsim

## Introduction

As trains interact differently with links than regular cars, the typical attributes like `capacity`, `lanes` and even
`freespeed` might not be suitable for describing rail infrastructure.

railsim uses custom link and node attributes to describe the essential parts of the rail infrastructure.
This document specifies these custom attributes.

railsim supports the microscopic modelling of tracks, where each link represents a single track, and
a mesoscopic level of modelling, where a link may represent multiple tracks.

## Specification

We try to use the prefix `railsim` where it is appropriate.

### Link Attributes

#### grade

TODO

#### railsimTrainCapacity

The number of trains that can be on this link at the same time.
If the attribute is not provided, a default of 1 is used.

#### railsimResourceId

The id of a resource, i.e. a segment of links that share a constant capacity.

This can be used to denote certain blocks of links that can only be reserved as a whole.
One use-case is the modelling of bidirectional links, where one train blocks both directions.
MATSim uses uni-directional links. While on a road, cars might usually be able to pass each other
even on small roads by going very slow and near the edge, but trains cannot.

This can also be used to model intersections as one resource, which will restrict crossing trains.

The train capacity will be derived as the minimum of all included links of this resource. 
Links that have no resource id will be handled as individual resource.

#### railsimMaxSpeed

TODO

#### railsimMinimumTime

The minimum time ("minimum train headway time") for the switch at the end of the link (toNode).
If no link attribute is provided, a default of 0 is used.

#### railsimSpeed_ + vehicle type

The vehicle-specific freespeed on this link.
Please note that the actual vehicle-type must be used as part of the attribute name, see example.

Example:

```xml

<link id="A_B" from="A" to="B" length="200.0" freespeed="40" capacity="3600.0" permlanes="1" oneway="1" modes="rail">
    <attributes>
        <attribute name="railsimSpeed_ic2000" class="java.lang.Integer">44.444</attribute>
        <attribute name="railsimSpeed_fvdosto" class="java.lang.Integer">50.0</attribute>
    </attributes>
</link>

```

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
            <attribute name="railsimCapacity" class="java.lang.Integer">1</attribute>
            <attribute name="railsimResourceId" class="java.lang.String">AB</attribute>
        </attributes>
    </link>
    <link id="B_A" from="B" to="A" length="200.0" freespeed="50" capacity="3600.0" permlanes="1" oneway="1"
          modes="rail">
        <attributes>
            <attribute name="railsimCapacity" class="java.lang.Integer">1</attribute>
            <attribute name="railsimResourceId" class="java.lang.String">AB</attribute>
        </attributes>
    </link>
</links>
```

### Two tracks, each with a single direction

TODO

### Three tracks, with contraflow in the middle track

(ik, mu): Default case, all tracks are open for both directions. For more complex track layout we would tend to use the
microscopic modelling approach.

### Two tracks that intersect each other

If two tracks cross each other, e.g. like in the form of the letter `X` or a plus `+`, a train driving in one direction
effectively also blocks the intersecting tracks, even if they only share a common node, but not a common link.

There should be no additional link- or node-attributes necessary. The simulation should block the node in the case of 
`railsimCapacity = 1`, but not if the capacity is larger than 1. If the node is blocked, no other trains must be able
to cross this node/intersection.

