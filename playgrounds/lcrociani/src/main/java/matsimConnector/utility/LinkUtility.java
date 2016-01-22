package matsimConnector.utility;

import java.util.Set;

import matsimConnector.environment.TransitionArea;
import matsimConnector.scenario.CAEnvironment;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.mobsim.qsim.qnetsimengine.QCALink;
import org.matsim.core.mobsim.qsim.qnetsimengine.QVehicle;

import pedCA.environment.markers.FinalDestination;

public class LinkUtility {
	public static void initLink(Link link, double length, Set<String> modes){
		link.setLength(length);
		link.setFreespeed(Constants.PEDESTRIAN_SPEED);
		
		//TODO FIX THE FLOW CAPACITY
		double width = Constants.FAKE_LINK_WIDTH;
		double cap = length*Constants.FLOPW_CAP_PER_METER_WIDTH;
		//double cap = width*Constants.FLOPW_CAP_PER_METER_WIDTH;
		link.setCapacity(cap);
		link.setAllowedModes(modes);
	}

	public static void initLink(Link link, double length, int lanes,Set<String> modes){
		initLink(link, length, modes);
		link.setNumberOfLanes(lanes);
	}
	
	public static int getTransitionAreaWidth(Node borderNode, CAEnvironment environmentCA){
		int destinationId = IdUtility.nodeIdToDestinationId(borderNode.getId());
		FinalDestination tacticalDestination = (FinalDestination)environmentCA.getContext().getMarkerConfiguration().getDestination(destinationId);
		return (int)(tacticalDestination.getWidth()/Constants.CA_CELL_SIDE);
	}
	
	public static double getTransitionLinkWidth(Link link, CAEnvironment environmentCA){
		int destinationId = IdUtility.linkIdToDestinationId(link.getId());
		return ((FinalDestination)environmentCA.getContext().getMarkerConfiguration().getDestination(destinationId)).getWidth();
	}
	
	public static int getTransitionAreaWidth(Link link, CAEnvironment env) {
		return (int)(getTransitionLinkWidth(link, env)/Constants.CA_CELL_SIDE);
	}
	
	public static TransitionArea getDestinationTransitionArea(QVehicle vehicle){
		Node borderNode = vehicle.getCurrentLink().getFromNode();
		for (Link link : borderNode.getInLinks().values())
			if (link instanceof QCALink)
				return ((QCALink)link).getTransitionArea();
		
		throw new RuntimeException("QCALink not found!!!");
	}
}
