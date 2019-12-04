package org.matsim.osmNetworkReader;

import de.topobyte.osm4j.core.access.OsmHandler;
import de.topobyte.osm4j.core.model.iface.OsmBounds;
import de.topobyte.osm4j.core.model.iface.OsmNode;
import de.topobyte.osm4j.core.model.iface.OsmRelation;
import de.topobyte.osm4j.core.model.iface.OsmWay;
import de.topobyte.osm4j.pbf.protobuf.Osmformat;
import de.topobyte.osm4j.pbf.seq.PrimParser;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.matsim.api.core.v01.Coord;
import org.matsim.core.utils.geometry.CoordinateTransformation;

import java.io.IOException;
import java.io.InputStream;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Phaser;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiPredicate;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Log4j2
class NodesPbfParser extends PbfParser implements OsmHandler {

    private final Phaser phaser = new Phaser();
    private final AtomicInteger counter = new AtomicInteger();

    private final ExecutorService executor;
    private final BiPredicate<Coord, Integer> linkFilter;
    private final ConcurrentMap<Long, List<ProcessedOsmWay>> nodesToKeep;
    private final CoordinateTransformation coordinateTransformation;

    @Getter
    private final ConcurrentMap<Long, ProcessedOsmNode> nodes = new ConcurrentHashMap<>();

    int getCount() {
        return counter.get();
    }

    @Override
    void parse(InputStream inputStream) throws IOException {

        // register main thread at phaser
        phaser.register();
        super.parse(inputStream);
        log.info("awaiting all parsing tasks before interrupting the parsing");
        phaser.arriveAndAwaitAdvance();
        phaser.arriveAndDeregister();
    }

    @Override
    ParsingResult parse(Osmformat.HeaderBlock block) {
        return ParsingResult.Continue;
    }

    @Override
    protected ParsingResult parse(Osmformat.PrimitiveBlock block) {

        for (var primitiveGroup : block.getPrimitivegroupList()) {
            if (primitiveGroup.hasDense()) {
                phaser.register();
                executor.execute(() -> startParseDenseTask(block, primitiveGroup));
            }
            if (primitiveGroup.getNodesCount() > 0) {
                phaser.register();
                executor.execute(() -> startParseNodesTask(block, primitiveGroup));
            }
            if (primitiveGroup.getWaysCount() > 0) {
                // ways are supposed to occur after ways in an osm file therefore stop it here
                return ParsingResult.Abort;
            }
        }
        return ParsingResult.Continue;
    }

    // have these more or less identical methods, since I currently don't know how to handle IOExceptions otherwise
    private void startParseDenseTask(Osmformat.PrimitiveBlock block, Osmformat.PrimitiveGroup group) {

        try {
            var primParser = new PrimParser(block, false);
            primParser.parseDense(group.getDense(), this);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            phaser.arriveAndDeregister();
        }
    }

    private void startParseNodesTask(Osmformat.PrimitiveBlock block, Osmformat.PrimitiveGroup group) {

        try {
            var primParser = new PrimParser(block, false);
            primParser.parseNodes(group.getNodesList(), this);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            phaser.arriveAndDeregister();
        }
    }

    @Override
    public void handle(OsmNode osmNode) {

        counter.incrementAndGet();

        if (nodesToKeep.containsKey(osmNode.getId())) {

            var waysThatReferenceNode = nodesToKeep.get(osmNode.getId());
            var transformedCoord = coordinateTransformation.transform(new Coord(osmNode.getLongitude(), osmNode.getLatitude()));
            var filteredReferencingLinks = testWhetherReferencingLinksAreInFilter(transformedCoord, waysThatReferenceNode);

            var result = new ProcessedOsmNode(osmNode.getId(), filteredReferencingLinks, transformedCoord);
            this.nodes.put(result.getId(), result);
        }
        if (counter.get() % 500000 == 0) {
            log.info("Read: " + NumberFormat.getNumberInstance(Locale.US).format(counter.get()) + " nodes");
        }
    }

    private List<ProcessedOsmWay> testWhetherReferencingLinksAreInFilter(Coord coord, List<ProcessedOsmWay> waysThatReferenceNode) {

        return waysThatReferenceNode.stream()
                .filter(way -> linkFilter.test(coord, way.getLinkProperties().hierachyLevel))
                .collect(Collectors.toUnmodifiableList());
    }

    @Override
    public void handle(OsmBounds osmBounds) {
    }

    @Override
    public void handle(OsmWay osmWay) {
    }

    @Override
    public void handle(OsmRelation osmRelation) {
    }

    @Override
    public void complete() {
    }
}
