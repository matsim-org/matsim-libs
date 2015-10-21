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

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.apache.log4j.FileAppender;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.jfree.util.Log;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.otfvis.OTFVis;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.algorithms.EventWriterXML;
import org.matsim.core.mobsim.framework.AgentSource;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.framework.MobsimDriverAgent;
import org.matsim.core.mobsim.qsim.ActivityEngine;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.changeeventsengine.NetworkChangeEventsEngine;
import org.matsim.core.mobsim.qsim.interfaces.MobsimVehicle;
import org.matsim.core.mobsim.qsim.interfaces.Netsim;
import org.matsim.core.mobsim.qsim.qnetsimengine.QNetsimEngine;
import org.matsim.core.network.NetworkImpl;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;
import org.matsim.vis.otfvis.OTFClientLive;
import org.matsim.vis.otfvis.OTFVisConfigGroup;
import org.matsim.vis.otfvis.OnTheFlyServer;

import playground.agarwalamit.mixedTraffic.MixedTrafficVehiclesUtils;

/**
 * @author amit after ssix
 */

public class GenerateFundamentalDiagramData {

	static final Logger LOG = Logger.getLogger(GenerateFundamentalDiagramData.class);

	static String RUN_DIR ;
	static boolean isDumpingInputFiles = false; // includes config, network
	private boolean isWritingEventsFileForEachIteration = false;
	
	static String[] TRAVELMODES;	
	
	static boolean PASSING_ALLOWED = false;
	static boolean SEEPAGE_ALLOWED = false;
	static boolean WITH_HOLES = false;
	static String HOLE_SPEED = "15";

	private Double[] modalSplitInPCU;
	private boolean isUsingLiveOTFVis = false;
	private boolean isPlottingDistribution = false;

	private int reduceDataPointsByFactor = 1;

	private int flowUnstableWarnCount [] ;
	private int speedUnstableWarnCount [] ;

	private static InputsForFDTestSetUp inputs;
	private PrintStream writer;
	private Scenario scenario;

	static GlobalFlowDynamicsUpdator globalFlowDynamicsUpdator;
	static PassingEventsUpdator passingEventsUpdator;
	private Map<Id<VehicleType>, TravelModesFlowDynamicsUpdator> mode2FlowData;

	private Integer[] startingPoint;
	private Integer [] maxAgentDistribution;
	private Integer [] stepSize;

	public static void main(String[] args) {
		
		boolean isRunningOnCluster = false;
		
		if (args.length  > 0) isRunningOnCluster = true;
		
		if( ! isRunningOnCluster ) {
			
			args = new String [8];
			
			String my_dir = "../../../../repos/shared-svn/projects/mixedTraffic/triangularNetwork/run313/";
			String outFolder ="/singleModes/holes/2lanes/carBike/";
			
			args[0] = my_dir + outFolder ;
			args[1] = "car,bike"; // travel (main) modes
			args[2] = "1.0,1.0"; // modal split in pcu
			args[3] = "false"; // isPassingAllowed
			args[4] = "false"; // isSeepageAllowed
			args[5] = "true"; // isUsingHoles
			args[6] = "1"; // reduce number of data points by this factor
			args[7] = "false"; // is plotting modal split distribution
		}
		
		GenerateFundamentalDiagramData generateFDData = new GenerateFundamentalDiagramData();
		
		generateFDData.setRunDirectory(args[0]);
		generateFDData.setTravelModes(args[1].split(","));
		generateFDData.setModalSplit(args[2].split(",")); //in pcu
		generateFDData.setIsPassingAllowed(Boolean.valueOf(args[3]));
		generateFDData.setIsSeepageAllowed(Boolean.valueOf(args[4]));
		generateFDData.setIsUsingHoles(Boolean.valueOf(args[5])); 
		generateFDData.setReduceDataPointsByFactor(Integer.valueOf(args[6]));
		generateFDData.setIsPlottingDistribution(Boolean.valueOf(args[7]));
		
		generateFDData.setIsDumpingInputFiles(true);
		generateFDData.setIsUsingLiveOTFVis(false);
		generateFDData.setIsWritingEventsFileForEachIteration(true);
		
		generateFDData.run();
	}

