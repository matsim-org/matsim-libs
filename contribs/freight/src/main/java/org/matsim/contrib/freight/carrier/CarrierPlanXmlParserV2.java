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
import org.matsim.contrib.freight.carrier.CarrierCapabilities.Builder;
import org.matsim.contrib.freight.carrier.CarrierCapabilities.FleetSize;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.core.utils.io.MatsimXmlParser;
import org.matsim.core.utils.misc.Time;
import org.matsim.vehicles.EngineInformation;
import org.matsim.vehicles.EngineInformation.FuelType;
import org.matsim.vehicles.EngineInformationImpl;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.xml.sax.Attributes;

class CarrierPlanXmlParserV2 extends MatsimXmlParser {
	
	public static Logger logger = Logger.getLogger(CarrierPlanXmlParserV2.class);

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
	
	
	private Builder capabilityBuilder;

	private org.matsim.contrib.freight.carrier.CarrierVehicleType.Builder vehicleTypeBuilder;

	private Map<Id<VehicleType>, CarrierVehicleType> vehicleTypeMap = new HashMap<>();

	private double currentStartTime;
	
	private Map<Id<CarrierService>, CarrierService> serviceMap;

	/**
	 * Constructs a reader with an empty carriers-container for the carriers to be constructed. 
	 * 
	 * @param carriers which is a map that stores carriers
	 */
	public CarrierPlanXmlParserV2(Carriers carriers) {
		super();
		this.carriers = carriers;
	}

