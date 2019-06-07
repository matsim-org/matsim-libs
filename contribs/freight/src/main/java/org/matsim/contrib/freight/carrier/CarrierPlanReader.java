package org.matsim.contrib.freight.carrier;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import org.apache.log4j.Logger;
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
public class CarrierPlanReader extends MatsimXmlParser {

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

	private static final String VEHICLESTART = "earliestStart";

	private static final String VEHICLEEND = "latestEnd";

	private Carrier currentCarrier = null;

	private CarrierVehicle currentVehicle = null;

	private Tour.Builder currentTourBuilder = null;

	private Double currentStartTime = null;

	private Id<Link> previousActLoc = null;

	private String previousRouteContent;

	public Map<String, CarrierShipment> currentShipments = null;

	public Map<String, CarrierVehicle> vehicles = null;

	public Collection<ScheduledTour> scheduledTours = null;

	public CarrierPlan currentPlan = null;
	
	public Double currentScore;
	
	public boolean selected;

	public Carriers carriers;

	private double currentLegTransTime;

	private double currentLegDepTime;

	/**
	 * Constructs a reader with an empty carriers-container for the carriers to be constructed. 
	 * 
	 * @param carriers which is a map that stores carriers
	 */
	public CarrierPlanReader(Carriers carriers) {
		super();
		this.carriers = carriers;
		this.setValidating(false);
	}

//	/**
//	 * Reads a xml-file that contains carriers and their plans.
//	 * 
//	 * <p> Builds carriers and plans, and stores them in the carriers-object coming with this constructor.
//	 * 
//	 * @param filename
//	 */
//	public void read(String filename) {
//		logger.info("read carrier plans");
//		this.setValidating(false);
//		read(filename);
//		logger.info("done");
//
//	}
	
