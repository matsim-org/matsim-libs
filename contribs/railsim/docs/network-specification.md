# Network-Specification

## Introduction

As trains interact differently with links than regular cars, the typical attributes like `capacity` and `lanes` are not
suitable for describing rail infrastructure.

railsim uses custom link attributes to describe the essential parts of the rail infrastructure.
This document specifies these custom attributes.

For the maximum allowed speed on a link, the normal `freespeed` attribute is used.

## Specification

We use the prefix `railsim` where it is appropriate.

### Link Attributes

#### railsimTrainCapacity

The number of trains that can be on this link at the same time. If the attribute is not provided, a default of 1 is
used.
railsim supports the microscopic modelling of tracks, where each link represents a single track (`railsimCapacity` = 1
or default), and a mesoscopic level of modelling, where a link may represent multiple tracks (`railsimCapacity` > 1).

Example:

```xml

<link id="A" from="A" to="A0" length="500" freespeed="2.77" capacity="3600.0" permlanes="1" oneway="1" modes="rail">
    <attributes>
        <attribute name="railsimTrainCapacity" class="java.lang.Integer">3</attribute>
    </attributes>
</link>

```

#### railsimResourceId

The id of a resource, i.e. a segment of links that share a constant capacity.

This can be used to denote certain blocks of links that can only be reserved as a whole.
One use-case is the modelling of bidirectional links, where one train blocks both directions.
MATSim uses uni-directional links. While on a road, cars might usually be able to pass each other
even on small roads by going very slow and near the edge, but trains cannot.

This can also be used to model intersections as one resource, which will restrict crossing trains.

The train capacity will be derived as the minimum of all included links of this resource.
Links that have no resource id will be handled as individual resource.

#### railsimMinimumTime

The minimum time ("minimum train headway time") for the switch at the end of the link (toNode).
If no link attribute is provided, a default of 0 is used.

#### railsimEntry

Entry link of a station, triggers re-routing and serves as origin.

#### railsimExit

Exit link of a station, denotes a destination in re-routing.

Example:

```xml

<link id="hl" from="H" to="L" length="3000" freespeed="25" capacity="3600.0" permlanes="1" modes="rail">
    <attributes>
        <attribute name="railsimExit" class="java.lang.Boolean">true</attribute>
    </attributes>
</link>

```

#### railsimSpeed_ + vehicle type

TODO

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

## Microscopic

### Moving Block

Model tracks consisting of short links. Each link, except for the opposite link, has a unique resource ID and a capacity
of 1.

### Fixed block

Model blocks of links with a capacity of 1 each and identical resource IDs.

### Station

Each platform link has a distinct resource ID, except for the opposite link, and a capacity of 1. Each platform link has
a transit stop facility, which belongs to the same stop area id. Ingoing and outgoing links of the station have entry
and exit attributes.

### Examples

#### Single track with contraflow

Default value of `railsimCapacity` sets an own railsimResourceId for each track.

```xml

<links>
    <link id="A_B" from="A" to="B" length="200.0" freespeed="50" capacity="3600.0" permlanes="1" oneway="1"
          modes="rail">
        <attributes>
            <attribute name="railsimResourceId" class="java.lang.String">AB</attribute>
        </attributes>
    </link>
    <link id="B_A" from="B" to="A" length="200.0" freespeed="50" capacity="3600.0" permlanes="1" oneway="1"
          modes="rail">
        <attributes>
            <attribute name="railsimResourceId" class="java.lang.String">AB</attribute>
        </attributes>
    </link>
</links>
```

#### Two tracks, each with a single direction

Default value of `railsimResourceId` sets an own railsimResourceId for each track.

```xml

<links>
    <link id="A_B" from="A" to="B" length="200.0" freespeed="50" capacity="3600.0" permlanes="1" oneway="1"
          modes="rail">
    </link>
    <link id="B_A" from="B" to="A" length="200.0" freespeed="50" capacity="3600.0" permlanes="1" oneway="1"
          modes="rail">
    </link>
</links>
```

#### Three tracks, with contraflow in the middle track

```xml

<links>
    <link id="A_B" from="A" to="B" length="200.0" freespeed="50" capacity="3600.0" permlanes="1" oneway="1"
          modes="rail">
    </link>
    <link id="A_B_contra" from="A" to="B" length="200.0" freespeed="50" capacity="3600.0" permlanes="1" oneway="1"
          modes="rail">
        <attributes>
            <attribute name="railsimResourceId" class="java.lang.String">AB</attribute>
        </attributes>
    </link>
    <link id="B_A_contra" from="B" to="A" length="200.0" freespeed="50" capacity="3600.0" permlanes="1" oneway="1"
          modes="rail">
        <attributes>
            <attribute name="railsimResourceId" class="java.lang.String">AB</attribute>
        </attributes>
    </link>
    <link id="B_A" from="B" to="A" length="200.0" freespeed="50" capacity="3600.0" permlanes="1" oneway="1"
          modes="rail">
    </link>
</links>
```

#### Two tracks that intersect each other

If two tracks cross each other, e.g. like in the form of the letter `X` or a plus `+`, a train driving in one direction
effectively also blocks the intersecting tracks, even if they only share a common node, but not a common link.

There should be no additional link- or node-attributes necessary. The simulation should block the node in the case of
`railsimCapacity = 1`, but not if the capacity is larger than 1. If the node is blocked, no other trains must be able
to cross this node/intersection.

## Mesoscopic

### Route

Model tracks consisting of links with capacities exceeding 1. Opposite links share the same resource ID. There is no
differentiation between moving block and fixed block. A mesoscopic link can only initiate or terminate at points where a
physical track change is feasible.

### Station

The station consists of one or several links, including the opposite links, with the same resource ID. The capacity is
larger than 1 and corresponds to the number of tracks.

### Examples

#### Section with a capacity of 2

```xml

<links>
    <link id="l_bc_F" from="n_b2" to="n_c1" length="4000" freespeed="27.777" capacity="3600.0" permlanes="1"
          modes="rail">
        <attributes>
            <attribute name="railsimTrainCapacity" class="java.lang.Integer">2</attribute>
        </attributes>
    </link>
    <link id="l_bc_R" from="n_c1" to="n_b2" length="4000" freespeed="27.777" capacity="3600.0" permlanes="1"
          modes="rail">
        <attributes>
            <attribute name="railsimTrainCapacity" class="java.lang.Integer">2</attribute>
        </attributes>
    </link>
</links>
```
