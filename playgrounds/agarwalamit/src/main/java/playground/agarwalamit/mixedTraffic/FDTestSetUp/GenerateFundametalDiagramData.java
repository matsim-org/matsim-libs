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

package playground.agarwalamit.mixedTraffic.FDTestSetUp;

import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.FileAppender;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.algorithms.EventWriterXML;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.framework.MobsimDriverAgent;
import org.matsim.core.mobsim.qsim.ActivityEngine;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.TeleportationEngine;
import org.matsim.core.mobsim.qsim.agents.AgentFactory;
import org.matsim.core.mobsim.qsim.agents.PopulationAgentSource;
import org.matsim.core.mobsim.qsim.changeeventsengine.NetworkChangeEventsEngine;
import org.matsim.core.mobsim.qsim.interfaces.MobsimVehicle;
import org.matsim.core.mobsim.qsim.interfaces.Netsim;
import org.matsim.core.mobsim.qsim.qnetsimengine.QNetsimEngine;
import org.matsim.core.population.PopulationUtils;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;

import playground.agarwalamit.mixedTraffic.MixedTrafficVehiclesUtils;

/**
 * @author amit after ssix
 */

public class GenerateFundametalDiagramData {

	//	private static Integer[] TEST_DISTRIBUTION = {0,0,140};
	static final Logger log = Logger.getLogger(GenerateFundametalDiagramData.class);

	//CONFIGURATION: static variables used for aggregating configuration options

	public static final boolean PASSING_ALLOWED = true;
	private static final String OUTPUT_FOLDER = "seepage/carBikeSeepTrue/";
	public static final boolean WITH_HOLES = false;
	private static final String RUN_DIR = "../../runs-svn/mixedTraffic/";
	private static final String OUTPUT_FILE = RUN_DIR+OUTPUT_FOLDER+"/data.txt"; //"pathto\\data.txt";
	private static final String OUTPUT_EVENTS =RUN_DIR+OUTPUT_FOLDER+"/events.xml";// "pathto\\events.xml";
	public static final boolean writeInputFiles = true; // includes config,network and plans

	public final static String[] TRAVELMODES= {TransportMode.bike,TransportMode.car};	//identification of the different modes
	public final static Double[] MODAL_SPLIT = {1./2.,1./2.}; //modal split in PCU 
	//	private final static Integer[] Steps = {40,40,5/*,10*/};
	private final static Integer[] STARTING_POINT = {0,0,0};
	//	private final static Integer [] MIN_STEPS_POINTS = {4,1};

	private final int reduceDataPointsByFactor = 1;

	private int flowUnstableWarnCount [] = new int [TRAVELMODES.length];
	private int speedUnstableWarnCount [] = new int [TRAVELMODES.length];

	private static InputsForFDTestSetUp inputs;
	private PrintStream writer;
	private Scenario scenario;
	static GlobalFlowDynamicsUpdator globalFlowDynamicsUpdator;
	private Map<Id<VehicleType>, TravelModesFlowDynamicsUpdator> mode2FlowData;

	public GenerateFundametalDiagramData(){
		createLogFile();
	
		if (TRAVELMODES.length != MODAL_SPLIT.length){
			throw new RuntimeException("Modal split for each travel mode is necessray parameter, it is not defined correctly. Check your static variable!!! \n Aborting ...");
		}

		if(PASSING_ALLOWED) log.info("=======Passing is allowed.========");
		if(WITH_HOLES) log.info("======= Using double ended queue.=======");

		inputs = new InputsForFDTestSetUp(RUN_DIR+OUTPUT_FOLDER);
		inputs.run();
		scenario = inputs.getScenario();
		mode2FlowData = inputs.getTravelMode2FlowDynamicsData();
	}
	
	public static void main(String[] args) {
		GenerateFundametalDiagramData generateFDData = new GenerateFundametalDiagramData();
		generateFDData.openFileAndWriteHeader(OUTPUT_FILE);
		generateFDData.parametricRunAccordingToGivenModalSplit();
		//		dreieck.parametricRunAccordingToDistribution(Arrays.asList(MaxAgentDistribution), Arrays.asList(Steps));
		//		dreieck.singleRun(Arrays.asList(TEST_DISTRIBUTION));
		generateFDData.closeFile();
	}

