package org.matsim.osmNetworkReader;

import org.junit.Rule;
import org.junit.Test;
import org.matsim.testcases.MatsimTestUtils;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.Assert.assertEquals;

public class WaysPbfParserTest {

    private static ExecutorService executor = Executors.newSingleThreadExecutor();

    @Rule
    public MatsimTestUtils matsimTestUtils = new MatsimTestUtils();

    @Test
    public void parse_singleLink() throws IOException {

        var singleLink = Utils.createSingleLink();
        Path file = Paths.get(matsimTestUtils.getOutputDirectory(), "parallel-ways-parser-single-link.pbf");
        Utils.writeOsmData(singleLink.getNodes(), singleLink.getWays(), file);

        var waysParser = new WaysPbfParser(executor, LinkProperties.createLinkProperties());

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
            assertEquals(singleLink.getWays().get(0).getId(), wayReference.getId());
        }
    }

    @Test
    public void test_twoIntersectingWays() throws IOException {

        var twoIntersectingLinks = Utils.createTwoIntersectingLinksWithDifferentLevels();
        Path file = Paths.get(matsimTestUtils.getOutputDirectory(), "parallel-ways-parser-two-intersecting-links.pbf");
        Utils.writeOsmData(twoIntersectingLinks.getNodes(), twoIntersectingLinks.getWays(), file);

        var waysParser = new WaysPbfParser(executor, LinkProperties.createLinkProperties());

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