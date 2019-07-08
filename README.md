# super-sonic-osm-network-reader
Converts Osm-Data into a MATSim network. It uses the osm4j Framework to parse OSM's pbf format. 
After parsing the OSM file the conversion is parallized using Java's Stream-API.
The parser is currently 4 times faster than the MATSim standard parser.

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
        <version>0.1.0</version>
    </dependency>
</dependencies>
```
## Ho to use it
The reader uses the builder pattern. With a simple set up you can convert your network the following way
```
String file = "path/to/your/file.osm.pbf";
CoordinateTransformation coordinateTransformation = TransformationFactory.getCoordinateTransformation(TransformationFactory.WGS84, "EPSG:25832"); // you may choose your own target coordinate system, but UTM32 is a good choice if you run a simulatio in Germany
Network network = NetworkUtils.createNetwork();

new SupersonicOsmNetworkReader.Builder()
				.network(network)
				.coordinateTransformation(coordinateTransformation)
				.build()
				.read(file);

new NetworkWriter(network).write(output.toString());
```
