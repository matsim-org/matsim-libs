/* *********************************************************************** *
 * project: org.matsim.*
 * EstimatedGuidance
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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
package playground.vsptelematics.ha2;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.mobsim.framework.events.MobsimAfterSimStepEvent;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.vehicles.Vehicle;


/**
 * @author dgrether
 *
 */
public class EstimatedGuidance extends AbstractGuidance implements Guidance {

	
	private static final Logger log = Logger.getLogger(EstimatedGuidance.class);
	
	private Map<Id<Vehicle>, LinkEnterEvent> vehicleIdLinkEnterEventMap = new HashMap<>();
	private double vehOn1, vehOn2;

	private Link link5;
	private Link link4;

	private BufferedWriter writer;
	
	public EstimatedGuidance(Network network, String outfile) {
		super(network, outfile);
		this.reset(0);
		this.link4 = this.network.getLinks().get(id4);
		this.link5 = this.network.getLinks().get(id5);
		this.writer = IOUtils.getBufferedWriter(this.outputFilename);
		String header = "time[s] \t veh_route_1 \t veh_route_2 \t predicted_tt_1[s] \t predicted_tt_2[s] \t guidance[route]";
		try {
			writer.append(header);
			writer.newLine();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void notifyShutdown() {
		try {
			this.writer.flush();
			this.writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	
	@Override
	public Id getNextLink(double time) {
		return this.guidance;
	}

	@Override
	public void reset(int iteration) {
		this.vehicleIdLinkEnterEventMap.clear();
		this.vehOn1 = 0.0;
		this.vehOn2 = 0.0;
	}
	
	
	@Override
	public void handleEvent(LinkEnterEvent e) {
		if (e.getLinkId().equals(id2)) {
			this.vehicleIdLinkEnterEventMap.put(e.getVehicleId(), e);
			this.vehOn1++;
		}
		else if (e.getLinkId().equals(id3)){
			this.vehicleIdLinkEnterEventMap.put(e.getVehicleId(), e);
			this.vehOn2++;
		}
	}

	@Override
	public void handleEvent(LinkLeaveEvent e) {
		LinkEnterEvent enterEvent = null;
		if (e.getLinkId().equals(id4)){
			enterEvent = this.vehicleIdLinkEnterEventMap.get(e.getVehicleId());
			this.vehOn1--;
		}
		else if (e.getLinkId().equals(id5)){
			enterEvent = this.vehicleIdLinkEnterEventMap.get(e.getVehicleId());
			this.vehOn2--;
		}
	}

	@Override
	public void notifyMobsimAfterSimStep(MobsimAfterSimStepEvent e) {
		double time = e.getSimulationTime();
		double tt1, tt2 = 0.0;
		if (this.vehOn1 > 0.0) {
			tt1 = this.vehOn1 / (this.link4.getCapacity()/3600.0);
		}
		else {
			tt1 = this.ttFs1;
		}


		if (this.vehOn2 > 0.0){
			tt2 = this.vehOn2 / (this.link5.getCapacity()/3600.0);
		}
		else {
			tt2 = this.ttFs2;
		}

		String line = time + "\t" + this.vehOn1 + "\t" + this.vehOn2 + "\t" + tt1 + "\t" + tt2 + "\t";
//		log.error("tt1: "+tt1 + " tt2: " + tt2);
		if (tt1 <= tt2){
//			log.error("  guidance 1");
			this.guidance = id2;
			line = line.concat("1");
		}
		else {
//			log.error("  guidance 2");
			this.guidance = id3;
			line = line.concat("2");
		}
		try {
			this.writer.append(line);
			this.writer.newLine();
		} catch (IOException e1) {
			e1.printStackTrace();
		}

	}


}
