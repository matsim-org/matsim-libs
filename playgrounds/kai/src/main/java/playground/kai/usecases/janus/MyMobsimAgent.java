///* *********************************************************************** *
// * project: org.matsim.*												   *
// *                                                                         *
// * *********************************************************************** *
// *                                                                         *
// * copyright       : (C) 2008 by the members listed in the COPYING,        *
// *                   LICENSE and WARRANTY file.                            *
// * email           : info at matsim dot org                                *
// *                                                                         *
// * *********************************************************************** *
// *                                                                         *
// *   This program is free software; you can redistribute it and/or modify  *
// *   it under the terms of the GNU General Public License as published by  *
// *   the Free Software Foundation; either version 2 of the License, or     *
// *   (at your option) any later version.                                   *
// *   See also COPYING, LICENSE and WARRANTY file                           *
// *                                                                         *
// * *********************************************************************** */
//package playground.kai.usecases.janus;
//
//import org.apache.log4j.Logger;
//import org.janusproject.kernel.agent.Kernels;
//import org.matsim.api.core.v01.Id;
//import org.matsim.api.core.v01.TransportMode;
//import org.matsim.api.core.v01.network.Link;
//import org.matsim.api.core.v01.network.Network;
//import org.matsim.api.core.v01.network.Node;
//import org.matsim.api.core.v01.population.Person;
//import org.matsim.core.mobsim.framework.MobsimDriverAgent;
//import org.matsim.core.mobsim.qsim.interfaces.MobsimVehicle;
//import org.matsim.vehicles.Vehicle;
//
//import java.util.ArrayList;
//import java.util.Collections;
//import java.util.List;
//
//class MyMobsimAgent implements MobsimDriverAgent {
//
//	Id<Link> currentLinkId ;
//	private MobsimVehicle vehicle;
//	private final Network network;
//	private final JanusAgent janusAgent;
//
//	MyMobsimAgent(Network network) {
//		this.network = network ;
//		this.currentLinkId = new ArrayList<Link>( network.getLinks().values() ).get(0).getId() ;
//		// (start on some link)
//
//		this.janusAgent = new JanusAgent() ;
//		Kernels.get().launchLightAgent(janusAgent);
//	}
//
//	@Override
//	public Id<Link> getCurrentLinkId() {
//		return currentLinkId ;
//	}
//
//	@Override
//	public Id<Link> getDestinationLinkId() {
//		return null ; // agent does not know where it is heading
//	}
//
//	@Override
//	public Id<Person> getId() {
//		return Id.create("janusAgent", Person.class) ;
//	}
//
//	@Override
//	public State getState() {
//		return State.LEG ; // agent always wants to be under way
//	}
//
//	@Override
//	public double getActivityEndTime() {
//		return 0 ; // this is a dummy which fakes the initial activity.  Not sure why this is needed at all ...
//	}
//
//	@Override
//	public void endActivityAndComputeNextState(double now) {
//		throw new RuntimeException("not implemented"); // should never be called since agent never arrives
//	}
//
//	@Override
//	public void endLegAndComputeNextState(double now) {
//		throw new RuntimeException("not implemented"); // should never be called since agent never arrives
//	}
//
//	@Override
//	public void setStateToAbort(double now) {
//		try {
//			this.janusAgent.waitUntilTermination();
//		} catch (InterruptedException e) {
//			e.printStackTrace();
//		}
//	}
//
//	@Override
//	public Double getExpectedTravelTime() {
//		throw new RuntimeException("not implemented"); // should not be needed since agent does not teleport
//	}
//
//    @Override
//    public Double getExpectedTravelDistance() {
//        // TODO Auto-generated method stub
//        throw new RuntimeException("not implemented"); // should not be needed since agent does not teleport
//    }
//
//    @Override
//	public String getMode() {
//		return TransportMode.car ;
//	}
//
//	@Override
//	public void notifyArrivalOnLinkByNonNetworkMode(Id<Link> linkId) {
//		this.currentLinkId = linkId ;
//	}
//
//	@Override
//	public void setVehicle(MobsimVehicle veh) {
//		this.vehicle = veh ;
//	}
//
//	@Override
//	public MobsimVehicle getVehicle() {
//		return this.vehicle ;
//	}
//
//	@Override
//	public Id<Vehicle> getPlannedVehicleId() {
//		return Id.create("janusVehicle", Vehicle.class) ;
//	}
//
//	@Override
//	public Id<Link> chooseNextLinkId() {
//		// select a random outgoing link as an example.  Would need to be replaced by BDI input.
//		Node node = network.getLinks().get( currentLinkId ).getToNode() ;
//		List<Link> outgoingLinks = new ArrayList<Link>(node.getOutLinks().values() );
//		Collections.shuffle(outgoingLinks);
//		return outgoingLinks.get(0).getId() ;
//	}
//
//	@Override
//	public void notifyMoveOverNode(Id<Link> newLinkId) {
//		this.currentLinkId = newLinkId ;
//		Logger.getLogger(this.getClass()).warn(" just moved over node; currentLinkId: " + this.currentLinkId ) ;
//	}
//
//	@Override
//	public boolean isWantingToArriveOnCurrentLink() {
//		return false ; // this agent never wants to arrive
//	}
//
//
//}
