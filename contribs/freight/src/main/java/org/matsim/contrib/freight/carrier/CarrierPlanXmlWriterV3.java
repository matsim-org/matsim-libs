package org.matsim.contrib.freight.carrier;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.freight.carrier.Tour.Leg;
import org.matsim.contrib.freight.carrier.Tour.ServiceActivity;
import org.matsim.contrib.freight.carrier.Tour.ShipmentBasedActivity;
import org.matsim.contrib.freight.carrier.Tour.TourElement;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.utils.io.MatsimXmlWriter;
import org.matsim.core.utils.misc.Time;
import org.matsim.vehicles.VehicleType;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * A writer that writes carriers and their plans in an xml-file.
 *
 * @author sschroeder
 */
public class CarrierPlanXmlWriterV3 extends MatsimXmlWriter {

	private static Logger logger = Logger.getLogger(CarrierPlanXmlWriterV3.class);

	private Collection<Carrier> carriers;

	private Map<CarrierShipment, Id<CarrierShipment>> registeredShipments = new HashMap<>();

	private Map<CarrierService, Id<CarrierService>> serviceMap = new HashMap<>();

	/**
	 * Constructs the writer with the carriers to be written.
	 *
	 * @param carriers to be written
	 */
	public CarrierPlanXmlWriterV3(Carriers carriers) {
		super();
		this.carriers = carriers.getCarriers().values();
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
				writeServices(carrier, this.writer);
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
	}

	private void writeVehiclesAndTheirTypes(Carrier carrier, BufferedWriter writer) throws IOException {
		writer.write("\t\t\t<capabilities fleetSize=\"" + carrier.getCarrierCapabilities().getFleetSize().toString() + "\">\n");
//		writer.write("\t\t\t\t<vehicleTypes>\n");
//		for(CarrierVehicleType type : carrier.getCarrierCapabilities().getVehicleTypes()){
//			writer.write("\t\t\t\t\t<vehicleType id=\"" + type.getId() + "\">\n");
//			writer.write("\t\t\t\t\t\t<description>" + type.getDescription() + "</description>\n");
//			writer.write("\t\t\t\t\t\t<engineInformation fuelType=\"" + type.getEngineInformation().getFuelType().toString() + "\" gasConsumption=\"" + type.getEngineInformation().getGasConsumption() + "\"/>\n");
//			writer.write("\t\t\t\t\t\t<capacity>" + type.getCarrierVehicleCapacity() + "</capacity>\n");
//			writer.write("\t\t\t\t\t\t<costInformation fix=\"" + type.getVehicleCostInformation().fix + "\" perMeter=\"" + type.getVehicleCostInformation().perDistanceUnit + 
//					"\" perSecond=\"" + type.getVehicleCostInformation().perTimeUnit + "\"/>\n");
//			writer.write("\t\t\t\t\t</vehicleType>\n");
//		}
//		writer.write("\t\t\t\t</vehicleTypes>\n\n");
		//vehicles
		writer.write("\t\t\t\t<vehicles>\n");
		for (CarrierVehicle v : carrier.getCarrierCapabilities().getCarrierVehicles()) {
			Id<VehicleType> vehicleTypeId = v.getVehicleTypeId();
			if (vehicleTypeId == null) vehicleTypeId = v.getVehicleType().getId();
			if (vehicleTypeId == null) throw new IllegalStateException("vehicleTypeId is missing.");
			writer.write("\t\t\t\t\t<vehicle id=\"" + v.getVehicleId()
					+ "\" depotLinkId=\"" + v.getLocation()
					+ "\" typeId=\"" + vehicleTypeId.toString()
					+ "\" earliestStart=\"" + getTime(v.getEarliestStartTime())
					+ "\" latestEnd=\"" + getTime(v.getLatestEndTime())
					+ "\"");
			if (v.getSkills().values().isEmpty()) {
				writer.write("/>\n");
			} else {
				writer.write(">\n");
				writer.write("\t\t\t\t\t\t<skills>");
				Iterator<String> skillIteratior = v.getSkills().values().iterator();
				writer.write(skillIteratior.next());
				while(skillIteratior.hasNext()){
					writer.write(",");
					writer.write(skillIteratior.next());
				}
				writer.write("</skills>\n");
				writer.write("\t\t\t\t\t</vehicle>\n");
			}
		}
		writer.write("\t\t\t\t</vehicles>\n");
		writer.write("\t\t\t</capabilities>\n\n");
	}

