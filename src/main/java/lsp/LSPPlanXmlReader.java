package lsp;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.contrib.freight.carrier.*;
import org.matsim.core.api.internal.MatsimReader;
import org.matsim.core.utils.io.MatsimXmlParser;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import java.net.URL;
import java.util.Stack;

public final class LSPPlanXmlReader implements MatsimReader {
	private static final Logger log = LogManager.getLogger(LSPPlanXmlReader.class);
	private final LSPsPlanParser parser;


	public LSPPlanXmlReader( final LSPs lsPs, Carriers carriers) {
		System.setProperty("matsim.preferLocalDtds", "true");
		this.parser = new LSPsPlanParser(lsPs, carriers);
	}

	public void readFile(String filename) {
		try {
			this.parser.setValidating(true );
			this.parser.readFile(filename );
		} catch (Exception e) {
			log.warn("### Exception found while trying to read LSPPlan: Message: " + e.getMessage() + " ; cause: " + e.getCause() + " ; class " + e.getClass());
			throw e;
		}
	}

	public void readURL(URL url) {
		try {
			this.parser.readURL(url );
		} catch (Exception e) {
			log.warn("### Exception found while trying to read LSPPlan: Message: " + e.getMessage() + " ; cause: " + e.getCause() + " ; class " + e.getClass());
			if (e.getCause().getMessage().contains("cvc-elt.1")) { // "Cannot find the declaration of element" -> exception comes most probably because no validation information was found
				log.warn("read with validation = true failed. Try it again without validation... url: " + url.toString());
				parser.setValidating(false );
				parser.readURL(url );
			} else { //other problem: e.g. validation does not work, because of missing validation file.
				throw e;
			}
		}
	}


	private static final class LSPsPlanParser extends MatsimXmlParser {
		private final LSPs lsPs;
		private final Carriers carriers;

		private MatsimXmlParser delegate = null;

		LSPsPlanParser( LSPs lsPs, Carriers carriers ) {
			this.lsPs = lsPs;
			this.carriers = carriers;
		}

		public void startTag(String name, Attributes attributes, Stack<String> context) {
			if (LSPConstants.LSPS_DEFINITIONS.equalsIgnoreCase(name)) {
				String str = attributes.getValue("xsi:schemaLocation");
				log.info("Found following schemeLocation in lsPs definition file: " + str);
				if (str.contains( "lspsDefinitions_v1.0.xsd")) {
					delegate = new LSPPlanXmlParserV1(lsPs, carriers);
				} else {
					throw new RuntimeException("no reader found for " + str);
				}
			} else {
				this.delegate.startTag(name, attributes, context );
			}
		}

		public void endTag(String name, String content, Stack<String> context) {
			this.delegate.endTag(name, content, context );
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
