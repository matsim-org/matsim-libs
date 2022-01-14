package org.matsim.application.prepare.freight.dataProcessing;

import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkWriter;
import org.matsim.contrib.osm.networkReader.SupersonicOsmNetworkReader;
import org.matsim.core.network.algorithms.MultimodalNetworkCleaner;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ReadFreightNetwork {
    private static final String inputFile = "/Users/luchengqi/Documents/MATSimScenarios/GermanFreight/EU-DE-network/germany-eu-network.osm.pbf";
    private static final String outputFile = "/Users/luchengqi/Documents/MATSimScenarios/GermanFreight/EU-DE-network/testing-germany-eu-network.xml.gz";
    private static final CoordinateTransformation coordinateTransformation = TransformationFactory.getCoordinateTransformation(TransformationFactory.WGS84, "EPSG:25832");

    public static void main(String[] args) {

        Network network = new SupersonicOsmNetworkReader.Builder()
                .setCoordinateTransformation(coordinateTransformation)
                .setPreserveNodeWithId(id -> id == 2)
                .setAfterLinkCreated((link, osmTags, isReverse) -> link.setAllowedModes(new HashSet<>(List.of(TransportMode.car))))
                .build()
                .read(inputFile);

        var cleaner = new MultimodalNetworkCleaner(network);
        cleaner.run(Set.of(TransportMode.car));

        new NetworkWriter(network).write(outputFile);
    }
}
