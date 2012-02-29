package org.matsim.contrib.freight.carrier;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.contrib.freight.carrier.Tour.Leg;
import org.matsim.contrib.freight.carrier.Tour.ShipmentBasedActivity;
import org.matsim.contrib.freight.carrier.Tour.TourElement;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.utils.io.MatsimXmlWriter;


public class CarrierPlanWriter extends MatsimXmlWriter{

	private static Logger logger = Logger.getLogger(CarrierPlanWriter.class);

	private Collection<Carrier> carriers;
	
	private int idCounter = 0;
	
	private Map<CarrierShipment,Id> registeredShipments = new HashMap<CarrierShipment, Id>();

	public CarrierPlanWriter(Collection<Carrier> carriers) {
		super();
		this.carriers = carriers;
	}
	
	public void write(String filename) {
		logger.info("write carrier plans");
		try{
			openFile(filename);
			writeXmlHead();
			startCarriers(this.writer);
			for(Carrier carrier : carriers){
				startCarrier(carrier,this.writer);
				writeVehicles(carrier,this.writer);
				writeContracts(carrier,this.writer);
				writeScheduledTours(carrier,this.writer);
				endCarrier(this.writer);
			}
			endCarriers(this.writer);
			close();
			logger.info("done");
		}
		catch (IOException e) {
			e.printStackTrace();
			logger.error(e);
			System.exit(1);
		}
	}

	private void startCarriers(BufferedWriter writer) throws IOException {
		writer.write("\t<carriers>\n");
	}

	private void startCarrier(Carrier carrier, BufferedWriter writer) throws IOException {
		writer.write("\t\t<carrier id=\"" + carrier.getId() + "\" linkId=\"" + carrier.getDepotLinkId() + "\">\n");
	}

	private void writeVehicles(Carrier carrier, BufferedWriter writer) throws IOException {
		writer.write("\t\t\t<vehicles>\n");
		for(CarrierVehicle v : carrier.getCarrierCapabilities().getCarrierVehicles()){
			writer.write("\t\t\t\t<vehicle id=\"" + v.getVehicleId() + "\" linkId=\"" + v.getLocation() + "\"" +
			" cap=\"" + v.getCapacity() + "\" earliestStart=\"" + v.getEarliestStartTime() + "\" latestEnd=\"" + v.getLatestEndTime() + "\"/>\n");
		}
		writer.write("\t\t\t</vehicles>\n");
	}

	private void writeContracts(Carrier carrier, BufferedWriter writer) throws IOException {
		writer.write("\t\t\t<shipments>\n");
		for(CarrierContract contract : carrier.getContracts()){
			CarrierShipment s = contract.getShipment();
			Id shipmentId = createId();
			registeredShipments.put(s, shipmentId);
			writer.write("\t\t\t\t<shipment ");
			writer.write("id=\"" + shipmentId + "\" ");
			writer.write("from=\"" + s.getFrom() + "\" ");
			writer.write("to=\"" + s.getTo() + "\" ");
			writer.write("size=\"" + s.getSize() + "\" ");
			writer.write("startPickup=\"" + s.getPickupTimeWindow().getStart() + "\" ");
			writer.write("endPickup=\""  + s.getPickupTimeWindow().getEnd() + "\" ");
			writer.write("startDelivery=\"" + s.getDeliveryTimeWindow().getStart() + "\" ");
			writer.write("endDelivery=\"" + s.getDeliveryTimeWindow().getEnd()  + "\" ");
			writer.write("pickupServiceTime=\"" + s.getPickupServiceTime()  + "\" ");
			writer.write("deliveryServiceTime=\"" + s.getDeliveryServiceTime()  + "\"/>\n");		
		}
		writer.write("\t\t\t</shipments>\n");
	}

	private void writeScheduledTours(Carrier carrier, BufferedWriter writer) throws IOException {
		if(carrier.getSelectedPlan() == null){
			return;
		}
		writer.write("\t\t\t<tours>\n");
		for(ScheduledTour tour : carrier.getSelectedPlan().getScheduledTours()){
			writer.write("\t\t\t\t<tour ");
			writer.write("vehicleId=\"" + tour.getVehicle().getVehicleId() + "\" ");
			writer.write("start=\"" + tour.getDeparture() + "\">\n");
			writer.write("\t\t\t\t\t<act type=\"" + FreightConstants.START + "\" />\n");
			for(TourElement tourElement : tour.getTour().getTourElements()){
				if(tourElement instanceof Leg){
					Leg leg = (Leg) tourElement;
					writer.write("\t\t\t\t\t<leg>");
					if(leg.getRoute() != null){
						writer.write("\n");
						writer.write("\t\t\t\t\t\t<route>");
						boolean firstLink = true;
						for(Id id : ((NetworkRoute)leg.getRoute()).getLinkIds()){
							if(firstLink){
								writer.write(id.toString());
								firstLink = false;
							}
							else{
								writer.write(" " + id.toString());
							}
						}
						writer.write("</route>\n");
						writer.write("\t\t\t\t\t</leg>\n");
					}
					else{
						writer.write("</leg>\n");
					}
				}
				if(tourElement instanceof ShipmentBasedActivity){
					ShipmentBasedActivity act = (ShipmentBasedActivity) tourElement;
					writer.write("\t\t\t\t\t<act ");
					writer.write("type=\"" + act.getActivityType() + "\" ");
					writer.write("shipmentId=\"" + registeredShipments.get(act.getShipment()) + "\"");
					writer.write("/>\n");
				}
				
			}
			writer.write("\t\t\t\t\t<act type=\"" + FreightConstants.END + "\"/>\n");
			writer.write("\t\t\t\t</tour>\n");
		}
		writer.write("\t\t\t</tours>\n");
	}

	private void endCarrier(BufferedWriter writer) throws IOException {
		writer.write("\t\t</carrier>\n");
		registeredShipments.clear();
	}

	private Id createId() {
		idCounter++;
		return new IdImpl(idCounter);
	}

	private void endCarriers(BufferedWriter writer) throws IOException {
		writer.write("\t</carriers>\n");
		
	}
}
