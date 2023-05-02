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

#### trainCapacity

TODO

#### trainOppositeDirectionLink

TODO

### Node Attributes

TODO

## Examples

### Single track with contraflow

```xml
<link id="A_B" from="A" to="B" length="200.0" freespeed="50" capacity="3600.0" permlanes="1" oneway="1" modes="rail" >
  <attributes>
    <attribute name="trainCapacity" class="java.lang.Integer">1</attribute>
    <attribute name="trainOppositeDirectionLink" class="java.lang.String">B_A</attribute>
  </attributes>
</link>
<link id="B_A" from="B" to="A" length="200.0" freespeed="50" capacity="3600.0" permlanes="1" oneway="1" modes="rail" >
  <attributes>
    <attribute name="trainCapacity" class="java.lang.Integer">1</attribute>
    <attribute name="trainOppositeDirectionLink" class="java.lang.String">A_B</attribute>
  </attributes>
</link>
```

### Two tracks, each with a single direction

TODO

### Three tracks, with contraflow in the middle track

TODO

### Two tracks that intersect each other

It two tracks cross each other, e.g. like in the form of the letter `X` or a plus `+`, a train driving in one direction
effetively also blocks the intersecting tracks, even if they only share a common node, but not a common link.

TODO
