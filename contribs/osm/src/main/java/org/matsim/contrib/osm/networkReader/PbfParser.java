package org.matsim.contrib.osm.networkReader;

import de.topobyte.osm4j.pbf.Constants;
import de.topobyte.osm4j.pbf.protobuf.Fileformat;
import de.topobyte.osm4j.pbf.protobuf.Osmformat;
import de.topobyte.osm4j.pbf.util.BlobHeader;
import de.topobyte.osm4j.pbf.util.BlockData;
import de.topobyte.osm4j.pbf.util.PbfUtil;

import java.io.*;

abstract class PbfParser {

    void parse(InputStream input) throws IOException {

        DataInputStream data = new DataInputStream(input);
		ParsingResult parsingResult = ParsingResult.Continue;
        while (parsingResult.equals(ParsingResult.Continue)) {
            try {
                parsingResult = parseBlob(data);
            } catch (EOFException e) {
                break;
            }
        }
    }

    private ParsingResult parseBlob(DataInput data) throws IOException {

        BlobHeader header = PbfUtil.parseHeader(data);
        Fileformat.Blob blob = PbfUtil.parseBlock(data, header.getDataLength());
        return parse(header, blob);
    }

    private ParsingResult parse(BlobHeader header, Fileformat.Blob blob)
            throws IOException {
        BlockData blockData = PbfUtil.getBlockData(blob);

        String type = header.getType();
        if (type.equals(Constants.BLOCK_TYPE_DATA)) {
            Osmformat.PrimitiveBlock primBlock = Osmformat.PrimitiveBlock
                    .parseFrom(blockData.getBlobData());
            return parse(primBlock);
        } else if (type.equals(Constants.BLOCK_TYPE_HEADER)) {
            Osmformat.HeaderBlock headerBlock = Osmformat.HeaderBlock
                    .parseFrom(blockData.getBlobData());
            return parse(headerBlock);
        } else {
            throw new IOException("invalid PBF block");
        }
    }

    abstract ParsingResult parse(Osmformat.HeaderBlock block) throws IOException;

    abstract ParsingResult parse(Osmformat.PrimitiveBlock block) throws IOException;

    enum ParsingResult {Continue, Abort}
}
