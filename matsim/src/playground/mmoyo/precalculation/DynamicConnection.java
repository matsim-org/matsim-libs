package playground.mmoyo.precalculation;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.network.NodeImpl;
import org.matsim.core.population.ActivityImpl;
import org.matsim.transitSchedule.api.TransitSchedule;

import playground.mmoyo.PTRouter.PTValues;

/**Converts a staticConnection into leg*/
public class DynamicConnection {

	private double departureTime;
	private double walkDistance1= Double.NEGATIVE_INFINITY;
	private double walkDistance2= Double.NEGATIVE_INFINITY;
	private double walkTime1 =  Double.NEGATIVE_INFINITY;
	private double walkTime2 =  Double.NEGATIVE_INFINITY;
	private double avgWalkSpeed = new PTValues().AV_WALKING_SPEED;
	private TransitSchedule transitSchedule;
	private PlainTimeTable plainTimeTable;
	private Map <Coord, Collection<NodeImpl>> nearStopMap; 
	private Map <Id, List<StaticConnection>> connectionMap;
	private NetworkLayer net;

	public DynamicConnection(TransitSchedule transitSchedule, NetworkLayer net, Map <Coord, Collection<NodeImpl>> nearStopMap, Map <Id, List<StaticConnection>> connectionMap ) {
		this.transitSchedule =  transitSchedule;
		this.net = net;
		this.nearStopMap = nearStopMap; 
		this.connectionMap = connectionMap;
		this.plainTimeTable = new PlainTimeTable(this.transitSchedule);
	}

	public void findConnection(ActivityImpl act1, ActivityImpl act2, StaticConnection staticConnection ){
		Collection<NodeImpl> nearStops1 = nearStopMap.get(act1.getCoord());		
		Collection<NodeImpl> nearStops2 = nearStopMap.get(act2.getCoord());
		
		for (PTtrip ptTrip : staticConnection.getTripList()){
			//Leg leg = createLeg(act1.getEndTime(), ptTrip);
			
			//create Activity: a transfer consist in waiting the next PT vehicle
			//Id firstLinkId= leg.getRoute().getStartLinkId();
			//ActivityImpl act = new ActivityImpl("wait pt", transitSchedule.getFacilities().get(ptTrip.getBoardFacilityId()).getCoord(), net.getLink(firstLinkId));
		
		}
	}
		
	public void  convertToLegs(StaticConnection staticConnection){
		//public List<Leg> convertToLegs(StaticConnection staticConnection){
		//->must return a lest of legs.  Already with ptActivities??
		
		
		for (PTtrip ptTrip : staticConnection.getTripList()){
			/*
			Leg leg = createLeg(act1.getEndTime(), ptTrip);
			//create Activity: a transfer consist in waiting the next PT vehicle
			Id firstLinkId= leg.getRoute().getStartLinkId();
			ActivityImpl act = new ActivityImpl("wait pt", transitSchedule.getFacilities().get(ptTrip.getBoardFacilityId()).getCoord(), net.getLink(firstLinkId));
			*/
		}
	}

	
	
}

