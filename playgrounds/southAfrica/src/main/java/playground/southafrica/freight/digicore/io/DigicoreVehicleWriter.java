/* *********************************************************************** *
 * project: org.matsim.*
 * DigicoreVehicleWriter.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

package playground.southafrica.freight.digicore.io;

import java.io.IOException;

import org.apache.log4j.Logger;
import org.matsim.core.api.internal.MatsimWriter;
import org.matsim.core.utils.io.MatsimXmlWriter;
import org.matsim.core.utils.io.UncheckedIOException;

import playground.southafrica.freight.digicore.containers.DigicoreActivity;
import playground.southafrica.freight.digicore.containers.DigicoreChain;
import playground.southafrica.freight.digicore.containers.DigicoreVehicle;

public class DigicoreVehicleWriter extends MatsimXmlWriter implements MatsimWriter{
	private final static Logger LOG = Logger.getLogger(DigicoreVehicleWriter.class);

		
	public DigicoreVehicleWriter(){
		super();
	}

	
	@Override
	public void write(final String filename){
		LOG.error("Cannot write Digicore vehicle without the vehicle being passed.");
		LOG.error("Rather use the method write(filename, vehicle)");
		throw new IllegalArgumentException();
	}
	
	
	public void write(final String filename, DigicoreVehicle vehicle) {
//		LOG.info("Writing Digicore vehicle");
		writeV1(filename, vehicle);
//		LOG.info("Done.");
	}
	
	
	
	public void writeV1(final String filename, DigicoreVehicle vehicle){
		String dtd = "http://matsim.org/files/dtd/digicoreVehicle_v1.dtd";
		DigicoreVehicleWriterHandler handler = new DigicoreVehicleWriterHandlerImpl_v1();
		
		try {
			openFile(filename);
			writeXmlHead();
			writeDoctype("digicoreVehicle", dtd);
			
			handler.startVehicle(vehicle, this.writer);
			for(DigicoreChain chain : vehicle.getChains()){
				handler.startChain(this.writer);
				for(DigicoreActivity activity : chain){
					handler.startActivity(activity, this.writer);
					handler.endActivity(this.writer);
				}
				handler.endChain(this.writer);
			}
			handler.endVehicle(this.writer);
			this.writer.close();
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
		
	}

}

