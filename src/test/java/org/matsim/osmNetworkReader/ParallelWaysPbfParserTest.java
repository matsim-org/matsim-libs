package org.matsim.osmNetworkReader;

import com.slimjars.dist.gnu.trove.list.array.TLongArrayList;
import de.topobyte.osm4j.core.model.impl.Node;
import de.topobyte.osm4j.core.model.impl.Tag;
import de.topobyte.osm4j.core.model.impl.Way;
import org.junit.Test;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.Assert.assertEquals;

public class ParallelWaysPbfParserTest {

    private static ExecutorService executor = Executors.newSingleThreadExecutor();

    @Test
    public void parse_singleLink() throws IOException {

        var node1 = new Node(1, 0, 0);
        var node2 = new Node(2, 100, 100);
        var node3 = new Node(3, 0, 200);
        var nodeReference = new TLongArrayList(new long[]{node1.getId(), node2.getId(), node3.getId()});
        var tags = List.of(new Tag(OsmTags.HIGHWAY, Utils.MOTORWAY));
        var way = new Way(1, nodeReference, tags);
        Path file = Paths.get("parallel-ways-parser-single-link.pbf");
        Utils.writeOsmData(List.of(node1, node2, node3), List.of(way), file);

        var waysParser = new ParallelWaysPbfParser(executor, LinkProperties.createLinkProperties());

        try (var fileInputStream = new FileInputStream(file.toFile())) {
            var input = new BufferedInputStream(fileInputStream);
            waysParser.parse(input);
        }

        var ways = waysParser.getWays();
        assertEquals(1, ways.size());

        var nodes = waysParser.getNodes();
        assertEquals(3, nodes.size());
        for (var wayReferences : nodes.values()) {
            assertEquals(1, wayReferences.size());
            var wayReference = wayReferences.get(0);
            assertEquals(way.getId(), wayReference.getId());
        }
    }

    @Test
    public void test_twoIntersectingWays() throws IOException {

        var node1 = new Node(1, 0, 0);
        var node2 = new Node(2, 100, 100);
        var node3 = new Node(3, 0, 200);
        var node4 = new Node(4, 200, 0);
        var node5 = new Node(5, 200, 200);
        var nodeReferenceForWay1 = new TLongArrayList(new long[]{node1.getId(), node2.getId(), node3.getId()});
        var nodeReferenceForWay2 = new TLongArrayList(new long[]{node4.getId(), node2.getId(), node5.getId()});
        var tags = List.of(new Tag(OsmTags.HIGHWAY, Utils.MOTORWAY));
        var way1 = new Way(1, nodeReferenceForWay1, tags);
        var way2 = new Way(2, nodeReferenceForWay2, tags);
        Path file = Paths.get("parallel-ways-parser-two-intersecting-links.pbf");
        Utils.writeOsmData(List.of(node1, node2, node3), List.of(way1, way2), file);

        var waysParser = new ParallelWaysPbfParser(executor, LinkProperties.createLinkProperties());

        try (var fileInputStream = new FileInputStream(file.toFile())) {
            var input = new BufferedInputStream(fileInputStream);
            waysParser.parse(input);
        }

        var ways = waysParser.getWays();
        assertEquals(2, ways.size());

        var nodes = waysParser.getNodes();
        assertEquals(5, nodes.size());

        assertEquals(2, nodes.get(2L).size());
        assertEquals(1, nodes.get(1L).size());
        assertEquals(1, nodes.get(1L).size());
        assertEquals(1, nodes.get(1L).size());
        assertEquals(1, nodes.get(1L).size());
    }
}