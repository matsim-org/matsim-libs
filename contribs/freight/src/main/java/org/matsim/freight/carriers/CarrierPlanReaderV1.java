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

package org.matsim.freight.carriers;

import java.util.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.core.utils.io.MatsimXmlParser;
import org.matsim.core.utils.misc.Time;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.xml.sax.Attributes;

/**
 * A reader that reads carriers and their plans.
 *
 * @author sschroeder
 *
 */
class CarrierPlanReaderV1 extends MatsimXmlParser {

	private static final Logger logger = LogManager.getLogger(CarrierPlanReaderV1.class);
	private static final String CARRIERS = "carriers";
	private static final String CARRIER = "carrier";
	private static final String LINK_ID = "linkId";
	private static final String SHIPMENTS = "shipments";
	private static final String SHIPMENT = "shipment";
	private static final String ID = "id";
	private static final String FROM = "from";
	private static final String TO = "to";
	private static final String SIZE = "size";
	private static final String ACTIVITY = "act";
	private static final String TYPE = "type";
	private static final String SHIPMENT_ID = "shipmentId";
	private static final String VEHICLE = "vehicle";
	private static final String VEHICLES = "vehicles";
	private static final String VEHICLE_EARLIEST_START = "earliestStart";
	private static final String VEHICLE_LATEST_END = "latestEnd";

	private Carrier currentCarrier = null;
	private CarrierVehicle currentVehicle = null;
	private Tour.Builder currentTourBuilder = null;
	private Double currentStartTime = null;
	private Id<Link> previousActLoc = null;
	private String previousRouteContent;
	private Map<String, CarrierShipment> currentShipments = null;
	private Map<String, CarrierVehicle> vehicles = null;
	private Collection<ScheduledTour> scheduledTours = null;
	private Double currentScore;
	private boolean selected;
	private final Carriers carriers;
	private double currentLegTransTime;
	private double currentLegDepTime;
	private final CarrierVehicleTypes carrierVehicleTypes;

	/**
	 * Constructs a reader with an empty carriers-container for the carriers to be constructed.
	 *
	 * @param carriers which is a map that stores carriers
	 * @param carrierVehicleTypes which is a map that stores carrierVehicleTypes
	 */
	public CarrierPlanReaderV1( Carriers carriers, CarrierVehicleTypes carrierVehicleTypes ) {
		super(ValidationType.DTD_OR_XSD);
		this.carriers = carriers;
		this.carrierVehicleTypes = carrierVehicleTypes;
		this.setValidating(false);
	}

