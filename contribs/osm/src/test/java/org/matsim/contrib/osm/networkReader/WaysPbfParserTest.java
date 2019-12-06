package org.matsim.contrib.osm.networkReader;

import org.junit.Rule;
import org.junit.Test;
import org.matsim.testcases.MatsimTestUtils;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.Assert.assertEquals;

public class WaysPbfParserTest {

    private static ExecutorService executor = Executors.newSingleThreadExecutor();

    @Rule
    public MatsimTestUtils matsimTestUtils = new MatsimTestUtils();

    @Test
    public void parse_singleLink() throws IOException {

        Utils.WaysAndLinks singleLink = Utils.createSingleLink();
        Path file = Paths.get(matsimTestUtils.getOutputDirectory(), "parallel-ways-parser-single-link.pbf");
        Utils.writeOsmData(singleLink.getNodes(), singleLink.getWays(), file);

        WaysPbfParser waysParser = new WaysPbfParser(executor, LinkProperties.createLinkProperties());

        try (InputStream fileInputStream = new FileInputStream(file.toFile())) {
            waysParser.parse(fileInputStream);
        }

        ConcurrentMap<Long, ProcessedOsmWay> ways = waysParser.getWays();
        assertEquals(1, ways.size());

        ConcurrentMap<Long, List<ProcessedOsmWay>> wayReferencesMap = waysParser.getNodes();
        assertEquals(3, wayReferencesMap.size());
        for (List<ProcessedOsmWay> wayReferences : wayReferencesMap.values()) {
            assertEquals(1, wayReferences.size());
            ProcessedOsmWay wayReference = wayReferences.get(0);
            assertEquals(singleLink.getWays().get(0).getId(), wayReference.getId());
        }
    }

    @Test
    public void test_twoIntersectingWays() throws IOException {

        Utils.WaysAndLinks twoIntersectingLinks = Utils.createTwoIntersectingLinksWithDifferentLevels();
        Path file = Paths.get(matsimTestUtils.getOutputDirectory(), "parallel-ways-parser-two-intersecting-links.pbf");
        Utils.writeOsmData(twoIntersectingLinks.getNodes(), twoIntersectingLinks.getWays(), file);

        WaysPbfParser waysParser = new WaysPbfParser(executor, LinkProperties.createLinkProperties());

        try (InputStream fileInputStream = new FileInputStream(file.toFile())) {
            waysParser.parse(fileInputStream);
        }

        ConcurrentMap<Long, ProcessedOsmWay> ways = waysParser.getWays();
        assertEquals(2, ways.size());

        ConcurrentMap<Long, List<ProcessedOsmWay>> nodes = waysParser.getNodes();
        assertEquals(5, nodes.size());

        assertEquals(2, nodes.get(2L).size());
        assertEquals(1, nodes.get(1L).size());
        assertEquals(1, nodes.get(1L).size());
        assertEquals(1, nodes.get(1L).size());
        assertEquals(1, nodes.get(1L).size());
    }
}