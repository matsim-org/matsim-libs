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
import org.matsim.core.utils.misc.Counter;

import playground.southafrica.freight.digicore.containers.DigicoreActivity;
import playground.southafrica.freight.digicore.containers.DigicoreChain;
import playground.southafrica.freight.digicore.containers.DigicoreVehicle;
import playground.southafrica.freight.digicore.containers.DigicoreVehicles;

public class DigicoreVehiclesWriter extends MatsimXmlWriter implements MatsimWriter{
	private final Logger log = Logger.getLogger(DigicoreVehiclesWriter.class);
	private Counter counter = new Counter("  vehicle # ");
	private DigicoreVehicles vehicles;

		
	public DigicoreVehiclesWriter(DigicoreVehicles vehicles){
		super();
		this.vehicles = vehicles;
	}

	
	@Override
	public void write(final String filename){
		log.info("Writing Digicore vehicles to file: " + filename);
		writeV1(filename);
	}
	
	
	public void writeV1(final String filename){
		String dtd = "http://matsim.org/files/dtd/digicoreVehicles_v1.dtd";
		DigicoreVehiclesWriterHandler handler = new DigicoreVehiclesWriterHandlerImpl_v1();
		
		try {
			openFile(filename);
			writeXmlHead();
			writeDoctype("digicoreVehicles", dtd);
			
			handler.startVehicles(this.vehicles, this.writer);
			for(DigicoreVehicle vehicle : this.vehicles.getVehicles().values()){
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
				counter.incCounter();
			}
			counter.printCounter();
			handler.endVehicles(this.writer);
			this.writer.close();
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}
}

