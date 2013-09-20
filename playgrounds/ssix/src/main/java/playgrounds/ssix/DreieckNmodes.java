/* *********************************************************************** *
 * project: org.matsim.*
 * DreieckNModes													   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package playgrounds.ssix;

import java.io.IOException;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.config.groups.VspExperimentalConfigGroup;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.framework.MobsimDriverAgent;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.agents.AgentFactory;
import org.matsim.core.mobsim.qsim.interfaces.MobsimVehicle;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.PopulationUtils;
import org.matsim.vehicles.VehicleCapacity;
import org.matsim.vehicles.VehicleCapacityImpl;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;

//import

/**
 * @author ssix
 */

public class DreieckNmodes {

	private static final Logger log = Logger.getLogger(DreieckNmodes.class);
	
	private static class MyRoundAndRoundAgent implements MobsimDriverAgent{
		
		private MyPersonDriverAgentImpl delegate;
		public boolean goHome;
		
		public MyRoundAndRoundAgent(Person p, Plan unmodifiablePlan, QSim qSim) {
			this.delegate = new MyPersonDriverAgentImpl(p, PopulationUtils.unmodifiablePlan(p.getSelectedPlan()), qSim);
			this.goHome = false;//false at start, modified when all data is extracted.
			}

		public final void endActivityAndComputeNextState(double now) {
			delegate.endActivityAndComputeNextState(now);
		}

		public final void endLegAndComputeNextState(double now) {
			delegate.endLegAndComputeNextState(now);
		}

		public final void abort(double now) {
			delegate.abort(now);
		}

		public boolean equals(Object obj) {
			return delegate.equals(obj);
		}

		public final double getActivityEndTime() {
			return delegate.getActivityEndTime();
		}

		public final Id getCurrentLinkId() {
			return delegate.getCurrentLinkId();
		}

		public final Double getExpectedTravelTime() {
			return delegate.getExpectedTravelTime();
		}

		public final String getMode() {
			return delegate.getMode();
		}

		public final Id getDestinationLinkId() {
			return delegate.getDestinationLinkId();
		}

		public final Id getId() {
			return delegate.getId();
		}

		public State getState() {
			return delegate.getState();
		}

		public final void notifyArrivalOnLinkByNonNetworkMode(Id linkId) {
			delegate.notifyArrivalOnLinkByNonNetworkMode(linkId);
		}

		@Override
		public Id chooseNextLinkId() {
			if (!(goHome)){
				Id forcedLeftTurnLinkId = new IdImpl((long)(3*DreieckStreckeSzenario3modes.subdivisionFactor - 1));
				if (!(delegate.getCurrentLinkId().equals(forcedLeftTurnLinkId))){
					return delegate.chooseNextLinkId();
				}
				Id afterLeftTurnLinkId = new IdImpl((long)(0));
				delegate.setCachedNextLinkId(afterLeftTurnLinkId);
				delegate.setCurrentLinkIdIndex(0);
				return afterLeftTurnLinkId;
			}
			Id forcedStraightForwardLinkId = new IdImpl((long)(DreieckStreckeSzenario3modes.subdivisionFactor - 1));
			if (!(delegate.getCurrentLinkId().equals(forcedStraightForwardLinkId))){
				return delegate.chooseNextLinkId();
			}
			Id afterGoingStraightForwardLinkId = new IdImpl((long)(3*DreieckStreckeSzenario3modes.subdivisionFactor));
			delegate.setCachedNextLinkId(afterGoingStraightForwardLinkId);
			delegate.setCurrentLinkIdIndex(3*DreieckStreckeSzenario3modes.subdivisionFactor);//This does work quite well so far and allows to end simulation.
			return afterGoingStraightForwardLinkId;
		}

		@Override
		public void notifyMoveOverNode(Id newLinkId) {
			delegate.notifyMoveOverNode(newLinkId);
		}

		@Override
		public void setVehicle(MobsimVehicle veh) {
			delegate.setVehicle(veh);
		}

		@Override
		public MobsimVehicle getVehicle() {
			return delegate.getVehicle();
		}

		@Override
		public Id getPlannedVehicleId() {
			return delegate.getPlannedVehicleId();
		}
	}

	private static class MyAgentFactory implements AgentFactory {

		private QSim qSim;

		public MyAgentFactory(QSim qSim) {
		this.qSim = qSim;
		}

		@Override
		public MobsimAgent createMobsimAgentFromPerson(Person p) {
			MyRoundAndRoundAgent agent = new MyRoundAndRoundAgent(p, PopulationUtils.unmodifiablePlan(p.getSelectedPlan()), this.qSim);
			return agent;
		}

	}
	