	private void parametricRunAccordingToDistribution(List<Integer> maxAgentDistribution, List<Integer> steps){
		//Check for size
		if ((maxAgentDistribution.size() != TRAVELMODES.length) || (steps.size() != TRAVELMODES.length)){ throw new RuntimeException("There should be as many maxValues and/or steps in the two given lists as there are modes in the simulation.");}

		List<List<Integer>> pointsToRun = this.createPointsToRun(maxAgentDistribution, steps);
		System.out.println(pointsToRun);
		for ( int i=0; i<pointsToRun.size(); i++){
			List<Integer> pointToRun = pointsToRun.get(i);
			System.out.println("Going into run "+pointToRun);
			this.singleRun(pointToRun);
		}
	}

	private void parametricRunAccordingToGivenModalSplit(){

		//		Creating minimal configuration respecting modal split in PCU and integer agent numbers
		List<Double> pcus = new ArrayList<Double>();
		for(int index =0 ;index<TRAVELMODES.length;index++){
			double tempPCU = MixedTrafficVehiclesUtils.getPCU(TRAVELMODES[index]);
			pcus.add(tempPCU);
		}

		List<Integer> minSteps = new ArrayList<Integer>();
		for (double modalSplit : Arrays.asList(MODAL_SPLIT)){
			minSteps.add(new Integer((int) (modalSplit*100)));
		}

		int commonMultiplier = 1;
		for (int i=0; i<TRAVELMODES.length; i++){
			double pcu = pcus.get(i);
			//heavy vehicles
			if ((pcu>1) && ((minSteps.get(i))%pcu != 0)){
				double lcm = getLCM((int) pcu, minSteps.get(i));
				commonMultiplier *= lcm/minSteps.get(i);
			}
		}
		for (int i=0; i<TRAVELMODES.length; i++){
			minSteps.set(i, (int) (minSteps.get(i)*commonMultiplier/pcus.get(i)));
		}
		int pgcd = getGCDOfList(minSteps);
		for (int i=0; i<TRAVELMODES.length; i++){
			minSteps.set(i, minSteps.get(i)/pgcd);
		}

		// for a faster simulation or to have less points on FD, minSteps is increased
		if(reduceDataPointsByFactor!=1) {
			log.info("===============");
			log.warn("Data points for FD will be reduced by a factor of "+reduceDataPointsByFactor+". "+
			"Make sure this is what you want because it will be more likely to have less or no points in congested regime.");
			log.info("===============");
			for(int index=0;index<minSteps.size();index++){
				minSteps.set(index, minSteps.get(index)*reduceDataPointsByFactor);
			}
		}
		//set up number of Points to run.
		double cellSizePerPCU = 7.5;
		double networkDensity = (InputsForFDTestSetUp.LINK_LENGTH/cellSizePerPCU)*3;
		double sumOfPCUInEachStep = 0;
		for(int index=0;index<TRAVELMODES.length;index++){
			sumOfPCUInEachStep +=  minSteps.get(index) * MixedTrafficVehiclesUtils.getPCU(TRAVELMODES[index]);
		}
		int numberOfPoints = (int) Math.ceil(networkDensity/sumOfPCUInEachStep) +5;

		List<List<Integer>> pointsToRun = new ArrayList<List<Integer>>();
		for (int m=1; m<numberOfPoints; m++){
			List<Integer> pointToRun = new ArrayList<Integer>();
			for (int i=0; i<GenerateFundametalDiagramData.TRAVELMODES.length; i++){
				pointToRun.add(minSteps.get(i)*m);
			}
			log.info("Number of Agents - \t"+pointToRun);
			pointsToRun.add(pointToRun);
		}

		//Effective iteration over all points 
		for ( int i=0; i<pointsToRun.size(); i++){
			List<Integer> pointToRun = pointsToRun.get(i);
			log.info("Going into run where number of Agents are - \t"+pointToRun);
			this.singleRun(pointToRun);
		}
	}