	private void writeShipments(Carrier carrier, BufferedWriter writer) throws IOException {
		if (carrier.getShipments().isEmpty()) return;
		writer.write("\t\t\t<shipments>\n");
		for (CarrierShipment s : carrier.getShipments()) {
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
					+ getTime(s.getDeliveryServiceTime()) + "\"");
			if(s.getSkills().values().isEmpty()){
				writer.write("/>\n");
			} else{
				writer.write(">\n");
				writer.write("\t\t\t\t\t<skills>");
				Iterator<String> skillsIterator = s.getSkills().values().iterator();
				writer.write(skillsIterator.next());
				while(skillsIterator.hasNext()){
					writer.write(",");
					writer.write(skillsIterator.next());
				}
				writer.write("</skills>\n");
				writer.write("\t\t\t\t</shipment>\n");
			}
		}
		writer.write("\t\t\t</shipments>\n\n");
	}

	private void writeServices(Carrier carrier, BufferedWriter writer) throws IOException {
		writer.write("\t\t\t<services>\n");
		for (CarrierService s : carrier.getServices()) {
			serviceMap.put(s, s.getId());
			writer.write("\t\t\t\t<service ");
			writer.write("id=\"" + s.getId().toString() + "\" ");
			writer.write("to=\"" + s.getLocationLinkId() + "\" ");
			// capacity which must be available when vehicle services this service.
			// i.e. this is a pick-up service.
			writer.write("capacityDemand=\"" + s.getCapacityDemand() + "\" ");
			writer.write("earliestStart=\"" + getTime(s.getServiceStartTimeWindow().getStart()) + "\" ");
			writer.write("latestEnd=\"" + getTime(s.getServiceStartTimeWindow().getEnd()) + "\" ");
			writer.write("serviceDuration=\"" + getTime(s.getServiceDuration()) + "\"/>\n");
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

		for (CarrierPlan plan : carrier.getPlans()) {
			writer.write("\t\t\t<plan");
			if (plan.getScore() != null) {
				writer.write(" score=\"" + plan.getScore().toString() + "\"");
			}
			if (carrier.getSelectedPlan() != null) {
				if (plan == carrier.getSelectedPlan()) {
					writer.write(" selected=\"true\"");
				} else {
					writer.write(" selected=\"false\"");
				}
			} else {
				writer.write(" selected=\"false\"");
			}
			writer.write(">\n");

			for (ScheduledTour tour : plan.getScheduledTours()) {
				writer.write("\t\t\t\t<tour ");
				writer.write("vehicleId=\"" + tour.getVehicle().getVehicleId()
						+ "\">\n");
				writer.write("\t\t\t\t\t<act type=\"" + FreightConstants.START
						+ "\" end_time=\"" + Time.writeTime(tour.getDeparture())
						+ "\"/>\n");
				for (TourElement tourElement : tour.getTour().getTourElements()) {
					if (tourElement instanceof Leg) {
						Leg leg = (Leg) tourElement;
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
					} else if (tourElement instanceof ShipmentBasedActivity) {
						ShipmentBasedActivity act = (ShipmentBasedActivity) tourElement;
						writer.write("\t\t\t\t\t<act ");
						writer.write("type=\"" + act.getActivityType() + "\" ");
						writer.write("shipmentId=\"" + registeredShipments.get(act.getShipment()) + "\" ");
						writer.write("/>\n");
					} else if (tourElement instanceof ServiceActivity) {
						ServiceActivity act = (ServiceActivity) tourElement;
						writer.write("\t\t\t\t\t<act ");
						writer.write("type=\"" + act.getActivityType() + "\" ");
						writer.write("serviceId=\"" + serviceMap.get(act.getService()) + "\" ");
						writer.write("/>\n");
					}

				}
				writer.write("\t\t\t\t\t<act type=\"" + FreightConstants.END
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
