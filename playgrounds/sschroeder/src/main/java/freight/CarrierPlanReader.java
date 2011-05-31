package freight;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.utils.io.MatsimXmlParser;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import playground.mzilske.freight.CarrierImpl;
import playground.mzilske.freight.CarrierPlan;
import playground.mzilske.freight.CarrierVehicle;
import playground.mzilske.freight.Offer;
import playground.mzilske.freight.ScheduledTour;
import playground.mzilske.freight.Shipment;
import playground.mzilske.freight.Tour;
import playground.mzilske.freight.TourBuilder;

public class CarrierPlanReader extends MatsimXmlParser{

	public static Logger logger = Logger.getLogger(CarrierPlanReader.class);
	
	public static String CARRIERS = "carriers";
	
	public static String CARRIER = "carrier";
	
	public static String LINKID = "linkId";
	
	public static String SHIPMENTS = "shipments";
	
	public static String SHIPMENT = "shipment";
	
	public static String ID = "id";
	
	public static String FROM = "from";
	
	public static String TO = "to";
	
	public static String SIZE = "size";
	
	public static String ACTIVITY = "act";
	
	public static String TYPE = "type";
	
	public static String SHIPMENTID = "shipmentId";
	
	public static String START = "start";
	
	public static String VEHICLE = "vehicle";
	
	public static String VEHICLES = "vehicles";
	
	public CarrierImpl currentCarrier = null;
	
	public CarrierVehicle currentVehicle = null;
	
	public TourBuilder currentTourBuilder = null;
	
	public Double currentStartTime = null; 
	
	public Map<String,Shipment> currentShipments = null;
	
	public Map<String,CarrierVehicle> vehicles = null;
	
	public Collection<ScheduledTour> scheduledTours = null;
	
	public CarrierPlan currentPlan = null;
	
	public Collection<CarrierImpl> carriers;
	
	public CarrierPlanReader(Collection<CarrierImpl> carriers) {
		super();
		this.carriers = carriers;
	}

	public void read(String filename){
	
		logger.info("read carrier plans");
		this.setValidating(false);
		parse(filename);
		logger.info("done");
	
	}

	
	
	@Override
	public void startTag(String name, Attributes atts, Stack<String> context) {
		if(name.equals(CARRIER)){
			currentCarrier = CarrierUtils.createCarrier(atts.getValue(ID), atts.getValue(LINKID));
		}
		if(name.equals(SHIPMENTS)){
			currentShipments = new HashMap<String, Shipment>();
		}
		if(name.equals(SHIPMENT)){
			Id from = makeId(atts.getValue(FROM));
			Id to = makeId(atts.getValue(TO));
			int size = getInt(atts.getValue(SIZE));
			String startPickup = atts.getValue("startPickup");
			Shipment shipment = null;
			if(startPickup == null){
				shipment = CarrierUtils.createShipment(from, to, size, 0, 24*3600, 0, 24*3600);
			}
			currentShipments.put(atts.getValue(ID), shipment);
			CarrierUtils.createAndAddContract(currentCarrier, shipment, new Offer());
		}
	
		if(name.equals(VEHICLES)){
			vehicles = new HashMap<String,CarrierVehicle>();
		}
		if(name.equals(VEHICLE)){
			String vId = atts.getValue(ID);
			CarrierVehicle vehicle = CarrierUtils.createAndAddVehicle(currentCarrier, vId, atts.getValue(LINKID),30);
			vehicles.put(vId,vehicle);
		}
		if(name.equals("tours")){ 
			scheduledTours = new ArrayList<ScheduledTour>();
		}
		if(name.equals("tour")){
			currentTourBuilder = new TourBuilder();
			String vehicleId = atts.getValue("vehicleId");
			currentVehicle = vehicles.get(vehicleId);
			currentStartTime = Double.parseDouble(atts.getValue(START));
		}
		if(name.equals(ACTIVITY)){
			if(atts.getValue(TYPE).equals("start")){
				currentTourBuilder.scheduleStart(currentVehicle.getLocation());
			}
			else if(atts.getValue(TYPE).equals("pickup")){
				String id = atts.getValue(SHIPMENTID);
				Shipment s = currentShipments.get(id);
				currentTourBuilder.schedulePickup(s);
			}
			else if(atts.getValue(TYPE).equals("delivery")){
				String id = atts.getValue(SHIPMENTID);
				Shipment s = currentShipments.get(id);
				currentTourBuilder.scheduleDelivery(s);
			}
			else if(atts.getValue(TYPE).equals("end")){
				currentTourBuilder.scheduleEnd(currentVehicle.getLocation());
			}
			else {
				String durationString = atts.getValue("duration");
				Double duration = null;
				if(durationString != null){
					duration = Double.parseDouble(durationString);
				}
				currentTourBuilder.scheduleGeneralActivity(atts.getValue(TYPE), makeId(atts.getValue(LINKID)), duration);
			}
		}
	}

	@Override
	public void endTag(String name, String content, Stack<String> context) {
		if(name.equals(VEHICLE)){
			
		}
		if(name.equals(VEHICLES)){
			
		}
		if(name.equals("carrier")){
			carriers.add(currentCarrier);
		}
		if(name.equals("tours")){
			currentPlan = new CarrierPlan(scheduledTours);
			currentCarrier.setSelectedPlan(currentPlan);
		}
		if(name.equals("tour")){
			Tour tour = currentTourBuilder.build();
			ScheduledTour sTour = new ScheduledTour(tour, currentVehicle, currentStartTime);
			scheduledTours.add(sTour);
		}
		if(name.equals("carrier")){
			
		}
		
	}

	private int getInt(String value) {
		return Integer.parseInt(value);
	}



	private Id makeId(String value) {
		return new IdImpl(value);
	}
	
	

}