	private List<List<Integer>> createPointsToRun(List<Integer> maxValues, List<Integer> steps) {
		//calculate number of points and creating starting point:
		int numberOfPoints = 1; 
		//TODO: set this back. Integer[] startingPoint = new Integer[maxValues.size()];
		//for (int i=0; i<maxValues.size(); i++){
		//	numberOfPoints *=  ( (maxValues.get(i).intValue() / steps.get(i).intValue()) + 1);
		//	startingPoint[i] = new Integer(0);
		//}
		numberOfPoints = 13920;
		//Actually going through the n-dimensional grid
		BinaryAdditionModule iterationModule = new BinaryAdditionModule(maxValues, steps, STARTING_POINT);
		List<List<Integer>> pointsToRun = new ArrayList<List<Integer>>();
		for (int i=0; i<numberOfPoints; i++){
			Integer[] newPoint = new Integer[maxValues.size()];
			for (int j=0; j<maxValues.size(); j++){
				newPoint[j] = (iterationModule.getPoint())[j];
			}
			pointsToRun.add(Arrays.asList(newPoint));
			String point = Arraytostring(iterationModule.getPoint());
			log.info("Just added point "+point+" to the collection.");
			if (i<numberOfPoints-1){
				iterationModule.add1();
			}
		}
		//System.out.println(pointsToRun.size());
		return pointsToRun;
	}

	private void singleRun(List<Integer> pointToRun) {
		inputs.createWantedPopulation(pointToRun, 2);

		for (int i=0; i<TRAVELMODES.length; i++){
			this.mode2FlowData.get(Id.create(TRAVELMODES[i],VehicleType.class)).setnumberOfAgents(pointToRun.get(i).intValue());
		}

		EventsManager events = EventsUtils.createEventsManager();

		globalFlowDynamicsUpdator = new GlobalFlowDynamicsUpdator(this.scenario, this.mode2FlowData);
		//		this.modesData = fundiN.getModesData();
		//		funfunfun = fundiN;
		events.addHandler(globalFlowDynamicsUpdator);
		events.addHandler(new EventWriterXML(OUTPUT_EVENTS));

		Netsim qSim = createModifiedQSim(this.scenario, events);

		qSim.run();

		for(int index=0;index<TRAVELMODES.length;index++){
			Id<VehicleType> veh = Id.create(TRAVELMODES[index], VehicleType.class);
			if(!mode2FlowData.get(veh).isFlowStable()) 
			{
				int existingCount = flowUnstableWarnCount[index]; existingCount++;
				flowUnstableWarnCount[index] = existingCount;
				log.warn("Flow stability is not reached for travel mode "+veh.toString()
						+" and simulation end time is reached. Output data sheet will have all zeros for such runs."
						+ "This is " + flowUnstableWarnCount[index]+ "th warning.");
				//				log.warn("Increasing simulation time could be a possible solution to avoid it.");
			}
			if(!mode2FlowData.get(veh).isSpeedStable()) 
			{
				int existingCount = speedUnstableWarnCount[index]; existingCount++;
				speedUnstableWarnCount[index] = existingCount;
				log.warn("Speed stability is not reached for travel mode "+veh.toString()
						+" and simulation end time is reached. Output data sheet will have all zeros for such runs."
						+ "This is " + speedUnstableWarnCount[index]+ "th warning.");
			}
		}

		writer.format("%d\t\t",globalFlowDynamicsUpdator.getGlobalData().numberOfAgents);
		for (int i=0; i < TRAVELMODES.length; i++){
			writer.format("%d\t", this.mode2FlowData.get(Id.create(TRAVELMODES[i],VehicleType.class)).numberOfAgents);
		}
		writer.print("\t");
		writer.format("%.2f\t", globalFlowDynamicsUpdator.getGlobalData().getPermanentDensity());
		for (int i=0; i < TRAVELMODES.length; i++){
			writer.format("%.2f\t", this.mode2FlowData.get(Id.create(TRAVELMODES[i],VehicleType.class)).getPermanentDensity());
		}
		writer.print("\t");
		writer.format("%.2f\t", globalFlowDynamicsUpdator.getGlobalData().getPermanentFlow());
		for (int i=0; i < TRAVELMODES.length; i++){
			writer.format("%.2f\t", this.mode2FlowData.get(Id.create(TRAVELMODES[i],VehicleType.class)).getPermanentFlow());
		}
		writer.print("\t");
		writer.format("%.2f\t", globalFlowDynamicsUpdator.getGlobalData().getPermanentAverageVelocity());
		for (int i=0; i < TRAVELMODES.length; i++){
			writer.format("%.2f\t", this.mode2FlowData.get(Id.create(TRAVELMODES[i],VehicleType.class)).getPermanentAverageVelocity());
		}
		writer.print("\n");
	}