	@Override
	public void startTag(String name, Attributes atts, Stack<String> context) {
		if (name.equals(CARRIER)) {
			String id = atts.getValue(ID);
			if(id == null) throw new IllegalStateException("carrierId is missing.");
			currentCarrier = CarrierImpl.newInstance(Id.create(id, Carrier.class));
		}
		//services
		else if (name.equals("services")) {
			serviceMap = new HashMap<>();
		}
		else if (name.equals("service")) {
			String idString = atts.getValue("id");
			if(idString == null) throw new IllegalStateException("service.id is missing.");
			Id<CarrierService> id = Id.create(idString, CarrierService.class);
			String toLocation = atts.getValue("to");
			if(toLocation == null) throw new IllegalStateException("service.to is missing. ");
			Id<Link> to = Id.create(toLocation, Link.class);
			CarrierService.Builder serviceBuilder = CarrierService.Builder.newInstance(id, to);
			String capDemandString = atts.getValue("capacityDemand");
			if(capDemandString != null) serviceBuilder.setCapacityDemand(getInt(capDemandString));
			String startString = atts.getValue("earliestStart");
			double start = parseTimeToDouble(startString);
			double end = Double.MAX_VALUE;
			String endString = atts.getValue("latestEnd");
			end = parseTimeToDouble(endString);
			serviceBuilder.setServiceStartTimeWindow(TimeWindow.newInstance(start, end));
			String serviceTimeString = atts.getValue("serviceDuration");
			if(serviceTimeString != null) serviceBuilder.setServiceDuration(parseTimeToDouble(serviceTimeString));
			CarrierService service = serviceBuilder.build();
			serviceMap.put(service.getId(), service);
			currentCarrier.getServices().add(service);
		}
		
		//shipments
		else if (name.equals(SHIPMENTS)) {
			currentShipments = new HashMap<String, CarrierShipment>();
		}
		else if (name.equals(SHIPMENT)) {
			String from = atts.getValue(FROM);
			if(from == null) throw new IllegalStateException("shipment.from is missing.");
			String to = atts.getValue(TO);
			if(to == null) throw new IllegalStateException("shipment.to is missing.");
			String sizeString = atts.getValue(SIZE);
			if(sizeString == null) throw new IllegalStateException("shipment.size is missing.");
			int size = getInt(sizeString);
			CarrierShipment.Builder shipmentBuilder = CarrierShipment.Builder.newInstance(Id.create(from, Link.class), Id.create(to, Link.class), size);
			
			String startPickup = atts.getValue("startPickup");
			String endPickup = atts.getValue("endPickup");
			String startDelivery = atts.getValue("startDelivery");
			String endDelivery = atts.getValue("endDelivery");
			String pickupServiceTime = atts.getValue("pickupServiceTime");
			String deliveryServiceTime = atts.getValue("deliveryServiceTime");
			
			if (startPickup != null && endPickup != null) shipmentBuilder.setPickupTimeWindow(TimeWindow.newInstance(parseTimeToDouble(startPickup), parseTimeToDouble(endPickup)));
			if(startDelivery != null && endDelivery != null) shipmentBuilder.setDeliveryTimeWindow(TimeWindow.newInstance(parseTimeToDouble(startDelivery), parseTimeToDouble(endDelivery)));
			if (pickupServiceTime != null) shipmentBuilder.setPickupServiceTime(parseTimeToDouble(pickupServiceTime)); 
			if (deliveryServiceTime != null) shipmentBuilder.setDeliveryServiceTime(parseTimeToDouble(deliveryServiceTime));
			
			CarrierShipment shipment = shipmentBuilder.build();
			currentShipments.put(atts.getValue(ID), shipment);
			currentCarrier.getShipments().add(shipment);
		}
		
		//capabilities
		else if(name.equals("capabilities")){
			String fleetSize = atts.getValue("fleetSize");
			if(fleetSize == null) throw new IllegalStateException("fleetSize is missing.");
			this.capabilityBuilder = CarrierCapabilities.Builder.newInstance();
			if(fleetSize.toUpperCase().equals(FleetSize.FINITE.toString())){ 
				this.capabilityBuilder.setFleetSize(FleetSize.FINITE);
			}
			else {
				this.capabilityBuilder.setFleetSize(FleetSize.INFINITE);
			}
		}
		
		//vehicle-type
		else if(name.equals("vehicleType")){
			String typeId = atts.getValue("id");
			if(typeId == null) throw new IllegalStateException("vehicleTypeId is missing.");
			this.vehicleTypeBuilder = CarrierVehicleType.Builder.newInstance(Id.create(typeId, VehicleType.class)); 
		}
		else if(name.equals("engineInformation")){
			String fuelType = atts.getValue("fuelType");
			String gasConsumption = atts.getValue("gasConsumption");
			EngineInformation engineInfo = new EngineInformationImpl(parseFuelType(fuelType), Double.parseDouble(gasConsumption));
			this.vehicleTypeBuilder.setEngineInformation(engineInfo);
		}
		else if(name.equals("costInformation")){
			String fix = atts.getValue("fix");
			String perMeter = atts.getValue("perMeter");
			String perSecond = atts.getValue("perSecond");
			if(fix != null) this.vehicleTypeBuilder.setFixCost(Double.parseDouble(fix));
			if(perMeter != null) this.vehicleTypeBuilder.setCostPerDistanceUnit(Double.parseDouble(perMeter));
			if(perSecond != null) this.vehicleTypeBuilder.setCostPerTimeUnit(Double.parseDouble(perSecond));
		}
		
		//vehicle
		else if (name.equals(VEHICLES)) {
			vehicles = new HashMap<String, CarrierVehicle>();
		}
		else if (name.equals(VEHICLE)) {
			String vId = atts.getValue(ID);
			if(vId == null) throw new IllegalStateException("vehicleId is missing.");
			String depotLinkId = atts.getValue("depotLinkId");
			if(depotLinkId == null) throw new IllegalStateException("depotLinkId of vehicle is missing.");
			CarrierVehicle.Builder vehicleBuilder = CarrierVehicle.Builder.newInstance(Id.create(vId, Vehicle.class), Id.create(depotLinkId, Link.class));
			String typeId = atts.getValue("typeId");
			if(typeId == null) throw new IllegalStateException("vehicleTypeId is missing.");
			CarrierVehicleType vehicleType = vehicleTypeMap.get(Id.create(typeId, VehicleType.class));
			vehicleBuilder.setTypeId(Id.create(typeId, VehicleType.class));
			if(vehicleType != null) vehicleBuilder.setType(vehicleType);
			String startTime = atts.getValue(VEHICLESTART);
			if(startTime != null) vehicleBuilder.setEarliestStart(parseTimeToDouble(startTime));
			String endTime = atts.getValue(VEHICLEEND);
			if(endTime != null) vehicleBuilder.setLatestEnd(parseTimeToDouble(endTime));
			
			CarrierVehicle vehicle = vehicleBuilder.build();
			capabilityBuilder.addVehicle(vehicle);
			vehicles.put(vId, vehicle);
		}
		
		//plans
		else if(name.equals("plan")){
			String score = atts.getValue("score");
			if(score != null) currentScore = parseTimeToDouble(score);
			String selected = atts.getValue("selected");
			if(selected == null ) this.selected = false;
			else if(selected.equals("true")) this.selected = true;
			else this.selected = false;
			scheduledTours = new ArrayList<ScheduledTour>();
		}
		else if (name.equals("tour")) {
			String vehicleId = atts.getValue("vehicleId");
			if(vehicleId == null) throw new IllegalStateException("vehicleId is missing in tour.");
			currentVehicle = vehicles.get(vehicleId);
			if(currentVehicle == null) throw new IllegalStateException("vehicle to vehicleId " + vehicleId + " is missing.");
			currentTourBuilder = Tour.Builder.newInstance();
		}
		else if (name.equals("leg")) {
			String depTime = atts.getValue("expected_dep_time");
			if(depTime == null) depTime = atts.getValue("dep_time");
			if(depTime == null) throw new IllegalStateException("leg.expected_dep_time is missing.");
			currentLegDepTime = parseTimeToDouble(depTime);
			String transpTime = atts.getValue("expected_transp_time");
			if(transpTime == null) transpTime = atts.getValue("transp_time");
			if(transpTime == null) throw new IllegalStateException("leg.expected_transp_time is missing.");
			currentLegTransTime = parseTimeToDouble(transpTime);
		}
		else if (name.equals(ACTIVITY)) {
			String type = atts.getValue(TYPE);
			if(type == null) throw new IllegalStateException("activity type is missing");
			String actEndTime = atts.getValue("end_time");
			if (type.equals("start")) {
				if(actEndTime == null) throw new IllegalStateException("endTime of activity \"" + type + "\" missing.");
				currentStartTime = parseTimeToDouble(actEndTime);
				previousActLoc = currentVehicle.getLocation();
				currentTourBuilder.scheduleStart(currentVehicle.getLocation(),TimeWindow.newInstance(currentVehicle.getEarliestStartTime(), currentVehicle.getLatestEndTime()));
				
			} else if (type.equals("pickup")) {
				String id = atts.getValue(SHIPMENTID);
				if(id == null) throw new IllegalStateException("pickup.shipmentId is missing.");
				CarrierShipment s = currentShipments.get(id);
				finishLeg(s.getFrom());
				currentTourBuilder.schedulePickup(s);
				previousActLoc = s.getFrom();
			} else if (type.equals("delivery")) {
				String id = atts.getValue(SHIPMENTID);
				if(id == null) throw new IllegalStateException("delivery.shipmentId is missing.");
				CarrierShipment s = currentShipments.get(id);
				finishLeg(s.getTo());
				currentTourBuilder.scheduleDelivery(s);
				previousActLoc = s.getTo();
			} else if (type.equals("service")){
				String id = atts.getValue("serviceId");
				if(id == null) throw new IllegalStateException("act.serviceId is missing.");
				CarrierService s = serviceMap.get(Id.create(id, CarrierService.class));
				if(s == null) throw new IllegalStateException("serviceId is not known.");
				finishLeg(s.getLocationLinkId());
				currentTourBuilder.scheduleService(s);
				previousActLoc = s.getLocationLinkId();
			} else if (type.equals("end")) {
				finishLeg(currentVehicle.getLocation());
				currentTourBuilder.scheduleEnd(currentVehicle.getLocation(), TimeWindow.newInstance(currentVehicle.getEarliestStartTime(),currentVehicle.getLatestEndTime()));
			}
			
		}
	}
	