	//CONFIGURATION: static variables used for aggregating configuration options
	public static int subdivisionFactor=3;//all sides of the triangle will be divided into subdivisionFactor links
	public static double length = 444.44;//in m, length of one the triangle sides.
	public static int NETWORK_CAPACITY = 2700;//in PCU/h
	private static String OUTPUT_DIR = "Z:\\WinHome\\Desktop\\workspace2\\playgrounds\\ssix\\output\\data_test.txt";
	private static String OUTPUT_EVENTS = "Z:\\WinHome\\Desktop\\workspace2\\playgrounds\\ssix\\output\\events_test.xml";
	
	private static double FREESPEED = 60.;						//in km/h, maximum authorized velocity on the track
	private static int NUMBER_OF_MODES = 3;
	private static String[] NAMES= {"bicycles","bikes","cars"};	//identification of the different modes
	private static Double[] Probabilities = {1/3., 1/3., 1/3.}; //modal split
	private static Double[] Pcus = {0.25, 0.25, 1.}; 			//PCUs of the different possible modes
	private static Double[] Speeds = {4.17, 16.67, 16.67};		//maximum velocities of the vehicle types, in m/s
	
	private PrintStream writer;
	private Scenario scenario;
	private Map<Id, ModeData> modesData;
	private int networkCapacity;//the capacity all links of the network will have
		
	
	public DreieckNmodes(int networkCapacity){
		this.networkCapacity = networkCapacity;
		
		//Checking that configuration data has the appropriate size:
		if (NAMES.length != NUMBER_OF_MODES){	throw new RuntimeException("There should be "+NUMBER_OF_MODES+" names for the different modes. Check your static variable NAMES!");}
		if (Probabilities.length != NUMBER_OF_MODES){ throw new RuntimeException("There should be "+NUMBER_OF_MODES+" probabilities for the different modes. Check your static variable Probabilities!");}
		if (Pcus.length != NUMBER_OF_MODES){ throw new RuntimeException("There should be "+NUMBER_OF_MODES+" PCUs for the different modes. Check your static variable Pcus!");}
		if (Speeds.length != NUMBER_OF_MODES){ throw new RuntimeException("There should be "+NUMBER_OF_MODES+" speeds for the different modes. Check your static variable Speeds!");}
		
		//Initializing scenario Config file
		Config config = ConfigUtils.createConfig();
		config.addQSimConfigGroup(new QSimConfigGroup());
		config.getQSimConfigGroup().setSnapshotStyle(QSimConfigGroup.SNAPSHOT_AS_QUEUE) ;
		config.getQSimConfigGroup().setMainModes(Arrays.asList(NAMES));
		config.getQSimConfigGroup().setStuckTime(100*3600.);//allows to overcome maximal density regime
		config.getQSimConfigGroup().setEndTime(14*3600);//allows to set agents to abort after getting the wanted data.
		//todo: is for actual network configurations correct, needs dependency on bigger network length 
		config.vspExperimental().addParam("vspDefaultsCheckingLevel", VspExperimentalConfigGroup.ABORT) ;
		// this may lead to abort during execution.  In such cases, please fix the configuration.  if necessary, talk
		// to me (kn).
		this.scenario = ScenarioUtils.createScenario(config);
		
		//Initializing modeData objects//TODO does not function well.
		this.modesData = new HashMap<Id, ModeData>();
		for (int i=0; i < NUMBER_OF_MODES; i++){
			Id modeId = new IdImpl(NAMES[i]);
			VehicleType vehicleType = VehicleUtils.getFactory().createVehicleType(modeId);
			vehicleType.setPcuEquivalents(Pcus[i]);
			vehicleType.setMaximumVelocity(Speeds[i]);
			VehicleCapacity cap = new VehicleCapacityImpl();
			cap.setSeats(3);//this is default for now, could be improved with mode-dependent vehicle capacity
			vehicleType.setCapacity(cap);
			ModeData modeData = new ModeData(modeId, vehicleType);
			this.modesData.put(modeId, modeData);
		}
	}
	
	public static void main(String[] args) {
		DreieckNmodes dreieck = new DreieckNmodes(NETWORK_CAPACITY);
		dreieck.fillNetworkData();
		dreieck.openFile(OUTPUT_DIR);
		//TODO: run somewhat
		dreieck.closeFile();
	}
	
