package org.matsim.osmNetworkReader;

import com.slimjars.dist.gnu.trove.list.array.TLongArrayList;
import de.topobyte.osm4j.core.model.impl.Node;
import de.topobyte.osm4j.core.model.impl.Tag;
import de.topobyte.osm4j.core.model.impl.Way;
import org.junit.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.core.utils.geometry.transformations.AtlantisToWGS84;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.Assert.*;

public class PrallelNodesPbfParserTest {

    private static ExecutorService executor = Executors.newSingleThreadExecutor();

    @Test
    public void parse_singleLink() throws IOException {

        var node1 = new Node(1, 0, 0);
        var node2 = new Node(2, 100, 100);
        var node3 = new Node(3, 0, 200);
        var nodeReference = new TLongArrayList(new long[]{node1.getId(), node2.getId(), node3.getId()});
        var tags = List.of(new Tag(OsmTags.HIGHWAY, Utils.MOTORWAY));
        var way = new Way(1, nodeReference, tags);

        Path file = Paths.get("parallel-nodes-parser-single-link.pbf");
        Utils.writeOsmData(List.of(node1, node2, node3), List.of(way), file);

        var waysParser = new ParallelWaysPbfParser(executor, LinkProperties.createLinkProperties());

        try (var fileInputStream = new FileInputStream(file.toFile())) {
            var input = new BufferedInputStream(fileInputStream);
            waysParser.parse(input);
        }

        var nodesParser = new PrallelNodesPbfParser(executor,
                (coord, level) -> true,
                id -> true,
                waysParser.getNodes(),
                Utils.transformation
        );

        try (var fileInputStream = new FileInputStream(file.toFile())) {
            var input = new BufferedInputStream(fileInputStream);
            waysParser.parse(input);
        }

        var nodes = nodesParser.getNodes();

        // we want all three nodes of the way
        assertEquals(3, nodes.size());

        // both end nodes should be preserved, but the middle one not.
        assertTrue(nodes.get(node1.getId()).isPreserve());
        assertFalse(nodes.get(node1.getId()).isPreserve());
        assertTrue(nodes.get(node1.getId()).isPreserve());
    }

    @Test
    public void parse_singleLink_withTransformation() throws IOException {

        final var transformation = new AtlantisToWGS84();

        var node1 = new Node(1, 0, 0);
        var node2 = new Node(2, 100, 100);
        var node3 = new Node(3, 0, 200);
        var nodeReference = new TLongArrayList(new long[]{node1.getId(), node2.getId(), node3.getId()});
        var tags = List.of(new Tag(OsmTags.HIGHWAY, Utils.MOTORWAY));
        var way = new Way(1, nodeReference, tags);

        Path file = Paths.get("parallel-nodes-parser-single-link-with-transformation.pbf");
        Utils.writeOsmData(List.of(node1, node2, node3), List.of(way), file);

        var waysParser = new ParallelWaysPbfParser(executor, LinkProperties.createLinkProperties());

        try (var fileInputStream = new FileInputStream(file.toFile())) {
            var input = new BufferedInputStream(fileInputStream);
            waysParser.parse(input);
        }

        var nodesParser = new PrallelNodesPbfParser(executor,
                (coord, level) -> true,
                id -> true,
                waysParser.getNodes(),
                transformation
        );

        try (var fileInputStream = new FileInputStream(file.toFile())) {
            var input = new BufferedInputStream(fileInputStream);
            waysParser.parse(input);
        }

        var nodes = nodesParser.getNodes();

        // we want all three nodes of the way
        assertEquals(3, nodes.size());

        var transformedNode1 = transformation.transform(new Coord(node1.getLatitude(), node2.getLongitude()));
        assertEquals(nodes.get(node1.getId()).getCoord(), transformedNode1);
    }
}