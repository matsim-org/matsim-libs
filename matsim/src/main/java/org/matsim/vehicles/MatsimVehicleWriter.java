/* *********************************************************************** *
 * project: org.matsim.*
 * VehicleDefinitionsWriterV1
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */
package org.matsim.vehicles;

import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.utils.io.MatsimXmlWriter;

/**
 * This is the new default Writer for the vehicles file.
 * It can easy pointed to the current version of VehicleReader (which is now V2)
 * @author kturner
 */
public final class MatsimVehicleWriter extends MatsimXmlWriter {

	private static final Logger log = LogManager.getLogger(MatsimVehicleWriter.class);

	private VehicleWriterV2 delegate;

	public MatsimVehicleWriter(Vehicles vehicles) {
		delegate = new VehicleWriterV2(vehicles);
	}

	/**
	 * Writes the vehicles in the current default format
	 * (currently vehicleDefinitions_v2.0.dtd).
	 */
	public void writeFile(String filename) {
		log.info( Gbl.aboutToWrite( "vehicles", filename) ) ;
		try{
			delegate.writeFile(filename);
		} catch( IOException e ){
			e.printStackTrace();
		}
	}

}
