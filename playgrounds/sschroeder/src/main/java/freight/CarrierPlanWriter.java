package freight;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.utils.io.MatsimXmlWriter;

import playground.mzilske.freight.CarrierImpl;
import playground.mzilske.freight.CarrierVehicle;
import playground.mzilske.freight.FreightConstants;
import playground.mzilske.freight.ScheduledTour;
import playground.mzilske.freight.Shipment;
import playground.mzilske.freight.Tour.Delivery;
import playground.mzilske.freight.Tour.GeneralActivity;
import playground.mzilske.freight.Tour.Pickup;
import playground.mzilske.freight.Tour.TourElement;

public class CarrierPlanWriter extends MatsimXmlWriter{

	private static Logger logger = Logger.getLogger(CarrierPlanWriter.class);

	private Collection<CarrierImpl> carriers;
	
	private int idCounter = 0;
	
	private Map<Shipment,Id> registeredShipments = new HashMap<Shipment, Id>();

	public CarrierPlanWriter(Collection<CarrierImpl> carriers) {
		super();
		this.carriers = carriers;
	}
	
	public void write(String filename) {
		logger.info("write carrier plans");
		try{
			openFile(filename);
			writeXmlHead();
			startCarriers(this.writer);
			for(CarrierImpl carrier : carriers){
				startCarrier(carrier,this.writer);
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

	private void endCarrier(BufferedWriter writer) throws IOException {
		writer.write("\t\t</carrier>\n");
		registeredShipments.clear();
	}

	private void startCarrier(CarrierImpl carrier, BufferedWriter writer) throws IOException {
		writer.write("\t\t<carrier id=\"" + carrier.getId() + "\" linkId=\"" + carrier.getDepotLinkId() + "\">\n");
		for(ScheduledTour tour : carrier.getSelectedPlan().getScheduledTours()){
			registeredShipments(tour.getTour().getShipments());
		}
		startShipments(writer);
		for(Shipment shipment : registeredShipments.keySet()){
			startShipment(registeredShipments.get(shipment), shipment, writer);
		}
		endShipments(writer);
		startVehicles(writer);
		for(CarrierVehicle v : carrier.getCarrierCapabilities().getCarrierVehicles()){
			startAndEndVehicle(v);
		}
		endVehicles(writer);
		startScheduledTours(writer);
		for(ScheduledTour tour : carrier.getSelectedPlan().getScheduledTours()){
			startScheduledTour(tour,writer);
			endScheduledTour(writer);
		}
		endScheduledTours(writer);
	}

	private void endScheduledTours(BufferedWriter writer) throws IOException {
		writer.write("\t\t\t</tours>\n");
		
	}

	private void startScheduledTours(BufferedWriter writer) throws IOException {
		writer.write("\t\t\t<tours>\n");
		
	}

	private void startAndEndVehicle(CarrierVehicle v) throws IOException {
		writer.write("\t\t\t\t<vehicle id=\"" + v.getVehicleId() + "\" linkId=\"" + v.getLocation() + "\"/>\n");
	}

	private void endScheduledTour(BufferedWriter writer) throws IOException {
		writer.write("\t\t\t\t</tour>\n");
	}

	private void startScheduledTour(ScheduledTour tour, BufferedWriter writer) throws IOException {
		writer.write("\t\t\t\t<tour ");
		writer.write("vehicleId=\"" + tour.getVehicle().getVehicleId() + "\" ");
		writer.write("linkId=\"" + tour.getVehicle().getLocation() + "\" ");
		writer.write("start=\"" + tour.getDeparture() + "\">\n");
		writer.write("\t\t\t\t\t<act type=\"" + FreightConstants.START + "\" />\n");
		for(TourElement tourElement : tour.getTour().getTourElements()){
			writer.write("\t\t\t\t\t<act ");
			writer.write("type=\"" + tourElement.getActivityType() + "\" ");
			if((tourElement instanceof Pickup) || (tourElement instanceof Delivery)){
				writer.write("shipmentId=\"" + registeredShipments.get(tourElement.getShipment()) + "\"");
			}
			else if(tourElement instanceof GeneralActivity){
				GeneralActivity genAct = (GeneralActivity)tourElement;
				writer.write("linkId=\"" + genAct.getLocation() + "\" ");
				writer.write("duration=\"" + genAct.getDuration() + "\"");
			}
			writer.write("/>\n");
		}
		writer.write("\t\t\t\t\t<act type=\"" + FreightConstants.END + "\"/>\n");
	}

	private void endVehicles(BufferedWriter writer) throws IOException {
		writer.write("\t\t\t</vehicles>\n");
		
	}

	private void startVehicles(BufferedWriter writer) throws IOException {
		writer.write("\t\t\t<vehicles>\n");
	}

	private void registeredShipments(List<Shipment> shipments) {
		for(Shipment s : shipments){
			Id shipmentId = createId();
			registeredShipments.put(s, shipmentId);
		}
	}

	private void endShipments(BufferedWriter writer) throws IOException {
		writer.write("\t\t\t</shipments>\n");
	}

	private void startShipments(BufferedWriter writer) throws IOException {
		writer.write("\t\t\t<shipments>\n");
	}

	private void startShipment(Id shipmentId, Shipment s, BufferedWriter writer) throws IOException {
		writer.write("\t\t\t\t<shipment ");
		writer.write("id=\"" + shipmentId + "\" ");
		writer.write("from=\"" + s.getFrom() + "\" ");
		writer.write("to=\"" + s.getTo() + "\" ");
		writer.write("size=\"" + s.getSize() + "\" ");
		writer.write("startPickup=\"" + s.getPickupTimeWindow().getStart() + "\" ");
		writer.write("endPickup=\""  + s.getPickupTimeWindow().getEnd() + "\" ");
		writer.write("startDelivery=\"" + s.getDeliveryTimeWindow().getStart() + "\" ");
		writer.write("endDelivery=\"" + s.getDeliveryTimeWindow().getEnd() + "\">\n");
	}

	private Id createId() {
		idCounter++;
		return new IdImpl(idCounter);
	}

	private void endCarriers(BufferedWriter writer) throws IOException {
		writer.write("\t</carriers>\n");
		
	}

	private void startCarriers(BufferedWriter writer) throws IOException {
		writer.write("\t<carriers>\n");
	}
}
