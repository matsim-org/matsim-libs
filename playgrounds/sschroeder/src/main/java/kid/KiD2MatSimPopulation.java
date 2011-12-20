/**
 * 
 */
package kid;

import kid.utils.KiDUtils;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.population.algorithms.PlanAlgorithm;

import utils.GeotoolsTransformation;

import com.vividsolutions.jts.geom.Coordinate;


/**
 * @author stefan
 *
 */
public class KiD2MatSimPopulation {
	
	static class KiDAgentPlanBuilder {
		
		private Plan plan;

		private boolean lastElementWasActivity = false;
		
		private boolean driverSet = false;
		
		private PlanAlgorithm router;
		
		public KiDAgentPlanBuilder(PlanAlgorithm router) {
			plan = new PlanImpl();
			this.router = router;
		}
		
		public void setDriverId(Id driverId) {
			plan.setPerson(new PersonImpl(driverId));
			driverSet = true;
		}

		public void scheduleActivity(String name, Id location, Integer depTime){
			if(lastElementWasActivity){
				throw new IllegalStateException("cannot schedule activity because last element has already been an activity");
			}
			Activity act = new ActivityImpl(name, location);
			if(depTime != null){
				act.setEndTime(depTime);
			}
			plan.addActivity(act);
			lastElementWasActivity = true;
		}
		
		public void scheduleLeg(){
			if(!lastElementWasActivity){
				throw new IllegalStateException("I must be an activity, but actually I am a leg");
			}
			plan.addLeg(new LegImpl(TransportMode.car));
			lastElementWasActivity = false;
		}
		
		public Plan build(){
			if(!driverSet){
				throw new IllegalStateException("driverId has not been set");
			}
			if(!lastElementWasActivity){
				throw new IllegalStateException("there is still an activity missing");
			}
			router.run(plan);
			return plan;
		}
	}
	
	static class ScheduledVehicleAgent {
		
		private static Logger logger = Logger.getLogger(ScheduledVehicleAgent.class);
		
		private ScheduledVehicle scheduledVehicle;
		
		private PlanAlgorithm router;
		
		private NetworkImpl network;
		
		GeotoolsTransformation transformation;

		public ScheduledVehicleAgent(ScheduledVehicle scheduledVehicle) {
			super();
			this.scheduledVehicle = scheduledVehicle;
		}

		public void setNetwork(NetworkImpl network) {
			this.network = network;
		}

		public void setRouter(PlanAlgorithm router) {
			this.router = router;
		}
		
		/**
		 * activity coordinates from kid-activities are assigned to network-links. thus, coordinate-system of network must be equal
		 * to coordinate-system of kid-activities. this transformation transforms kid-coordinate system to network-coordinate system.
		 * by default, kid-coordinates are in WGS84.
		 * 
		 * @param transformation
		 */
		public void setTransformation(GeotoolsTransformation transformation) {
			this.transformation = transformation;
		}

