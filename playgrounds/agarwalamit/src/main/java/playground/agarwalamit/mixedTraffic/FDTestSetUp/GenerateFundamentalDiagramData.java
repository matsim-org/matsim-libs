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
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.contrib.otfvis.OTFVis;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.algorithms.EventWriterXML;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.framework.MobsimDriverAgent;
import org.matsim.core.mobsim.qsim.ActivityEngine;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.TeleportationEngine;
import org.matsim.core.mobsim.qsim.agents.AgentFactory;
import org.matsim.core.mobsim.qsim.agents.PersonDriverAgentImpl;
import org.matsim.core.mobsim.qsim.agents.PopulationAgentSource;
import org.matsim.core.mobsim.qsim.changeeventsengine.NetworkChangeEventsEngine;
import org.matsim.core.mobsim.qsim.interfaces.MobsimVehicle;
import org.matsim.core.mobsim.qsim.interfaces.Netsim;
import org.matsim.core.mobsim.qsim.qnetsimengine.QNetsimEngine;
import org.matsim.core.mobsim.qsim.qnetsimengine.SeepageMobsimfactory.QueueWithBufferType;
import org.matsim.core.mobsim.qsim.qnetsimengine.SeepageNetworkFactory;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.matsim.vis.otfvis.OTFClientLive;
import org.matsim.vis.otfvis.OTFVisConfigGroup;
import org.matsim.vis.otfvis.OnTheFlyServer;

import playground.agarwalamit.mixedTraffic.MixedTrafficVehiclesUtils;

/**
 * @author amit after ssix
 */

public class GenerateFundamentalDiagramData {

	//	private static Integer[] TEST_DISTRIBUTION = {0,0,140};
	static final Logger log = Logger.getLogger(GenerateFundamentalDiagramData.class);

	//CONFIGURATION: static variables used for aggregating configuration options

	static boolean PASSING_ALLOWED = false;
	static boolean SEEPAGE_ALLOWED = false;
	private boolean SEEP_NETWORK_FACTORY = false;
	private final boolean LIVE_OTFVis = false;
	static boolean WITH_HOLES = false;
	boolean WRITE_FD_DATA = true;
	static String RUN_DIR ;
	public boolean isPlottingDistribution = false;

	static boolean writeInputFiles = true; // includes config,network and plans

	static String[] TRAVELMODES;	//identification of the different modes
	static Double[] MODAL_SPLIT; //modal split in PCU 

	private int reduceDataPointsByFactor = 1;

	private int flowUnstableWarnCount [] ;
	private int speedUnstableWarnCount [] ;

	private static InputsForFDTestSetUp inputs;
	private PrintStream writer;
	private Scenario scenario;

	static GlobalFlowDynamicsUpdator globalFlowDynamicsUpdator;
	static PassingEventsUpdator passingEventsUpdator;
	private Map<Id<VehicleType>, TravelModesFlowDynamicsUpdator> mode2FlowData;

	private Integer[] STARTING_POINT;
	private Integer [] MAX_AGENT_DISTRIBUTION;
	private Integer [] Step_Size;

	/**
	 * Overall density to vehicular flow and speed.
	 */
	private Map<Double, Map<String, Tuple<Double, Double>>> outData = new HashMap<Double, Map<String,Tuple<Double,Double>>>();
	public static String HOLE_SPEED = "15";

