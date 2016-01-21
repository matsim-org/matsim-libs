/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */
package playground.agarwalamit.mixedTraffic.FDTestSetUp;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.ConfigWriter;
import org.matsim.core.config.groups.QSimConfigGroup.LinkDynamics;
import org.matsim.core.config.groups.QSimConfigGroup.SnapshotStyle;
import org.matsim.core.config.groups.QSimConfigGroup.TrafficDynamics;
import org.matsim.core.config.groups.VspExperimentalConfigGroup.VspDefaultsCheckingLevel;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.network.NetworkWriter;
import org.matsim.core.network.VariableIntervalTimeVariantLinkFactory;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;

import playground.agarwalamit.mixedTraffic.MixedTrafficVehiclesUtils;

/**
 * @author amit
 */
public class InputsForFDTestSetUp {
	static final int SUBDIVISION_FACTOR = 1; //all sides of the triangle will be divided into subdivisionFactor links
	static final double LINK_LENGTH = 1000;//in m, length of one the triangle sides.
	static final double NO_OF_LANES = 1;
	static final String HOLE_SPEED = "15";
	static final double MAX_ACT_END_TIME = 1800.0; // agents departs randomly between 0 and MAX_ACT_END_TIME

	private final double LINK_CAPACITY = 2700; //in PCU/h
	private final double END_TIME = 24*3600;
	private final double FREESPEED = 60.;	//in km/h, maximum authorized velocity on the track
	private double stuckTime = 10;

	private LinkDynamics linkDynamics = LinkDynamics.FIFO;
	private TrafficDynamics trafficDynamics = TrafficDynamics.queue;

	private String [] travelModes;
	private Double[] modalSplitInPCU;
	private Scenario scenario;
	private boolean isTimeDependentNetwork ;

	private Map<Id<VehicleType>, TravelModesFlowDynamicsUpdator> vehicle2TravelModesData;

	void run(){

		if (travelModes.length != modalSplitInPCU.length){
			throw new RuntimeException("Modal split for each travel mode is necessray parameter, it is not defined correctly. Check your static variable!!! \n Aborting ...");
		}

		stuckTime = this.travelModes.length==1 && (this.travelModes[0]=="car" || this.travelModes[0]=="truck") ? 60 : 10;
		setUpConfig();
		createTriangularNetwork();
		fillTravelModeData();
	}

	private void setUpConfig(){
		GenerateFundamentalDiagramData.LOG.info("==========Creating config ============");
		Config config = ConfigUtils.createConfig();

		config.qsim().setMainModes(Arrays.asList(this.travelModes));
		config.qsim().setStuckTime(stuckTime);//allows to overcome maximal density regime
		config.qsim().setEndTime(END_TIME);

		config.qsim().setLinkDynamics(linkDynamics.toString());
		GenerateFundamentalDiagramData.LOG.info("==========The chosen link dynamics is "+linkDynamics+". =========="); 

		config.qsim().setTrafficDynamics(trafficDynamics);
		GenerateFundamentalDiagramData.LOG.info("==========The chosen traffic dynamics is "+trafficDynamics+". ==========");

		config.qsim().setSnapshotStyle(SnapshotStyle.queue);
		
		if(trafficDynamics.equals(TrafficDynamics.withHoles)) {
			config.qsim().setSnapshotStyle(SnapshotStyle.withHoles); // to see holes in OTFVis
			config.setParam("WITH_HOLE", "HOLE_SPEED", HOLE_SPEED);
		}

		if(linkDynamics.equals(LinkDynamics.SeepageQ)){
			config.qsim().setSeepMode("bike");
			config.qsim().setSeepModeStorageFree(false);
			config.qsim().setRestrictingSeepage(true);
		}

		config.vspExperimental().setVspDefaultsCheckingLevel( VspDefaultsCheckingLevel.abort );
		scenario = ScenarioUtils.createScenario(config);
	}

