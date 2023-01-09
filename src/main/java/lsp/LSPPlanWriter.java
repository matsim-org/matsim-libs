package lsp;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.core.utils.io.MatsimXmlWriter;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Collection;

public class LSPPlanWriter extends MatsimXmlWriter {

    private static final  Logger logger = LogManager.getLogger(LSPPlanWriter.class);
    private final Collection<LSP> lsps;

    public LSPPlanWriter(LSPs lsPs) {
        super();
        this.lsps = lsPs.getLSPs().values();
    }


    public void write(String filename) {
        logger.info("write lsp");
        try {
            openFile(filename);
            writeXmlHead();
			// Ich habe das mal hier "hoch gezogen". Ist dann etwas analoger zu dem CarrierWriter.
			writer.write("\t<LSPs>\n");
			for (LSP lsp : lsps) {
				writeTypes(lsp, this.writer); //Übergibt dann nun den LSP mit und du kannst direkt drauf zugreifen.
			}
			writer.write("\t</LSPs>\n\n");
			//Ende der Hochziehung
            close();
            logger.info("done");
        } catch ( IOException e) {
            e.printStackTrace();
            logger.error(e);
            System.exit(1);
        }
    }

    private void writeTypes(LSP lsp,  BufferedWriter writer )throws IOException {
		//Leicht umsortiert und hier musst du dann schauen, was du ausgeben willst :)
		writer.write("\t\t\t<tpString>" + lsp + "</tpString>\n");
		for (LSPResource resource : lsp.getResources()) {
			writer.write("\t\t<clientElements id=\"" + resource.getClientElements() + "\">\n");
		}
    }

}

