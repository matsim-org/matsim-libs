package org.matsim.contrib.freight.carrier;

import org.apache.log4j.Logger;
import org.matsim.core.api.internal.MatsimReader;
import org.matsim.core.utils.io.MatsimXmlParser;
import org.matsim.utils.objectattributes.AttributeConverter;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

/**
 * A reader that reads carriers and their plans.
 * 
 * @author sschroeder
 *
 */
public class CarrierPlanXmlReader implements MatsimReader {

	private static Logger log = Logger.getLogger(CarrierPlanXmlReader.class);

	private static final String CARRIERS = "carriers";

	private final CarriersPlanReader reader;

	public CarrierPlanXmlReader( final Carriers carriers ) {
		this.reader = new CarriersPlanReader( carriers ) ;
	}

	@Override
	public void readFile( String filename ){
		reader.readFile( filename );
	}

	@Override
	public void readURL( URL url ){
		reader.readURL( url );
	}

	public void readStream( InputStream inputStream ){
		reader.parse( inputStream ) ;
	}

	private static final class CarriersPlanReader extends MatsimXmlParser {
		private final Carriers carriers;

		private MatsimXmlParser delegate = null;

		private Map<Class<?>, AttributeConverter<?>> converters = new HashMap<>();

		CarriersPlanReader(Carriers carriers) {
			this.carriers = carriers ;
			this.setValidating(false);          //remove later, when having an idea, how to handle "Missing DOCTYPE" Error.
		}

		@Override
		public void startTag(final String name, final Attributes atts, final Stack<String> context) {
			if ( CARRIERS.equalsIgnoreCase( name ) ) {
				String str = atts.getValue( "xsi:schemaLocation" );
				log.info("Found following schemeLocation in carriers definition file: " + str);
				if (str == null){
					log.warn("No validation information found. Using ReaderV2 instead.");
					delegate = new CarrierPlanXmlParserV2( carriers ) ;
				} else if ( str.contains( "carriersDefinitons_v1.0.xsd" ) ){
					delegate = new CarrierPlanReaderV1(carriers);
				} else if ( str.contains( "carriersDefinitons_v2.0.xsd" ) ) { //This is the current one - but no validation file existing, kmt aug19
					delegate = new CarrierPlanXmlParserV2( carriers ) ;
//				} else if ( str.contains( "carriersDefinitons_v3.0.xsd" ) ) { //Not available yet
//					delegate = new CarrierPlanXmlParserV3( carriers ) ;
				} else {
					throw new RuntimeException("no reader found for " + str ) ;
				}
			} else{
				this.delegate.startTag( name, atts, context );
			}
		}

		@Override
		public void endTag(final String name, final String content, final Stack<String> context) {
			this.delegate.endTag(name, content, context);
		}

		@Override
		public void endDocument() {
			try {
				this.delegate.endDocument();
			} catch ( SAXException e) {
				throw new RuntimeException(e);
			}
		}
	}
}
