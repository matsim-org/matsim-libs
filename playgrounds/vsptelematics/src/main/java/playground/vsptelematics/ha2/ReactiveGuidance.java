/* *********************************************************************** *
 * project: org.matsim.*
 * ReactiveGuidance
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.VehicleEntersTrafficEvent;
import org.matsim.api.core.v01.events.VehicleLeavesTrafficEvent;
import org.matsim.api.core.v01.events.handler.VehicleEntersTrafficEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleLeavesTrafficEventHandler;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.events.algorithms.Vehicle2DriverEventHandler;
import org.matsim.core.mobsim.framework.events.MobsimAfterSimStepEvent;
import org.matsim.core.utils.io.IOUtils;

/**
 * @author dgrether
 * 
 */
public class ReactiveGuidance extends AbstractGuidance implements Guidance, VehicleEntersTrafficEventHandler, VehicleLeavesTrafficEventHandler {

	private static final Logger log = Logger.getLogger(ReactiveGuidance.class);

	private double[] ttRoute1;
	private double[] ttRoute2;
	private double vehOn1, vehOn2;

	private Map<Id, LinkEnterEvent> personIdLinkEnterEventMap = new HashMap<Id, LinkEnterEvent>();

	private List<Double> currentTravelTimesRoute1 = new ArrayList<Double>();
	private List<Double> currentTravelTimesRoute2 = new ArrayList<Double>();

	private BufferedWriter writer;
	
	Vehicle2DriverEventHandler vehicle2driver = new Vehicle2DriverEventHandler();

	public ReactiveGuidance(Network network, String outfile) {
		super(network, outfile);
		this.reset(0);
		for (int i = 0; i < this.ttRoute1.length; i++) {
			this.ttRoute1[i] = ttFs1;
			this.ttRoute2[i] = ttFs2;
		}
		this.writer = IOUtils.getBufferedWriter(this.outputFilename);
		String header = "time[s] \t veh_route_1 \t veh_route_2 \t avg_tt_1[s] \t avg_tt_2[s] \t guidance[route]";
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
	public void reset(int iteration){
		this.personIdLinkEnterEventMap.clear();
		ttRoute1 = new double[30]; 
		ttRoute2 = new double[30];
		this.currentTravelTimesRoute1.clear();
		this.currentTravelTimesRoute2.clear();
		this.vehOn1 = 0.0;
		this.vehOn2 = 0.0;
	}
	
	@Override
	public void handleEvent(LinkEnterEvent e) {
		if (e.getLinkId().equals(id2)) { 
			this.personIdLinkEnterEventMap.put(vehicle2driver.getDriverOfVehicle(e.getVehicleId()), e);
			this.vehOn1++;
		}
		else if (e.getLinkId().equals(id3)){
			this.personIdLinkEnterEventMap.put(vehicle2driver.getDriverOfVehicle(e.getVehicleId()), e);
			this.vehOn2++;
		}
	}
	
	@Override
	public void handleEvent(LinkLeaveEvent e) {
		LinkEnterEvent enterEvent = null;
		if (e.getLinkId().equals(id4)){
			enterEvent = this.personIdLinkEnterEventMap.remove(vehicle2driver.getDriverOfVehicle(e.getVehicleId()));
			this.currentTravelTimesRoute1.add(e.getTime() - enterEvent.getTime());
			this.vehOn1--;
		}
		else if (e.getLinkId().equals(id5)){
			enterEvent = this.personIdLinkEnterEventMap.remove(vehicle2driver.getDriverOfVehicle(e.getVehicleId()));
			this.currentTravelTimesRoute2.add(e.getTime() - enterEvent.getTime());
			this.vehOn2--;
		}
	}

	@Override
	public Id getNextLink(double time) {
		return this.guidance;
	}

	@Override
	public void notifyMobsimAfterSimStep(MobsimAfterSimStepEvent e) {
		double time = e.getSimulationTime();
		int slot = (int) time % 30;
		// Route 1
		if (!this.currentTravelTimesRoute1.isEmpty()) {
			double tt = 0.0;
			for (Double t : this.currentTravelTimesRoute1)
				tt += t;
			tt = tt / this.currentTravelTimesRoute1.size();
			ttRoute1[slot] = tt;
		}
		else {
			ttRoute1[slot] = ttFs1;
		}
		double avgTT1 = this.getAvgTt(ttRoute1);

		// Route 2
		if (!this.currentTravelTimesRoute2.isEmpty()) {
			double tt = 0.0;
			for (Double t : this.currentTravelTimesRoute2)
				tt += t;
			tt = tt / this.currentTravelTimesRoute2.size();
			ttRoute2[slot] = tt;
		}
		else {
			ttRoute2[slot] = ttFs2;
		}
		double avgTT2 = this.getAvgTt(ttRoute2);
		
		//calc guidance
//		log.error("current tt 1 : " + ttRoute1[slot] + " current tt 2 : " + ttRoute2[slot] + " at "
//				+ Time.writeTime(time));
//		log.error("avg tt 1 : " + avgTT1 + " avg tt 2 : " + avgTT2 + " at " + Time.writeTime(time));
		String line = time + "\t" + this.vehOn1 + "\t" + this.vehOn2 + "\t" + avgTT1 + "\t" + avgTT2 + "\t";
		if (avgTT1 <= avgTT2) {
			this.guidance = id2;
//			log.error("  guidance 1 ");
			line = line.concat("1");
		}
		else {
			this.guidance = id3;
//			log.error("  guidance 2 ");
			line = line.concat("2");
		}
		try {
			this.writer.append(line);
			this.writer.newLine();
		} catch (IOException e1) {
			e1.printStackTrace();
		}
	}

	private double getAvgTt(double[] tts) {
		double ttavg = 0.0;
		for (double t : tts) {
			ttavg += t;
		}
		return ttavg / tts.length;
	}


	@Override
	public void handleEvent(VehicleEntersTrafficEvent event) {
		vehicle2driver.handleEvent(event);
	}


	@Override
	public void handleEvent(VehicleLeavesTrafficEvent event) {
		vehicle2driver.handleEvent(event);
	}

}
