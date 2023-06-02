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

package org.matsim.contrib.freight.carrier;

import com.google.inject.Inject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.freight.carrier.Tour.Leg;
import org.matsim.contrib.freight.carrier.Tour.ServiceActivity;
import org.matsim.contrib.freight.carrier.Tour.ShipmentBasedActivity;
import org.matsim.contrib.freight.carrier.Tour.TourElement;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.io.MatsimXmlWriter;
import org.matsim.core.utils.io.UncheckedIOException;
import org.matsim.core.utils.misc.Time;
import org.matsim.utils.objectattributes.AttributeConverter;
import org.matsim.utils.objectattributes.attributable.AttributesXmlWriterDelegate;
import org.matsim.vehicles.VehicleType;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.*;

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

	private final Map<CarrierShipment, Id<CarrierShipment>> registeredShipments = new HashMap<>();

	private final Map<CarrierService, Id<CarrierService>> serviceMap = new HashMap<>();

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

	private void writeRootElement() throws UncheckedIOException, IOException {
		List<Tuple<String, String>> atts = new ArrayList<>();
		atts.add(createTuple(XMLNS, MatsimXmlWriter.MATSIM_NAMESPACE));
		atts.add(createTuple(XMLNS + ":xsi", DEFAULTSCHEMANAMESPACELOCATION));
		atts.add(createTuple("xsi:schemaLocation", MATSIM_NAMESPACE + " " + DEFAULT_DTD_LOCATION + "carriersDefinitions_v2.1.xsd"));
		this.writeStartTag("carriers", atts);
	}

	private void startCarrier(Carrier carrier, BufferedWriter writer)
			throws IOException {
		this.writeStartTag("carrier", List.of(
				createTuple("id", carrier.getId().toString())), false, true
		);
		attributesWriter.writeAttributes("\t\t", writer, carrier.getAttributes(), false);
	}

	private void writeVehiclesAndTheirTypes(Carrier carrier)throws IOException {
		this.writeStartTag("capabilities", List.of(
				createTuple("fleetSize", carrier.getCarrierCapabilities().getFleetSize().toString())
		));
		//vehicles
		this.writeStartTag("vehicles", null);
		for (CarrierVehicle v : carrier.getCarrierCapabilities().getCarrierVehicles().values()) {
			Id<VehicleType> vehicleTypeId = v.getVehicleTypeId();
			if(vehicleTypeId == null) vehicleTypeId = v.getType() == null ? null : v.getType().getId();
			if(vehicleTypeId == null) throw new IllegalStateException("vehicleTypeId is missing.");
			this.writeStartTag("vehicle", List.of(
					createTuple("id", v.getId().toString()),
					createTuple("depotLinkId", v.getLinkId().toString()),
					createTuple("typeId", vehicleTypeId.toString()),
					createTuple("earliestStart", getTime(v.getEarliestStartTime())),
					createTuple("latestEnd", getTime(v.getLatestEndTime()))), true
			);
		}
		this.writeEndTag("vehicles");
		this.writeEndTag("capabilities");
	}

	private void writeShipments(Carrier carrier, BufferedWriter writer) throws IOException {
		if(carrier.getShipments().isEmpty()) return;
		this.writeStartTag("shipments", null);
		for (CarrierShipment s : carrier.getShipments().values()) {
			Id<CarrierShipment> shipmentId = s.getId();
			registeredShipments.put(s, shipmentId);
			if (s.getAttributes().isEmpty()) {
				writeShipment(s, shipmentId, true, false);
			} else {
				writeShipment(s, shipmentId, false, true);
				this.attributesWriter.writeAttributes("\t\t\t\t", writer, s.getAttributes(), false);
				this.writeEndTag("shipment");
			}
		}
		this.writeEndTag("shipments");
	}

	private void writeShipment(CarrierShipment s, Id<CarrierShipment> shipmentId, boolean closeElement, boolean lineBreak) {
		this.writeStartTag("shipment", List.of(
				createTuple("id", shipmentId.toString()),
				createTuple("from", s.getFrom().toString()),
				createTuple("to", s.getTo().toString()),
				createTuple("size", s.getSize()),
				createTuple("startPickup", getTime(s.getPickupTimeWindow().getStart())),
				createTuple("endPickup", getTime(s.getPickupTimeWindow().getEnd())),
				createTuple("startDelivery", getTime(s.getDeliveryTimeWindow().getStart())),
				createTuple("endDelivery", getTime(s.getDeliveryTimeWindow().getEnd())),
				createTuple("pickupServiceTime", getTime(s.getPickupServiceTime())),
				createTuple("deliveryServiceTime", getTime(s.getDeliveryServiceTime()))), closeElement, lineBreak
		);
	}

	private void writeServices(Carrier carrier, BufferedWriter writer) throws IOException {
		if(carrier.getServices().isEmpty()) return;
		this.writeStartTag("services", null);
		for (CarrierService s : carrier.getServices().values()) {
			serviceMap.put(s, s.getId());
			if (s.getAttributes().isEmpty()) {
				writeService(s, true, false);
			} else {
				writeService(s, false, true);
				this.attributesWriter.writeAttributes("\t\t\t\t", writer, s.getAttributes(), false);
				this.writeEndTag("service");
			}
		}
		this.writeEndTag("services");

	}

	private void writeService(CarrierService s, boolean closeElement, boolean lineBreak) {
		this.writeStartTag("service", List.of(
				createTuple("id", s.getId().toString()),
				createTuple("to", s.getLocationLinkId().toString()),
				createTuple("capacityDemand", s.getCapacityDemand()),
				createTuple("earliestStart", getTime(s.getServiceStartTimeWindow().getStart())),
				createTuple("latestEnd", getTime(s.getServiceStartTimeWindow().getEnd())),
				createTuple("serviceDuration", getTime(s.getServiceDuration()))), closeElement, lineBreak
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
		this.writeStartTag("plans", null);
		for (CarrierPlan plan : carrier.getPlans()){
			if (plan.getScore() != null) {
				if (carrier.getSelectedPlan() != null && plan == carrier.getSelectedPlan()) {
					this.writeStartTag("plan", List.of(
							createTuple("score", plan.getScore()),
							createTuple("selected", "true")));
				} else if (carrier.getSelectedPlan() != null && plan != carrier.getSelectedPlan()) {
					this.writeStartTag("plan", List.of(
							createTuple("score", plan.getScore()),
							createTuple("selected", "false")));
				} else {
					this.writeStartTag("plan", List.of(
							createTuple("score", plan.getScore()),
							createTuple("selected", "false")));
				}
			}
			else {
				if (carrier.getSelectedPlan() != null && plan == carrier.getSelectedPlan()) {
					this.writeStartTag("plan", List.of(
							createTuple("selected", "true")));
				} else if (carrier.getSelectedPlan() != null && plan != carrier.getSelectedPlan()) {
					this.writeStartTag("plan", List.of(
							createTuple("selected", "false")));
				} else {
					this.writeStartTag("plan", List.of(
							createTuple("selected", "false")));
				}
			}

			if (!plan.getAttributes().isEmpty()) {
				writer.write(NL);
				this.attributesWriter.writeAttributes("\t\t\t\t", writer, plan.getAttributes(),false);
			}

			for (ScheduledTour tour : plan.getScheduledTours()) {
				this.writeStartTag("tour", List.of(
						createTuple("tourId", tour.getTour().getId().toString()),
						createTuple("vehicleId",  tour.getVehicle().getId().toString())
				));
				this.writeStartTag("act", List.of(
						createTuple("type", FreightConstants.START),
						createTuple("end_time", Time.writeTime(tour.getDeparture()))), true
				);
				for (TourElement tourElement : tour.getTour().getTourElements()) {
					if (tourElement instanceof Leg leg) {
						this.writeStartTag("leg", List.of(
								createTuple("expected_dep_time", Time.writeTime(leg.getExpectedDepartureTime())),
								createTuple("expected_transp_time", Time.writeTime(leg.getExpectedTransportTime()))
						));
						if (leg.getRoute() != null) {
							this.writeStartTag("route", null);
							boolean firstLink = true;
							for (Id<Link> id : ((NetworkRoute) leg.getRoute())
									.getLinkIds()) {
								if (firstLink) {
									writer.write(id.toString());
									firstLink = false;
								} else {
									writer.write(" " + id.toString());
								}
							}
							writer.write("</route>");
							this.setIndentationLevel(6);
							this.writeEndTag("leg");
						} else {
							writeEndTag("leg");
						}
					}
					else if (tourElement instanceof ShipmentBasedActivity act) {
						this.writeStartTag("act", List.of(
								createTuple("type", act.getActivityType()),
								createTuple("shipmentId", registeredShipments.get(act.getShipment()).toString())), true
						);
					}
					else if (tourElement instanceof ServiceActivity act) {
						this.writeStartTag("act", List.of(
								createTuple("type", act.getActivityType()),
								createTuple("serviceId", serviceMap.get(act.getService()).toString())), true
						);
					}
				}
				this.writeStartTag("act", List.of(
						createTuple("type", FreightConstants.END)), true
				);
				this.writeEndTag("tour");
			}
			this.writeEndTag("plan");
		}
		this.writeEndTag("plans");
	}

	private void endCarrier() throws IOException {
		this.writeEndTag("carrier");
		registeredShipments.clear();
	}

	private void writeEndElement() throws IOException {
		this.writeEndTag("carriers");
	}
}
