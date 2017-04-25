package playground.pbouman.transitfares;

import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.HashSet;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.PersonLeavesVehicleEvent;
import org.matsim.api.core.v01.events.PersonMoneyEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.TransitDriverStartsEvent;
import org.matsim.api.core.v01.events.VehicleEntersTrafficEvent;
import org.matsim.api.core.v01.events.VehicleLeavesTrafficEvent;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.api.core.v01.events.handler.PersonLeavesVehicleEventHandler;
import org.matsim.api.core.v01.events.handler.TransitDriverStartsEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleEntersTrafficEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleLeavesTrafficEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.controler.events.AfterMobsimEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.AfterMobsimListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.events.algorithms.Vehicle2DriverEventHandler;
import org.matsim.core.events.handler.EventHandler;
import org.matsim.core.scenario.MutableScenario;

import playground.pbouman.transitfares.FarePolicies.DiscountInterval;

public class FareHandler implements
										EventHandler,
										PersonEntersVehicleEventHandler,
										PersonLeavesVehicleEventHandler,
										LinkLeaveEventHandler,
										TransitDriverStartsEventHandler,
										AfterMobsimListener,
										StartupListener,
										VehicleLeavesTrafficEventHandler,
										VehicleEntersTrafficEventHandler

{

//	public static final double DEFAULT_FIXED_PRICE = 0.01;
//	public static final double DEFAULT_UNIT_PRICE = 0.01;
	
	private FarePolicies policies;
	
//	private LinkedList<DiscountInterval> discountIntervals;
//	private LinkedList<PricingPolicy> pricingPolicies;
	private MutableScenario scenario;
//	private double transferGracePeriod;

	private HashMap<Id, Id> driverToVehicle;
	private HashMap<Id, Id> vehicleToDriver;
	private HashMap<Id, Id> vehicleToLine;
	private HashMap<Id, Id> vehicleToRoute;
	private HashMap<Id, HashSet<Id>> vehicleToPassengers;
	
	private HashMap<Id, DiscountInterval> agentDiscount;
	private HashMap<Id, Double> lastCheckinTime;
	private HashMap<Id, Double> lastCheckoutTime;
	private HashMap<Id<Person>, Double> agentTotalFare;
	private HashMap<Id, Double> agentCurrentDistance;
	private HashMap<Id, Double> agentTotalDistance;
	
	private String output;
	
	private Vehicle2DriverEventHandler delegate = new Vehicle2DriverEventHandler() ;
	
	@Override
	public void handleEvent(VehicleEntersTrafficEvent event) {
		delegate.handleEvent(event);
	}

	@Override
	public void handleEvent(VehicleLeavesTrafficEvent event) {
		delegate.handleEvent(event);
	}

	public FareHandler(Scenario s)
	{
		this(s,null);
	}
	
	public FareHandler(Scenario s, String oFile)
	{
		scenario = (MutableScenario) s;
		reset(0);
		output = oFile;
	}
	
	@Override
	public void handleEvent(PersonLeavesVehicleEvent event)
	{
		
		Id<Person> personId = event.getPersonId();
		Id vid = event.getVehicleId();
		Id lid = vehicleToLine.get(vid);
		Id rid = vehicleToRoute.get(vid);
		
		// Determine the fixed and the distance based pricing
		//double fixedPrice = DEFAULT_FIXED_PRICE;
		//double distPrice = DEFAULT_UNIT_PRICE;
		
		double fixedPrice = policies.getPolicyFixedPrice(lid.toString(),rid.toString());
		double distPrice = policies.getPolicyDistancePrice(lid.toString(),rid.toString());
		
		//for (PricingPolicy pp : pricingPolicies)
		//{
		//	if (pp.appliesTo(type,lid.toString(),rid.toString()))
		//	{
		//		fixedPrice = pp.fixedPrice;
		//		distPrice = pp.kmPrice;
		//	}
		//}
		
		// Remove this passenger from the vehicle
		vehicleToPassengers.get(vid).remove(personId);
		
		double price = 0;
		// Add the distance based pricing
		price += agentCurrentDistance.get(personId) / 1000 * distPrice;
		// If there is no transfer period, or if it is too large, add the fixed price
		double transferTime = Double.POSITIVE_INFINITY;
		if (lastCheckoutTime.containsKey(personId))
			transferTime = lastCheckoutTime.get(personId) - lastCheckinTime.get(personId);
		if (transferTime > policies.getTransferGracePeriod())
			price += fixedPrice;
		// If a discount applies, apply it
		DiscountInterval discount = agentDiscount.get(event.getPersonId());
		if (discount != null )
			price = price * (1-discount.discount);
		// Add the price to the total fare for the agent 
		if (!agentTotalFare.containsKey(personId))
			agentTotalFare.put(personId, price);
		else
			agentTotalFare.put(personId, price + agentTotalFare.get(personId));

		// Store the new checkout time for this passenger
		// During the next checkout, this time is relevant to determine the transfer-gap
		lastCheckoutTime.put(personId, event.getTime());
		
	}

	@Override
	public void handleEvent(PersonEntersVehicleEvent event)
	{
		Id personId = event.getPersonId();
		Id vehicleId = event.getVehicleId();
		Id lineId = vehicleToLine.get(vehicleId);
		Id routeId = vehicleToRoute.get(vehicleId);

		// The travel distance for this current journey is clearly 0 at the moment
		agentCurrentDistance.put(personId, 0d);
		
		// Add the passenger to the set of passengers in the vehicle
		vehicleToPassengers.get(vehicleId).add(personId);
		
		
		// Remove the old discount for the passenger and check whether a discount currently applies
		agentDiscount.remove(personId);
		DiscountInterval interval = policies.getDiscountInterval(event.getTime(), lineId.toString(), routeId.toString());
		if (interval != null)
			agentDiscount.put(personId, interval);
		
	//	for (DiscountInterval di : discountIntervals)
	//		if (di.matches(event.getTime(), type, lineId.toString(), routeId.toString()))
	//			agentDiscount.put(personId, di);

		// Store the checkin time for this passenger
		lastCheckinTime.put(event.getPersonId(), event.getTime());
		
		
	}

	@Override
	public void handleEvent(LinkLeaveEvent event)
	{
		// Add the length of the link to the distances of all passengers in the vehicle
		// the driver is 
		
		Id<Person> driverId = delegate.getDriverOfVehicle(event.getVehicleId()) ;
		
		// yyyyyy this might now be going from one data structure to another and back ... I am just brute-force fixing.  kai, mar'17
		
		if (driverToVehicle.get( driverId ) != null)
//		if (driverToVehicle.get(event.getDriverId()) != null)
		{
			Link link = scenario.getNetwork().getLinks().get(event.getLinkId());
			Id vehicle = driverToVehicle.get(driverId);
			for (Id p : vehicleToPassengers.get(vehicle))
			{
				if (agentCurrentDistance.containsKey(p))
					agentCurrentDistance.put(p, link.getLength() + agentCurrentDistance.get(p));
				else
					agentCurrentDistance.put(p, link.getLength());
				
				if (agentTotalDistance.containsKey(p))
					agentTotalDistance.put(p,  link.getLength() + agentTotalDistance.get(p));
				else
					agentTotalDistance.put(p, link.getLength());				
			}
		}
	}

	@Override
	public void handleEvent(TransitDriverStartsEvent event)
	{
		/* We want to make sure a driver is unique to a vehicle and a vehicle has a single driver
		 * Therefore, we have two maps to check this 1:1 relation efficiently
		 */
		
		
		if (   vehicleToDriver.containsKey(event.getVehicleId())
			|| driverToVehicle.containsKey(event.getDriverId()))
			{
				vehicleToDriver.remove(event.getVehicleId());
				driverToVehicle.remove(event.getDriverId());
			}
		vehicleToDriver.put(event.getVehicleId(), event.getDriverId());
		driverToVehicle.put(event.getDriverId(), event.getVehicleId());
		
		if (!vehicleToPassengers.containsKey(event.getVehicleId()))
			vehicleToPassengers.put(event.getVehicleId(), new HashSet<Id>());
	
		vehicleToLine.put(event.getVehicleId(), event.getTransitLineId());
		vehicleToRoute.put(event.getVehicleId(), event.getTransitRouteId());
	}
	
	@Override
	public void notifyStartup(StartupEvent event)
	{
		event.getServices().getEvents().addHandler(this);
		MutableScenario scenario = (MutableScenario) event.getServices().getScenario();
		policies = new FarePolicies(scenario);
		scenario.addScenarioElement(FarePolicies.ELEMENT_NAME, policies);
	}
	
	@Override
	public void notifyAfterMobsim(AfterMobsimEvent event)
	{
		double rev = 0;
		
		for (Id<Person> personId : agentTotalFare.keySet())
		{
			double fare = agentTotalFare.get(personId);
			double factor = 1;
			AgentSensitivities as = (AgentSensitivities) scenario.getScenarioElement(AgentSensitivities.ELEMENT_NAME);
			if (as != null)
			{
				factor = as.getSensitivity(personId);
			}
			
			PersonMoneyEvent ame =  new PersonMoneyEvent(24*3600, personId, -(fare*factor));
			rev += fare;
			event.getServices().getEvents().processEvent(ame);
		}
		if (output != null)
		{
			try
			{
				PrintWriter pw = new PrintWriter(new FileOutputStream(output,true));
				pw.println("Iteration "+event.getIteration()+";"+rev);
				pw.flush();
				pw.close();
			}
			catch (Exception e) {}
		}
		
		reset(0);
	}

	
	@Override
	public void reset(int iteration)
	{
		agentDiscount = new HashMap<>();
		lastCheckinTime = new HashMap<>();
		lastCheckoutTime = new HashMap<>();
		agentTotalFare = new HashMap<>();
		agentCurrentDistance = new HashMap<>();
		agentTotalDistance = new HashMap<>();
		driverToVehicle = new HashMap<>();
		vehicleToDriver = new HashMap<>();
		vehicleToPassengers = new HashMap<>();
		vehicleToLine = new HashMap<>();
		vehicleToRoute = new HashMap<>();
	}
	
/*	private String getVehicleType(Id vehicleId)
	{
		String type = "";
		if (scenario instanceof ScenarioImpl)
		{
			ScenarioImpl si = (ScenarioImpl) scenario;
			VehicleType vt = si.getVehicles().getVehicles().get(vehicleId).getType();
			type = vt.getDescription();
		}
		return type;
	}
*/
}
