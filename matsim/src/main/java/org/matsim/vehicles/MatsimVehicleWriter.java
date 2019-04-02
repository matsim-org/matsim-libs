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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.io.MatsimXmlWriter;
import org.matsim.core.utils.io.UncheckedIOException;

/**
 * This is the new default Writer for the vehicles file.
 * It can easy pointed to the current version of VehicleReader (which is now V2)
 * @author kturner
 */
public class MatsimVehicleWriter extends MatsimXmlWriter {
  
	private static final Logger log = Logger.getLogger(MatsimVehicleWriter.class);

	private VehicleWriterV1 delegate;

	public MatsimVehicleWriter(Vehicles vehicles) {
		delegate = new VehicleWriterV1(vehicles);
	}

	/**
	 * Writes the vehicles in the current default format
	 * (currently vehicleDefinitions_v1.dtd).
	 */
	public void writeFile(String filename) throws UncheckedIOException {
		log.info( Gbl.aboutToWrite( "vehicles", filename) ) ;
		delegate.writeFile(filename);
	}


//	public void putAttributeConverters(Map<Class<?>, AttributeConverter<?>> converters) {
//		this.converters.putAll(converters);
//	}
//
//	public final void writeV1(final String filename) {
//		new FacilitiesWriterV1(coordinateTransformation, facilities).write(filename);
//	}


}
