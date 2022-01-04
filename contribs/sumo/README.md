# SUMO Integration

This packages offers functionality to convert data from [SUMO](https://sumo.dlr.de/]) (Simulation of Urban MObility) into MATSim formats.

## Network import

It is possible to import SUMO networks and make use of lane-specific information, which can be used with MATSim lane features.
Converting from OSM to SUMO and then to MATSim allows integrating lane information and turn restrictions, which are not considered in the MATSim OSM contrib.


### Example Usage

This example assumes that [osmosis](https://wiki.openstreetmap.org/wiki/Osmosis#Downloading) and SUMO variables are present on the computer and set correctly.

First, convert an osm rbf file into .osm format. This step can also be used to do some first filtering:

    $(osmosis) --rb file=$INPUT\
     --tf accept-ways highway=motorway,motorway_link,trunk,trunk_link,primary,primary_link,secondary_link,secondary,tertiary,motorway_junction,residential,unclassified,living_street\
     --bounding-box top=51.65 left=6.00 bottom=50.60 right=7.56\
     --used-node --wx $OUTPUT
     
In the next step a SUMO network has to be generated using the tool `netconvert`. The given projection should already be the one desired in the MATSim network.
This tool can also be used to filter specific link types and simplify junctions, which are not needed on the most detailed level in MATSim:
     

	$(SUMO_HOME)/bin/netconvert --geometry.remove --ramps.guess --junctions.join --tls.discard-simple --tls.join\
	 --type-files $(SUMO_HOME)/data/typemap/osmNetconvert.typ.xml,$(SUMO_HOME)/data/typemap/osmNetconvertUrbanDe.typ.xml\
	 --roundabouts.guess --remove-edges.isolated\
	 --no-internal-links --keep-edges.by-vclass passenger --remove-edges.by-type highway.track,highway.services,highway.unsurfaced\
	 --remove-edges.by-vclass hov,tram,rail,rail_urban,rail_fast,pedestrian\
	 --output.original-names --output.street-names\
	 --proj "+proj=utm +zone=32 +ellps=GRS80 +towgs84=0,0,0,0,0,0,0 +units=m +no_defs"\
	 --osm-files $INPUT -o=$OUTPUT
	 
At Last, the generated network needs to be converted into a MATSim network. This step will also output a `lanes` file. This can be either done from command line (See help for more info):

    java -cp [JAR FILE] org.matsim.contrib.sumo.SumoNetworkConverter --help
    
or via code:

    SumoNetworkConverter converter = SumoNetworkConverter.newInstance(List.of(input), output, "EPSG:4326", "EPSG:4326");
    converter.convert(network, lanes);
