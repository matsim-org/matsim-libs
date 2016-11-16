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
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.core.api.internal.MatsimWriter;
import org.matsim.core.utils.io.MatsimXmlWriter;
import org.matsim.core.utils.io.UncheckedIOException;

import playground.southafrica.freight.digicore.containers.DigicoreActivity;
import playground.southafrica.freight.digicore.containers.DigicoreChain;
import playground.southafrica.freight.digicore.containers.DigicoreChainElement;
import playground.southafrica.freight.digicore.containers.DigicorePosition;
import playground.southafrica.freight.digicore.containers.DigicoreTrace;
import playground.southafrica.freight.digicore.containers.DigicoreVehicle;

public class DigicoreVehicleWriter extends MatsimXmlWriter implements MatsimWriter{
	private final static Logger LOG = Logger.getLogger(DigicoreVehicleWriter.class);
	private DigicoreVehicle vehicle;

		
	public DigicoreVehicleWriter(DigicoreVehicle vehicle){
		super();
		this.vehicle = vehicle;
	}

	
	@Override
	public void write(final String filename){
		writeV2(filename);
	}
	
	
	public void writeV1(final String filename){
		String dtd = "http://matsim.org/files/dtd/digicoreVehicle_v1.dtd";
		DigicoreVehicleWriterHandler handler = new DigicoreVehicleWriterHandlerImpl_v1();
		
		try {
			openFile(filename);
			writeXmlHead();
			writeDoctype("digicoreVehicle", dtd);
			
			handler.startVehicle(vehicle, this.writer);
			for(DigicoreChain chain : vehicle.getChains()){
				handler.startChain(this.writer);
				List<DigicoreActivity> activities = chain.getAllActivities();
				for(DigicoreActivity activity : activities){
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

	public void writeV2(final String filename){
		String dtd = "http://matsim.org/files/dtd/digicoreVehicle_v2.dtd";
		DigicoreVehicleWriterHandler handler = new DigicoreVehicleWriterHandlerImpl_v2();
		 
		try {
			openFile(filename);
			writeXmlHead();
			writeDoctype("digicoreVehicle", dtd);
			
			handler.startVehicle(vehicle, this.writer);
			for(DigicoreChain chain : vehicle.getChains()){
				handler.startChain(this.writer);
				
				for(DigicoreChainElement element : chain){
					if(element instanceof DigicoreActivity){
						DigicoreActivity activity = (DigicoreActivity)element;
						handler.startActivity(activity, this.writer);
						handler.endActivity(this.writer);
					} else if(element instanceof DigicoreTrace){
						DigicoreTrace trace = (DigicoreTrace)element;
						handler.startTrace(trace, this.writer);
						for(DigicorePosition pos : trace){
							handler.startPosition(pos, this.writer);
							handler.endPosition(this.writer);
						}
						handler.endTrace(this.writer);
					} else{
						throw new RuntimeException("Unknown chain element type: " + element.getClass().toString());
					}
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

