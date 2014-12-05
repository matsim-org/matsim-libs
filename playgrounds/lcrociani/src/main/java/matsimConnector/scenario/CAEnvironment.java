package matsimConnector.scenario;


import java.util.HashMap;
import java.util.Map;

import matsimConnector.environment.TransitionArea;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;

import pedCA.context.Context;
import pedCA.environment.markers.FinalDestination;
import pedCA.environment.network.CANetwork;

public class CAEnvironment {
	private Id<CAEnvironment> id;
	private Context context;
	private Network network;
	private Map <Id<Link>,TransitionArea> transitionAreas;
	
	public CAEnvironment(String id, Context context){
		this(Id.create(id,CAEnvironment.class),context);
	}
	
	public CAEnvironment(Id<CAEnvironment> id, Context context){
		this.id = id;
		this.context = context;
		this.transitionAreas = new HashMap<Id<Link>,TransitionArea>();
	}

	public Id<CAEnvironment> getId(){
		return id;
	}
	
	public Context getContext(){
		return context;
	}
	
	public CANetwork getCANetwork(){
		return context.getNetwork();
	}
	
	public void setNetwork(Network network){
		this.network = network;
	}
	
	public Network getNetwork(){
		return network;
	}
	
	public void addTransitionArea(Id<Link> linkId, TransitionArea transitionArea){
		transitionAreas.put(linkId, transitionArea);
	}
	
	public Map<Id<Link>,TransitionArea> getTransitionAreas(){
		return transitionAreas;
	}
	
	public FinalDestination getDestination(int destinationId){
		return (FinalDestination) getContext().getMarkerConfiguration().getDestination(destinationId);
	}

}
