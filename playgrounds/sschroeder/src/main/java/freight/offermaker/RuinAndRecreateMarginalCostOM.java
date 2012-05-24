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

package freight.offermaker;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.contrib.freight.carrier.Carrier;
import org.matsim.contrib.freight.carrier.CarrierContract;
import org.matsim.contrib.freight.carrier.CarrierCostFunction;
import org.matsim.contrib.freight.carrier.CarrierOffer;
import org.matsim.contrib.freight.carrier.CarrierPlan;
import org.matsim.contrib.freight.carrier.CarrierPlanBuilder;
import org.matsim.contrib.freight.carrier.CarrierShipment;
import org.matsim.contrib.freight.carrier.CarrierUtils;
import org.matsim.contrib.freight.carrier.CarrierVehicle;
import org.matsim.contrib.freight.vrp.basics.Locations;

import playground.mzilske.freight.OfferMaker;

public class RuinAndRecreateMarginalCostOM implements OfferMaker{

	private Carrier carrier;
	
	private CarrierVehicle carrierVehicle;

	private Locations locations;
	
	private CarrierCostFunction carrierCostCalculator;
	
	private CarrierPlanBuilder carrierPlanBuilder;
	
	private static Logger logger = Logger.getLogger(RuinAndRecreateMarginalCostOM.class);
	
	
	public void setCarrierPlanBuilder(CarrierPlanBuilder carrierPlanBuilder) {
		this.carrierPlanBuilder = carrierPlanBuilder;
	}


	public void setCarrierCostCalculator(CarrierCostFunction carrierCostCalculator) {
		this.carrierCostCalculator = carrierCostCalculator;
	}


	public RuinAndRecreateMarginalCostOM(Carrier carrier, Locations locations) {
		super();
		this.carrier = carrier;
		carrierVehicle = carrier.getCarrierCapabilities().getCarrierVehicles().iterator().next();
		this.locations = locations;
	}

	
	@Override
	public CarrierOffer requestOffer(Id linkId, Id linkId2, int shipmentSize, Double memorizedPrice) {
		return null;
	}

	@Override
	public CarrierOffer requestOffer(Id from, Id to, int size, Double startPickup, Double endPickup, Double startDelivery, Double endDelivery, Double memorizedPrice) {
		if(memorizedPrice != null){
//			if(MatsimRandom.getRandom().nextDouble() < 0.8){
				CarrierOffer offer = new CarrierOffer();
				offer.setId(carrier.getId());
				offer.setPrice(memorizedPrice);
				return offer;
//			}
		}

		CarrierShipment requestedShipment = CarrierUtils.createShipment(from, to, size, startPickup, endPickup, startDelivery, endDelivery);
		List<CarrierContract> carrierContracts = new ArrayList<CarrierContract>(carrier.getContracts());
		double scoreWithoutRequestedShipment = 0.0;
		double mcCost;

		if(!carrierContracts.isEmpty()){
			CarrierPlan plan = carrierPlanBuilder.buildPlan(carrier.getCarrierCapabilities(), carrierContracts);
			scoreWithoutRequestedShipment = plan.getScore();
		}

		carrierContracts.add(CarrierUtils.createContract(requestedShipment));
		CarrierPlan newPlan = carrierPlanBuilder.buildPlan(carrier.getCarrierCapabilities(), carrierContracts);
		double scoreWithRequestedShipment = newPlan.getScore();
		double marginalCostInMeters;
		marginalCostInMeters = scoreWithRequestedShipment - scoreWithoutRequestedShipment;
		if(marginalCostInMeters < 0){
			logger.warn("marginal costs lower than 0");
			marginalCostInMeters = 0;
		}
		mcCost = carrierCostCalculator.calculateCost(carrierVehicle, marginalCostInMeters, 0.0);
		
		CarrierOffer offer = new CarrierOffer();
		offer.setId(carrier.getId());
		offer.setPrice(mcCost);
		return offer;
	}

	@Override
	public void init() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void reset() {
		// TODO Auto-generated method stub
		
	}
}
