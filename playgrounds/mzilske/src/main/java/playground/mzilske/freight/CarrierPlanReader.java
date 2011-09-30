package playground.mzilske.freight;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.utils.io.MatsimXmlParser;
import org.xml.sax.Attributes;


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
	
	private static final String CAPACITY = "cap";
	
	public Carrier currentCarrier = null;
	
	public CarrierVehicle currentVehicle = null;
	
	public TourBuilder currentTourBuilder = null;
	
	public Double currentStartTime = null; 
	
	public Map<String,CarrierShipment> currentShipments = null;
	
	public Map<String,CarrierVehicle> vehicles = null;
	
	public Collection<ScheduledTour> scheduledTours = null;
	
	public CarrierPlan currentPlan = null;
	
	public Collection<Carrier> carriers;
	
	public CarrierPlanReader(Collection<Carrier> carriers) {
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
			currentShipments = new HashMap<String, CarrierShipment>();
		}
		if(name.equals(SHIPMENT)){
			Id from = makeId(atts.getValue(FROM));
			Id to = makeId(atts.getValue(TO));
			int size = getInt(atts.getValue(SIZE));
			String startPickup = atts.getValue("startPickup");
			String endPickup = atts.getValue("endPickup");
			String startDelivery = atts.getValue("startDelivery");
			String endDelivery = atts.getValue("endDelivery");
			CarrierShipment shipment = null;
			if(startPickup == null ){
				shipment = CarrierUtils.createShipment(from, to, size, 0, 24*3600, 0, 24*3600);
			}
			else{
				shipment = CarrierUtils.createShipment(from, to, size, getDouble(startPickup), getDouble(endPickup), getDouble(startDelivery), getDouble(endDelivery));
			}
			currentShipments.put(atts.getValue(ID), shipment);
			CarrierUtils.createAndAddContract(currentCarrier, shipment, new CarrierOffer());
		}
	
		if(name.equals(VEHICLES)){
			vehicles = new HashMap<String,CarrierVehicle>();
		}
		if(name.equals(VEHICLE)){
			String vId = atts.getValue(ID);
			String linkId = atts.getValue(LINKID);
			String capacity = atts.getValue(CAPACITY);
			Integer cap = null;
			if(capacity != null){
				cap = getInt(capacity);
				if(cap == 0){
					logger.warn("vehicle " + vId + " capacity is 0.");
				}
			}
			else{
				throw new IllegalStateException("no capacity available");
			}
			CarrierVehicle vehicle = CarrierUtils.createAndAddVehicle(currentCarrier, vId, linkId, cap);
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
				CarrierShipment s = currentShipments.get(id);
				currentTourBuilder.schedulePickup(s);
			}
			else if(atts.getValue(TYPE).equals("delivery")){
				String id = atts.getValue(SHIPMENTID);
				CarrierShipment s = currentShipments.get(id);
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

	private double getDouble(String value) {
		return Double.parseDouble(value);
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
