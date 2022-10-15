/*
 *  *********************************************************************** *
 *  * project: org.matsim.*
 *  * *********************************************************************** *
 *  *                                                                         *
 *  * copyright       : (C) 2022 by the members listed in the COPYING,        *
 *  *                   LICENSE and WARRANTY file.                            *
 *  * email           : info at matsim dot org                                *
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  *   This program is free software; you can redistribute it and/or modify  *
 *  *   it under the terms of the GNU General Public License as published by  *
 *  *   the Free Software Foundation; either version 2 of the License, or     *
 *  *   (at your option) any later version.                                   *
 *  *   See also COPYING, LICENSE and WARRANTY file                           *
 *  *                                                                         *
 *  * ***********************************************************************
 */

package example.lsp.simulationTrackers;

import lsp.LSPSimulationTracker;
import lsp.LSPUtils;
import lsp.LogisticsSolution;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.VehicleLeavesTrafficEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleLeavesTrafficEventHandler;
import org.matsim.contrib.freight.events.FreightServiceEndEvent;
import org.matsim.contrib.freight.events.FreightServiceStartEvent;
import org.matsim.contrib.freight.events.FreightTourStartEvent;
import org.matsim.contrib.freight.events.eventhandler.FreightServiceEndEventHandler;
import org.matsim.contrib.freight.events.eventhandler.FreightServiceStartEventHandler;
import org.matsim.contrib.freight.events.eventhandler.FreightTourStartEventHandler;
import org.matsim.core.controler.events.AfterMobsimEvent;
import org.matsim.core.controler.listener.AfterMobsimListener;
import org.matsim.core.events.handler.EventHandler;

import java.util.ArrayList;
import java.util.Collection;

/*package-private*/ class LinearCostTracker implements AfterMobsimListener, LSPSimulationTracker<LogisticsSolution>,
		LinkEnterEventHandler,
		VehicleLeavesTrafficEventHandler,
		FreightTourStartEventHandler,
		FreightServiceStartEventHandler,
		FreightServiceEndEventHandler,
		LinkLeaveEventHandler {

	private final Collection<EventHandler> eventHandlers;
	private final double shareOfFixedCosts;
	//	private final Collection<LSPInfo> infos;
	private double distanceCosts;
	private double timeCosts;
	private double loadingCosts;
	private double vehicleFixedCosts;
	private int totalNumberOfShipments;
	private int totalWeightOfShipments;
	private double fixedUnitCosts;
	private double linearUnitCosts;
	private LogisticsSolution logisticsSolution;

	public LinearCostTracker(double shareOfFixedCosts) {
		this.shareOfFixedCosts = shareOfFixedCosts;
//		CostInfo costInfo = new CostInfo();
//		infos = new ArrayList<>();
//		infos.add(costInfo);
		this.eventHandlers = new ArrayList<>();
	}


	public final Collection<EventHandler> getEventHandlers() {
		return eventHandlers;
	}

	@Override
	public void notifyAfterMobsim(AfterMobsimEvent event) {
		for (EventHandler handler : eventHandlers) {
			if (handler instanceof TourStartHandler) {
				TourStartHandler startHandler = (TourStartHandler) handler;
				this.vehicleFixedCosts = startHandler.getVehicleFixedCosts();
			}
			if (handler instanceof DistanceAndTimeHandler) {
				DistanceAndTimeHandler distanceHandler = (DistanceAndTimeHandler) handler;
				this.distanceCosts = distanceHandler.getDistanceCosts();
				this.timeCosts = distanceHandler.getTimeCosts();
			}
			if (handler instanceof CollectionServiceHandler) {
				CollectionServiceHandler collectionHandler = (CollectionServiceHandler) handler;
				totalNumberOfShipments = collectionHandler.getTotalNumberOfShipments();
				System.out.println(totalNumberOfShipments);
				totalWeightOfShipments = collectionHandler.getTotalWeightOfShipments();
				loadingCosts = collectionHandler.getTotalLoadingCosts();
			}
		}

		double totalCosts = distanceCosts + timeCosts + loadingCosts + vehicleFixedCosts;
		fixedUnitCosts = (totalCosts * shareOfFixedCosts) / totalNumberOfShipments;
		linearUnitCosts = (totalCosts * (1 - shareOfFixedCosts)) / totalWeightOfShipments;

//		CostInfo info = (CostInfo) infos.iterator().next();
//		for(LSPInfoFunctionValue value : info.getFunction().getValues()) {
//			if(value instanceof example.lsp.simulationTrackers.FixedCostFunctionValue) {
//				((example.lsp.simulationTrackers.FixedCostFunctionValue)value).setValue(fixedUnitCosts);
//			}
//			if(value instanceof example.lsp.simulationTrackers.LinearCostFunctionValue) {
//				((example.lsp.simulationTrackers.LinearCostFunctionValue)value).setValue(linearUnitCosts);
//			}
//		}
//		info.setFixedCost( fixedUnitCosts );
//		info.setVariableCost( linearUnitCosts );
		LSPUtils.setFixedCost(this.logisticsSolution, fixedUnitCosts);
		LSPUtils.setVariableCost(this.logisticsSolution, linearUnitCosts);


	}


	@Override
	public void reset(int iteration) {
		distanceCosts = 0;
		timeCosts = 0;
		loadingCosts = 0;
		vehicleFixedCosts = 0;
		totalNumberOfShipments = 0;
		totalWeightOfShipments = 0;
		fixedUnitCosts = 0;
		linearUnitCosts = 0;

	}


//	@Override public Attributes getAttributes(){
//		return attributes;
//	}
	@Override public void setEmbeddingContainer( LogisticsSolution pointer ){
		this.logisticsSolution = pointer;
	}
	@Override public void handleEvent( LinkEnterEvent event ){
		for( EventHandler eventHandler : this.eventHandlers ){
			if ( eventHandler instanceof LinkEnterEventHandler ) {
				((LinkEnterEventHandler) eventHandler).handleEvent( event );
			}
		}
	}
	@Override public void handleEvent( VehicleLeavesTrafficEvent event ){
		for( EventHandler eventHandler : this.eventHandlers ){
			if ( eventHandler instanceof VehicleLeavesTrafficEventHandler ) {
				((VehicleLeavesTrafficEventHandler) eventHandler).handleEvent( event );
			}
		}
	}
	@Override public void handleEvent( FreightTourStartEvent event ){
		for( EventHandler eventHandler : this.eventHandlers ){
			if ( eventHandler instanceof FreightTourStartEventHandler) {
				((FreightTourStartEventHandler) eventHandler).handleEvent( event );
			}
		}
	}
	@Override public void handleEvent( FreightServiceEndEvent event ){
		for( EventHandler eventHandler : this.eventHandlers ){
			if ( eventHandler instanceof FreightServiceEndEventHandler) {
				((FreightServiceEndEventHandler) eventHandler).handleEvent( event );
			}
		}
	}
	@Override public void handleEvent( FreightServiceStartEvent event ){
		for( EventHandler eventHandler : this.eventHandlers ){
			if ( eventHandler instanceof FreightServiceStartEventHandler) {
				((FreightServiceStartEventHandler) eventHandler).handleEvent( event );
			}
		}
	}
	@Override public void handleEvent( LinkLeaveEvent event ){
		for( EventHandler eventHandler : this.eventHandlers ){
			if ( eventHandler instanceof LinkLeaveEventHandler ) {
				((LinkLeaveEventHandler) eventHandler).handleEvent( event );
			}
		}
	}
//	@Override public LogisticsSolution getEmbeddingContainer(){
//		throw new RuntimeException( "not implemented" );
//	}
}
