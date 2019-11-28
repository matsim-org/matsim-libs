package org.matsim.osmNetworkReader;

import de.topobyte.osm4j.core.access.OsmHandler;
import de.topobyte.osm4j.core.model.iface.OsmBounds;
import de.topobyte.osm4j.core.model.iface.OsmNode;
import de.topobyte.osm4j.core.model.iface.OsmRelation;
import de.topobyte.osm4j.core.model.iface.OsmWay;
import de.topobyte.osm4j.pbf.protobuf.Osmformat;
import de.topobyte.osm4j.pbf.seq.BlockParser;
import de.topobyte.osm4j.pbf.seq.PrimParser;
import de.topobyte.osm4j.pbf.util.PbfUtil;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Phaser;
import java.util.concurrent.atomic.AtomicInteger;

@RequiredArgsConstructor
@Log4j2
public class ParallelWaysPbfParser extends BlockParser implements OsmHandler {

    private final Phaser phaser = new Phaser();
    private final ExecutorService executor;

    @Getter
    private final Set<OsmWay> ways = ConcurrentHashMap.newKeySet();
    @Getter
    private final ConcurrentMap<Long, Integer> nodes = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, LinkProperties> linkProperties;
    private final AtomicInteger counter = new AtomicInteger();

    private static boolean isStreetOfInterest(OsmWay way, ConcurrentMap<String, LinkProperties> linkProperties) {
        for (int i = 0; i < way.getNumberOfTags(); i++) {
            String tag = way.getTag(i).getKey();
            String tagvalue = way.getTag(i).getValue();
            if (tag.equals(OsmTags.HIGHWAY) && linkProperties.containsKey(tagvalue)) return true;
        }
        return false;
    }

    public int getCounter() {
        return counter.get();
    }

    @Override
    public void parse(InputStream inputStream) throws IOException {
        log.info("Register main thread at phaser.");
        phaser.register(); // register the main thread which reads from the file
        super.parse(inputStream);
        log.info("deregister main thread from phaser");
        phaser.arriveAndDeregister();
    }

    @Override
    protected void parse(Osmformat.HeaderBlock block) throws IOException {
        Osmformat.HeaderBBox box = block.getBbox();
        this.handle(PbfUtil.bounds(box));
    }

    @Override
    protected void parse(Osmformat.PrimitiveBlock block) throws IOException {

        for (var primitiveGroup : block.getPrimitivegroupList()) {
            if (primitiveGroup.getWaysCount() > 0)
                executor.execute(() -> startParseWaysTask(block, primitiveGroup));
            if (primitiveGroup.getRelationsCount() > 0) {
                // wait for all parser tasks to finish
                log.info("awaiting all parsing tasks before interrupting the parsing");
                phaser.arriveAndAwaitAdvance();
                log.info("All parsing tasks have finished. stop reading the file.");
                // relations are supposed to occur after ways in an osm file therefore stop it here
                throw new EOFException("don't want to read all the relations");
            }
        }
    }

    private void startParseWaysTask(Osmformat.PrimitiveBlock block, Osmformat.PrimitiveGroup group) {

        phaser.register();
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
    public void handle(OsmBounds osmBounds) throws IOException {

    }

    @Override
    public void handle(OsmNode osmNode) throws IOException {

    }

    @Override
    public void handle(OsmWay osmWay) throws IOException {

        counter.incrementAndGet();
        if (isStreetOfInterest(osmWay, linkProperties)) {
            ways.add(osmWay);
            for (int i = 0; i < osmWay.getNumberOfNodes(); i++) {
                nodes.merge(osmWay.getNodeId(i), 1, Integer::sum);
            }
        }
        if (counter.get() % 100000 == 0) {
            log.info("Read: " + counter.get() / 1000 + "K ways");
        }
    }

    @Override
    public void handle(OsmRelation osmRelation) throws IOException {

    }

    @Override
    public void complete() throws IOException {

    }
}