	private void consistencyCheckAndInitialize(){
		if(isDumpingInputFiles) {
			createLogFile();
		}

		if (TRAVELMODES.length != modalSplitInPCU.length){
			throw new RuntimeException("Modal split for each travel mode is necessray parameter, it is not defined correctly. Check your static variable!!! \n Aborting ...");
		}

		if(PASSING_ALLOWED) LOG.info("=======Passing is allowed.========");
		if(SEEPAGE_ALLOWED) LOG.info("=======Seepage is allowed.========");
		if(WITH_HOLES) LOG.info("======= Using double ended queue.=======");

		if(isDumpingInputFiles && RUN_DIR==null) throw new RuntimeException("Config, nework and plan file can not be written without a directory location.");
		if(RUN_DIR==null) throw new RuntimeException("Location to write data for FD is not set. Aborting...");
		
		if(reduceDataPointsByFactor != 1) {
			LOG.info("===============");
			LOG.warn("Number of modes for each mode type in FD will be reduced by a factor of "+reduceDataPointsByFactor+". "+
					"Make sure this is what you want because it will be more likely to have less or no points in congested regime in absence of queue model with holes.");
			LOG.info("===============");
		}
		
		if(isWritingEventsFileForEachIteration) Log.warn("This will write one event file corresponding to each iteration and thus ");

		flowUnstableWarnCount = new int [TRAVELMODES.length];
		speedUnstableWarnCount = new int [TRAVELMODES.length];
	}

	public void run(){

		consistencyCheckAndInitialize();

		inputs = new InputsForFDTestSetUp();
		inputs.run();
		scenario = inputs.getScenario();

		mode2FlowData = inputs.getTravelMode2FlowDynamicsData();

		openFileAndWriteHeader(RUN_DIR+"/data.txt");

		if(isPlottingDistribution){
			parametricRunAccordingToDistribution();	
		} else parametricRunAccordingToGivenModalSplit();

		closeFile();
	}

	public void setRunDirectory(String runDir) {
		RUN_DIR = runDir;
	}

	public void setIsPassingAllowed(boolean isPassingAllowed) {
		PASSING_ALLOWED = isPassingAllowed;
	}

	public void setIsSeepageAllowed(boolean isSeepageAllowed) {
		SEEPAGE_ALLOWED = isSeepageAllowed;
	}

	public void setIsDumpingInputFiles(boolean writeInputFiles) {
		isDumpingInputFiles = writeInputFiles;
	}

	public void setTravelModes(String[] travelModes) {
		TRAVELMODES = travelModes;
	}

	public void setModalSplit(String [] modalSplit) {
		this.modalSplitInPCU = new Double [modalSplit.length];
		for (int ii = 0; ii <modalSplit.length; ii ++){
			this.modalSplitInPCU [ii] = Double.valueOf(modalSplit[ii]);
		}
	}

	public void setReduceDataPointsByFactor(int reduceDataPointsByFactor) {
		this.reduceDataPointsByFactor = reduceDataPointsByFactor;
	}

	public void setIsUsingHoles(boolean isUsingHole) {
		WITH_HOLES = isUsingHole;
	}

	public void setIsPlottingDistribution(boolean isPlottingDistribution) {
		this.isPlottingDistribution = isPlottingDistribution;
	}

	public void setIsUsingLiveOTFVis(boolean liveOTFVis) {
		this.isUsingLiveOTFVis = liveOTFVis;
	}

	public void setIsWritingEventsFileForEachIteration(
			boolean isWritingEventsFileForEachIteration) {
		this.isWritingEventsFileForEachIteration = isWritingEventsFileForEachIteration;
	}