	private Netsim createModifiedQSim(Scenario sc, EventsManager events) {
		QSim qSim = new QSim(sc, events);
		ActivityEngine activityEngine = new ActivityEngine();
		qSim.addMobsimEngine(activityEngine);
		qSim.addActivityHandler(activityEngine);

		QNetsimEngine netsimEngine = new QNetsimEngine(qSim);
		qSim.addMobsimEngine(netsimEngine);
		qSim.addDepartureHandler(netsimEngine.getDepartureHandler());
		TeleportationEngine teleportationEngine = new TeleportationEngine();
		qSim.addMobsimEngine(teleportationEngine);

		log.info("=======================");
		log.info("Modifying AgentFactory by modifying mobsim agents' next link so that, "
				+ "agents keep moving on the track.");
		log.info("=======================");
		AgentFactory agentFactory = new MyAgentFactory(qSim);

		if (sc.getConfig().network().isTimeVariantNetwork()) {
			qSim.addMobsimEngine(new NetworkChangeEventsEngine());		
		}
		PopulationAgentSource agentSource = new PopulationAgentSource(sc.getPopulation(), agentFactory, qSim);

		//modification: Mobsim needs to know the different vehicle types (and their respective physical parameters)
		Map<String, VehicleType> travelModesTypes = new HashMap<String, VehicleType>();
		for (Id<VehicleType> id : mode2FlowData.keySet()){
			VehicleType vT = mode2FlowData.get(id).getVehicleType();
			travelModesTypes.put(id.toString(), vT);
		}
		agentSource.setModeVehicleTypes(travelModesTypes);

		qSim.addAgentSource(agentSource);
		return qSim;
	}

