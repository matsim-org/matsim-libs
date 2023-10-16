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

import com.google.inject.Inject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.io.MatsimXmlWriter;
import org.matsim.core.utils.misc.Time;
import org.matsim.freight.carriers.Tour.Leg;
import org.matsim.freight.carriers.Tour.ServiceActivity;
import org.matsim.freight.carriers.Tour.ShipmentBasedActivity;
import org.matsim.freight.carriers.Tour.TourElement;
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
 * @deprecated Use {@link CarrierPlanWriter} instead which writes the newest format
 */
@Deprecated
public class CarrierPlanXmlWriterV2 extends MatsimXmlWriter {

	@SuppressWarnings("unused")
	private static final  Logger logger = LogManager.getLogger(CarrierPlanXmlWriterV2.class);

	private final Collection<Carrier> carriers;

	private final Map<CarrierShipment, Id<CarrierShipment>> registeredShipments = new HashMap<>();

	private final Map<CarrierService, Id<CarrierService>> serviceMap = new HashMap<>();

	private final AttributesXmlWriterDelegate attributesWriter = new AttributesXmlWriterDelegate();
	private final List<Tuple<String, String>> atts = new ArrayList<>();


	/**
	 * Constructs the writer with the carriers to be written.
	 *
	 * @param carriers to be written
	 */
	public CarrierPlanXmlWriterV2(Carriers carriers) {
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

			startCarriers(this.writer);
			for (Carrier carrier : carriers) {
				startCarrier(carrier, this.writer);
				writeVehiclesAndTheirTypes(carrier, this.writer);
				writeShipments(carrier, this.writer);
				writeServices(carrier,this.writer);
				writePlans(carrier, this.writer);
				endCarrier(this.writer);
			}
			endCarriers(this.writer);
			close();
			logger.info("done");
		} catch (IOException e) {
			e.printStackTrace();
			logger.error(e);
			System.exit(1);
		}
	}

	private void startCarriers(BufferedWriter writer) throws IOException {
		writer.write("\t<carriers>\n");
	}

	private void startCarrier(Carrier carrier, BufferedWriter writer)
			throws IOException {
		writer.write("\t\t<carrier id=\"" + carrier.getId() + "\">\n");
		attributesWriter.writeAttributes("\t\t\t", writer, carrier.getAttributes());
	}

	private void writeVehiclesAndTheirTypes(Carrier carrier, BufferedWriter writer)throws IOException {
		writer.write("\t\t\t<capabilities fleetSize=\""+ carrier.getCarrierCapabilities().getFleetSize().toString() + "\">\n");
		//vehicles
		writer.write("\t\t\t\t<vehicles>\n");
		for (CarrierVehicle v : carrier.getCarrierCapabilities().getCarrierVehicles().values()) {
			Id<VehicleType> vehicleTypeId = v.getVehicleTypeId();
			if(vehicleTypeId == null) vehicleTypeId = v.getType() == null ? null : v.getType().getId();
			if(vehicleTypeId == null) throw new IllegalStateException("vehicleTypeId is missing.");
			writer.write("\t\t\t\t\t<vehicle id=\"" + v.getId()
					+ "\" depotLinkId=\"" + v.getLinkId()
					+ "\" typeId=\"" + vehicleTypeId
					+ "\" earliestStart=\"" + getTime(v.getEarliestStartTime())
					+ "\" latestEnd=\"" + getTime(v.getLatestEndTime())
					+ "\"/>\n");
		}
		writer.write("\t\t\t\t</vehicles>\n\n");
		writer.write("\t\t\t</capabilities>\n\n");
	}

	private void writeShipments(Carrier carrier, BufferedWriter writer) throws IOException {
		if(carrier.getShipments().isEmpty()) return;
		writer.write("\t\t\t<shipments>\n");
		for (CarrierShipment s : carrier.getShipments().values()) {
			// CarrierShipment s = contract.getShipment();
			Id<CarrierShipment> shipmentId = s.getId();
			registeredShipments.put(s, shipmentId);
			writer.write("\t\t\t\t<shipment ");
			writer.write("id=\"" + shipmentId + "\" ");
			writer.write("from=\"" + s.getFrom() + "\" ");
			writer.write("to=\"" + s.getTo() + "\" ");
			writer.write("size=\"" + s.getSize() + "\" ");
			writer.write("startPickup=\""
					+ getTime(s.getPickupTimeWindow().getStart()) + "\" ");
			writer.write("endPickup=\""
					+ getTime(s.getPickupTimeWindow().getEnd()) + "\" ");
			writer.write("startDelivery=\""
					+ getTime(s.getDeliveryTimeWindow().getStart()) + "\" ");
			writer.write("endDelivery=\""
					+ getTime(s.getDeliveryTimeWindow().getEnd()) + "\" ");
			writer.write("pickupServiceTime=\""
					+ getTime(s.getPickupServiceTime()) + "\" ");
			writer.write("deliveryServiceTime=\""
					+ getTime(s.getDeliveryServiceTime()));
			if (s.getAttributes().isEmpty()){
				writer.write("\"/>\n");
			} else {
				writer.write("\">\n");
				this.attributesWriter.writeAttributes("\t\t\t\t\t", writer, s.getAttributes());
				writer.write("\t\t\t\t</shipment>\n");
			}
		}
		writer.write("\t\t\t</shipments>\n\n");
	}

	private void writeServices(Carrier carrier, BufferedWriter writer) throws IOException {
		writer.write("\t\t\t<services>\n");
		for (CarrierService s : carrier.getServices().values()) {
			serviceMap.put(s, s.getId());
			writer.write("\t\t\t\t<service ");
			writer.write("id=\"" + s.getId().toString() + "\" ");
			writer.write("to=\"" + s.getLocationLinkId() + "\" ");
			// capacity which must be available when vehicle services this service.
			// i.e. this is a pick-up service.
			writer.write("capacityDemand=\"" + s.getCapacityDemand() + "\" ");
			writer.write("earliestStart=\"" + getTime(s.getServiceStartTimeWindow().getStart()) + "\" ");
			writer.write("latestEnd=\"" + getTime(s.getServiceStartTimeWindow().getEnd()) + "\" ");
			writer.write("serviceDuration=\"" + getTime(s.getServiceDuration()));
			if (s.getAttributes().isEmpty()){
				writer.write("\"/>\n");
			} else {
				writer.write("\">\n");
				this.attributesWriter.writeAttributes("\t\t\t\t\t", writer, s.getAttributes());
				writer.write("\t\t\t\t</service>\n");
			}

		}
		writer.write("\t\t\t</services>\n\n");

	}

	private String getTime(double time) {
		return Time.writeTime(time);
	}

	private void writePlans(Carrier carrier, BufferedWriter writer)
			throws IOException {
		if (carrier.getSelectedPlan() == null) {
			return;
		}

		for(CarrierPlan plan : carrier.getPlans()){
			writer.write("\t\t\t<plan");
			if(plan.getScore() != null){
				writer.write(" score=\"" + plan.getScore().toString() + "\"");
			}
			if(carrier.getSelectedPlan() != null){
				if(plan == carrier.getSelectedPlan()){
					writer.write(" selected=\"true\"");
				}
				else{
					writer.write(" selected=\"false\"");
				}
			}
			else{
				writer.write(" selected=\"false\"");
			}
			writer.write(">\n");

			for (ScheduledTour tour : plan.getScheduledTours()) {
				writer.write("\t\t\t\t<tour ");
				writer.write("vehicleId=\"" + tour.getVehicle().getId()
						+ "\">\n");
				writer.write("\t\t\t\t\t<act type=\"" + CarrierConstants.START
						+ "\" end_time=\"" + Time.writeTime(tour.getDeparture())
						+ "\"/>\n");
				for (TourElement tourElement : tour.getTour().getTourElements()) {
					if (tourElement instanceof Leg leg) {
						writer.write("\t\t\t\t\t<leg expected_dep_time=\""
								+ Time.writeTime(leg.getExpectedDepartureTime())
								+ "\" expected_transp_time=\""
								+ Time.writeTime(leg.getExpectedTransportTime())
								+ "\">");
						if (leg.getRoute() != null) {
							writer.write("\n");
							writer.write("\t\t\t\t\t\t<route>");
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
							writer.write("</route>\n");
							writer.write("\t\t\t\t\t</leg>\n");
						} else {
							writer.write("</leg>\n");
						}
					}
					else if (tourElement instanceof ShipmentBasedActivity act) {
						writer.write("\t\t\t\t\t<act ");
						writer.write("type=\"" + act.getActivityType() + "\" ");
						writer.write("shipmentId=\"" + registeredShipments.get(act.getShipment()) + "\" ");
						writer.write("/>\n");
					}
					else if (tourElement instanceof ServiceActivity act){
						writer.write("\t\t\t\t\t<act ");
						writer.write("type=\"" + act.getActivityType() + "\" ");
						writer.write("serviceId=\"" + serviceMap.get(act.getService()) + "\" ");
						writer.write("/>\n");
					}

				}
				writer.write("\t\t\t\t\t<act type=\"" + CarrierConstants.END
						+ "\"/>\n");
				writer.write("\t\t\t\t</tour>\n");
			}
			writer.write("\t\t\t</plan>\n\n");
		}

	}

	private void endCarrier(BufferedWriter writer) throws IOException {
		writer.write("\t\t</carrier>\n\n");
		registeredShipments.clear();
	}

	private void endCarriers(BufferedWriter writer) throws IOException {
		writer.write("\t</carriers>\n");

	}
}