		public Plan createPlan(){
			KiDAgentPlanBuilder planBuilder = new KiDAgentPlanBuilder(router);
			planBuilder.setDriverId(scheduledVehicle.getVehicle().getId());
			TransportLeg lastLeg = null;
			Id lastActivityLocation = null;
			logger.debug("#tpChains="+ scheduledVehicle.getScheduledTransportChains().size());
			for(ScheduledTransportChain sTransportChain : scheduledVehicle.getScheduledTransportChains()){
				boolean firstLeg = true;
				for(TransportLeg leg : sTransportChain.getTransportLegs()){
					Id activityLocationLink = null;
					Integer depTime = KiDUtils.getDepartureTimeInSeconds(leg);
					if(depTime == null){
						throw new IllegalStateException("no depTime available");
					}
					if(firstLeg){
						Coordinate fromCoord = KiDUtils.getFromGeocode(leg);
						if(!KiDUtils.coordinateIsGeoCodable(fromCoord)){
							continue;
						}
						activityLocationLink = getLinkId(KiDUtils.getFromGeocode(leg));
						planBuilder.scheduleActivity("Start", activityLocationLink, depTime);
						firstLeg = false;
					}
					else{
						Coordinate toCoord = KiDUtils.getToGeocode(lastLeg);
						if(!KiDUtils.coordinateIsGeoCodable(toCoord)){
							continue;
						}
						activityLocationLink = getLinkId(KiDUtils.getToGeocode(lastLeg));
						planBuilder.scheduleActivity(KiDUtils.getActivity(lastLeg), activityLocationLink, depTime);
					}
					logger.debug("activityLocation=" + activityLocationLink + "; depTime=" + depTime);
					planBuilder.scheduleLeg();
					lastLeg = leg;
					lastActivityLocation = activityLocationLink;
				}
			}
			Coordinate toCoord = KiDUtils.getToGeocode(lastLeg);
			if(!KiDUtils.coordinateIsGeoCodable(toCoord)){
				planBuilder.scheduleActivity(KiDUtils.getActivity(lastLeg), lastActivityLocation, null);
				logger.debug("finalDest=" + lastActivityLocation + "; depTime=null");
			}
			else{
				Id finalDestinationLink = getLinkId(KiDUtils.getToGeocode(lastLeg));
				planBuilder.scheduleActivity(KiDUtils.getActivity(lastLeg), finalDestinationLink, null);
				logger.debug("finalDest=" + finalDestinationLink + "; depTime=null");
			}
			Plan plan = planBuilder.build();
			return plan;
		}
		
		

		private Id getLinkId(Coordinate coordinate){
			Coordinate transformedCoord = transformation.transform(coordinate);
			Coord coord = new CoordImpl(transformedCoord.x, transformedCoord.y);
			Link link = network.getNearestLink(coord);
			return link.getId();
		}
	}
	
	private static Logger logger = Logger.getLogger(KiD2MatSimPopulation.class);
	
	private ScheduledVehicles scheduledVehicles;
	
	private NetworkImpl network;
	
	public void setNetwork(NetworkImpl network) {
		this.network = network;
	}

	public void setRouter(PlanAlgorithm router) {
		this.router = router;
	}

	private PlanAlgorithm router;
	
	private Population population;
	
	private GeotoolsTransformation transformation;
	
	/**
	 * activity coordinates from kid-activities are assigned to network-links. thus, coordinate-system of network must be equal
	 * to coordinate-system of kid-activities. this transformation transforms kid-coordinate system to network-coordinate system.
	 * by default, kid-coordinates are in WGS84.
	 * 
	 * @param transformation
	 */
	public void setTransformation(GeotoolsTransformation transformation) {
		this.transformation = transformation;
	}

	public KiD2MatSimPopulation(Population population, ScheduledVehicles scheduledVehicles) {
		super();
		this.scheduledVehicles = scheduledVehicles;
		this.population = population;
		verify();
	}
	
	private void verify() {
		if(population == null || scheduledVehicles == null){
			throw new IllegalStateException("population or scheduledVehicles is null. this cannot be.");
		}
	}

	public Population createPopulation(){
		logger.info("create plan agents for " + scheduledVehicles.getScheduledVehicles().values().size() + " agents");
		int counter = 1;
		int nextInfo = 2;
		for(ScheduledVehicle scheduledVehicle : scheduledVehicles.getScheduledVehicles().values()){
			if(counter == nextInfo){
				logger.info("vehicles " + counter);
				nextInfo *= 2;
			}
			ScheduledVehicleAgent vehicleAgent = new ScheduledVehicleAgent(scheduledVehicle);
			vehicleAgent.setTransformation(transformation);
			vehicleAgent.setNetwork(network);
			vehicleAgent.setRouter(router);
			Plan plan = vehicleAgent.createPlan();
			Person person = new PersonImpl(scheduledVehicle.getVehicle().getId());
			person.addPlan(plan);
			population.addPerson(person);
			counter++;
		}
		return population;
	}
	
}
