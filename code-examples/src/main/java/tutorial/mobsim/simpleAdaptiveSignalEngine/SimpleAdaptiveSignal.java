/* *********************************************************************** *
 * project: kai
 * SimpleAdaptiveSignalEngine.java
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

package tutorial.mobsim.simpleAdaptiveSignalEngine;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import javax.inject.Inject;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.events.HasLinkId;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.VehicleEntersTrafficEvent;
import org.matsim.api.core.v01.events.VehicleLeavesTrafficEvent;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.events.handler.BasicEventHandler;
import org.matsim.core.mobsim.framework.events.MobsimBeforeSimStepEvent;
import org.matsim.core.mobsim.framework.events.MobsimInitializedEvent;
import org.matsim.core.mobsim.framework.listeners.MobsimBeforeSimStepListener;
import org.matsim.core.mobsim.framework.listeners.MobsimInitializedListener;
import org.matsim.core.mobsim.qsim.interfaces.Netsim;
import org.matsim.core.mobsim.qsim.interfaces.SignalGroupState;
import org.matsim.core.mobsim.qsim.interfaces.SignalizeableItem;
import org.matsim.core.utils.io.IOUtils;

/**
 * Example for a simple adaptive signal that reacts on agent behavior.
 * 
 * It does not need any signal input as basis.
 * It creates the signals as a MobsimInitializedListener before the simulation is started.
 * There are two conflicting streams, link 4 and link 5.
 * During the simulation the adaptive signal switches link 5 to green whenever vehicles want to leave it.
 * 
 * @author nagel
 * @author tthunig
 */
class SimpleAdaptiveSignal implements MobsimBeforeSimStepListener, MobsimInitializedListener, BasicEventHandler {

	@Inject Scenario scenario;
	
	private Queue<Double> vehicleExitTimesOnLink5 = new LinkedList<>() ;
	private long cnt4 = 0 ;
	private long cnt5 = 0 ;
	private OutputDirectoryHierarchy controlerIO;
	private Writer out ;
	
	private SignalizeableItem signalLink4;
	private SignalizeableItem signalLink5;
	
	class Result {
		int iteration ;
		double shareUp ;
		double shareDown ;
	} ;
	
	private List<Result> results = new ArrayList<>() ;
	
	@Inject
	public SimpleAdaptiveSignal(OutputDirectoryHierarchy controlerIO) {
		this.controlerIO = controlerIO ;
	}

	@Override
	public void notifyMobsimInitialized(MobsimInitializedEvent e) {
		Netsim mobsim = (Netsim) e.getQueueSimulation() ;
		signalLink4 = (SignalizeableItem) mobsim.getNetsimNetwork().getNetsimLink(Id.createLinkId("4")) ;
		signalLink5 = (SignalizeableItem) mobsim.getNetsimNetwork().getNetsimLink(Id.createLinkId("5")) ;
		signalLink4.setSignalized(true);
		signalLink5.setSignalized(true);
	}

	@Override
	public void notifyMobsimBeforeSimStep(MobsimBeforeSimStepEvent e) {
		double now = e.getSimulationTime();
		
		// switch the signal on link 5 to green when vehicles want to leave link 5 now
		final Double dpTime = this.vehicleExitTimesOnLink5.peek();
		if ( dpTime !=null && dpTime < now ) {
			signalLink4.setSignalStateAllTurningMoves(SignalGroupState.RED) ;
			signalLink5.setSignalStateAllTurningMoves(SignalGroupState.GREEN) ;
		} else {
			signalLink4.setSignalStateAllTurningMoves(SignalGroupState.GREEN) ;
			signalLink5.setSignalStateAllTurningMoves(SignalGroupState.RED) ;
		}

//		/* alternative approach: switch between the signals every other second
//		 * independent of link counts and vehicle behavior */
//		if ( now<20.*60 && (long)now%2==0 ) { 
//			link4.setSignalStateAllTurningMoves(SignalGroupState.RED) ;
//			link5.setSignalStateAllTurningMoves(SignalGroupState.GREEN);
//		} else {
//			link4.setSignalStateAllTurningMoves(SignalGroupState.GREEN) ;
//			link5.setSignalStateAllTurningMoves(SignalGroupState.RED);
//		}
	}

	@Override
	public void handleEvent(Event event) {
		switch (event.getEventType()){
		/* store the desired link exit time when a vehicle is entering a link
		 * and count the vehicles */
		case VehicleEntersTrafficEvent.EVENT_TYPE:
			// for the first link every vehicle needs one second without delay
			storeDesiredExitTimeAndCountVehiclesOnLink(((HasLinkId) event).getLinkId(), event.getTime() + 1.);
			break;
		case LinkEnterEvent.EVENT_TYPE:
			// calculate earliest link exit time
			Link link5 = scenario.getNetwork().getLinks().get(Id.createLinkId(5));
			double freespeedTt = link5.getLength() / link5.getFreespeed();
			// this is the earliest time where matsim sets the agent to the next link
			double matsimFreespeedTT = Math.floor(freespeedTt + 1);	
			storeDesiredExitTimeAndCountVehiclesOnLink(((HasLinkId) event).getLinkId(), event.getTime() + matsimFreespeedTT);
			break;
			
		// remove the desired exit time from the collection when the vehicle leaves the link
		case LinkLeaveEvent.EVENT_TYPE:
		case VehicleLeavesTrafficEvent.EVENT_TYPE:
			if ( ((HasLinkId) event).getLinkId().equals(Id.create("5", Link.class)) ) {
				this.vehicleExitTimesOnLink5.remove() ;
			}
			break;
		default:
			break;
		}
	}

	private void storeDesiredExitTimeAndCountVehiclesOnLink(Id<Link> linkId, double desiredExitTime) {
		if ( linkId.equals(Id.createLinkId("5")) ) {
			// remember the desired exit time (1 second after entering traffic)
			this.vehicleExitTimesOnLink5.add( desiredExitTime ) ;
			// every vehicle that enters the link increases the counter:
			cnt5++ ;
		} else if ( linkId.equals(Id.createLinkId("4")) ) {
			/* in this example we do not want to know the exit time of vehicles on link 4
			 * but we still count the vehicles on it */
			cnt4++ ;
		}
	}

	@Override
	public void reset(int iteration) {
		double sum = cnt4 + cnt5 ;
		Logger.getLogger(this.getClass()).warn("iteration: " + iteration + " cnt4: " + (cnt4/sum) + " cnt5: " + (cnt5/sum) ) ; 
		
		Result result = new Result() ;
		result.iteration = iteration ;
		result.shareUp = cnt5/sum ;
		result.shareDown = cnt4/sum ;
		results.add(result) ;
		
		cnt4 = 0 ;
		cnt5 = 0 ;

		if ( out==null ) {
			out = IOUtils.getBufferedWriter(controlerIO.getOutputFilename("split.txt")) ;
		}
		
		try {
			out.write( result.iteration + "\t" + result.shareUp + "\t" + result.shareDown + "\n" ) ;
			out.flush() ;
		} catch (IOException e) {
			e.printStackTrace();
		}		
	}

	static public void main( String[] args ) {
		RunSimpleAdaptiveSignalExample.main( args ) ;
	}
}
