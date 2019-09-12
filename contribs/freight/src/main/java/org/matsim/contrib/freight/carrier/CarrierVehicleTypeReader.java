package org.matsim.contrib.freight.carrier;

import java.io.InputStream;
import java.net.URL;
import java.util.Map;
import java.util.Stack;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.api.internal.MatsimReader;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.utils.io.MatsimXmlParser;
import org.matsim.vehicles.MatsimVehicleReader;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;
import org.matsim.vehicles.Vehicles;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

/**
 * Reader reading carrierVehicleTypes from an xml-file.
 * 
 * @author sschroeder
 *
 */
public class CarrierVehicleTypeReader implements MatsimReader{
	private static final Logger log = Logger.getLogger(CarrierVehicleTypeReader.class);
	private static final String MSG="With early carrier vehicle type file formats, there will be an expected exception in the following." ;
	private final CarrierVehicleTypeParser reader;

	public CarrierVehicleTypeReader( final CarrierVehicleTypes types ) {
		System.setProperty("matsim.preferLocalDtds", "true");       //can be removed later, once the carriersDefiniton_v2.0.xsd is online
		this.reader = new CarrierVehicleTypeParser( types ) ;
//		log.setLevel( Level.DEBUG );
	}

	@Override
	public void readFile( String filename ){
		log.info(MSG) ;
		try {
			reader.setValidating(true) ;
			reader.readFile( filename );
		} catch (Exception e) {
			log.warn("### Exception: Message=" + e.getMessage() + " ; cause=" + e.getCause() + " ; class=" + e.getClass());
			if (e.getCause().getMessage().contains("cvc-elt.1.a")) { // "Cannot find the declaration of element" -> exception comes most probably because no validation information was found
				log.warn("read with validation = true failed. Try it again without validation. filename: " + filename);
				reader.setValidating(false);
				reader.readFile(filename);
			} else { //other problem: e.g. validation does not work, because of missing validation file.
				throw  e;
			}
		}
	}

	@Override
	public void readURL( URL url ){
		log.info(MSG) ;
		try {
			reader.setValidating(true) ;
			reader.readURL(url);
		}  catch (Exception e) {
			log.warn("### Exception: Message=" + e.getMessage() );
			log.warn("### Exception: Cause=" + e.getCause() );
			log.warn("### Exception: Class=" + e.getClass() );
			if (e.getCause().getMessage().contains("cvc-elt.1.a")) { // "Cannot find the declaration of element" -> exception comes most probably because no validation information was found
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
			reader.setValidating(true) ;
			reader.parse( inputStream ) ;
		} catch (Exception e)
		{log.warn("### Exception found while trying to read Carrier Vehicle Type: Message: " + e.getMessage() + " ; cause: " + e.getCause() + " ; class " + e.getClass());
			if (e.getCause().getMessage().contains("cvc-elt.1.a")) { // "Cannot find the declaration of element" -> exception comes most probably because no validation information was found
				log.warn("read with validation = true failed. Try it again without validation... ");
				reader.setValidating(false);
				reader.parse(inputStream);
			} else { //other problem: e.g. validation does not work, because of missing validation file.
				throw  e;
			}
		}
	}

	private static final class CarrierVehicleTypeParser extends MatsimXmlParser {

		private final CarrierVehicleTypes vehicleTypes;
		private Vehicles vehicles = null ;
		private MatsimXmlParser delegate = null;

		CarrierVehicleTypeParser(CarrierVehicleTypes vehicleTypes) {
			this.vehicleTypes = vehicleTypes ;
		}

		@Override
		public void startTag(final String name, final Attributes atts, final Stack<String> context) {
			log.debug("Reading start tag. name: " + name + " , attributes: " + atts.toString() + " , context: " + context);
			if ( "vehicleTypes".equalsIgnoreCase( name ) ) {
				String str = atts.getValue( "xsi:schemaLocation" );
				log.info("Found following schemeLocation in carriers definition file: " + str);
				if (str == null){
					log.warn( "No validation information found. Using ReaderV1." );
					delegate = new CarrierVehicleTypeReaderV1( vehicleTypes );
				} else{
					throw new RuntimeException( "should not happen" ) ;
				}
			} else if ( "vehicleDefinitions".equalsIgnoreCase( name ) ){
				String str = atts.getValue( "xsi:schemaLocation" );
				if ( str==null ){
					throw new RuntimeException( "should not happen" );
				} else {
					log.warn("Using central vehicle parser") ;
					vehicles = VehicleUtils.createVehiclesContainer();
					delegate = new MatsimVehicleReader.VehicleReader(vehicles) ;
					// only takes a vehicle container as argument :-(
				}

				// Note that if it is a v1 file, it starts with
				// <vehicleTypes>.  If it is a later file, it starts with <vehicleDefinitions>. kai, sep'19

			}
			this.delegate.startTag( name, atts, context );
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
			if ( vehicles != null ) {
				for( Map.Entry<Id<VehicleType>, VehicleType> entry : vehicles.getVehicleTypes().entrySet() ){
					vehicleTypes.getVehicleTypes().put( entry.getKey(), entry.getValue() ) ;
					// need to copy from vehicles container to provided vehicle types :-(
				}
			}
		}
	}

}
