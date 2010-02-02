package playground.christoph.withinday;

import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.population.routes.NetworkRouteWRefs;
import org.matsim.core.population.routes.NodeNetworkRouteImpl;
import org.matsim.core.utils.misc.RouteUtils;
import org.matsim.ptproject.qsim.DriverAgent;
import org.matsim.ptproject.qsim.QSim;
import org.matsim.ptproject.qsim.QVehicle;

import playground.christoph.events.ExtendedAgentReplanEventImpl;
import playground.christoph.withinday.mobsim.WithinDayPersonAgent;
import playground.christoph.withinday.replanning.WithinDayDuringLegReplanner;

public class ReplannerYoungPeople extends WithinDayDuringLegReplanner {

	private Network network;

	@Override
	public WithinDayDuringLegReplanner clone() {
		return this;
	}
	
	public ReplannerYoungPeople(Id id, Network network)
	{
		super(id);
		this.network = network;
	}

	@Override
	public boolean doReplanning(DriverAgent driverAgent) {
		// If we don't have a valid Replanner.
		if (this.planAlgorithm == null) return false;
		
		// If we don't have a valid WithinDayPersonAgent
		if (driverAgent == null) return false;
		
		WithinDayPersonAgent withinDayPersonAgent = null;
		if (!(driverAgent instanceof WithinDayPersonAgent)) return false;
		else
		{
			withinDayPersonAgent = (WithinDayPersonAgent) driverAgent;
		}
		
		PersonImpl person = (PersonImpl)withinDayPersonAgent.getPerson();
		PlanImpl selectedPlan = (PlanImpl)person.getSelectedPlan(); 
		
		// If we don't have a selected plan
		if (selectedPlan == null) return false;
				
		Leg currentLeg = driverAgent.getCurrentLeg();
		Activity nextActivity = selectedPlan.getNextActivity(currentLeg);
		
		
		ActivityImpl newWorkAct=new ActivityImpl("w",new IdImpl("22"));
		newWorkAct.setDuration(3600);
		
		
		
				
				
		// If it is not a car Leg we don't replan it.
		if (!currentLeg.getMode().equals(TransportMode.car)) return false;

		/*
		 * Get the index and the currently next Node on the route.
		 * Entries with a lower index have already been visited!
		 */
		Leg currentLegClone = new LegImpl((LegImpl)currentLeg);
		int currentNodeIndex = withinDayPersonAgent.getCurrentNodeIndex();
		NetworkRouteWRefs route = (NetworkRouteWRefs) currentLegClone.getRoute();

		QVehicle vehicle = withinDayPersonAgent.getVehicle();
		
		/* 
		 * If the Vehicle is already on the destination Link we don't need any
		 * further Replanning.
		 */
		if (vehicle.getCurrentLink().getId().equals(route.getEndLinkId())) return true;
		

		// create dummy data for the "new" activities
		String type = "w";
		// This would be the "correct" Type - but it is slower and is not necessary
		//String type = this.plan.getPreviousActivity(leg).getType();

		ActivityImpl newFromAct = new ActivityImpl(type, vehicle.getCurrentLink().getToNode().getCoord(), vehicle.getCurrentLink());

		newFromAct.setStartTime(time);
		newFromAct.setEndTime(time);

		NodeNetworkRouteImpl subRoute = new NodeNetworkRouteImpl(vehicle.getCurrentLink(), route.getEndLink());

		// put the new route in a new leg
		Leg newLeg = new LegImpl(currentLeg.getMode());
		newLeg.setDepartureTime(time);
//		newLeg.setTravelTime(currentLeg.getTravelTime());
//		newLeg.setRoute(subRoute);

		Activity nextAct=selectedPlan.getNextActivity(currentLeg);
		
		Leg homeLeg=selectedPlan.getNextLeg(nextAct);
		Activity homeAct=selectedPlan.getNextActivity(homeLeg);
		
		
		
		
		// create new plan and select it
		PlanImpl newPlan = new PlanImpl(person);
		newPlan.addActivity(newFromAct);
		newPlan.addLeg(newLeg);
		newPlan.addActivity(newWorkAct);
		newPlan.addLeg(homeLeg);
		newPlan.addActivity(homeAct);
		
		this.planAlgorithm.run(newPlan);
		
		int legPos=selectedPlan.getActLegIndex(currentLeg);
		
		// backup homeleg Route
		Route backupRoute = homeLeg.getRoute();
		
		selectedPlan.removeLeg(legPos);
		
		// restore Route
		homeLeg.setRoute(backupRoute);
		
			// get new calculated Route
		NetworkRouteWRefs newRoute = (NetworkRouteWRefs) newLeg.getRoute();

		// use VehicleId from existing Route
		newRoute.setVehicleId(vehicle.getId());

		// get Nodes from the current Route
		List<Node> nodesBuffer = RouteUtils.getNodes(route, this.network);
				
		// remove Nodes after the current Position in the Route
		nodesBuffer.subList(currentNodeIndex - 1, nodesBuffer.size()).clear();
		
		// Merge already driven parts of the Route with the new routed parts.
		nodesBuffer.addAll(RouteUtils.getNodes(newRoute, this.network));

		// Update Route by replacing the Nodes.
		route.setNodes(route.getStartLink(), nodesBuffer, route.getEndLink());

		// Update Distance
		double distance = 0.0;
		for (Id id : route.getLinkIds())
		{
			distance = distance + this.network.getLinks().get(id).getLength();
		}
		distance = distance + this.network.getLinks().get(route.getEndLinkId()).getLength(); 
		route.setDistance(distance);
		
		// con't 
		selectedPlan.insertLegAct(legPos, (LegImpl) currentLegClone, newWorkAct);
		

		
		// finally reset the cached next Link of the PersonAgent - it may have changed!
		withinDayPersonAgent.resetCachedNextLink();

		return true;
	}

}
