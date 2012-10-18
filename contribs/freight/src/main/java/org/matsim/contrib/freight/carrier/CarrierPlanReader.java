package org.matsim.contrib.freight.carrier;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.contrib.freight.carrier.Tour.Leg;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.population.routes.LinkNetworkRouteImpl;
import org.matsim.core.utils.io.MatsimXmlParser;
import org.matsim.core.utils.misc.NetworkUtils;
import org.matsim.core.utils.misc.Time;
import org.xml.sax.Attributes;

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

	private static final String CAPACITY = "cap";

	private static final String VEHICLESTART = "earliestStart";

	private static final String VEHICLEEND = "latestEnd";

	private Carrier currentCarrier = null;

	private CarrierVehicle currentVehicle = null;

	private ScheduledTourBuilder currentTourBuilder = null;

	private Double currentStartTime = null;

	private Leg currentLeg = null;

	private Id previousActLoc = null;

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

	public CarrierPlanReader(Carriers carriers) {
		super();
		this.carriers = carriers;
	}

	public void read(String filename) {

		logger.info("read carrier plans");
		this.setValidating(false);
		parse(filename);
		logger.info("done");

	}

	@Override
	public void startTag(String name, Attributes atts, Stack<String> context) {
		if (name.equals(CARRIER)) {
			currentCarrier = new CarrierFactory().createCarrier(
					atts.getValue(ID), atts.getValue(LINKID));
		}
		if (name.equals(SHIPMENTS)) {
			currentShipments = new HashMap<String, CarrierShipment>();
		}
		if (name.equals(SHIPMENT)) {
			String from = atts.getValue(FROM);
			String to = atts.getValue(TO);
			int size = getInt(atts.getValue(SIZE));
			String startPickup = atts.getValue("startPickup");
			String endPickup = atts.getValue("endPickup");
			String startDelivery = atts.getValue("startDelivery");
			String endDelivery = atts.getValue("endDelivery");
			String pickupServiceTime = atts.getValue("pickupServiceTime");
			String deliveryServiceTime = atts.getValue("deliveryServiceTime");
			CarrierShipment shipment = null;
			if (startPickup == null) {
				shipment = new CarrierFactory().createShipment(from, to, size,
						0, 24 * 3600, 0, 24 * 3600);
			} else {
				shipment = new CarrierFactory().createShipment(from, to, size,
						getDouble(startPickup), getDouble(endPickup),
						getDouble(startDelivery), getDouble(endDelivery));
			}
			if (pickupServiceTime == null) {
				shipment.setPickupServiceTime(0.0);
			} else {
				shipment.setPickupServiceTime(getDouble(pickupServiceTime));
			}
			if (deliveryServiceTime == null) {
				shipment.setDeliveryServiceTime(0.0);
			} else {
				shipment.setDeliveryServiceTime(getDouble(deliveryServiceTime));
			}
			currentShipments.put(atts.getValue(ID), shipment);
			currentCarrier.getShipments().add(shipment);
		}

		if (name.equals(VEHICLES)) {
			vehicles = new HashMap<String, CarrierVehicle>();
		}
		if (name.equals(VEHICLE)) {
			String vId = atts.getValue(ID);
			String linkId = atts.getValue(LINKID);
			String capacity = atts.getValue(CAPACITY);
			String startTime = atts.getValue(VEHICLESTART);
			String endTime = atts.getValue(VEHICLEEND);
			Integer cap = getInt(capacity);
			String typeId = atts.getValue("typeId");
			if (typeId == null) {
				logger.warn("no vehicle type. set type='default' -> defaultVehicleType (see CarrierVehicleTypeImpl)");
				typeId = "default";
			}
			CarrierVehicle vehicle = new CarrierFactory().createAndAddVehicle(
					currentCarrier, vId, linkId, cap, typeId);
			if (startTime == null) {
				vehicle.setEarliestStartTime(0.0);
			} else {
				vehicle.setEarliestStartTime(getDouble(startTime));
			}
			if (endTime == null) {
				vehicle.setLatestEndTime(Double.MAX_VALUE);
			} else {
				vehicle.setLatestEndTime(getDouble(endTime));
			}
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
			currentTourBuilder = new ScheduledTourBuilder(currentVehicle);
		}
		if (name.equals("leg")) {
			currentLegDepTime = getDouble(atts.getValue("dep_time"));
			currentLegTransTime = getDouble(atts.getValue("transp_time"));
		}
		if (name.equals(ACTIVITY)) {
			if (atts.getValue(TYPE).equals("start")) {
				currentTourBuilder.scheduleStart(getDouble(atts
						.getValue("end_time")));
				previousActLoc = currentVehicle.getLocation();
			} else if (atts.getValue(TYPE).equals("pickup")) {
				String id = atts.getValue(SHIPMENTID);
				CarrierShipment s = currentShipments.get(id);
				finishLeg(s.getFrom());
				currentTourBuilder.schedulePickup(s,
						getDouble(atts.getValue("end_time")));
				previousActLoc = s.getFrom();
			} else if (atts.getValue(TYPE).equals("delivery")) {
				String id = atts.getValue(SHIPMENTID);
				CarrierShipment s = currentShipments.get(id);
				finishLeg(s.getTo());
				currentTourBuilder.scheduleDelivery(s,
						getDouble(atts.getValue("end_time")));
				previousActLoc = s.getTo();
			} else if (atts.getValue(TYPE).equals("end")) {
				finishLeg(currentVehicle.getLocation());
				currentTourBuilder.scheduleEnd();
			}
		}
	}

	private void finishLeg(Id toLocation) {
		LinkNetworkRouteImpl route = null;
		if (previousRouteContent != null) {
			List<Id> linkIds = NetworkUtils.getLinkIds(previousRouteContent);
			route = new LinkNetworkRouteImpl(previousActLoc, toLocation);
			if (!linkIds.isEmpty()) {
				route.setLinkIds(previousActLoc, linkIds, toLocation);
			}
		}
		currentTourBuilder.scheduleLeg(route, currentLegDepTime,
				currentLegTransTime);
		currentLeg = null;
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
			ScheduledTour sTour = currentTourBuilder.build();
			scheduledTours.add(sTour);
		}
	}

	private int getInt(String value) {
		return Integer.parseInt(value);
	}

	private Id makeId(String value) {
		return new IdImpl(value);
	}

}
