# super-sonic-osm-network-reader
Converts Osm-Data into a MATSim network. It uses the osm4j Framework to parse OSM's pbf format. 
After parsing the OSM file the conversion is parallized using Java's Stream-API.
The parser is currently 5 times faster than the MATSim standard parser.

## How to install
The project is published via Jitpack. To include it into your project simply add the following to your pom.xml

```
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>

<dependencies>
    <dependency>
        <groupId>com.github.Janekdererste</groupId>
        <artifactId>super-sonic-osm-network-reader</artifactId>
        <version>0.2.0</version>
    </dependency>
</dependencies>
```
## How to use it
The reader uses the builder pattern. With a simple set up you can convert your network the following way
```
String file = "path/to/your/file.osm.pbf";
String outputFile = "path/to/your/matsim-network.xml.gz";
// you may choose your own target coordinate system, but UTM32 is a good choice if you run a simulation in Germany
CoordinateTransformation coordinateTransformation = TransformationFactory.getCoordinateTransformation(TransformationFactory.WGS84, "EPSG:25832");

var network = SupersonicOsmNetworkReader.builder()
     .coordinateTransformation(coordinateTransformation)
     .build()
     .read(file);

new NetworkWriter(network).write(outputFile);
```

It is also possible to refine the result of the network parsing. The below example shows all possible features of the 
reader.

1. It is possible to apply a link filter. The below example includes all links which are of hierarchy level "motorway".
For simplicity filtering for a coordinate is skipped but this is the place to test whether a coordinate is within a 
certain area. This method is called for the from- and to nodes of a link.
2. The reader generally tries to simplify the network as much as possible. If it is necessary to preserve certain nodes
for e.g. implementing counts it is possible to omit the simplification for certain node ids. The below example 
prevents the reader to remove the node with id: 2.
3. It is possible to override the default properties wich are assigned to a link of a certain hierarchy level. E.g. one could change the freespeed of highways by adding a new LinkProperties object for the 'highway' tag. The example below adds LinkProperties for residential links, which are otherwise ignored.
4. After creating a link the reader will call the 'afterLinkCreated' hook with the newly created link, the original osm
tags, and a flag whether it is the forward or reverse direction of an osm-way. The below example sets the allowed 
transport mode on all links to 'car' and 'bike'. 
 
 ```
 String file = "path/to/your/file.osm.pbf";
 String outputFile = "path/to/your/matsim-network.xml.gz";
 // you may choose your own target coordinate system, but UTM32 is a good choice if you run a simulation in Germany
 CoordinateTransformation coordinateTransformation =  TransformationFactory.getCoordinateTransformation(TransformationFactory.WGS84, "EPSG:25832");
 var linkProperties = Map.of(
    "residential", new LinkProperties(9, 1, 30.0 / 3.6, 1500, false)
 );
 
var network = SupersonicOsmNetworkReader.builder()
     .coordinateTransformation(coordinateTransformation)
     .includeLinkAtCoordWithHierarchy((coord, hierachyLevel) -> hierachyLevel == LinkProperties.LEVEL_MOTORWAY)
     .preserveNodeWithId(id -> id == 2)
     .overridingLinkProperties(linkProperties)
     .afterLinkCreated((link, osmTags, isReverse) -> link.setAllowedModes(Set.of(TransportMode.car, TransportMode.bike)))
     .build()
     .read(file);
 
 new NetworkWriter(network).write(outputFile);
 ```
