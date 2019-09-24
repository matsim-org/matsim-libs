package org.matsim.contrib.freight.carrier;

import org.apache.log4j.Logger;
import org.matsim.core.api.internal.MatsimReader;
import org.matsim.core.utils.io.MatsimXmlParser;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import java.io.InputStream;
import java.net.URL;
import java.util.Stack;

/**
 * A reader that reads carriers and their plans.
 * 
 * @author sschroeder
 *
 */
public class CarrierPlanXmlReader implements MatsimReader {
	private static final Logger log = Logger.getLogger( CarrierPlanXmlReader.class );
	private static final String MSG="With early carrier plans file formats, there will be an expected exception in the following." ;

	private static final String CARRIERS = "carriers";

	private final CarriersPlanReader reader;

	public CarrierPlanXmlReader( final Carriers carriers ) {
		System.setProperty("matsim.preferLocalDtds", "true");       //can be removed later, once the carriersDefiniton_v2.0.xsd is online
		this.reader = new CarriersPlanReader( carriers ) ;
	}

	@Override
	public void readFile( String filename ){
		log.info(MSG) ;
		try {
			reader.readFile( filename );
		} catch (Exception e) {
			log.warn("### Exception found while trying to read CarrierPlan: Message: " + e.getMessage() + " ; cause: " + e.getCause() + " ; class " + e.getClass());
			if (e.getCause().getMessage().contains("cvc-elt.1")) { // "Cannot find the declaration of element" -> exception comes most probably because no validation information was found
					log.warn("read with validation = true failed. Try it again without validation... filename: " + filename);
					reader.setValidating(false);
					reader.readFile(filename);
			} else { //other problem: e.g. validation does not work, because of missing validation file.
			throw  e;}
		}
	}

	@Override
	public void readURL( URL url ){
		log.info(MSG) ;
		try {
			reader.readURL(url);
		}  catch (Exception e) {
			log.warn("### Exception found while trying to read CarrierPlan: Message: " + e.getMessage() + " ; cause: " + e.getCause() + " ; class " + e.getClass());
			if (e.getCause().getMessage().contains("cvc-elt.1")) { // "Cannot find the declaration of element" -> exception comes most probably because no validation information was found
				log.warn("read with validation = true failed. Try it again without validation... url: " + url.toString());
			reader.setValidating(false);
			reader.readURL(url);
			} else { //other problem: e.g. validation does not work, because of missing validation file.
				throw  e;
			}
		}
	}

	public void readStream( InputStream inputStream ){
		log.info(MSG) ;
		try {
			reader.parse( inputStream ) ;
		} catch (Exception e)
		{log.warn("### Exception found while trying to read CarrierPlan: Message: " + e.getMessage() + " ; cause: " + e.getCause() + " ; class " + e.getClass());
			if (e.getCause().getMessage().contains("cvc-elt.1")) { // "Cannot find the declaration of element" -> exception comes most probably because no validation information was found
				log.warn("read with validation = true failed. Try it again without validation... ");
			reader.setValidating(false);
			reader.parse(inputStream);
			} else { //other problem: e.g. validation does not work, because of missing validation file.
				throw  e;}
		}
	}

	private static final class CarriersPlanReader extends MatsimXmlParser {
		private final Carriers carriers;

		private MatsimXmlParser delegate = null;

		CarriersPlanReader(Carriers carriers) {
			this.carriers = carriers ;
//			this.setValidating(false);          //remove later, when having an idea, how to handle "Missing DOCTYPE" Error.
		}

		@Override
		public void startTag(final String name, final Attributes atts, final Stack<String> context) {
//			log.debug("Reading start tag. name: " + name + " , attributes: " + atts.toString() + " , context: " + context);
			if ( CARRIERS.equalsIgnoreCase( name ) ) {
				String str = atts.getValue( "xsi:schemaLocation" );
				log.info("Found following schemeLocation in carriers definition file: " + str);
				if (str == null){
					log.warn("No validation information found. Using ReaderV2 instead.");
					delegate = new CarrierPlanXmlParserV2( carriers ) ;
				} else if ( str.contains( "carriersDefinitons_v1.0.xsd" ) ){
					log.info("Found carriersDefinitons_v1.0.xsd. Using CarrierPlanReaderV1.");
					delegate = new CarrierPlanReaderV1(carriers);
				} else if ( str.contains( "carriersDefinitions_v2.0.xsd" ) ) { //This is the current one - but no validation file existing, kmt aug19
					log.warn("Found carriersDefinitions_v2.0.xsd. Using CarrierPlanReaderV2.");
					delegate = new CarrierPlanXmlParserV2( carriers );
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
