/*
 *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       :  (C) 2024 by the members listed in the COPYING,       *
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
 * ***********************************************************************
 */

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

package org.matsim.freight.logistics.resourceImplementations;

import org.matsim.api.core.v01.Id;
import org.matsim.core.controler.events.AfterMobsimEvent;
import org.matsim.core.controler.listener.AfterMobsimListener;
import org.matsim.freight.carriers.CarrierService;
import org.matsim.freight.carriers.Tour;
import org.matsim.freight.carriers.Tour.ServiceActivity;
import org.matsim.freight.carriers.Tour.TourElement;
import org.matsim.freight.carriers.events.CarrierTourEndEvent;
import org.matsim.freight.carriers.events.eventhandler.CarrierTourEndEventHandler;
import org.matsim.freight.logistics.LSPCarrierResource;
import org.matsim.freight.logistics.LSPResource;
import org.matsim.freight.logistics.LSPSimulationTracker;
import org.matsim.freight.logistics.LogisticChainElement;
import org.matsim.freight.logistics.shipment.LSPShipment;
import org.matsim.freight.logistics.shipment.ShipmentLeg;
import org.matsim.freight.logistics.shipment.ShipmentPlanElement;
import org.matsim.freight.logistics.shipment.ShipmentUtils;

// ***** (Begin) Ex CollectionRun *** //
public class LSPTourEndEventHandler
		implements AfterMobsimListener, CarrierTourEndEventHandler, LSPSimulationTracker<LSPShipment> {
	// Todo: I have made it (temporarily) public because of junit tests :( -- need to find another way
	// to do the junit testing. kmt jun'23

	private final CarrierService carrierService;
	private final LogisticChainElement logisticChainElement;
	private final LSPCarrierResource resource;
	private final Tour tour;
	private LSPShipment lspShipment;

	public LSPTourEndEventHandler(
			LSPShipment lspShipment,
			CarrierService carrierService,
			LogisticChainElement logisticChainElement,
			LSPCarrierResource resource,
			Tour tour) {
		this.lspShipment = lspShipment;
		this.carrierService = carrierService;
		this.logisticChainElement = logisticChainElement;
		this.resource = resource;
		this.tour = tour;
	}

	@Override
	public void reset(int iteration) {
		// TODO Auto-generated method stub
	}

	@Override
	public void handleEvent(CarrierTourEndEvent event) {
		if (event.getTourId().equals(tour.getId())) {
      for (TourElement element : tour.getTourElements()) {
        if (element instanceof ServiceActivity serviceActivity) {
          if (serviceActivity.getService().getId() == carrierService.getId()
              && event.getCarrierId() == resource.getCarrier().getId()) {
            if (resource instanceof CollectionCarrierResource) {
              logTransport(event, tour); // ex Collection run
              logUnload(event, tour); // ex Collection run
            } else if (resource instanceof MainRunCarrierResource) {
              logUnload(event); //ex Main Run
              logTransport(event); // ex Main Run
            }
          }
        }
	  }
		}
	}

	// ex Collection run
	private void logUnload(CarrierTourEndEvent event, Tour tour) {
		ShipmentUtils.LoggedShipmentUnloadBuilder builder =
				ShipmentUtils.LoggedShipmentUnloadBuilder.newInstance();
		builder.setStartTime(event.getTime());
		builder.setEndTime(event.getTime() + getTotalUnloadingTime(tour));
		builder.setLogisticChainElement(logisticChainElement);
		builder.setResourceId(resource.getId());
		builder.setCarrierId(event.getCarrierId());
		ShipmentPlanElement unload = builder.build();
		String idString =
				unload.getResourceId()
						+ ""
						+ unload.getLogisticChainElement().getId()
						+ unload.getElementType();
		Id<ShipmentPlanElement> unloadId = Id.create(idString, ShipmentPlanElement.class);
		lspShipment.getShipmentLog().addPlanElement(unloadId, unload);
	}

	// ex Collection run
	private void logTransport(CarrierTourEndEvent event, Tour tour) {
		String idString = resource.getId() + "" + logisticChainElement.getId() + "TRANSPORT";
		Id<ShipmentPlanElement> id = Id.create(idString, ShipmentPlanElement.class);
		ShipmentPlanElement abstractPlanElement =
				lspShipment.getShipmentLog().getPlanElements().get(id);
		if (abstractPlanElement instanceof ShipmentLeg transport) {
			transport.setEndTime(event.getTime());
			transport.setToLinkId(tour.getEndLinkId());
		}
	}

	// Ex Main Run //
	private void logUnload(CarrierTourEndEvent event) {
		ShipmentUtils.LoggedShipmentUnloadBuilder builder =
				ShipmentUtils.LoggedShipmentUnloadBuilder.newInstance();
		builder.setStartTime(event.getTime() - getTotalUnloadingTime(tour));
		builder.setEndTime(event.getTime());
		builder.setLogisticChainElement(logisticChainElement);
		builder.setResourceId(resource.getId());
		builder.setCarrierId(event.getCarrierId());
		ShipmentPlanElement unload = builder.build();
		String idString =
				unload.getResourceId()
						+ ""
						+ unload.getLogisticChainElement().getId()
						+ unload.getElementType();
		Id<ShipmentPlanElement> unloadId = Id.create(idString, ShipmentPlanElement.class);
		lspShipment.getShipmentLog().addPlanElement(unloadId, unload);
	}

	// Ex Main Run //
	private void logTransport(CarrierTourEndEvent event) {
		String idString = resource.getId() + "" + logisticChainElement.getId() + "TRANSPORT";
		Id<ShipmentPlanElement> id = Id.create(idString, ShipmentPlanElement.class);
		ShipmentPlanElement abstractPlanElement =
				lspShipment.getShipmentLog().getPlanElements().get(id);
		if (abstractPlanElement instanceof ShipmentLeg transport) {
			transport.setEndTime(event.getTime() - getTotalUnloadingTime(tour));
			transport.setToLinkId(event.getLinkId());
		}
	}

	private double getTotalUnloadingTime(Tour tour) {
		double totalTime = 0;
		for (TourElement element : tour.getTourElements()) {
			if (element instanceof ServiceActivity serviceActivity) {
				totalTime = totalTime + serviceActivity.getDuration();
			}
		}
		return totalTime;
	}

	public CarrierService getCarrierService() {
		return carrierService;
	}

	public LSPShipment getLspShipment() {
		return lspShipment;
	}

	public LogisticChainElement getLogisticChainElement() {
		return logisticChainElement;
	}

	public Id<LSPResource> getResourceId() {
		return resource.getId();
	}

	@Override
	public void setEmbeddingContainer(LSPShipment pointer) {
		this.lspShipment = pointer;
	}

	@Override
	public void notifyAfterMobsim(AfterMobsimEvent event) {}


	// ***** (Begin) Ex MainRun *** //





}