	@Override
	public void endTag(String name, String content, Stack<String> context) {
		if(name.equals("capabilities")){
			currentCarrier.setCarrierCapabilities(capabilityBuilder.build());
		}
		else if(name.equals("capacity")){
			if(content == null) throw new IllegalStateException("vehicle-capacity is missing.");
			vehicleTypeBuilder.setCapacity(Integer.parseInt(content));
		}
		else if(name.equals("vehicleType")){
			CarrierVehicleType type = vehicleTypeBuilder.build();
			vehicleTypeMap.put(type.getId(),type);
			capabilityBuilder.addType(type);
		}
		else if (name.equals("route")) {
			this.previousRouteContent = content;
		}
		
		else if (name.equals("carrier")) {
			carriers.getCarriers().put(currentCarrier.getId(), currentCarrier);
		}
		else if (name.equals("plan")) {
			currentPlan = new CarrierPlan(currentCarrier, scheduledTours);
			currentPlan.setScore(currentScore);
			currentCarrier.getPlans().add(currentPlan);
			if(this.selected){
				currentCarrier.setSelectedPlan(currentPlan);
			}
		}
		else if (name.equals("tour")) {
			ScheduledTour sTour = ScheduledTour.newInstance(currentTourBuilder.build(),currentVehicle,currentStartTime);
			scheduledTours.add(sTour);
		}
		else if(name.equals("description")){
			vehicleTypeBuilder.setDescription(content);
		}
	}

	private FuelType parseFuelType(String fuelType) {
		if(fuelType.equals(FuelType.diesel.toString())){
			return FuelType.diesel;
		}
		else if(fuelType.equals(FuelType.electricity.toString())){
			return FuelType.electricity;
		}
		else if(fuelType.equals(FuelType.gasoline.toString())){
			return FuelType.gasoline;
		}
		throw new IllegalStateException("fuelType " + fuelType + " is not supported");
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
		currentTourBuilder.addLeg(currentTourBuilder.createLeg(route, currentLegDepTime,currentLegTransTime));
		previousRouteContent = null;
	}

	private double parseTimeToDouble(String timeString) {
		if (timeString.contains(":")) {
			return Time.parseTime(timeString);
		} else {
			return Double.parseDouble(timeString);
		}

	}

	private int getInt(String value) {
		return Integer.parseInt(value);
	}

}
