package org.matsim.core.mobsim.qsim.qnetsimengine;

import java.util.Collection;

import matsimConnector.agents.Pedestrian;
import matsimConnector.engine.CAAgentFactory;
import matsimConnector.environment.TransitionArea;
import matsimConnector.events.CAAgentConstructEvent;
import matsimConnector.scenario.CAEnvironment;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.mobsim.qsim.interfaces.MobsimVehicle;
import org.matsim.vis.snapshotwriters.VisData;

public class QCALink extends AbstractQLink {

	private final Link link;
	private final QNetwork qNetwork;
	//private QLinkInternalI qLink;
	private final CAEnvironment environmentCA;
	private final CAAgentFactory agentFactoryCA;
	private final TransitionArea transitionArea;

	public QCALink(Link link, QNetwork network, QLinkInternalI qLink, CAEnvironment environmentCA, CAAgentFactory agentFactoryCA, TransitionArea transitionArea) {
		super(link, network);
		this.link = link;
		//this.qLink = qLink;
		this.qNetwork = network;
		this.environmentCA = environmentCA;
		this.agentFactoryCA = agentFactoryCA;
		this.transitionArea = transitionArea;
	}
	
	public Id<Link> getLinkId(){
		return link.getId();
	}

	@Override
	public Link getLink() {
		// TODO Auto-generated method stub
		return link;
	}

	@Override
	public void recalcTimeVariantAttributes(double time) {
		// NOT NEEDED
		throw new RuntimeException("Method not needed for the moment");
	}

	@Override
	public Collection<MobsimVehicle> getAllNonParkedVehicles() {
		// NOT NEEDED
		throw new RuntimeException("Method not needed for the moment");
	}

	@Override
	public VisData getVisData() {
		// NOT NEEDED
		throw new RuntimeException("Method not needed for the moment");
	}

	@Override
	QNode getToNode() {
		// NOT NEEDED
		throw new RuntimeException("Method not needed for the moment");
	}

	@Override
	boolean doSimStep(double now) {
		// NOT NEEDED
		throw new RuntimeException("Method not needed for the moment");
	}

	@Override
	void addFromUpstream(QVehicle veh) {
		Pedestrian pedestrian = this.agentFactoryCA.buildPedestrian(environmentCA.getId(),veh,transitionArea);		
				
		double now = this.qNetwork.simEngine.getMobsim().getSimTimer().getTimeOfDay();
		this.qNetwork.simEngine.getMobsim().getEventsManager().processEvent(new LinkEnterEvent(
				now, veh.getId(), this.getLink().getId()));
		this.qNetwork.simEngine.getMobsim().getEventsManager().processEvent(new CAAgentConstructEvent(
				now, pedestrian));
	}

	public void notifyMoveOverBorderNode(QVehicle vehicle, Id<Link> nextLinkId){
		double now = this.qNetwork.simEngine.getMobsim().getSimTimer().getTimeOfDay();
		network.simEngine.getMobsim().getEventsManager().processEvent(new LinkLeaveEvent(
				now, vehicle.getId(), this.link.getId()));
		network.simEngine.getMobsim().getEventsManager().processEvent(new LinkEnterEvent(
				now, vehicle.getId(), nextLinkId));
	}
	
	@Override
	boolean isNotOfferingVehicle() {
		// NOT NEEDED
		throw new RuntimeException("Method not needed for the moment");
	}
	
	@Override
	public QVehicle popFirstVehicle() {		
		// NOT NEEDED
		throw new RuntimeException("Method not needed for the moment");
	}

	@Override
	public QVehicle getFirstVehicle() {
		// NOT NEEDED
		throw new RuntimeException("Method not needed for the moment");
	}

	@Override
	double getLastMovementTimeOfFirstVehicle() {
		// NOT NEEDED
		throw new RuntimeException("Method not needed for the moment");
	}

	@Override
	boolean hasGreenForToLink(Id<Link> toLinkId) {
		// NOT NEEDED
		throw new RuntimeException("Method not needed for the moment");
	}

	@Override
	boolean isAcceptingFromUpstream() {
		return transitionArea.acceptPedestrians();
	}

	public TransitionArea getTransitionArea() {
		return transitionArea;
	}

}
