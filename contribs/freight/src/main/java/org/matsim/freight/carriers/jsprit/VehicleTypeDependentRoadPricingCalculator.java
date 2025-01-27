/*
 *   *********************************************************************** *
 *   project: org.matsim.*
 *   *********************************************************************** *
 *                                                                           *
 *   copyright       : (C)  by the members listed in the COPYING,        *
 *                     LICENSE and WARRANTY file.                            *
 *   email           : info at matsim dot org                                *
 *                                                                           *
 *   *********************************************************************** *
 *                                                                           *
 *     This program is free software; you can redistribute it and/or modify  *
 *     it under the terms of the GNU General Public License as published by  *
 *     the Free Software Foundation; either version 2 of the License, or     *
 *     (at your option) any later version.                                   *
 *     See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                           *
 *   ***********************************************************************
 *
 */
package org.matsim.freight.carriers.jsprit;

import com.graphhopper.jsprit.core.problem.vehicle.VehicleType;
import java.util.*;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.roadpricing.RoadPricingScheme;
import org.matsim.contrib.roadpricing.RoadPricingSchemeImpl.Cost;

/**
 * Calculator that manages and calculates vehicle type dependent road pricing schemas.
 *
 * @author stefan schröder
 *
 */
@Deprecated // use RoadPricingScheme
public class VehicleTypeDependentRoadPricingCalculator {

	interface TollCalculator {
		double getTollAmount(Cost cost, Link link);
	}

	static class LinkCalc implements TollCalculator {


		@Override
		public double getTollAmount(Cost cost, Link link) {
			if(cost == null) return 0.0;
			return cost.amount;
		}

	}

	static class DistanceCalc implements TollCalculator{

		@Override
		public double getTollAmount(Cost cost, Link link) {
			if(cost == null) return 0.0;
			return cost.amount*link.getLength();
		}

	}


	private final Map<Id<org.matsim.vehicles.VehicleType>, Collection<RoadPricingScheme>> schemes = new HashMap<>();

	private final Map<String,TollCalculator> calculators = new HashMap<>();

	/**
	 * Gets an unmodifiable list of {@link RoadPricingScheme} for a {@link VehicleType} with input-id.
	 */
	public Collection<RoadPricingScheme> getPricingSchemes(Id<org.matsim.vehicles.VehicleType> vehicleType){
		Collection<RoadPricingScheme> collection = schemes.get(vehicleType);
		return Collections.unmodifiableCollection(Objects.requireNonNullElse(collection, Collections.emptyList()));
	}

	/**
	 * Gets the toll-amount for vehicleType on {@link Link} link at time 'time'.
	 *
	 * <p>In case of <code>RoadPricingScheme.TOLL_TYPE_CORDON</code> the toll amount on a link is equivalent to <code>rps.getLinkCostInfo(...).amount</code>.
	 *
	 * <p>In case of <code>RoadPricingScheme.TOLL_TYPE_DISTANCE</code> the toll amount on a link is equivalent to <code>rps.getLinkCostInfo(...).amount*link.getLength()</code>.
	 *
	 * @param vehicleType type of vehicle
	 * @param link the link
	 * @param time the time, measured in seconds from midnight
	 * @return the toll amount (positive for cost)
	 */
	public double getTollAmount(Id<org.matsim.vehicles.VehicleType> vehicleType, Link link, double time){
		Collection<RoadPricingScheme> pricingSchemes = getPricingSchemes(vehicleType);
		if(pricingSchemes == null) return 0.0;
		double toll = 0.0;
		for(RoadPricingScheme rps : pricingSchemes){
			Cost linkCostInfo = rps.getLinkCostInfo(link.getId(), time, null, null);
			toll += calculators.get(rps.getType()).getTollAmount(linkCostInfo,link);
		}
		return toll;
	}


	public void addPricingScheme(String vehicleTypeId, RoadPricingScheme pricingScheme){
		Id<org.matsim.vehicles.VehicleType> typeId = Id.create(vehicleTypeId, org.matsim.vehicles.VehicleType.class);
		addPricingScheme(typeId, pricingScheme);
	}

	/**
	 * Adds a {@link RoadPricingScheme} for vehicleTypeId.
	 *
	 * <p>Currently, only Cordon and Distance tolls can be calculated.
	 *
	 * <p>In case of <code>RoadPricingScheme.TOLL_TYPE_CORDON</code> the toll amount on a link is equivalent to <code>rps.getLinkCostInfo(...).amount</code>.
	 *
	 * <p>In case of <code>RoadPricingScheme.TOLL_TYPE_DISTANCE</code> the toll amount on a link is equivalent to <code>rps.getLinkCostInfo(...).amount*link.getLength()</code>.
	 * <p>
	 *
	 * @param vehicleTypeId vehicle type
	 * @param pricingScheme the {@link RoadPricingScheme} the road pricing scheme
	 */
	@SuppressWarnings("WeakerAccess")
	public void addPricingScheme(Id<org.matsim.vehicles.VehicleType> vehicleTypeId, RoadPricingScheme pricingScheme){
		Collection<RoadPricingScheme> list = schemes.computeIfAbsent(vehicleTypeId, k -> new ArrayList<>());
		list.add(pricingScheme);
		if(pricingScheme.getType().equals(RoadPricingScheme.TOLL_TYPE_CORDON)){
			throw new RuntimeException("the matsim cordon toll implementation no longer exists; link pricing is probably what you want");
		}
		else if(pricingScheme.getType().equals(RoadPricingScheme.TOLL_TYPE_LINK)){
			calculators.put(pricingScheme.getType(), new LinkCalc() );
		}
		else if(pricingScheme.getType().equals(RoadPricingScheme.TOLL_TYPE_DISTANCE)){
			calculators.put(pricingScheme.getType(), new DistanceCalc());
		}
        else throw new IllegalStateException("""
					toll type specification missing. if you use xml add specification to root node like this
					<roadpricing type="cordon" name="link"> if it is a link toll and\s
					<roadpricing type="distance" name="distance"> if it is a distance toll""");
	}

	/**
	 * Removes {@link RoadPricingScheme} for vehicleTypeId.
	 *
	 * @param vehicleTypeId vehicle type
	 */
	@SuppressWarnings("unused")
	public void removePricingSchemes(Id<org.matsim.vehicles.VehicleType> vehicleTypeId){
		schemes.remove(vehicleTypeId);
	}

	/**
	 * Checks whether there is a {@link RoadPricingScheme} for vehicleType 'typeId'.
	 *
	 * @param typeId vehicle type
	 * @return true if there is a pricing-scheme, false otherwise
	 */
	public boolean containsKey(Id<org.matsim.vehicles.VehicleType> typeId) {
		return schemes.containsKey(typeId);
	}

	/**
	 * Gets an unmodifiable map of pricing schemes.
	 */
	public Map<Id<org.matsim.vehicles.VehicleType>,Collection<RoadPricingScheme>> getSchemes(){
		return Collections.unmodifiableMap(schemes);
	}
}
