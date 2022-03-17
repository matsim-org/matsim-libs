package org.matsim.application.prepare.freight.dataProcessing;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkWriter;
import org.matsim.application.MATSimAppCommand;
import org.matsim.application.options.CrsOptions;
import org.matsim.contrib.osm.networkReader.SupersonicOsmNetworkReader;
import org.matsim.core.network.algorithms.MultimodalNetworkCleaner;
import picocli.CommandLine;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ReadFreightNetwork implements MATSimAppCommand {
    private static final Logger log = LogManager.getLogger(ReadFreightNetwork.class);

    @CommandLine.Option(names = "--input", description = "input pbf file", required = true)
    private Path input;

    @CommandLine.Option(names = "--output", description = "output network file", required = true)
    private Path output;

    @CommandLine.Mixin
    private CrsOptions crs = new CrsOptions();
    // Input CRS: WGS84 (EPSG:4326). Recommended target CRS: EPSG:25832 or EPSG:5677

    public static void main(String[] args) {
        new ReadFreightNetwork().execute(args);
    }

    @Override
    public Integer call() throws Exception {
        Network network = new SupersonicOsmNetworkReader.Builder()
                .setCoordinateTransformation(crs.getTransformation())
                .setPreserveNodeWithId(id -> id == 2)
                .setAfterLinkCreated((link, osmTags, isReverse) -> link.setAllowedModes(new HashSet<>(List.of(TransportMode.car))))
                .build()
                .read(input.toString());

        var cleaner = new MultimodalNetworkCleaner(network);
        cleaner.run(Set.of(TransportMode.car));

        new NetworkWriter(network).write(output.toString());
        return 0;
    }
}
