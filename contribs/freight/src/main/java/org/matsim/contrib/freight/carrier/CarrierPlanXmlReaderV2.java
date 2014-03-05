package org.matsim.contrib.freight.carrier;

import org.apache.log4j.Logger;

/**
 * A reader that reads carriers and their plans.
 * 
 * @author sschroeder
 *
 */
public class CarrierPlanXmlReaderV2 {

	private static Logger logger = Logger.getLogger(CarrierPlanXmlReaderV2.class);
	
	private CarrierPlanXmlParserV2 delegate;

	/**
	 * Constructs a reader with an empty carriers-container for the carriers to be constructed. 
	 * 
	 * @param carriers which is a map that stores carriers
	 */
	public CarrierPlanXmlReaderV2(Carriers carriers) {
		this.delegate = new CarrierPlanXmlParserV2(carriers);
	}
	
	/**
	 * Reads a xml-file that contains carriers and their plans.
	 * 
	 * <p> Builds carriers and plans, and stores them in the carriers-object coming with this constructor.
	 * 
	 * @param filename
	 */
	/* This is somewhat problematic for me (JWJoubert, Nov '13). The MatsimXmlParser
	 * has a parse method, yet when calling it, it results in an XML error. Maybe 
	 * it would be better to 
	 * a) use a dtd file, and
	 * b) rather use the infrastructure provided by the MatsimXmlParser, and 
	 *    override it if required.
	 * 
	 * I've posted a similar comment for the CarrierVehicleTypeReader.
	 */
	public void read(String filename) {
		logger.info("read carrier plans");
		this.delegate.setValidating(false);
		this.delegate.parse(filename);
		logger.info("done");

	}

}
