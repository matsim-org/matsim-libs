package kid;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.population.algorithms.PlanAlgorithm;

import com.vividsolutions.jts.geom.Coordinate;


public class ScheduledVehicleAgent {
	
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
		AgentPlanBuilder planBuilder = new AgentPlanBuilder(router);
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
					activityLocationLink = getLinkId(KiDUtils.getFromGeocode(leg));
					planBuilder.scheduleActivity("Start", activityLocationLink, depTime);
					firstLeg = false;
				}
				else{
					activityLocationLink = getLinkId(KiDUtils.getToGeocode(lastLeg));
					planBuilder.scheduleActivity(KiDUtils.getActivity(lastLeg), activityLocationLink, depTime);
				}
				logger.debug("activityLocation=" + activityLocationLink + "; depTime=" + depTime);
				planBuilder.scheduleLeg();
				lastLeg = leg;
				lastActivityLocation = activityLocationLink;
			}
		}
		Id finalDestinationLink = getLinkId(KiDUtils.getToGeocode(lastLeg));
		planBuilder.scheduleActivity(KiDUtils.getActivity(lastLeg), finalDestinationLink, null);
		logger.debug("finalDest=" + finalDestinationLink + "; depTime=null");
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
