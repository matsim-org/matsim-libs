package org.matsim.osmNetworkReader;

import de.topobyte.osm4j.core.access.OsmHandler;
import de.topobyte.osm4j.core.model.iface.OsmBounds;
import de.topobyte.osm4j.core.model.iface.OsmNode;
import de.topobyte.osm4j.core.model.iface.OsmRelation;
import de.topobyte.osm4j.core.model.iface.OsmWay;
import de.topobyte.osm4j.core.model.util.OsmModelUtil;
import de.topobyte.osm4j.pbf.protobuf.Osmformat;
import de.topobyte.osm4j.pbf.seq.PrimParser;
import de.topobyte.osm4j.pbf.util.PbfUtil;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Phaser;
import java.util.concurrent.atomic.AtomicInteger;

@RequiredArgsConstructor
@Log4j2
class WaysPbfParser extends PbfParser implements OsmHandler {

    private final Phaser phaser = new Phaser();
    private final ExecutorService executor;

    @Getter
    private final ConcurrentMap<Long, ProcessedOsmWay> ways = new ConcurrentHashMap<>();
    @Getter
    private final ConcurrentMap<Long, List<ProcessedOsmWay>> nodes = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, LinkProperties> linkProperties;
    private final AtomicInteger counter = new AtomicInteger();

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

        for (var primitiveGroup : block.getPrimitivegroupList()) {
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
            var primParser = new PrimParser(block, false);
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

        var tags = OsmModelUtil.getTagsAsMap(osmWay);

        if (isStreetOfInterest(tags)) {

            var linkProperty = this.linkProperties.get(tags.get(OsmTags.HIGHWAY));
            var wayWrapper = ProcessedOsmWay.create(osmWay, tags, linkProperty);
            ways.put(osmWay.getId(), wayWrapper);

            for (int i = 0; i < osmWay.getNumberOfNodes(); i++) {
                var nodeId = osmWay.getNodeId(i);
                nodes.computeIfAbsent(nodeId, id -> new ArrayList<>()).add(wayWrapper);
            }
        }
        if (counter.get() % 100000 == 0) {
            log.info("Read: " + counter.get() / 1000 + "K ways");
        }
    }

    private boolean isStreetOfInterest(Map<String, String> tags) {
        return tags.containsKey(OsmTags.HIGHWAY) && linkProperties.containsKey(tags.get(OsmTags.HIGHWAY));
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
