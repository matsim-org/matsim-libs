/* *********************************************************************** *
 * project: org.matsim.*
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
package playground.droeder.laemmer;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.LinkEnterEvent;
import org.matsim.core.api.experimental.events.LinkLeaveEvent;
import org.matsim.core.api.experimental.events.handler.LinkEnterEventHandler;
import org.matsim.core.api.experimental.events.handler.LinkLeaveEventHandler;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.mobsim.framework.events.MobsimAfterSimStepEvent;
import org.matsim.core.mobsim.framework.listeners.MobsimAfterSimStepListener;
import org.matsim.core.utils.io.IOUtils;

import ucar.ma2.ForbiddenConversionException;

/**
 * @author droeder
 *
 */
public class QueueLengthHandler implements LinkEnterEventHandler, LinkLeaveEventHandler,
										//PersonEntersVehicleEventHandler, PersonLeavesVehicleEventHandler,
											MobsimAfterSimStepListener, IterationStartsListener, IterationEndsListener,
											StartupListener{
	

	private SortedMap<Id, LinkInfo> linkInfo;
	private BufferedWriter queueWriter;
	private BufferedWriter vehicleOnLinkWriter;

	public QueueLengthHandler(Network net){
		this.linkInfo = new TreeMap<Id, LinkInfo>();
		for(Link l: net.getLinks().values()){
			this.linkInfo.put(l.getId(), new LinkInfo(l));
		}
	}
	
	
	@Override
	public void reset(int iteration) {
		for(LinkInfo l: this.linkInfo.values()){
			l.reset();
		}
	}

	@Override
	public void handleEvent(LinkLeaveEvent event) {
		this.linkInfo.get(event.getLinkId()).unregisterVehicle(event.getVehicleId());
	}

	@Override
	public void handleEvent(LinkEnterEvent event) {
		this.linkInfo.get(event.getLinkId()).registerLinkEnter(event.getVehicleId(), event.getTime());
	}

//	@Override
//	public void handleEvent(PersonEntersVehicleEvent event) {
//		this.linkInfo.get(event.getLinkId()).unregisterVehicle(event.getVehicleId());		
//	}
//	
//	@Override
//	public void handleEvent(PersonLeavesVehicleEvent event) {
//		
//	}
	
	@Override
	public void notifyMobsimAfterSimStep(MobsimAfterSimStepEvent event) {
		System.out.println("test");
		try {
			this.queueWriter.write(event.getSimulationTime() + ";");
			this.vehicleOnLinkWriter.write(event.getSimulationTime() + ";");
			for(LinkInfo l: this.linkInfo.values()){
				this.queueWriter.write(l.getNrVehInQueue(event.getSimulationTime()) + ";");
				this.vehicleOnLinkWriter.write(l.getNrVehOnLink() + ";");
			}
			this.queueWriter.write("\n");
			this.vehicleOnLinkWriter.write("\n");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void notifyIterationEnds(IterationEndsEvent event) {
		try {
			this.queueWriter.flush();
			this.queueWriter.close();
			this.vehicleOnLinkWriter.flush();
			this.vehicleOnLinkWriter.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void notifyIterationStarts(IterationStartsEvent event) {
		this.queueWriter = IOUtils.getBufferedWriter(
				event.getControler().getControlerIO().getIterationFilename(event.getIteration(), "queueLength.txt"));
		this.vehicleOnLinkWriter = IOUtils.getBufferedWriter(
				event.getControler().getControlerIO().getIterationFilename(event.getIteration(), "vehiclesOnLinks.txt"));
		try {
			this.queueWriter.write("time;");
			this.vehicleOnLinkWriter.write("time;");
			for(Id id: this.linkInfo.keySet()){
				this.queueWriter.write(id.toString() + ";");
				this.vehicleOnLinkWriter.write(id.toString() + ";");
			}
			this.queueWriter.write("\n");
			this.vehicleOnLinkWriter.write("\n");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private class LinkInfo{
		
		private double time2pass;
		private Map<Id, CarInfo> carsOnLink;
		
		public LinkInfo(Link l){
			this.time2pass = l.getLength()/l.getFreespeed();
			this.carsOnLink = new HashMap<Id, CarInfo>();
		}
		
		public void registerLinkEnter(Id id, double time){
			this.carsOnLink.put(id, new CarInfo(id, time + this.time2pass));
		}
		
//		public void registerPersonEntersVehicle(Id id, double time){
//			this.carsOnLink.put(id, new CarInfo(id, time));
//		}
		
		public void unregisterVehicle(Id id){
			this.carsOnLink.remove(id);
		}
	
		public double getNrVehOnLink(){
			return this.carsOnLink.size();
		}
		
		public double getNrVehInQueue(double time){
			Double cnt = 0.;
			for(CarInfo c: this.carsOnLink.values()){
				if(c.queueing(time)){
					cnt++;
				}
			}
			return cnt;
		}
		
		public void reset(){
			this.carsOnLink = new HashMap<Id, CarInfo>();
		}
	}
	
	private class CarInfo{
		
		private double timeAtLinkEnd;
		private Id id;

		public CarInfo(Id id, double timeAtLinkEnd){
			this.id = id;
			this.timeAtLinkEnd = timeAtLinkEnd;
		}
		
		public boolean queueing(double time){
			if(this.timeAtLinkEnd > time){
				return false;
			}else{
				return true;
			}
		}
	}

	@Override
	public void notifyStartup(StartupEvent event) {
		event.getControler().getEvents().addHandler(this);
	}
}
