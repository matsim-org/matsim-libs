package org.matsim.mosaic;

import java.io.Serializable;

/**
 * Configuration class for MatsimMosaic ambassador. This class follows the naming scheme and conventions of mosaic.
 */
public final class CMATSimMosaic implements Serializable {

	/**
	 * Class name of the MATSim scenario to run. This scenario needs to be compatible with the matsim application contrib.
	 */
	public String scenario;

	/**
	 * Optional path to MATSim config for the scenario. If not given the default of the application will be used.
	 */
	public String config;

	/**
	 * Additional parameters passed to the MATSim scenario.
	 */
	public String additionalSumoParameters = "";

}

