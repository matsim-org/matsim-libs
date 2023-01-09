package lsp;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.core.utils.io.MatsimXmlWriter;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Collection;

public class LSPPlanWriter extends MatsimXmlWriter {

    private static final  Logger logger = LogManager.getLogger(LSPPlanWriter.class);
    private final Collection<LSP> lsPs;

    public LSPPlanWriter(LSPs lsPs) {
        super();
        this.lsPs = lsPs.getLSPs().values();
    }


    public void write(String filename) {
        logger.info("write lsp");
        try {
            openFile(filename);
            writeXmlHead();
            writeTypes(this.writer);
            close();
            logger.info("done");
        } catch ( IOException e) {
            e.printStackTrace();
            logger.error(e);
            System.exit(1);
        }
    }

    private void writeTypes( BufferedWriter writer )throws IOException {
        writer.write("\t<LSPs>\n");
        for( LSPResource lsp : lsp.getResources()) {
            writer.write("\t\t<clientElements id=\"" + lsp.getClientElements() + "\">\n");
            writer.write("\t\t\t<tpString>" + lsp.toString() + "</tpString>\n");
        }
        writer.write("\t</LSPs>\n\n");
    }

}

