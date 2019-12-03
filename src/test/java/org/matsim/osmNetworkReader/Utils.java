package org.matsim.osmNetworkReader;

import de.topobyte.osm4j.core.model.iface.OsmNode;
import de.topobyte.osm4j.core.model.iface.OsmWay;
import de.topobyte.osm4j.pbf.seq.PbfWriter;
import lombok.extern.log4j.Log4j2;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.IdentityTransformation;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;

@Log4j2
public class Utils {

    static final CoordinateTransformation transformation = new IdentityTransformation();
    static final String MOTORWAY = "motorway";
    static final String TRUNK = "trunk";
    static final String TERTIARY = "tertiary";


    static void writeOsmData(Collection<OsmNode> nodes, Collection<OsmWay> ways, Path file) {

        try (OutputStream outputStream = Files.newOutputStream(file)) {
            var writer = new PbfWriter(outputStream, true);
            for (OsmNode node : nodes) {
                writer.write(node);
            }

            for (OsmWay way : ways) {
                writer.write(way);
            }
            writer.complete();
        } catch (IOException e) {
            log.error("could not write osm data");
            e.printStackTrace();
        }
    }
}
