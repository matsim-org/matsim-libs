package org.matsim.contrib.bicycle.network;

import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.NetworkWriter;
import org.matsim.api.core.v01.network.Node;
import org.matsim.contrib.osm.networkReader.OsmBicycleReader;
import org.matsim.core.network.algorithms.MultimodalNetworkCleaner;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;

import java.util.Set;

public class CreateBicycleNetworkWithElevation {

    private static final String outputCRS = "EPSG:25832"; //UTM-32
    private static final String inputOsmFile = "path/to/your/input/file.osm.pbf";
    private static final String inputTiffFile = "path/to/your/elevation/tiff-file.tif";
    private static final String outputFile = "path/to/your/output/network.xml.gz";

    public static void main(String[] args) {

        var elevationParser = new ElevationDataParser(inputTiffFile, outputCRS);
        var transformation = TransformationFactory.getCoordinateTransformation(TransformationFactory.WGS84, outputCRS);

        var network = new OsmBicycleReader.Builder()
                .setCoordinateTransformation(transformation)
                .setAfterLinkCreated((link, tags, direction) -> {

                    addElevationIfNecessary(link.getFromNode(), elevationParser);
                    addElevationIfNecessary(link.getToNode(), elevationParser);
                })
                .build()
                .read(inputOsmFile);

        new MultimodalNetworkCleaner(network).run(Set.of(TransportMode.car));
        new MultimodalNetworkCleaner(network).run(Set.of(TransportMode.bike));

        new NetworkWriter(network).write(outputFile);
    }

    private static synchronized void addElevationIfNecessary(Node node, ElevationDataParser elevationParser) {

        if (!node.getCoord().hasZ()) {
            var elevation = elevationParser.getElevation(node.getCoord());
            var newCoord = CoordUtils.createCoord(node.getCoord().getX(), node.getCoord().getY(), elevation);
            // I think it should work to replace the coord on the node reference, since the network only stores references
            // to the node and the internal quad tree only references the x,y-values and the node. janek 4.2020
            node.setCoord(newCoord);
        }
    }
}
