package org.matsim.contrib.freight.carrier;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import com.graphhopper.jsprit.core.problem.job.Shipment;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.freight.carrier.Tour.Leg;
import org.matsim.contrib.freight.carrier.Tour.ShipmentBasedActivity;
import org.matsim.contrib.freight.carrier.Tour.TourElement;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.utils.io.MatsimXmlWriter;
import org.matsim.core.utils.misc.Time;

/**
 * A writer that writes carriers and their plans in an xml-file.
 * 
 * @author sschroeder
 *
 */
public class CarrierPlanWriter extends MatsimXmlWriter {

	private static Logger logger = Logger.getLogger(CarrierPlanWriter.class);

	private Collection<Carrier> carriers;

	private int idCounter = 0;

	private Map<CarrierShipment, Id<Shipment>> registeredShipments = new HashMap<CarrierShipment, Id<Shipment>>();

	/**
	 * Constructs the writer with the carriers to be written.
	 * 
	 * @param carriers to be written
	 */
	public CarrierPlanWriter(Collection<Carrier> carriers) {
		super();
		this.carriers = carriers;
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
				writeVehicles(carrier, this.writer);
				writeShipments(carrier, this.writer);
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

	private void writeVehicles(Carrier carrier, BufferedWriter writer)
			throws IOException {
		writer.write("\t\t\t<vehicles>\n");
		for (CarrierVehicle v : carrier.getCarrierCapabilities().getCarrierVehicles()) {
			writer.write("\t\t\t\t<vehicle id=\"" + v.getId()
					+ "\" linkId=\"" + v.getLocation() + "\"" + "\" typeId=\""
					+ v.getVehicleTypeId().toString()
					+ "\" earliestStart=\"" + getTime(v.getEarliestStartTime())
					+ "\" latestEnd=\"" + getTime(v.getLatestEndTime())
					+ "\"/>\n");
		}
		writer.write("\t\t\t</vehicles>\n\n");
	}

	private void writeShipments(Carrier carrier, BufferedWriter writer)
			throws IOException {
		writer.write("\t\t\t<shipments>\n");
		for (CarrierShipment s : carrier.getShipments()) {
			// CarrierShipment s = contract.getShipment();
			Id<Shipment> shipmentId = Id.create(++idCounter, Shipment.class);
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
					+ getTime(s.getDeliveryServiceTime()) + "\"/>\n");
		}
		writer.write("\t\t\t</shipments>\n\n");
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
				writer.write("\t\t\t\t\t<act type=\"" + FreightConstants.START
						+ "\" end_time=\"" + Time.writeTime(tour.getDeparture())
						+ "\"/>\n");
				for (TourElement tourElement : tour.getTour().getTourElements()) {
					if (tourElement instanceof Leg) {
						Leg leg = (Leg) tourElement;
						writer.write("\t\t\t\t\t<leg dep_time=\""
								+ Time.writeTime(leg.getExpectedDepartureTime())
								+ "\" transp_time=\""
								+ Time.writeTime(leg.getExpectedTransportTime())
								+ "\">");
						if (leg.getRoute() != null) {
							writer.write("\n");
							writer.write("\t\t\t\t\t\t<route>");
							boolean firstLink = true;
							for (Id<Link> id : ((NetworkRoute) leg.getRoute()).getLinkIds()) {
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
					if (tourElement instanceof ShipmentBasedActivity) {
						ShipmentBasedActivity act = (ShipmentBasedActivity) tourElement;
						writer.write("\t\t\t\t\t<act ");
						writer.write("type=\"" + act.getActivityType() + "\" ");
						writer.write("shipmentId=\""
								+ registeredShipments.get(act.getShipment())
								+ "\" ");
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
