package org.matsim.contrib.freight.carrier;

import org.apache.log4j.Logger;
import org.matsim.core.api.internal.MatsimReader;

import java.net.URL;

/**
 * A reader that reads carriers and their plans.
 * 
 * @author sschroeder
 *
 */
public class CarrierPlanXmlReaderV3 implements MatsimReader {

	private static Logger logger = Logger.getLogger(CarrierPlanXmlReaderV3.class);

	private CarrierPlanXmlParserV3 delegate;

	/**
	 * Constructs a reader with an empty carriers-container for the carriers to be constructed.
	 *
	 * @param carriers which is a map that stores carriers
	 */
	public CarrierPlanXmlReaderV3(Carriers carriers) {
		this.delegate = new CarrierPlanXmlParserV3(carriers);
	}
	
	/**
	 * Reads a xml-file that contains carriers and their plans.
	 * 
	 * <p> Builds carriers and plans, and stores them in the carriers-object coming with this constructor.
	 * 
	 * @param filename
	 */
	@Override
	public void readFile(String filename) {
		logger.info("read carrier plans");
		this.delegate.setValidating(false);
		this.delegate.readFile(filename);
		logger.info("done");
	}

	@Override
	public void readURL( URL url  ) {
		this.delegate.setValidating( false );
		this.delegate.parse( url );
	}

}