	private void openFileAndWriteHeader(String dir) {
		try {
			writer = new PrintStream(dir);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		writer.print("n\t");
		for (int i=0; i < TRAVELMODES.length; i++){
			String str = this.mode2FlowData.get(Id.create(TRAVELMODES[i],VehicleType.class)).getModeId().toString();
			String strn = "n_"+str;
			writer.print(strn+"\t");
		}
		writer.print("\tk\t");
		for (int i=0; i < TRAVELMODES.length; i++){
			String str = this.mode2FlowData.get(Id.create(TRAVELMODES[i],VehicleType.class)).getModeId().toString();
			String strk = "k_"+str;
			writer.print(strk+"\t");
		}
		writer.print("\tq\t");
		for (int i=0; i < TRAVELMODES.length; i++){
			String str = this.mode2FlowData.get(Id.create(TRAVELMODES[i],VehicleType.class)).getModeId().toString();
			String strq = "q_"+str;
			writer.print(strq+"\t");
		}
		writer.print("\tv\t");
		for (int i=0; i < TRAVELMODES.length; i++){
			String str = this.mode2FlowData.get(Id.create(TRAVELMODES[i],VehicleType.class)).getModeId().toString();
			String strv = "v_"+str;
			writer.print(strv+"\t");
		}
		writer.print("\n");
	}

	private void closeFile() {
		writer.close();
	}

	private static String Arraytostring(Integer[] list){
		int n = list.length;
		String str = "";
		for (int i=0; i<n; i++){
			str += list[i].intValue();
			str += " ";
		}
		return str;
	}

	private int getGCD(int a, int b){
		if(b==0) return a;
		else return getGCD(b, a%b);
	}

	private int getLCM(int a, int b){
		return a*b/getGCD(a,b);
	}

	private int getGCDOfList(List<Integer> list){
		int i, a, b, gcd;
		a = list.get(0);
		gcd = 1;
		for (i = 1; i < list.size(); i++){
			b = list.get(i);
			gcd = a*b/getLCM(a, b);
			a = gcd;
		}
		return gcd;
	}
	
	private void createLogFile(){
		PatternLayout layout = new PatternLayout();
		 String conversionPattern = " %d %4p %c{1} %L %m%n";
		 layout.setConversionPattern(conversionPattern);
		FileAppender appender;
		try {
			appender = new FileAppender(layout, RUN_DIR+OUTPUT_FOLDER+"/logfile.log",false);
		} catch (IOException e1) {
			throw new RuntimeException("File not found.");
		}
		log.addAppender(appender);
	}
	
	private static class MyRoundAndRoundAgent implements MobsimDriverAgent{

		private MyPersonDriverAgentImpl delegate;
		public boolean goHome;

		public MyRoundAndRoundAgent(Person p, Plan unmodifiablePlan, QSim qSim) {
			this.delegate = new MyPersonDriverAgentImpl(p, PopulationUtils.unmodifiablePlan(p.getSelectedPlan()), qSim);
			this.goHome = false;//false at start, modified when all data is extracted.
		}

		@Override
		public final void endActivityAndComputeNextState(double now) {
			delegate.endActivityAndComputeNextState(now);
		}

		@Override
		public final void endLegAndComputeNextState(double now) {
			delegate.endLegAndComputeNextState(now);
		}

		@Override
		public final void setStateToAbort(double now) {
			delegate.setStateToAbort(now);
		}

		@Override
		public boolean equals(Object obj) {
			return delegate.equals(obj);
		}

		@Override
		public final double getActivityEndTime() {
			return delegate.getActivityEndTime();
		}

		@Override
		public final Id<Link> getCurrentLinkId() {
			return delegate.getCurrentLinkId();
		}

		@Override
		public final Double getExpectedTravelTime() {
			return delegate.getExpectedTravelTime();
		}

		@Override
		public final String getMode() {
			return delegate.getMode();
		}

		@Override
		public final Id<Link> getDestinationLinkId() {
			return delegate.getDestinationLinkId();
		}

		@Override
		public final Id<Person> getId() {
			return delegate.getId();
		}

		@Override
		public State getState() {
			return delegate.getState();
		}

		@Override
		public final void notifyArrivalOnLinkByNonNetworkMode(Id linkId) {
			delegate.notifyArrivalOnLinkByNonNetworkMode(linkId);
		}

		@Override
		public Id<Link> chooseNextLinkId() {
			if (GenerateFundametalDiagramData.globalFlowDynamicsUpdator.isPermanent()){ 
				goHome = true; 
			}

			Id<Link> lastLinkOnTriangularTrack = Id.createLinkId(3*InputsForFDTestSetUp.SUBDIVISION_FACTOR - 1);
			Id<Link> lastLinkOfBase = Id.createLinkId(InputsForFDTestSetUp.SUBDIVISION_FACTOR - 1);

			if(delegate.getCurrentLinkId().equals(lastLinkOnTriangularTrack)){
				// person is on the last link of track and thus will continue moving on the track
				Id<Link> afterLeftTurnLinkId = Id.createLinkId((0));
				delegate.setCachedNextLinkId(afterLeftTurnLinkId);
				delegate.setCurrentLinkIdIndex(0);
				return afterLeftTurnLinkId;
			} else if(delegate.getCurrentLinkId().equals(lastLinkOfBase) && goHome){
				// if person is on the last link of the base and permament regime is reached.
				// send person to arrive.
				Id<Link> afterGoingStraightForwardLinkId = Id.createLinkId(3*InputsForFDTestSetUp.SUBDIVISION_FACTOR);
				delegate.setCachedNextLinkId(afterGoingStraightForwardLinkId);
				delegate.setCurrentLinkIdIndex(3*InputsForFDTestSetUp.SUBDIVISION_FACTOR);//This does work quite well so far and allows to end simulation.
				return afterGoingStraightForwardLinkId;
			} else {
				return delegate.chooseNextLinkId();
			}
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
		public Id<Vehicle > getPlannedVehicleId() {
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
}