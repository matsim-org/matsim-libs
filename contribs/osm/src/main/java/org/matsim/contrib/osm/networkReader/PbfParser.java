package org.matsim.contrib.osm.networkReader;

import de.topobyte.osm4j.core.access.OsmHandler;
import de.topobyte.osm4j.core.model.iface.OsmBounds;
import de.topobyte.osm4j.core.model.iface.OsmNode;
import de.topobyte.osm4j.core.model.iface.OsmRelation;
import de.topobyte.osm4j.core.model.iface.OsmWay;
import de.topobyte.osm4j.pbf.Constants;
import de.topobyte.osm4j.pbf.protobuf.Fileformat;
import de.topobyte.osm4j.pbf.protobuf.Osmformat;
import de.topobyte.osm4j.pbf.seq.PrimParser;
import de.topobyte.osm4j.pbf.util.BlobHeader;
import de.topobyte.osm4j.pbf.util.BlockData;
import de.topobyte.osm4j.pbf.util.PbfUtil;

import java.io.*;
import java.nio.file.Path;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Phaser;
import java.util.function.Consumer;

public class PbfParser implements OsmHandler {

    private final Consumer<OsmNode> nodeHandler;
    private final Consumer<OsmWay> waysHandler;
    private final Consumer<OsmRelation> relationHandler;

    private final Phaser phaser = new Phaser();
    private final ExecutorService executor;
    private PbfParser(Consumer<OsmNode> nodeHandler, Consumer<OsmWay> waysHandler, Consumer<OsmRelation> relationHandler, ExecutorService executor) {
        this.nodeHandler = nodeHandler;
        this.waysHandler = waysHandler;
        this.relationHandler = relationHandler;
        this.executor = executor;
    }

    public void parse(Path file) {
        try (InputStream fileInputStream = new BufferedInputStream(new FileInputStream(file.toFile()))) {
            parse(fileInputStream);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

   public void parse(InputStream input) throws IOException {

        DataInputStream data = new DataInputStream(input);
        ParsingResult parsingResult = ParsingResult.Continue;
        phaser.register(); // register the main thread which reads from the file
        while (parsingResult.equals(ParsingResult.Continue)) {
            try {
                parsingResult = parseBlob(data);
            } catch (EOFException e) {
                break;
            }
        }
        phaser.arriveAndAwaitAdvance();
        phaser.arriveAndDeregister();
    }

    private ParsingResult parseBlob(DataInput data) throws IOException {

        BlobHeader header = PbfUtil.parseHeader(data);
        Fileformat.Blob blob = PbfUtil.parseBlock(data, header.getDataLength());
        return parse(header, blob);
    }

    private ParsingResult parse(BlobHeader header, Fileformat.Blob blob)
            throws IOException {
        BlockData blockData = PbfUtil.getBlockData(blob);

        if (header.getType().equals(Constants.BLOCK_TYPE_DATA)) {
            Osmformat.PrimitiveBlock primBlock = Osmformat.PrimitiveBlock
                    .parseFrom(blockData.getBlobData());
            return parse(primBlock);
        } else if (header.getType().equals(Constants.BLOCK_TYPE_HEADER)) {
            return ParsingResult.Continue;
        } else {
            throw new IOException("invalid PBF block");
        }
    }

    private ParsingResult parse(Osmformat.PrimitiveBlock block) {

        for (Osmformat.PrimitiveGroup primitiveGroup : block.getPrimitivegroupList()) {
            if (primitiveGroup.hasDense() && nodeHandler != null) {
                phaser.register();
                executor.execute(() -> startParseDenseTask(block, primitiveGroup));
            } else if (primitiveGroup.getNodesCount() > 0 && nodeHandler != null) {
                phaser.register();
                executor.execute(() -> startParseNodesTask(block, primitiveGroup));
            } else if (primitiveGroup.getWaysCount() > 0 && waysHandler == null && relationHandler == null) {
                return ParsingResult.Abort;
            } else if (primitiveGroup.getWaysCount() > 0 && waysHandler != null) {
                phaser.register();
                executor.execute(() -> startParseWaysTask(block, primitiveGroup));
            } else if (primitiveGroup.getRelationsCount() > 0 && relationHandler == null) {
                return ParsingResult.Abort;
            } else if (primitiveGroup.getRelationsCount() > 0) {
                phaser.register();
                executor.execute(() -> startParseRelationsTask(block, primitiveGroup));
            }
        }
        return ParsingResult.Continue;
    }

    // have these more or less identical methods, since I currently don't know how to handle IOExceptions otherwise
    private void startParseDenseTask(Osmformat.PrimitiveBlock block, Osmformat.PrimitiveGroup group) {

        try {
            PrimParser primParser = new PrimParser(block, false);
            primParser.parseDense(group.getDense(), this);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            phaser.arriveAndDeregister();
        }
    }

    private void startParseNodesTask(Osmformat.PrimitiveBlock block, Osmformat.PrimitiveGroup group) {

        try {
            PrimParser primParser = new PrimParser(block, false);
            primParser.parseNodes(group.getNodesList(), this);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            phaser.arriveAndDeregister();
        }
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

    private void startParseRelationsTask(Osmformat.PrimitiveBlock block, Osmformat.PrimitiveGroup group) {

        try {
            PrimParser primParser = new PrimParser(block, false);
            primParser.parseRelations(group.getRelationsList(), this);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            phaser.arriveAndDeregister();
        }
    }

    @Override
    public void handle(OsmBounds bounds) {
        // do nothing
    }

    @Override
    public void handle(OsmNode node) {
        this.nodeHandler.accept(node);
    }

    @Override
    public void handle(OsmWay way) {
        this.waysHandler.accept(way);
    }

    @Override
    public void handle(OsmRelation relation) {
        this.relationHandler.accept(relation);
    }

    @Override
    public void complete() {
        //nothing to do here
    }

    enum ParsingResult {Continue, Abort}

    public static class Builder {
        private Consumer<OsmNode> nodeHandler;
        private Consumer<OsmWay> waysHandler;
        private Consumer<OsmRelation> relationHandler;
        private ExecutorService executor;

        public Builder setNodeHandler(Consumer<OsmNode> nodeHandler) {
            this.nodeHandler = nodeHandler;
            return this;
        }

        public Builder setWaysHandler(Consumer<OsmWay> waysHandler) {
            this.waysHandler = waysHandler;
            return this;
        }

        public Builder setRelationHandler(Consumer<OsmRelation> relationHandler) {
            this.relationHandler = relationHandler;
            return this;
        }

        public Builder setExecutor(ExecutorService executor) {
            this.executor = executor;
            return this;
        }

        public PbfParser build() {

            if (executor == null) throw new RuntimeException("Executor must be provided");
            if (nodeHandler == null && waysHandler == null && relationHandler == null)
                throw new RuntimeException("No handler was provided. This way, the file is parsed without extracting information.");
            return new PbfParser(nodeHandler, waysHandler, relationHandler, executor);
        }
    }
}