	private void parametricRunAccordingToGivenModalSplit(){

		//	Creating minimal configuration respecting modal split in PCU and integer agent numbers
		List<Double> pcus = new ArrayList<Double>();
		for(int index =0 ;index<TRAVELMODES.length;index++){
			double tempPCU = MixedTrafficVehiclesUtils.getPCU(TRAVELMODES[index]);
			pcus.add(tempPCU);
		}

		List<Integer> minSteps = new ArrayList<Integer>();
		for (double modalSplit : Arrays.asList(modalSplitInPCU)){
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

		if(reduceDataPointsByFactor!=1) {
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
			LOG.info("Number of Agents - \t"+pointToRun);
			pointsToRun.add(pointToRun);
		}

		for ( int i=0; i<pointsToRun.size(); i++){
			List<Integer> pointToRun = pointsToRun.get(i);
			LOG.info("===============");
			LOG.info("Going into run where number of Agents are - \t"+pointToRun);
			Log.info("Further, " + (pointsToRun.size() - i) +" combinations will be simulated.");
			LOG.info("===============");
			this.singleRun(pointToRun);
		}
	}

	private void parametricRunAccordingToDistribution(){

		this.startingPoint = new Integer [TRAVELMODES.length];
		this.stepSize = new Integer [TRAVELMODES.length];

		for(int ii=0;ii<TRAVELMODES.length;ii++){
			this.startingPoint [ii] =0;
			this.stepSize [ii] = this.reduceDataPointsByFactor*1;
		}
		this.startingPoint = new Integer[] {1,1};

		maxAgentDistribution = new Integer [TRAVELMODES.length];
		double cellSizePerPCU = ((NetworkImpl) this.scenario.getNetwork()).getEffectiveCellSize();
		double networkDensity = (InputsForFDTestSetUp.LINK_LENGTH/cellSizePerPCU) * 3 * InputsForFDTestSetUp.NO_OF_LANES;

		for(int ii=0;ii<maxAgentDistribution.length;ii++){
			double pcu = this.mode2FlowData.get(Id.create(TRAVELMODES[ii],VehicleType.class)).getVehicleType().getPcuEquivalents();
			int maxNumberOfVehicle = (int) Math.floor(networkDensity/pcu)+1;
			maxAgentDistribution[ii] = maxNumberOfVehicle;
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
			numberOfPoints *= (int) Math.floor((maxAgentDistribution[jj]-startingPoint[jj])/stepSize[jj])+1;
		}

		if(numberOfPoints > 1000) LOG.warn("Total number of points to run is "+numberOfPoints+". This may take long time. "
				+ "For lesser time to get the data reduce data points by some factor.");

		//Actually going through the n-dimensional grid
		BinaryAdditionModule iterationModule = new BinaryAdditionModule(Arrays.asList(maxAgentDistribution), Arrays.asList(stepSize), startingPoint);
		List<List<Integer>> pointsToRun = new ArrayList<List<Integer>>();
		for (int i=0; i<numberOfPoints; i++){
			Integer[] newPoint = new Integer[maxAgentDistribution.length];
			for (int j=0; j<newPoint.length; j++){
				newPoint[j] = (iterationModule.getPoint())[j];
			}
			pointsToRun.add(Arrays.asList(newPoint));
			String point = Arraytostring(iterationModule.getPoint());
			LOG.info("Just added point "+point+" to the collection.");
			if (i<numberOfPoints-1){
				iterationModule.add1();
			}
		}
		return pointsToRun;
	}

	private void singleRun(List<Integer> pointToRun) {
		
		person2Mode.clear();
		
		for (int i=0; i<TRAVELMODES.length; i++){
			for (int ii = 0; ii < pointToRun.get(i); ii++){
				Id<Person> personId = Id.createPersonId(person2Mode.size());
				person2Mode.put(personId,TRAVELMODES[i]);
			}
			
			this.mode2FlowData.get(Id.create(TRAVELMODES[i],VehicleType.class)).setnumberOfAgents(pointToRun.get(i).intValue());
		}

		EventsManager events = EventsUtils.createEventsManager();

		globalFlowDynamicsUpdator = new GlobalFlowDynamicsUpdator( this.mode2FlowData);
		passingEventsUpdator  = new PassingEventsUpdator();

		events.addHandler(globalFlowDynamicsUpdator);
		
		if(TRAVELMODES.length > 1)	events.addHandler(passingEventsUpdator);

		EventWriterXML eventWriter = null;
		
		if(isWritingEventsFileForEachIteration){
			String eventsDir = RUN_DIR+"/events/";
			
			if (! new File(eventsDir).exists() ) new File(eventsDir).mkdir();
			
			eventWriter = new EventWriterXML(eventsDir+"/events"+pointToRun.toString()+".xml");
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
				LOG.warn("Flow stability is not reached for travel mode "+veh.toString()
						+" and simulation end time is reached. Output data sheet will have all zeros for such runs."
						+ "This is " + flowUnstableWarnCount[index]+ "th warning.");
				//				log.warn("Increasing simulation time could be a possible solution to avoid it.");
			}
			if(!mode2FlowData.get(veh).isSpeedStable()) 
			{
				stableState = false;
				int existingCount = speedUnstableWarnCount[index]; existingCount++;
				speedUnstableWarnCount[index] = existingCount;
				LOG.warn("Speed stability is not reached for travel mode "+veh.toString()
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

		if( stableState ) {
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
			
			if( TRAVELMODES.length > 1 ) {

				writer.format("%.2f\t", passingEventsUpdator.getNoOfCarsPerKm());

				writer.format("%.2f\t", passingEventsUpdator.getTotalBikesPassedByAllCarsPerKm());

				writer.format("%.2f\t", passingEventsUpdator.getAvgBikesPassingRate());

			}
			
			writer.print("\n");
		}

		if(isWritingEventsFileForEachIteration) eventWriter.closeFile();
	}
	
	static final Map<Id<Person>, String> person2Mode = new HashMap<Id<Person>, String>();

	private Netsim createModifiedQSim(Scenario sc, EventsManager events) {
		final QSim qSim = new QSim(sc, events);
		ActivityEngine activityEngine = new ActivityEngine(events, qSim.getAgentCounter());
		qSim.addMobsimEngine(activityEngine);
		qSim.addActivityHandler(activityEngine);

		QNetsimEngine netsimEngine  = new QNetsimEngine(qSim);

		qSim.addMobsimEngine(netsimEngine);
		qSim.addDepartureHandler(netsimEngine.getDepartureHandler());

		LOG.info("=======================");
		LOG.info("Mobsim agents' are directly added to AgentSource.");
		LOG.info("=======================");

		if (sc.getConfig().network().isTimeVariantNetwork()) {
			qSim.addMobsimEngine(new NetworkChangeEventsEngine());		
		}

		//modification: Mobsim needs to know the different vehicle types (and their respective physical parameters)
		final Map<String, VehicleType> travelModesTypes = new HashMap<String, VehicleType>();
		for (Id<VehicleType> id : mode2FlowData.keySet()){
			VehicleType vT = mode2FlowData.get(id).getVehicleType();
			travelModesTypes.put(id.toString(), vT);
		}

		AgentSource agentSource = new AgentSource() {

			@Override
			public void insertAgentsIntoMobsim() {

				for ( Id<Person> personId : person2Mode.keySet()) {
					String travelMode = person2Mode.get(personId);
					double actEndTime = (new Random().nextDouble())*900;

					MobsimAgent agent = new MySimplifiedRoundAndRoundAgent(personId, actEndTime, travelMode);
					qSim.insertAgentIntoMobsim(agent);

					final Vehicle vehicle = VehicleUtils.getFactory().createVehicle(Id.create(agent.getId(), Vehicle.class), travelModesTypes.get(travelMode));
					final Id<Link> linkId4VehicleInsertion = Id.createLinkId("home");
					qSim.createAndParkVehicleOnLink(vehicle, linkId4VehicleInsertion);
				}
			}
		};

		qSim.addAgentSource(agentSource);

		if ( isUsingLiveOTFVis ) {
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
		
		if( TRAVELMODES.length > 1 ) {
			writer.print("noOfCarsPerkm \t");

			writer.print("totalBikesPassedByAllCarsPerKm \t");

			writer.print("avgBikePassingRatePerkm \t");
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
			appender = new FileAppender(layout, RUN_DIR+"/logfile.log",false);
		} catch (IOException e1) {
			throw new RuntimeException("File not found.");
		}
		LOG.addAppender(appender);
	}

	static class MySimplifiedRoundAndRoundAgent implements MobsimAgent, MobsimDriverAgent {

		private static final Id<Link> FIRST_LINK_ID_OF_MIDDEL_BRANCH_OF_TRACK = Id.createLinkId(InputsForFDTestSetUp.SUBDIVISION_FACTOR);
		private static final Id<Link> LAST_LINK_ID_OF_BASE = Id.createLinkId(InputsForFDTestSetUp.SUBDIVISION_FACTOR-1);
		private static final Id<Link> LAST_LINK_ID_OF_TRACK = Id.createLinkId(3*InputsForFDTestSetUp.SUBDIVISION_FACTOR-1);
		private static final Id<Link> FIRST_LINK_LINK_ID_OF_BASE = Id.createLinkId(0);
		private static final Id<Link> ORIGIN_LINK_ID = Id.createLinkId("home");
		private static final Id<Link> DESTINATION_LINK_ID = Id.createLinkId("work");

		public MySimplifiedRoundAndRoundAgent(Id<Person> agentId, double actEndTime, String travelMode) {
			personId = agentId;
			mode = travelMode;
			this.actEndTime = actEndTime;
			this.plannedVehicleId = Id.create(agentId, Vehicle.class);
		}

		private final Id<Person> personId;
		private final Id<Vehicle> plannedVehicleId;
		private final String mode;
		private final double actEndTime;

		private MobsimVehicle vehicle ;
		public boolean isArriving= false;

		private Id<Link> currentLinkId = ORIGIN_LINK_ID;
		private State agentState= MobsimAgent.State.ACTIVITY;;

		@Override
		public Id<Link> getCurrentLinkId() {
			return this.currentLinkId;
		}

		@Override
		public Id<Link> getDestinationLinkId() {
			return DESTINATION_LINK_ID;
		}

		@Override
		public Id<Person> getId() {
			return this.personId;
		}

		@Override
		public Id<Link> chooseNextLinkId() {

			if (GenerateFundamentalDiagramData.globalFlowDynamicsUpdator.isPermanent()){ 
				isArriving = true; 
			}

			if( LAST_LINK_ID_OF_TRACK.equals(this.currentLinkId) || ORIGIN_LINK_ID.equals(this.currentLinkId)){
				//person departing from home OR last link of the track
				return FIRST_LINK_LINK_ID_OF_BASE;
			} else if(LAST_LINK_ID_OF_BASE.equals(this.currentLinkId)){
				if ( isArriving) {
					return DESTINATION_LINK_ID ;
				} else {
					return FIRST_LINK_ID_OF_MIDDEL_BRANCH_OF_TRACK ;
				}
			}  else if (DESTINATION_LINK_ID.equals(this.currentLinkId)){
				return null;// this will send agent for arrival
			} else {
				Id<Link> existingLInkId = this.currentLinkId;
				return Id.createLinkId(Integer.valueOf(existingLInkId.toString())+1);
			}
		}

		@Override
		public void notifyMoveOverNode(Id<Link> newLinkId) {
			this.currentLinkId = newLinkId;
		}

		@Override
		public boolean isWantingToArriveOnCurrentLink() {
			if ( this.chooseNextLinkId()==null ) {
				return true ;
			} else {
				return false ;
			}
		}

		@Override
		public void setVehicle(MobsimVehicle veh) {
			this.vehicle = veh ;
		}

		@Override
		public MobsimVehicle getVehicle() {
			return this.vehicle ;
		}

		@Override
		public Id<Vehicle> getPlannedVehicleId() {
			return this.plannedVehicleId;
		}

		@Override
		public State getState() {
			return agentState;
		}

		@Override
		public double getActivityEndTime() {
			if(isArriving && this.agentState.equals(MobsimAgent.State.ACTIVITY)) {
				return Double.POSITIVE_INFINITY; // let agent go to sleep.
			}
			return this.actEndTime;
		}

		@Override
		public void endActivityAndComputeNextState(double now) {
			agentState= MobsimAgent.State.LEG;
		}

		@Override
		public void endLegAndComputeNextState(double now) {
			agentState=MobsimAgent.State.ACTIVITY;
		}

		@Override
		public void setStateToAbort(double now) {
			throw new RuntimeException("not implemented");
		}

		@Override
		public Double getExpectedTravelTime() {
			throw new RuntimeException("not implemented");
		}

		@Override
		public Double getExpectedTravelDistance() {
			throw new RuntimeException("not implemented");
		}

		@Override
		public String getMode() {
			return mode;
		}

		@Override
		public void notifyArrivalOnLinkByNonNetworkMode(Id<Link> linkId) {
			throw new RuntimeException("not implemented");
		}

	}
}