	/**
	 * It will generate a triangular network. 
	 * Each link is subdivided in number of sub division factor.
	 */
	private void createTriangularNetwork(){
		GenerateFundamentalDiagramData.LOG.info("==========Creating network=========");
		Network network = scenario.getNetwork();

		if(isTimeDependentNetwork) {
			scenario.getConfig().network().setTimeVariantNetwork(true);
			NetworkImpl netImpl = (NetworkImpl) scenario.getNetwork();
			netImpl.getFactory().setLinkFactory( new VariableIntervalTimeVariantLinkFactory() );
		}

		//nodes of the equilateral triangle base starting, left node at (0,0)
		for (int i = 0; i<SUBDIVISION_FACTOR+1; i++){
			double x=0, y=0;
			x = (LINK_LENGTH/SUBDIVISION_FACTOR)*i;
			Coord coord = new Coord(x, y);
			Id<Node> id = Id.createNodeId(i);

			Node node = scenario.getNetwork().getFactory().createNode(id, coord);
			network.addNode(node);	
		}
		//nodes of the triangle right side
		for (int i = 0; i<SUBDIVISION_FACTOR; i++){
			double x = LINK_LENGTH - ((LINK_LENGTH/SUBDIVISION_FACTOR))*Math.cos(Math.PI/3)*(i+1);
			double y = (LINK_LENGTH/SUBDIVISION_FACTOR)*Math.sin(Math.PI/3)*(i+1);
			Coord coord = new Coord(x, y);
			Id<Node> id = Id.createNodeId(SUBDIVISION_FACTOR+i+1);

			Node node = scenario.getNetwork().getFactory().createNode(id, coord);
			network.addNode(node);
		}
		//nodes of the triangle left side
		for (int i = 0; i<SUBDIVISION_FACTOR-1; i++){
			double x = LINK_LENGTH/2 - (LINK_LENGTH / SUBDIVISION_FACTOR)*Math.cos(Math.PI/3)*(i+1);
			double y = Math.tan(Math.PI/3)*x;
			Coord coord = new Coord(x, y);
			Id<Node> id = Id.createNodeId(2*SUBDIVISION_FACTOR+i+1);

			Node node = scenario.getNetwork().getFactory().createNode(id, coord);
			network.addNode(node);
		}
		//additional startNode and endNode for home and work activities
		double x = -50.0;
		Coord coord = new Coord(x, 0.0);
		Node startNode = scenario.getNetwork().getFactory().createNode(Id.createNodeId("home"), coord);
		network.addNode(startNode);

		coord = new Coord(LINK_LENGTH + 50.0, 0.0);
		Id<Node> endNodeId = Id.createNodeId("work");
		Node endNode = scenario.getNetwork().getFactory().createNode(endNodeId, coord);
		network.addNode(endNode);

		Set<String> allowedModes = new HashSet<>(Arrays.asList(this.travelModes));

		// triangle links
		for (int i = 0; i<3*SUBDIVISION_FACTOR; i++){
			Id<Node> idFrom = Id.createNodeId(i);
			Id<Node> idTo;
			if (i != 3*SUBDIVISION_FACTOR-1)
				idTo = Id.createNodeId(i+1);
			else
				idTo = Id.createNodeId(0);
			Node from = network.getNodes().get(idFrom);
			Node to = network.getNodes().get(idTo);

			Link link =scenario.getNetwork().getFactory().createLink(Id.createLinkId(i), from, to);
			link.setCapacity(LINK_CAPACITY);
			link.setFreespeed(FREESPEED/3.6);
			link.setLength(LINK_LENGTH);
			link.setNumberOfLanes(NO_OF_LANES);
			link.setAllowedModes(allowedModes);

			network.addLink(link);
		}

		//additional startLink and endLink for home and work activities
		Id<Link> startLinkId = Id.createLinkId("home");
		Link startLink = scenario.getNetwork().getFactory().createLink( startLinkId, startNode, scenario.getNetwork().getNodes().get(Id.createNodeId(0)));
		startLink.setCapacity(100*LINK_CAPACITY);
		startLink.setFreespeed(FREESPEED);
		startLink.setLength(25.);
		startLink.setNumberOfLanes(1.);
		startLink.setAllowedModes(allowedModes);
		network.addLink(startLink);

		Id<Link> endLinkId = Id.createLinkId("work");
		Link endLink = scenario.getNetwork().getFactory().createLink(endLinkId, scenario.getNetwork().getNodes().get(Id.createNodeId(SUBDIVISION_FACTOR)), endNode);
		endLink.setCapacity(100*LINK_CAPACITY);
		endLink.setFreespeed(FREESPEED);
		endLink.setLength(25.);
		endLink.setNumberOfLanes(1.);
		endLink.setAllowedModes(allowedModes);
		network.addLink(endLink);
	}

	private void fillTravelModeData(){
		vehicle2TravelModesData = new HashMap<>();
		for (int i=0; i < this.travelModes.length; i++){
			Id<VehicleType> modeId = Id.create(this.travelModes[i],VehicleType.class);
			VehicleType vehicleType = VehicleUtils.getFactory().createVehicleType(modeId);
			vehicleType.setPcuEquivalents(MixedTrafficVehiclesUtils.getPCU(this.travelModes[i]));
			vehicleType.setMaximumVelocity(MixedTrafficVehiclesUtils.getSpeed(this.travelModes[i]));
			TravelModesFlowDynamicsUpdator modeData = new TravelModesFlowDynamicsUpdator(vehicleType,this.travelModes.length);
			vehicle2TravelModesData.put(modeId, modeData);
		}
	}

	Scenario getScenario(){
		return scenario;
	}

	Map<Id<VehicleType>, TravelModesFlowDynamicsUpdator> getTravelMode2FlowDynamicsData(){
		return vehicle2TravelModesData;
	}

	void dumpInputFiles(String dir){
		new ConfigWriter(scenario.getConfig()).write(dir+"/config.xml");
		new NetworkWriter(scenario.getNetwork()).write(dir+"/network.xml");
	}

	public void setLinkDynamics(LinkDynamics linkDynamic) {
		linkDynamics = linkDynamic;
	}

	public void setTrafficDynamics(TrafficDynamics trafficDynamic){
		trafficDynamics = trafficDynamic;
	}

	public String[] getTravelModes() {
		return travelModes;
	}

	public void setTravelModes(String[] travelModes) {
		this.travelModes = travelModes;
	}

	public Double[] getModalSplit(){
		return this.modalSplitInPCU;
	}

	public void setTimeDependentNetwork(boolean isTimeDependentNetwork) {
		this.isTimeDependentNetwork = isTimeDependentNetwork;
	}

	public boolean isTimeDependentNetwork() {
		return isTimeDependentNetwork;
	}

	public void setModalSplit(String [] modalSplit) {
		this.modalSplitInPCU = new Double [modalSplit.length];
		for (int ii = 0; ii <modalSplit.length; ii ++){
			this.modalSplitInPCU [ii] = Double.valueOf(modalSplit[ii]);
		}
	}
}