	@Override
	public void startTag(String name, Attributes atts, Stack<String> context) {
		if (name.equals(CARRIER)) {
			String id = atts.getValue(ID);
			currentCarrier = CarrierImpl.newInstance(Id.create(id, Carrier.class)); 
		}
		if (name.equals(SHIPMENTS)) {
			currentShipments = new HashMap<String, CarrierShipment>();
		}
		if (name.equals(SHIPMENT)) {
			String id = atts.getValue(ID);
			String from = atts.getValue(FROM);
			String to = atts.getValue(TO);
			int size = getInt(atts.getValue(SIZE));
			String startPickup = atts.getValue("startPickup");
			String endPickup = atts.getValue("endPickup");
			String startDelivery = atts.getValue("startDelivery");
			String endDelivery = atts.getValue("endDelivery");
			String pickupServiceTime = atts.getValue("pickupServiceTime");
			String deliveryServiceTime = atts.getValue("deliveryServiceTime");
			CarrierShipment.Builder shipmentBuilder = CarrierShipment.Builder.newInstance(Id.create(id, CarrierShipment.class), Id.create(from, Link.class), Id.create(to, Link.class), size);
			if (startPickup == null) {
				shipmentBuilder.setPickupTimeWindow(TimeWindow.newInstance(0.0, Integer.MAX_VALUE)).setDeliveryTimeWindow(TimeWindow.newInstance(0.0, Integer.MAX_VALUE));
			} else {
				shipmentBuilder.setPickupTimeWindow(TimeWindow.newInstance(getDouble(startPickup), getDouble(endPickup))).
						setDeliveryTimeWindow(TimeWindow.newInstance(getDouble(startDelivery), getDouble(endDelivery)));
			}
			if (pickupServiceTime != null) shipmentBuilder.setPickupServiceTime(getDouble(pickupServiceTime)); 
			if (deliveryServiceTime != null) shipmentBuilder.setDeliveryServiceTime(getDouble(deliveryServiceTime));
			CarrierShipment shipment = shipmentBuilder.build();
			currentShipments.put(atts.getValue(ID), shipment);
			currentCarrier.getShipments().add(shipment);
		}

		if (name.equals(VEHICLES)) {
			vehicles = new HashMap<String, CarrierVehicle>();
		}
		if (name.equals(VEHICLE)) {
			String vId = atts.getValue(ID);
			String linkId = atts.getValue(LINKID);
			String startTime = atts.getValue(VEHICLESTART);
			String endTime = atts.getValue(VEHICLEEND);
			String typeId = atts.getValue("typeId");
			if (typeId == null) {
				logger.warn("no vehicle type. set type='default' -> defaultVehicleType (see CarrierVehicleTypeImpl)");
				typeId = "default";
			}
			CarrierVehicle.Builder vehicleBuilder = CarrierVehicle.Builder.newInstance(Id.create(vId, Vehicle.class), Id.create(linkId, Link.class));
			vehicleBuilder.setTypeId(Id.create(typeId, VehicleType.class));
			vehicleBuilder.setType(CarrierVehicleType.Builder.newInstance(Id.create(typeId, VehicleType.class)).build());
			if (startTime != null) vehicleBuilder.setEarliestStart(getDouble(startTime));
			if (endTime != null) vehicleBuilder.setLatestEnd(getDouble(endTime));
			CarrierVehicle vehicle = vehicleBuilder.build();
			currentCarrier.getCarrierCapabilities().getCarrierVehicles().add(vehicle);
			vehicles.put(vId, vehicle);
		}
		if(name.equals("plan")){
			String score = atts.getValue("score");
			if(score != null){
				currentScore = getDouble(score);
			}
			else{
				currentScore = null;
			}
			String selected = atts.getValue("selected");
			if(selected == null ) {
				this.selected = false;
			}
			else if(selected.equals("true")){
				this.selected = true;
			}
			else{
				this.selected = false;
			}
			scheduledTours = new ArrayList<ScheduledTour>();
		}
		if (name.equals("tour")) {
			String vehicleId = atts.getValue("vehicleId");
			currentVehicle = vehicles.get(vehicleId);
			currentTourBuilder = Tour.Builder.newInstance();
		}
		if (name.equals("leg")) {
			currentLegDepTime = getDouble(atts.getValue("dep_time"));
			currentLegTransTime = getDouble(atts.getValue("transp_time"));
		}
		if (name.equals(ACTIVITY)) {
			if (atts.getValue(TYPE).equals("start")) {
				currentStartTime = getDouble(atts.getValue("end_time"));
				previousActLoc = currentVehicle.getLocation();
				currentTourBuilder.scheduleStart(currentVehicle.getLocation(),TimeWindow.newInstance(currentVehicle.getEarliestStartTime(), currentVehicle.getLatestEndTime()));
			} else if (atts.getValue(TYPE).equals("pickup")) {
				String id = atts.getValue(SHIPMENTID);
				CarrierShipment s = currentShipments.get(id);
				finishLeg(s.getFrom());
				currentTourBuilder.schedulePickup(s);
				previousActLoc = s.getFrom();
			} else if (atts.getValue(TYPE).equals("delivery")) {
				String id = atts.getValue(SHIPMENTID);
				CarrierShipment s = currentShipments.get(id);
				finishLeg(s.getTo());
				currentTourBuilder.scheduleDelivery(s);
				previousActLoc = s.getTo();
			} else if (atts.getValue(TYPE).equals("end")) {
				finishLeg(currentVehicle.getLocation());
				currentTourBuilder.scheduleEnd(currentVehicle.getLocation(), TimeWindow.newInstance(currentVehicle.getEarliestStartTime(),currentVehicle.getLatestEndTime()));
			}
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
			currentPlan = new CarrierPlan(currentCarrier, scheduledTours);
			currentPlan.setScore(currentScore);
			currentCarrier.getPlans().add(currentPlan);
			if(this.selected){
				currentCarrier.setSelectedPlan(currentPlan);
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
