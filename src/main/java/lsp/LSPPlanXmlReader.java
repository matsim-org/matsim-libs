package lsp;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.contrib.freight.carrier.*;
import org.matsim.core.api.internal.MatsimReader;
import org.matsim.core.utils.io.MatsimXmlParser;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import java.io.InputStream;
import java.net.URL;
import java.util.Stack;

public class LSPPlanXmlReader implements MatsimReader {
    private static final Logger log = LogManager.getLogger(LSPPlanXmlReader.class);
    private final LSPsPlanReader reader;


    public LSPPlanXmlReader(LSPs lsPs, Carriers carriers, CarrierVehicleTypes carrierVehicleTypes) {
        System.setProperty("matsim.preferLocalDtds", "true");
        this.reader = new LSPsPlanReader(lsPs, carriers, carrierVehicleTypes);
    }

    public void readFile(String filename) {
        try {
            this.reader.setValidating(false);
            this.reader.readFile(filename);
        } catch (Exception e) {
            log.warn("### Exception found while trying to read LSPPlan: Message: " + e.getMessage() + " ; cause: " + e.getCause() + " ; class " + e.getClass());
//            if (e.getCause().getMessage().contains("cvc-elt.1")) { // "Cannot find the declaration of element" -> exception comes most probably because no validation information was found
//                log.warn("read with validation = true failed. Try it again without validation... filename: " + filename);
//                reader.setValidating(false);
//                reader.readFile(filename);
//            } else { //other problem: e.g. validation does not work, because of missing validation file.
                throw  e;
//		}
        }
    }

    public void readURL(URL url) {
        try {
            this.reader.readURL(url);
        } catch (Exception var3) {
            Logger var10000 = log;
            String var10001 = var3.getMessage();
            var10000.warn("### Exception found while trying to read LSPPlan: Message: " + var10001 + " ; cause: " + var3.getCause() + " ; class " + var3.getClass());
            if (!var3.getCause().getMessage().contains("cvc-elt.1")) {
                throw var3;
            }

            log.warn("read with validation = true failed. Try it again without validation... url: " + url.toString());
            this.reader.setValidating(false);
            this.reader.readURL(url);
        }
    }

    public void readStream(InputStream inputStream) {
        try {
            this.reader.setValidating(false);
            this.reader.parse(inputStream);
        } catch (Exception var3) {
            Logger var10000 = log;
            String var10001 = var3.getMessage();
            var10000.warn("### Exception found while trying to read LSPPlan: Message: " + var10001 + " ; cause: " + var3.getCause() + " ; class " + var3.getClass());
            throw var3;
        }
    }

    private static final class LSPsPlanReader extends MatsimXmlParser {
        private final LSPs lsPs;
        private final Carriers carriers;
        private final CarrierVehicleTypes carrierVehicleTypes;
        private MatsimXmlParser delegate = null;

        LSPsPlanReader (LSPs lsPs, Carriers carriers, CarrierVehicleTypes carrierVehicleTypes) {
            this.lsPs = lsPs;
            this.carriers = carriers;
            this.carrierVehicleTypes = carrierVehicleTypes;
        }

        public void startTag(String name, Attributes attributes, Stack<String> context) {
            this.delegate = new LSPPlanXmlParser(this.lsPs, this.carriers, this.carrierVehicleTypes);
        }

        public void endTag(String name, String content, Stack<String> context) {
            this.delegate.endTag(name, content, context);
        }

        public void endDocument() {
            try {
                this.delegate.endDocument();
            } catch (SAXException var2) {
                throw new RuntimeException(var2);
            }
        }
    }
}