	@Override
	public void startTag(String name, Attributes attributes, Stack<String> context) {
		switch( name ){
			case CARRIER:{
				String id = attributes.getValue( ID );
				currentCarrier = CarriersUtils.createCarrier( Id.create( id, Carrier.class ) );
				break;
			}
			case SHIPMENTS:{
				currentShipments = new HashMap<>();
				break;
			}
			case SHIPMENT:{
				String id = attributes.getValue( ID );
				String from = attributes.getValue( FROM );
				String to = attributes.getValue( TO );
				int size = getInt( attributes.getValue( SIZE ) );
				String startPickup = attributes.getValue( "startPickup" );
				String endPickup = attributes.getValue( "endPickup" );
				String startDelivery = attributes.getValue( "startDelivery" );
				String endDelivery = attributes.getValue( "endDelivery" );
				String pickupServiceTime = attributes.getValue( "pickupServiceTime" );
				String deliveryServiceTime = attributes.getValue( "deliveryServiceTime" );
				CarrierShipment.Builder shipmentBuilder = CarrierShipment.Builder.newInstance( Id.create( id, CarrierShipment.class ),
					  Id.create( from, Link.class ), Id.create( to, Link.class ), size );
				if( startPickup == null ){
					shipmentBuilder.setPickupStartingTimeWindow( TimeWindow.newInstance( 0.0, Integer.MAX_VALUE ) ).setDeliveryStartingTimeWindow(
						  TimeWindow.newInstance( 0.0, Integer.MAX_VALUE ) );
				} else{
					shipmentBuilder.setPickupStartingTimeWindow( TimeWindow.newInstance( getDouble( startPickup ), getDouble( endPickup ) ) ).
						setDeliveryStartingTimeWindow(
																										   TimeWindow.newInstance(
																											     getDouble(
																													 startDelivery ),
																											     getDouble(
																													 endDelivery ) ) );
				}
				if( pickupServiceTime != null ) shipmentBuilder.setPickupDuration( getDouble( pickupServiceTime ) );
				if( deliveryServiceTime != null ) shipmentBuilder.setDeliveryDuration( getDouble( deliveryServiceTime ) );
				CarrierShipment shipment = shipmentBuilder.build();
				currentShipments.put( attributes.getValue( ID ), shipment );
				CarriersUtils.addShipment(currentCarrier, shipment);
//				currentCarrier.getShipments().put( shipment.getId(), shipment );
				break ;
			}
			case VEHICLES:
			{
				vehicles = new HashMap<>();
				break;
			}
			case VEHICLE:{
				String vId = attributes.getValue( ID );
				String linkId = attributes.getValue(LINK_ID);
				String startTime = attributes.getValue(VEHICLE_EARLIEST_START);
				String endTime = attributes.getValue(VEHICLE_LATEST_END);

				String typeId = attributes.getValue( "typeId" );
				if( typeId == null ){
					logger.warn( "no vehicle type. set type='default' -> defaultVehicleType (see CarrierVehicleTypeImpl)" );
					typeId = "default";
				}

				VehicleType vehicleType = carrierVehicleTypes.getVehicleTypes().get( Id.create( typeId, VehicleType.class ) );
				if ( vehicleType==null ) {
					throw new RuntimeException( "VehicleTypeId=" + typeId + " is missing" );
				}

				CarrierVehicle.Builder vehicleBuilder = CarrierVehicle.Builder.newInstance( Id.create( vId, Vehicle.class ),
					  Id.create( linkId, Link.class ), vehicleType );
				if( startTime != null ) vehicleBuilder.setEarliestStart( getDouble( startTime ) );
				if( endTime != null ) vehicleBuilder.setLatestEnd( getDouble( endTime ) );
				CarrierVehicle vehicle = vehicleBuilder.build();
				CarriersUtils.addCarrierVehicle(currentCarrier, vehicle);
				vehicles.put( vId, vehicle );
				break;
			}
			case "plan":
			{
				String score = attributes.getValue("score");
				if(score != null){
					currentScore = getDouble(score);
				}
				else{
					currentScore = null;
				}
				String selected = attributes.getValue("selected");
				if(selected == null ) {
					this.selected = false;
				}
				else this.selected = selected.equals("true");
				scheduledTours = new ArrayList<>();
				break ;
			}
			case "tour":
			{
				String vehicleId = attributes.getValue("vehicleId");
				currentVehicle = vehicles.get(vehicleId);
				currentTourBuilder = Tour.Builder.newInstance(Id.create("unknown", Tour.class));
				break ;
			}
			case "leg":
			{
				currentLegDepTime = getDouble(attributes.getValue("dep_time"));
				currentLegTransTime = getDouble(attributes.getValue("transp_time"));
				break ;
			}
			case ACTIVITY:
			{
				switch (attributes.getValue(TYPE)) {
					case "start" -> {
						currentStartTime = getDouble(attributes.getValue("end_time"));
						previousActLoc = currentVehicle.getLinkId();
						currentTourBuilder.scheduleStart(currentVehicle.getLinkId(),
								TimeWindow.newInstance(currentVehicle.getEarliestStartTime(), currentVehicle.getLatestEndTime()));
					}
					case "pickup" -> {
						String id = attributes.getValue(SHIPMENT_ID);
						CarrierShipment s = currentShipments.get(id);
						finishLeg(s.getPickupLinkId());
						currentTourBuilder.schedulePickup(s);
						previousActLoc = s.getPickupLinkId();
					}
					case "delivery" -> {
						String id = attributes.getValue(SHIPMENT_ID);
						CarrierShipment s = currentShipments.get(id);
						finishLeg(s.getDeliveryLinkId());
						currentTourBuilder.scheduleDelivery(s);
						previousActLoc = s.getDeliveryLinkId();
					}
					case "end" -> {
						finishLeg(currentVehicle.getLinkId());
						currentTourBuilder.scheduleEnd(currentVehicle.getLinkId(),
								TimeWindow.newInstance(currentVehicle.getEarliestStartTime(), currentVehicle.getLatestEndTime()));
					}
				}
				break ;
			}
			case CARRIERS:
			case "route":
				// do nothing
				break ;
			default:
				throw new RuntimeException( "encountered xml tag=" + name + " which cannot be processed; aborting ..." ) ;
				// in particular for <attributes>, which someone should implement. kai, may'19
		}
	}

	private void finishLeg(Id<Link> toLocation) {
		NetworkRoute route = null;
		if (previousRouteContent != null) {
			List<Id<Link>> linkIds = NetworkUtils.getLinkIds(previousRouteContent);
			route = RouteUtils.createLinkNetworkRouteImpl(previousActLoc, toLocation);
			if (!linkIds.isEmpty()) {
				route.setLinkIds(previousActLoc, linkIds, toLocation);
			}
		}
		currentTourBuilder.addLeg(currentTourBuilder.createLeg(route, currentLegDepTime, currentLegTransTime));
		previousRouteContent = null;
	}

	private double getDouble(String timeString) {
		if (timeString.contains(":")) {
			return Time.parseTime(timeString);
		} else {
			return Double.parseDouble(timeString);
		}

	}

	@Override
	public void endTag(String name, String content, Stack<String> context) {
		if (name.equals("route")) {
			this.previousRouteContent = content;
		}
		if (name.equals("carrier")) {
			carriers.getCarriers().put(currentCarrier.getId(), currentCarrier);
		}
		if (name.equals("plan")) {
			CarrierPlan currentPlan = new CarrierPlan( currentCarrier, scheduledTours );
			currentPlan.setScore(currentScore );
			currentCarrier.getPlans().add( currentPlan );
			if(this.selected){
				currentCarrier.setSelectedPlan( currentPlan );
			}
		}
		if (name.equals("tour")) {
			ScheduledTour sTour = ScheduledTour.newInstance(currentTourBuilder.build(), currentVehicle, currentStartTime);
			scheduledTours.add(sTour);
		}
	}

	private int getInt(String value) {
		return Integer.parseInt(value);
	}

}
