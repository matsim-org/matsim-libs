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

import static org.matsim.freight.carriers.CarrierConstants.*;

import com.google.inject.Inject;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.io.MatsimXmlWriter;
import org.matsim.core.utils.misc.Time;
import org.matsim.utils.objectattributes.AttributeConverter;
import org.matsim.utils.objectattributes.attributable.AttributesXmlWriterDelegate;
import org.matsim.vehicles.VehicleType;

/**
 * A writer that writes carriers and their plans in a xml-file.
 *
 * @author sschroeder
 *
 */
/*package-private*/ class CarrierPlanXmlWriterV2_1 extends MatsimXmlWriter {

	@SuppressWarnings("unused")
	private static final  Logger logger = LogManager.getLogger(CarrierPlanXmlWriterV2_1.class);

	private final Collection<Carrier> carriers;

	private final AttributesXmlWriterDelegate attributesWriter = new AttributesXmlWriterDelegate();


	/**
	 * Constructs the writer with the carriers to be written.
	 *
	 * @param carriers to be written
	 */
	public CarrierPlanXmlWriterV2_1(Carriers carriers) {
		super();
		this.carriers = carriers.getCarriers().values();
	}

	@Inject
	public void putAttributeConverters( final Map<Class<?>, AttributeConverter<?>> converters ) {
		attributesWriter.putAttributeConverters( converters );
	}

	/**
	 * Writes carriers and their plans into a xml-file.
	 *
	 * @param filename should be the target xml-file
	 */
	public void write(String filename) {
		logger.info("write carrier plans");
		try {
			openFile(filename);
			writeXmlHead();
			writeRootElement();
			for (Carrier carrier : carriers) {
				startCarrier(carrier, this.writer);
				writeVehiclesAndTheirTypes(carrier);
				writeServices(carrier,this.writer);
				writeShipments(carrier, this.writer);
				writePlans(carrier, this.writer);
				endCarrier();
			}
			writeEndElement();
			close();
			logger.info("done");
		} catch (IOException e) {
			e.printStackTrace();
			logger.error(e);
			System.exit(1);
		}
	}

	private void writeRootElement() throws UncheckedIOException {
		List<Tuple<String, String>> atts = new ArrayList<>();
		atts.add(createTuple(XMLNS, MatsimXmlWriter.MATSIM_NAMESPACE));
		atts.add(createTuple(XMLNS + ":xsi", DEFAULTSCHEMANAMESPACELOCATION));
		atts.add(createTuple("xsi:schemaLocation", MATSIM_NAMESPACE + " " + DEFAULT_DTD_LOCATION + "carriersDefinitions_v2.1.xsd"));
		this.writeStartTag(CARRIERS, atts);
	}

	private void startCarrier(Carrier carrier, BufferedWriter writer) {
		this.writeStartTag(CARRIER, List.of(
				createTuple(ID, carrier.getId().toString())), false, true
		);
		attributesWriter.writeAttributes("\t\t", writer, carrier.getAttributes(), false);
	}

	private void writeVehiclesAndTheirTypes(Carrier carrier) {
		this.writeStartTag(CAPABILITIES, List.of(
				createTuple(FLEET_SIZE, carrier.getCarrierCapabilities().getFleetSize().toString())
		));
		//vehicles
		this.writeStartTag(VEHICLES, null);
		for (CarrierVehicle v : carrier.getCarrierCapabilities().getCarrierVehicles().values()) {
			Id<VehicleType> vehicleTypeId = v.getVehicleTypeId();
			if(vehicleTypeId == null) vehicleTypeId = v.getType() == null ? null : v.getType().getId();
			if(vehicleTypeId == null) throw new IllegalStateException("vehicleTypeId is missing.");
			this.writeStartTag(VEHICLE, List.of(
					createTuple(ID, v.getId().toString()),
					createTuple(DEPOT_LINK_ID, v.getLinkId().toString()),
					createTuple(TYPE_ID, vehicleTypeId.toString()),
					createTuple(EARLIEST_START, getTime(v.getEarliestStartTime())),
					createTuple(LATEST_END, getTime(v.getLatestEndTime()))), true
			);
		}
		this.writeEndTag(VEHICLES);
		this.writeEndTag(CAPABILITIES);
	}

	private void writeShipments(Carrier carrier, BufferedWriter writer) {
		if(carrier.getShipments().isEmpty()) return;
		this.writeStartTag(SHIPMENTS, null);
		for (CarrierShipment s : carrier.getShipments().values()) {
			Id<CarrierShipment> shipmentId = s.getId();
			if (s.getAttributes().isEmpty()) {
				writeShipment(s, shipmentId, true, false);
			} else {
				writeShipment(s, shipmentId, false, true);
				this.attributesWriter.writeAttributes("\t\t\t\t", writer, s.getAttributes(), false);
				this.writeEndTag(SHIPMENT);
			}
		}
		this.writeEndTag(SHIPMENTS);
	}

	private void writeShipment(CarrierShipment s, Id<CarrierShipment> shipmentId, boolean closeElement, boolean lineBreak) {
		this.writeStartTag(SHIPMENT, List.of(
				createTuple(ID, shipmentId.toString()),
				createTuple(FROM, s.getPickupLinkId().toString()),
				createTuple(TO, s.getDeliveryLinkId().toString()),
				createTuple(SIZE, s.getCapacityDemand()),
				createTuple(START_PICKUP, getTime(s.getPickupStartingTimeWindow().getStart())),
				createTuple(END_PICKUP, getTime(s.getPickupStartingTimeWindow().getEnd())),
				createTuple(START_DELIVERY, getTime(s.getDeliveryStartingTimeWindow().getStart())),
				createTuple(END_DELIVERY, getTime(s.getDeliveryStartingTimeWindow().getEnd())),
				createTuple(PICKUP_SERVICE_TIME, getTime(s.getPickupDuration())),
				createTuple(DELIVERY_SERVICE_TIME, getTime(s.getDeliveryDuration()))), closeElement, lineBreak
		);
	}

	private void writeServices(Carrier carrier, BufferedWriter writer) {
		if(carrier.getServices().isEmpty()) return;
		this.writeStartTag(SERVICES, null);
		for (CarrierService s : carrier.getServices().values()) {
			if (s.getAttributes().isEmpty()) {
				writeService(s, true, false);
			} else {
				writeService(s, false, true);
				this.attributesWriter.writeAttributes("\t\t\t\t", writer, s.getAttributes(), false);
				this.writeEndTag(SERVICE);
			}
		}
		this.writeEndTag(SERVICES);

	}

	private void writeService(CarrierService s, boolean closeElement, boolean lineBreak) {
		this.writeStartTag(SERVICE, List.of(
				createTuple(ID, s.getId().toString()),
				createTuple(TO, s.getServiceLinkId().toString()),
				createTuple(CAPACITY_DEMAND, s.getCapacityDemand()),
				createTuple(EARLIEST_START, getTime(s.getServiceStaringTimeWindow().getStart())),
				createTuple(LATEST_END, getTime(s.getServiceStaringTimeWindow().getEnd())),
				createTuple(SERVICE_DURATION, getTime(s.getServiceDuration()))), closeElement, lineBreak
		);
	}

	private String getTime(double time) {
		return Time.writeTime(time);
	}

	private void writePlans(Carrier carrier, BufferedWriter writer)
			throws IOException {
		if (carrier.getSelectedPlan() == null) {
			return;
		}
		this.writeStartTag(PLANS, null);
		for (CarrierPlan plan : carrier.getPlans()){
			if (plan.getScore() != null) {
				if (carrier.getSelectedPlan() != null && plan == carrier.getSelectedPlan()) {
					this.writeStartTag(PLAN, List.of(
							createTuple(SCORE, plan.getScore()),
							createTuple(SELECTED, "true")));
				} else if (carrier.getSelectedPlan() != null && plan != carrier.getSelectedPlan()) {
					this.writeStartTag(PLAN, List.of(
							createTuple(SCORE, plan.getScore()),
							createTuple(SELECTED, "false")));
				} else {
					this.writeStartTag(PLAN, List.of(
							createTuple(SCORE, plan.getScore()),
							createTuple(SELECTED, "false")));
				}
			}
			else {
				if (carrier.getSelectedPlan() != null && plan == carrier.getSelectedPlan()) {
					this.writeStartTag(PLAN, List.of(
							createTuple(SELECTED, "true")));
				} else if (carrier.getSelectedPlan() != null && plan != carrier.getSelectedPlan()) {
					this.writeStartTag(PLAN, List.of(
							createTuple(SELECTED, "false")));
				} else {
					this.writeStartTag(PLAN, List.of(
							createTuple(SELECTED, "false")));
				}
			}

			if (!plan.getAttributes().isEmpty()) {
				writer.write(NL);
				this.attributesWriter.writeAttributes("\t\t\t\t", writer, plan.getAttributes(),false);
			}

			for (ScheduledTour tour : plan.getScheduledTours()) {
				this.writeStartTag(TOUR, List.of(
						createTuple(TOUR_ID, tour.getTour().getId().toString()),
						createTuple(VEHICLE_ID,  tour.getVehicle().getId().toString())
				));
				this.writeStartTag(ACTIVITY, List.of(
						createTuple(TYPE, CarrierConstants.START),
						createTuple(END_TIME, Time.writeTime(tour.getDeparture()))), true
				);
				for (Tour.TourElement tourElement : tour.getTour().getTourElements()) {
					if (tourElement instanceof Tour.Leg leg) {
						this.writeStartTag(LEG, List.of(
								createTuple(EXPECTED_DEP_TIME, Time.writeTime(leg.getExpectedDepartureTime())),
								createTuple(EXPECTED_TRANSP_TIME, Time.writeTime(leg.getExpectedTransportTime()))
						));
						if (leg.getRoute() != null) {
							this.writeStartTag(ROUTE, null);
							boolean firstLink = true;
							for (Id<Link> id : ((NetworkRoute) leg.getRoute()).getLinkIds()) {
								if (firstLink) {
									writer.write(id.toString());
									firstLink = false;
								} else {
									writer.write(" " + id.toString());
								}
							}
							writer.write("</" + ROUTE + ">");
							this.setIndentationLevel(6);
							this.writeEndTag(LEG);
						} else {
							writeEndTag(LEG);
						}
					}
					else if (tourElement instanceof Tour.ShipmentBasedActivity act) {
						this.writeStartTag(ACTIVITY, List.of(
								createTuple(TYPE, act.getActivityType()),
								createTuple(SHIPMENT_ID, act.getShipment().getId().toString())), true
						);
						if (!carrier.getShipments().containsKey(act.getShipment().getId())) {
							logger.error("Shipment with id {} is contained in the carriers plan, but not available in the list of shipments. Carrier with carrierId: {}", act.getShipment().getId().toString(), carrier.getId());
						}
					}
					else if (tourElement instanceof Tour.ServiceActivity act) {
						this.writeStartTag(ACTIVITY, List.of(
								createTuple(TYPE, act.getActivityType()),
								createTuple(SERVICE_ID, act.getService().getId().toString())), true
						);
						if (!carrier.getServices().containsKey(act.getService().getId())) {
							logger.error("service with id {} is contained in the carriers plan, but not available in the list of services. Carrier with carrierId: {}", act.getService().getId().toString(), carrier.getId());
						}
					}
				}
				this.writeStartTag(ACTIVITY, List.of(
						createTuple(TYPE, CarrierConstants.END)), true
				);
				this.writeEndTag(TOUR);
			}
			this.writeEndTag(PLAN);
		}
		this.writeEndTag(PLANS);
	}

	private void endCarrier() {
		this.writeEndTag(CARRIER);
	}

	private void writeEndElement() {
		this.writeEndTag(CARRIERS);
	}
}
