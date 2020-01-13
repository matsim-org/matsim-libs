package org.matsim.contrib.osm.networkReader;

import de.topobyte.osm4j.core.access.OsmHandler;
import de.topobyte.osm4j.core.model.iface.OsmBounds;
import de.topobyte.osm4j.core.model.iface.OsmNode;
import de.topobyte.osm4j.core.model.iface.OsmRelation;
import de.topobyte.osm4j.core.model.iface.OsmWay;
import de.topobyte.osm4j.core.model.util.OsmModelUtil;
import de.topobyte.osm4j.pbf.protobuf.Osmformat;
import de.topobyte.osm4j.pbf.seq.PrimParser;
import de.topobyte.osm4j.pbf.util.PbfUtil;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Phaser;
import java.util.concurrent.atomic.AtomicInteger;

class WaysPbfParser extends PbfParser implements OsmHandler {

    private static Logger log = Logger.getLogger(WaysPbfParser.class);

    private final Phaser phaser = new Phaser();
    private final ExecutorService executor;

    private final ConcurrentMap<Long, ProcessedOsmWay> ways = new ConcurrentHashMap<>();
    private final ConcurrentMap<Long, List<ProcessedOsmWay>> nodes = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, LinkProperties> linkPropertiesMap;
    private final AtomicInteger counter = new AtomicInteger();

    public WaysPbfParser(ExecutorService executor, ConcurrentMap<String, LinkProperties> linkPropertiesMap) {
        this.executor = executor;
        this.linkPropertiesMap = linkPropertiesMap;
    }

    public ConcurrentMap<Long, ProcessedOsmWay> getWays() {
        return ways;
    }

    public ConcurrentMap<Long, List<ProcessedOsmWay>> getNodes() {
        return nodes;
    }

    public int getCounter() {
        return counter.get();
    }

    @Override
    void parse(InputStream inputStream) throws IOException {

        phaser.register(); // register the main thread which reads from the file
        super.parse(inputStream);
        log.info("awaiting all parsing tasks before interrupting the parsing");
        phaser.arriveAndAwaitAdvance();
        phaser.arriveAndDeregister();
    }

    @Override
    ParsingResult parse(Osmformat.HeaderBlock block) {
        Osmformat.HeaderBBox box = block.getBbox();
        this.handle(PbfUtil.bounds(box));
        return ParsingResult.Continue;
    }

    @Override
    ParsingResult parse(Osmformat.PrimitiveBlock block) {

        for (Osmformat.PrimitiveGroup primitiveGroup : block.getPrimitivegroupList()) {
            if (primitiveGroup.getWaysCount() > 0) {
                phaser.register();
                executor.execute(() -> startParseWaysTask(block, primitiveGroup));
            }
            if (primitiveGroup.getRelationsCount() > 0) {

                // relations are supposed to occur after ways in an osm file therefore stop it here
                return ParsingResult.Abort;
            }
        }
        return ParsingResult.Continue;
    }

    private void startParseWaysTask(Osmformat.PrimitiveBlock block, Osmformat.PrimitiveGroup group) {

        try {
            PrimParser primParser = new PrimParser(block, false);
            primParser.parseWays(group.getWaysList(), this);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            phaser.arriveAndDeregister();
        }
    }

    @Override
    public void handle(OsmWay osmWay) {

        counter.incrementAndGet();

        Map<String, String> tags = OsmModelUtil.getTagsAsMap(osmWay);

        if (isStreetOfInterest(tags)) {

            LinkProperties linkProperty = this.linkPropertiesMap.get(tags.get(OsmTags.HIGHWAY));
            ProcessedOsmWay processedWay = ProcessedOsmWay.create(osmWay, tags, linkProperty);
            ways.put(osmWay.getId(), processedWay);

            for (int i = 0; i < osmWay.getNumberOfNodes(); i++) {
                long nodeId = osmWay.getNodeId(i);
                List<ProcessedOsmWay> referencingWays = nodes.computeIfAbsent(nodeId, id -> new ArrayList<>());
                // we actually want to sycn on this very list
                synchronized (referencingWays) {
                    referencingWays.add(processedWay);
                }
            }
        }
        if (counter.get() % 100000 == 0) {
            log.info("Read: " + NumberFormat.getNumberInstance(Locale.US).format(counter.get()) + " ways");
        }
    }

    private boolean isStreetOfInterest(Map<String, String> tags) {
        return tags.containsKey(OsmTags.HIGHWAY) && linkPropertiesMap.containsKey(tags.get(OsmTags.HIGHWAY));
    }

    @Override
    public void handle(OsmBounds osmBounds) {
    }

    @Override
    public void handle(OsmNode osmNode) {
    }

    @Override
    public void handle(OsmRelation osmRelation) {
    }

    @Override
    public void complete() {
    }

}