	public static void main(String[] args) {

		String RUN_DIR = "../../../repos/shared-svn/projects/mixedTraffic/triangularNetwork/run311";

		String OUTPUT_FOLDER ="/carTruckPassing/";
		// seepageAllowed, runDir, useHoles, useModifiedNetworkFactory, hole speed, distribution

		String [] travelModes= {"car","truck"};
		Double [] modalSplit = {1.,1.}; // in pcu

		GenerateFundamentalDiagramData generateFDData = new GenerateFundamentalDiagramData();

		generateFDData.setTravelModes(travelModes);
		generateFDData.setModalSplit(modalSplit);
		generateFDData.setPassingAllowed(true);
		generateFDData.setSeepageAllowed(false);
		generateFDData.setIsWritingFinalFdData(true);
		generateFDData.setWriteInputFiles(true);
		generateFDData.setRunDirectory(RUN_DIR+OUTPUT_FOLDER);
		generateFDData.setUseHoles(false);
		generateFDData.setReduceDataPointsByFactor(4);
		generateFDData.setUsingSeepNetworkFactory(false);
		//		HOLE_SPEED = args[4];
		generateFDData.setIsPlottingDistribution(false);
		generateFDData.run();

	}

	private void consistencyCheckAndInitialize(){
		if(writeInputFiles) {
			createLogFile();
		}

		if (TRAVELMODES.length != MODAL_SPLIT.length){
			throw new RuntimeException("Modal split for each travel mode is necessray parameter, it is not defined correctly. Check your static variable!!! \n Aborting ...");
		}

		if(PASSING_ALLOWED) log.info("=======Passing is allowed.========");
		if(SEEPAGE_ALLOWED) log.info("=======Seepage is allowed.========");
		if(WITH_HOLES) log.info("======= Using double ended queue.=======");

		if(writeInputFiles && RUN_DIR==null) throw new RuntimeException("Config, nework and plan file can not be written without a directory location.");
		if(WRITE_FD_DATA && RUN_DIR==null) throw new RuntimeException("Location to write data for FD is not set. Aborting...");

		flowUnstableWarnCount = new int [TRAVELMODES.length];
		speedUnstableWarnCount = new int [TRAVELMODES.length];
	}

	/**
	 * @param outputFile final data will be written to this file
	 */
	public void run(){

		consistencyCheckAndInitialize();

		inputs = new InputsForFDTestSetUp();
		inputs.run();
		scenario = inputs.getScenario();

		mode2FlowData = inputs.getTravelMode2FlowDynamicsData();

		if(WRITE_FD_DATA) openFileAndWriteHeader(RUN_DIR+"/data.txt");

		if(isPlottingDistribution){
			parametricRunAccordingToDistribution();	
		} else parametricRunAccordingToGivenModalSplit();

		if(WRITE_FD_DATA) closeFile();
	}

	public void setRunDirectory(String runDir) {
		RUN_DIR = runDir;
	}

	public void setPassingAllowed(boolean isPassingAllowed) {
		PASSING_ALLOWED = isPassingAllowed;
	}

	public void setSeepageAllowed(boolean isSeepageAllowed) {
		SEEPAGE_ALLOWED = isSeepageAllowed;
	}

	public void setWriteInputFiles(boolean writeInputFiles) {
		GenerateFundamentalDiagramData.writeInputFiles = writeInputFiles;
	}

	public void setTravelModes(String[] travelModes) {
		TRAVELMODES = travelModes;
	}

	public void setModalSplit(Double[] modalSplit) {
		MODAL_SPLIT = modalSplit;
	}

	public void setReduceDataPointsByFactor(int reduceDataPointsByFactor) {
		this.reduceDataPointsByFactor = reduceDataPointsByFactor;
	}

	public void setUseHoles(boolean isUsingHole) {
		WITH_HOLES = isUsingHole;
	}

	private void setUsingSeepNetworkFactory(boolean isUsingMySeepNetworkFactory){
		SEEP_NETWORK_FACTORY = isUsingMySeepNetworkFactory;
	}

	public void setIsPlottingDistribution(boolean isPlottingDistribution) {
		this.isPlottingDistribution = isPlottingDistribution;
	}

	public Map<Double, Map<String, Tuple<Double, Double>>> getOutData() {
		return outData;
	}

