# super-sonic-osm-network-reader
Converts Osm-Data into a MATSim network. It uses the osm4j Framework to parse OSM's pbf format. 
After parsing the OSM file the conversion is parallized using Java's Stream-API.
The parser is currently 4 times faster than the MATSim standard parser.