	private void fillNetworkData(){
		Network network = scenario.getNetwork();
		int capMax = 100*networkCapacity;
		
		//NODES
		//nodes of the triangle base
		for (int i = 0; i<subdivisionFactor+1; i++){
			double x=0, y=0;
			if (i>0){
				for (int j=0; j<i; j++){
					x += DreieckNmodes.length/DreieckNmodes.subdivisionFactor;
				}
			}
			Coord coord = scenario.createCoord(x, y);
			Id id = new IdImpl((long)i);
			
			Node node = scenario.getNetwork().getFactory().createNode(id, coord);
			network.addNode(node);	
		}
		//nodes of the triangle right side
		for (int i = 0; i<DreieckNmodes.subdivisionFactor; i++){
			double x = DreieckNmodes.length - (DreieckNmodes.length/(2*DreieckNmodes.subdivisionFactor))*(i+1);
			double y = Math.sqrt(3.)*(DreieckNmodes.length-x);
			Coord coord = scenario.createCoord(x, y);
			Id id = new IdImpl((long)(DreieckNmodes.subdivisionFactor+i+1));
			
			Node node = scenario.getNetwork().getFactory().createNode(id, coord);
			network.addNode(node);
		}
		//nodes of the triangle left side
		for (int i = 0; i<DreieckNmodes.subdivisionFactor-1; i++){
			double x = DreieckNmodes.length/2 - (DreieckNmodes.length/(2*DreieckNmodes.subdivisionFactor))*(i+1);
			double y = Math.sqrt(3.)*x;
			Coord coord = scenario.createCoord(x, y);
			Id id = new IdImpl((long)(2*DreieckNmodes.subdivisionFactor+i+1));
			
			Node node = scenario.getNetwork().getFactory().createNode(id, coord);
			network.addNode(node);
		}
		//additional startNode and endNode for home and work activities
		Coord coord = scenario.createCoord(-50.0, 0.0);
		Id startId = new IdImpl(-1);
		Node startNode = scenario.getNetwork().getFactory().createNode(startId, coord);
		network.addNode(startNode);
		coord = scenario.createCoord(DreieckNmodes.length+50.0, 0.0);
		Id endId = new IdImpl(3*DreieckNmodes.subdivisionFactor);
		Node endNode = scenario.getNetwork().getFactory().createNode(endId, coord);
		network.addNode(endNode);
		
		//LINKS
		//all triangle links
		for (int i = 0; i<3*DreieckNmodes.subdivisionFactor; i++){
			Id idFrom = new IdImpl((long)i);
			Id idTo;
			if (i != 3*DreieckNmodes.subdivisionFactor-1)
				idTo = new IdImpl((long)(i+1));
			else
				idTo = new IdImpl(0);
			Node from = network.getNodes().get(idFrom);
			Node to = network.getNodes().get(idTo);
			
			Link link = this.scenario.getNetwork().getFactory().createLink(idFrom, from, to);
			link.setCapacity(this.networkCapacity);
			link.setFreespeed(DreieckNmodes.FREESPEED/3.6);
			link.setLength(calculateLength(from,to));
			link.setNumberOfLanes(1.);
			network.addLink(link);
		}
		//additional startLink and endLink for home and work activities
		Link startLink = this.scenario.getNetwork().getFactory().createLink(startId, startNode, this.scenario.getNetwork().getNodes().get(new IdImpl(0)));
		startLink.setCapacity(capMax);
		startLink.setFreespeed(DreieckNmodes.FREESPEED/3.6);
		startLink.setLength(25.);
		startLink.setNumberOfLanes(2.);
		network.addLink(startLink);
		Link endLink = this.scenario.getNetwork().getFactory().createLink(endId, this.scenario.getNetwork().getNodes().get(new IdImpl(DreieckNmodes.subdivisionFactor)), endNode);
		endLink.setCapacity(capMax);
		endLink.setFreespeed(DreieckNmodes.FREESPEED/3.6);
		endLink.setLength(25.);
		endLink.setNumberOfLanes(1.);
		network.addLink(endLink);
		
		//check with .xml and Visualizer
		//NetworkWriter writer = new NetworkWriter(network);
		//writer.write("./output/dreieck_network.xml");
	}
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	private void openFile(String dir) {
		try {
			writer = new PrintStream(dir);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		writer.print("agents_count\t\tk\t");
		for (int i=0; i < NUMBER_OF_MODES; i++){
			String str = this.modesData.get(new IdImpl(NAMES[i])).getModeId().toString().substring(0, 3);
			String strk = "k_"+str;
			writer.print(strk+"\t");
		}
		writer.print("\tq\t");
		for (int i=0; i < NUMBER_OF_MODES; i++){
			String str = this.modesData.get(new IdImpl(NAMES[i])).getModeId().toString().substring(0, 3);
			String strq = "q_"+str;
			writer.print(strq+"\t");
		}
		writer.print("\tv\t");
		for (int i=0; i < NUMBER_OF_MODES; i++){
			String str = this.modesData.get(new IdImpl(NAMES[i])).getModeId().toString().substring(0, 3);
			String strv = "v_"+str;
			writer.print(strv+"\t");
		}
		writer.print("\n");
	}
	
	private void closeFile() {
		writer.close();
	}
	
	private double calculateLength(Node from, Node to){
		double x1 = from.getCoord().getX();
		double y1 = from.getCoord().getY();
		double x2 = to.getCoord().getX();
		double y2 = to.getCoord().getY();
		return Math.sqrt((x2-x1)*(x2-x1)+(y2-y1)*(y2-y1));
	}

}
