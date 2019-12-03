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
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Phaser;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

@RequiredArgsConstructor
@Log4j2
public class PrallelNodesPbfParser extends PbfParser implements OsmHandler {

    private final Phaser phaser = new Phaser();
    private final AtomicInteger counter = new AtomicInteger();

    private final ExecutorService executor;
    private final BiPredicate<Coord, Integer> linkFilter;
    private final Predicate<Long> preserveNodesFilter;
    private final ConcurrentMap<Long, List<ParallelWaysPbfParser.OsmWayWrapper>> nodesToKeep;
    private final CoordinateTransformation coordinateTransformation;

    @Getter
    private final ConcurrentMap<Long, LightOsmNode> nodes = new ConcurrentHashMap<>();

    @Override
    void parse(InputStream inputStream) throws IOException {

        log.info("Register main thread at phaser");
        phaser.register();
        super.parse(inputStream);
        log.info("awaiting all parsing tasks before interrupting the parsing");
        phaser.arriveAndAwaitAdvance();
        log.info("deregister main thread from phaser");
        phaser.arriveAndDeregister();
    }

    @Override
    ParsingResult parse(Osmformat.HeaderBlock block) throws IOException {
        return ParsingResult.Continue;
    }

    @Override
    protected ParsingResult parse(Osmformat.PrimitiveBlock block) throws IOException {

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
    public void handle(OsmBounds osmBounds) throws IOException {

    }

    @Override
    public void handle(OsmNode osmNode) throws IOException {

        counter.incrementAndGet();

        if (nodesToKeep.containsKey(osmNode.getId())) {

            var waysThatReferenceNode = nodesToKeep.get(osmNode.getId());
            var isEndNode = isEndNodeOfAnyWay(osmNode, waysThatReferenceNode);

            if (waysThatReferenceNode.size() > 1 || isEndNode) {

                var nodeCoord = coordinateTransformation.transform(new Coord(osmNode.getLongitude(), osmNode.getLatitude()));
                var filteredReferenceCount = doSomeFiltering(nodeCoord, waysThatReferenceNode);

                LightOsmNode result;
                if (filteredReferenceCount > 1) {
                    result = new LightOsmNode(osmNode.getId(), true, nodeCoord);
                } else if (filteredReferenceCount == 1 && isEndNode) {
                    result = new LightOsmNode(osmNode.getId(), true, nodeCoord);
                } else if (filteredReferenceCount == 1 /*Test for isPreserve here*/) {
                    result = new LightOsmNode(osmNode.getId(), true, nodeCoord);
                } else {
                    result = new LightOsmNode(osmNode.getId(), false, nodeCoord);
                }

            }
        }
    }


    private boolean isEndNodeOfAnyWay(OsmNode node, List<ParallelWaysPbfParser.OsmWayWrapper> waysThatReferenceNode) {
        return waysThatReferenceNode.stream()
                .anyMatch(way -> way.getStartNodeId() == node.getId() || way.getEndNodeId() == node.getId());
    }

    private long doSomeFiltering(Coord nodeCoord, List<ParallelWaysPbfParser.OsmWayWrapper> waysThatReferenceNode) {

        return waysThatReferenceNode.stream()
                .filter(way -> linkFilter.test(nodeCoord, way.getLinkProperties().hierachyLevel))
                .count();
    }

    @Override
    public void handle(OsmWay osmWay) throws IOException {

    }

    @Override
    public void handle(OsmRelation osmRelation) throws IOException {

    }

    @Override
    public void complete() throws IOException {

    }
}
