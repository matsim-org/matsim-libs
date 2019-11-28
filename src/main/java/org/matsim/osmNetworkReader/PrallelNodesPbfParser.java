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
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.matsim.api.core.v01.Coord;
import org.matsim.core.utils.geometry.CoordinateTransformation;

import java.io.EOFException;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Phaser;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiPredicate;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Log4j2
public class PrallelNodesPbfParser extends BlockParser implements OsmHandler {

    private final Phaser phaser = new Phaser();
    private final AtomicInteger counter = new AtomicInteger();

    private final ExecutorService executor;
    private final BiPredicate<Coord, Integer> linkFilter;
    private final Predicate<Long> preserveNodesFilter;
    private final ConcurrentMap<Long, List<OsmNodeToKeep>> nodesToKeep;
    private final CoordinateTransformation coordinateTransformation;

    @Override
    protected void parse(Osmformat.HeaderBlock block) throws IOException {
        Osmformat.HeaderBBox bbox = block.getBbox();
        this.handle(PbfUtil.bounds(bbox));
    }

    @Override
    protected void parse(Osmformat.PrimitiveBlock block) throws IOException {

        for (var primitiveGroup : block.getPrimitivegroupList()) {
            if (primitiveGroup.hasDense()) {
                executor.execute(() -> startParseDenseTask(block, primitiveGroup));
            }
            if (primitiveGroup.getNodesCount() > 0) {
                executor.execute(() -> startParseNodesTask(block, primitiveGroup));
            }
            if (primitiveGroup.getWaysCount() > 0) {
                // wait for all parser tasks to finish
                log.info("awaiting all parsing tasks before interrupting the parsing");
                phaser.arriveAndAwaitAdvance();
                log.info("All parsing tasks have finished. stop reading the file.");
                // relations are supposed to occur after ways in an osm file therefore stop it here
                throw new EOFException("don't want to read all the relations");
            }
        }
    }

    private void startParseDenseTask(Osmformat.PrimitiveBlock block, Osmformat.PrimitiveGroup group) {

        phaser.register();
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

        phaser.register();
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

            var references = nodesToKeep.get(osmNode.getId());

            // if node is used by more than one way or is an end node of any way
            if (references.size() > 1 || references.stream().anyMatch(OsmNodeToKeep::isEndNode)) {

                Coord coord = coordinateTransformation.transform(new Coord(osmNode.getLatitude(), osmNode.getLatitude()));
                // we must test whether this node should be included
                var referencesWhichMatchFilter = references.stream()
                        .filter(ref -> linkFilter.test(coord, ref.getHierachyLevel()))
                        .collect(Collectors.toList());

                // if still more than 1 way references this node preserve it
                if (referencesWhichMatchFilter.size() > 1) {
                    // use this node
                }
                // if only one way references this node but it is its end node
                else if (referencesWhichMatchFilter.size() == 1 && referencesWhichMatchFilter.get(0).isEndNode()) {
                    // use this node
                } else {
                    // we could exclude the node, except the preserve nodes filter prohibits this
                    if (preserveNodesFilter.test(osmNode.getId())) {
                        // use this node
                    } else {
                        // don't use this node
                    }
                }
            } else if (preserveNodesFilter.test(osmNode.getId())) {
                // use this node
            } else {
                // don't use this node
            }
        }
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