	public void setIsWritingFinalFdData(boolean isWritingFinalData) {
		WRITE_FD_DATA = isWritingFinalData;
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

		if(minSteps.size()==1){
			minSteps.set(0, 1);
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
		double cellSizePerPCU = ((NetworkImpl) scenario.getNetwork()).getEffectiveCellSize();
		double networkDensity = (InputsForFDTestSetUp.LINK_LENGTH/cellSizePerPCU) * 3 * InputsForFDTestSetUp.NO_OF_LANES;
		double sumOfPCUInEachStep = 0;
		for(int index=0;index<TRAVELMODES.length;index++){
			sumOfPCUInEachStep +=  minSteps.get(index) * MixedTrafficVehiclesUtils.getPCU(TRAVELMODES[index]);
		}
		int numberOfPoints = (int) Math.ceil(networkDensity/sumOfPCUInEachStep) +5;

		List<List<Integer>> pointsToRun = new ArrayList<List<Integer>>();
		for (int m=1; m<numberOfPoints; m++){
			List<Integer> pointToRun = new ArrayList<Integer>();
			for (int i=0; i<GenerateFundamentalDiagramData.TRAVELMODES.length; i++){
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

	private void parametricRunAccordingToDistribution(){

		this.STARTING_POINT = new Integer [TRAVELMODES.length];
		this.Step_Size = new Integer [TRAVELMODES.length];

		for(int ii=0;ii<TRAVELMODES.length;ii++){
			this.STARTING_POINT [ii] =0;
			this.Step_Size [ii] = this.reduceDataPointsByFactor*1;
		}
		this.STARTING_POINT = new Integer[] {1,1};

		MAX_AGENT_DISTRIBUTION = new Integer [TRAVELMODES.length];
		double cellSizePerPCU = ((NetworkImpl) this.scenario.getNetwork()).getEffectiveCellSize();
		double networkDensity = (InputsForFDTestSetUp.LINK_LENGTH/cellSizePerPCU) * 3 * InputsForFDTestSetUp.NO_OF_LANES;

		for(int ii=0;ii<MAX_AGENT_DISTRIBUTION.length;ii++){
			double pcu = this.mode2FlowData.get(Id.create(TRAVELMODES[ii],VehicleType.class)).getVehicleType().getPcuEquivalents();
			int maxNumberOfVehicle = (int) Math.floor(networkDensity/pcu)+1;
			MAX_AGENT_DISTRIBUTION[ii] = maxNumberOfVehicle;
		}

		List<List<Integer>> pointsToRun = this.createPointsToRun();


		for ( int i=0; i<pointsToRun.size(); i++){
			List<Integer> pointToRun = pointsToRun.get(i);
			double density =0;
			for(int jj = 0; jj < TRAVELMODES.length;jj++ ){
				double pcu = this.mode2FlowData.get(Id.create(TRAVELMODES[jj],VehicleType.class)).getVehicleType().getPcuEquivalents();
				density += pcu *pointToRun.get(jj) ;
			}

			if(density <= networkDensity+5){
				System.out.println("Going into run "+pointToRun);
				this.singleRun(pointToRun);
			} 
		}
	}

	private List<List<Integer>> createPointsToRun() {

		int numberOfPoints = 1; 

		for(int jj=0;jj<TRAVELMODES.length;jj++){
			numberOfPoints *= (int) Math.floor((MAX_AGENT_DISTRIBUTION[jj]-STARTING_POINT[jj])/Step_Size[jj])+1;
		}

		if(numberOfPoints > 1000) log.warn("Total number of points to run is "+numberOfPoints+". This may take long time. "
				+ "For lesser time to get the data reduce data points by some factor.");

		//Actually going through the n-dimensional grid
		BinaryAdditionModule iterationModule = new BinaryAdditionModule(Arrays.asList(MAX_AGENT_DISTRIBUTION), Arrays.asList(Step_Size), STARTING_POINT);
		List<List<Integer>> pointsToRun = new ArrayList<List<Integer>>();
		for (int i=0; i<numberOfPoints; i++){
			Integer[] newPoint = new Integer[MAX_AGENT_DISTRIBUTION.length];
			for (int j=0; j<newPoint.length; j++){
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
		passingEventsUpdator  = new PassingEventsUpdator();

		events.addHandler(globalFlowDynamicsUpdator);
		events.addHandler(passingEventsUpdator);

		EventWriterXML eventWriter = new EventWriterXML(RUN_DIR+"/events.xml");
		if(writeInputFiles){
			events.addHandler(eventWriter);
		}

		Netsim qSim = createModifiedQSim(this.scenario, events);

		qSim.run();

		boolean stableState = true;
		for(int index=0;index<TRAVELMODES.length;index++){
			Id<VehicleType> veh = Id.create(TRAVELMODES[index], VehicleType.class);
			if(!mode2FlowData.get(veh).isFlowStable()) 
			{
				stableState = false;
				int existingCount = flowUnstableWarnCount[index]; existingCount++;
				flowUnstableWarnCount[index] = existingCount;
				log.warn("Flow stability is not reached for travel mode "+veh.toString()
						+" and simulation end time is reached. Output data sheet will have all zeros for such runs."
						+ "This is " + flowUnstableWarnCount[index]+ "th warning.");
				//				log.warn("Increasing simulation time could be a possible solution to avoid it.");
			}
			if(!mode2FlowData.get(veh).isSpeedStable()) 
			{
				stableState = false;
				int existingCount = speedUnstableWarnCount[index]; existingCount++;
				speedUnstableWarnCount[index] = existingCount;
				log.warn("Speed stability is not reached for travel mode "+veh.toString()
						+" and simulation end time is reached. Output data sheet will have all zeros for such runs."
						+ "This is " + speedUnstableWarnCount[index]+ "th warning.");
			}
		}
		if(!globalFlowDynamicsUpdator.isPermanent()) stableState=false;

		// sometimes higher density points are also executed (stuck time), to exclude them density check.
		double cellSizePerPCU = ((NetworkImpl) scenario.getNetwork()).getEffectiveCellSize();
		double networkDensity = (InputsForFDTestSetUp.LINK_LENGTH/cellSizePerPCU) * 3 * InputsForFDTestSetUp.NO_OF_LANES;

		if(stableState){
			double globalLinkDensity = globalFlowDynamicsUpdator.getGlobalData().getPermanentDensity();
			if(globalLinkDensity > networkDensity/3+10) stableState =false; //+10; since we still need some points at max density to show zero speed.
		}

		if(WRITE_FD_DATA && stableState) {
			writer.format("%d\t",globalFlowDynamicsUpdator.getGlobalData().numberOfAgents);
			for (int i=0; i < TRAVELMODES.length; i++){
				writer.format("%d\t", this.mode2FlowData.get(Id.create(TRAVELMODES[i],VehicleType.class)).numberOfAgents);
			}
			writer.format("%.2f\t", globalFlowDynamicsUpdator.getGlobalData().getPermanentDensity());
			for (int i=0; i < TRAVELMODES.length; i++){
				writer.format("%.2f\t", this.mode2FlowData.get(Id.create(TRAVELMODES[i],VehicleType.class)).getPermanentDensity());
			}
			writer.format("%.2f\t", globalFlowDynamicsUpdator.getGlobalData().getPermanentFlow());
			for (int i=0; i < TRAVELMODES.length; i++){
				writer.format("%.2f\t", this.mode2FlowData.get(Id.create(TRAVELMODES[i],VehicleType.class)).getPermanentFlow());
			}
			writer.format("%.2f\t", globalFlowDynamicsUpdator.getGlobalData().getPermanentAverageVelocity());
			for (int i=0; i < TRAVELMODES.length; i++){
				writer.format("%.2f\t", this.mode2FlowData.get(Id.create(TRAVELMODES[i],VehicleType.class)).getPermanentAverageVelocity());
			}
			writer.format("%.2f\t", passingEventsUpdator.getNoOfCarsPerKm());

			writer.format("%.2f\t", passingEventsUpdator.getTotalBikesPassedByAllCarsPerKm());

			writer.format("%.2f\t", passingEventsUpdator.getAvgBikesPassingRate());
			writer.print("\n");
		}
		//storing data in map
		Map<String, Tuple<Double, Double>> mode2FlowSpeed = new HashMap<String, Tuple<Double,Double>>();
		for(int i=0; i < TRAVELMODES.length; i++){

			Tuple<Double, Double> flowSpeed = 
					new Tuple<Double, Double>(this.mode2FlowData.get(Id.create(TRAVELMODES[i],VehicleType.class)).getPermanentFlow(),
							this.mode2FlowData.get(Id.create(TRAVELMODES[i],VehicleType.class)).getPermanentAverageVelocity());
			mode2FlowSpeed.put(TRAVELMODES[i], flowSpeed);
			outData.put(globalFlowDynamicsUpdator.getGlobalData().getPermanentDensity(), mode2FlowSpeed);
		}

		if(writeInputFiles) eventWriter.closeFile();
	}

	private Netsim createModifiedQSim(Scenario sc, EventsManager events) {
		QSim qSim = new QSim(sc, events);
		ActivityEngine activityEngine = new ActivityEngine(events, qSim.getAgentCounter());
		qSim.addMobsimEngine(activityEngine);
		qSim.addActivityHandler(activityEngine);

		QNetsimEngine netsimEngine ;

		if(SEEP_NETWORK_FACTORY){
			log.warn("Using modified \"QueueWithBuffer\". Keep eyes open.");
			SeepageNetworkFactory seepNetFactory = new SeepageNetworkFactory(QueueWithBufferType.amit);
			netsimEngine = new QNetsimEngine(qSim,seepNetFactory);
		} else {
			netsimEngine = new QNetsimEngine(qSim);
		}

		qSim.addMobsimEngine(netsimEngine);
		qSim.addDepartureHandler(netsimEngine.getDepartureHandler());
		TeleportationEngine teleportationEngine = new TeleportationEngine(scenario, events);
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

		if ( LIVE_OTFVis ) {
			// otfvis configuration.  There is more you can do here than via file!
			final OTFVisConfigGroup otfVisConfig = ConfigUtils.addOrGetModule(qSim.getScenario().getConfig(), OTFVisConfigGroup.GROUP_NAME, OTFVisConfigGroup.class);
			otfVisConfig.setDrawTransitFacilities(false) ; // this DOES work
			//				otfVisConfig.setShowParking(true) ; // this does not really work

			OnTheFlyServer server = OTFVis.startServerAndRegisterWithQSim(sc.getConfig(), sc, events, qSim);
			OTFClientLive.run(sc.getConfig(), server);
		}

		return qSim;
	}

	private void openFileAndWriteHeader(String dir) {
		try {
			writer = new PrintStream(dir);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		writer.print("n \t");
		for (int i=0; i < TRAVELMODES.length; i++){
			String str = this.mode2FlowData.get(Id.create(TRAVELMODES[i],VehicleType.class)).getModeId().toString();
			String strn = "n_"+str;
			writer.print(strn+"\t");
		}
		writer.print("k \t");
		for (int i=0; i < TRAVELMODES.length; i++){
			String str = this.mode2FlowData.get(Id.create(TRAVELMODES[i],VehicleType.class)).getModeId().toString();
			String strk = "k_"+str;
			writer.print(strk+"\t");
		}
		writer.print("q \t");
		for (int i=0; i < TRAVELMODES.length; i++){
			String str = this.mode2FlowData.get(Id.create(TRAVELMODES[i],VehicleType.class)).getModeId().toString();
			String strq = "q_"+str;
			writer.print(strq+"\t");
		}
		writer.print("v \t");
		for (int i=0; i < TRAVELMODES.length; i++){
			String str = this.mode2FlowData.get(Id.create(TRAVELMODES[i],VehicleType.class)).getModeId().toString();
			String strv = "v_"+str;
			writer.print(strv+"\t");
		}
		writer.print("noOfCarsPerkm \t");

		writer.print("totalBikesPassedByAllCarsPerKm \t");

		writer.print("avgBikePassingRatePerkm \t");

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
			appender = new FileAppender(layout, RUN_DIR+"/logfile.log",false);
		} catch (IOException e1) {
			throw new RuntimeException("File not found.");
		}
		log.addAppender(appender);
	}

	private static class MyRoundAndRoundAgent implements MobsimDriverAgent{

		private PersonDriverAgentImpl delegate;
		public boolean isArriving;

		public MyRoundAndRoundAgent(Person p, Plan unmodifiablePlan, QSim qSim) {
			this.delegate = new PersonDriverAgentImpl(PopulationUtils.unmodifiablePlan(p.getSelectedPlan()), qSim);
			this.isArriving = false;//false at start, modified when all data is extracted.
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
		public Double getExpectedTravelDistance() {
			return delegate.getExpectedTravelDistance();
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
		public final void notifyArrivalOnLinkByNonNetworkMode(Id<Link> linkId) {
			delegate.notifyArrivalOnLinkByNonNetworkMode(linkId);
		}

		@Override
		public Id<Link> chooseNextLinkId() {
			if (GenerateFundamentalDiagramData.globalFlowDynamicsUpdator.isPermanent()){ 
				isArriving = true; 
			}

			if(delegate.getCurrentLinkId().equals(Id.createLinkId(InputsForFDTestSetUp.SUBDIVISION_FACTOR-1))){
				if ( isArriving) {
					return Id.createLinkId(3*InputsForFDTestSetUp.SUBDIVISION_FACTOR) ;
				} else {
					return Id.createLinkId(1) ;
				}
			} else if(delegate.getCurrentLinkId().equals(Id.createLinkId(3*InputsForFDTestSetUp.SUBDIVISION_FACTOR-1))){
				//last link on the track
				return Id.createLinkId(0);
			} else if (delegate.getCurrentLinkId().equals(Id.createLinkId(1))){
				
				return Id.createLinkId(2);
			} else return delegate.chooseNextLinkId();

//			Id<Link> lastLinkOnTriangularTrack = Id.createLinkId(3*InputsForFDTestSetUp.SUBDIVISION_FACTOR - 1);
//			Id<Link> lastLinkOfBase = Id.createLinkId(InputsForFDTestSetUp.SUBDIVISION_FACTOR - 1);
//
//			if(delegate.getCurrentLinkId().equals(lastLinkOnTriangularTrack)){
//				// person is on the last link of track and thus will continue moving on the track
//				Id<Link> afterLeftTurnLinkId = Id.createLinkId((0));
//				delegate.setCachedNextLinkId(afterLeftTurnLinkId);
//				delegate.setCurrentLinkIdIndex(0);
//				return afterLeftTurnLinkId;
//			} else if(delegate.getCurrentLinkId().equals(lastLinkOfBase) && goHome){
//				// if person is on the last link of the base and permament regime is reached.
//				// send person to arrive.
//				Id<Link> afterGoingStraightForwardLinkId = Id.createLinkId(3*InputsForFDTestSetUp.SUBDIVISION_FACTOR);
//				delegate.setCachedNextLinkId(afterGoingStraightForwardLinkId);
//				delegate.setCurrentLinkIdIndex(3*InputsForFDTestSetUp.SUBDIVISION_FACTOR);//This does work quite well so far and allows to end simulation.
//				return afterGoingStraightForwardLinkId;
//			} else {
//				return delegate.chooseNextLinkId();
//			}
			
		}

		@Override
		public void notifyMoveOverNode(Id<Link> newLinkId) {
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

		@Override
		public boolean isWantingToArriveOnCurrentLink() {
			// The following is the old condition: Being at the end of the plan means you arrive anyways, no matter if you are on the right or wrong link.
			// kai, nov'14
			if ( this.chooseNextLinkId()==null ) {
				return true ;
			} else {
				return false ;
			}
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