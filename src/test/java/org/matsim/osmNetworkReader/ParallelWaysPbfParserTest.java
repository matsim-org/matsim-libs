package org.matsim.osmNetworkReader;

import org.junit.Test;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.Assert.assertEquals;

public class ParallelWaysPbfParserTest {

    private static ExecutorService executor = Executors.newSingleThreadExecutor();

    @Test
    public void parse_singleLink() throws IOException {

       var singleLink = Utils.createSingleLink();
        Path file = Paths.get("parallel-ways-parser-single-link.pbf");
        Utils.writeOsmData(singleLink.getNodes(), singleLink.getWays(), file);

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
            assertEquals(singleLink.getWays().get(0).getId(), wayReference.getWay().getId());
        }
    }

    @Test
    public void test_twoIntersectingWays() throws IOException {

        var twoIntersectingLinks = Utils.createTwoIntersectingLinksWithDifferentLevels();
        Path file = Paths.get("parallel-ways-parser-two-intersecting-links.pbf");
        Utils.writeOsmData(twoIntersectingLinks.getNodes(), twoIntersectingLinks.getWays(), file);

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