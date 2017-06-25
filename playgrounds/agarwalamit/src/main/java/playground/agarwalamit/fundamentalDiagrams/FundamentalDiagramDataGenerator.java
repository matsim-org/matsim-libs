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

package playground.agarwalamit.fundamentalDiagrams;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.stream.Collectors;
import org.apache.log4j.FileAppender;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.jfree.util.Log;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.otfvis.OTFVis;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.ConfigWriter;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.algorithms.EventWriterXML;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.mobsim.framework.AgentSource;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.framework.MobsimDriverAgent;
import org.matsim.core.mobsim.qsim.ActivityEngine;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.changeeventsengine.NetworkChangeEventsEngine;
import org.matsim.core.mobsim.qsim.interfaces.MobsimVehicle;
import org.matsim.core.mobsim.qsim.interfaces.Netsim;
import org.matsim.core.mobsim.qsim.qnetsimengine.QNetsimEngine;
import org.matsim.core.network.VariableIntervalTimeVariantLinkFactory;
import org.matsim.core.network.io.NetworkWriter;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.facilities.Facility;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;
import org.matsim.vis.otfvis.OTFClientLive;
import org.matsim.vis.otfvis.OTFVisConfigGroup;
import org.matsim.vis.otfvis.OnTheFlyServer;

/**
 * @author amit after ssix
 */

public class FundamentalDiagramDataGenerator {

	public static final Logger LOG = Logger.getLogger(FundamentalDiagramDataGenerator.class);

	static final double MAX_ACT_END_TIME = 1800.;

	private String runDir ;
	private boolean isWritingEventsFileForEachIteration = false;

	private boolean isUsingLiveOTFVis = false;
	private boolean isPlottingDistribution = false;

	private int reduceDataPointsByFactor = 1;

	private int flowUnstableWarnCount [] ;
	private int speedUnstableWarnCount [] ;

	private PrintStream writer;
	private final Scenario scenario;

	private static GlobalFlowDynamicsUpdator globalFlowDynamicsUpdator;
	private static FDNetworkGenerator fdNetworkGenerator;

	private final Map<String, TravelModesFlowDynamicsUpdator> mode2FlowData = new HashMap<>();
	private final Map<Id<Person>, String> person2Mode = new HashMap<>();

	private Integer[] startingPoint;
	private Integer [] maxAgentDistribution;
	private Integer [] stepSize;

	private String[] travelModes;
	private Double[] modalShareInPCU;

	public FundamentalDiagramDataGenerator(final RaceTrackLinkProperties raceTrackLinkProperties, final Scenario scenario){
		fdNetworkGenerator = new FDNetworkGenerator(raceTrackLinkProperties);
		fdNetworkGenerator.createNetwork(scenario);
		this.scenario = scenario;
	}

	/**
	 * All default values will be used.
	 */
	public FundamentalDiagramDataGenerator(){
		this (ScenarioUtils.loadScenario(ConfigUtils.createConfig()));
	}

	/**
	 * A constructor to use the default values for the race track network.
	 * @param scenario
	 */
	public FundamentalDiagramDataGenerator(final Scenario scenario){
		this(new RaceTrackLinkProperties(
				1000.0,
				1600.0,
				60.0/3.6,
				1.0,
				new HashSet<>(
						scenario.getConfig().qsim().getMainModes())),
				scenario);
	}

	public void run(){
		checkForConsistencyAndInitialize();
		setUpConfig();

		openFileAndWriteHeader(runDir+"/data.txt");

		if(isPlottingDistribution){
			parametricRunAccordingToDistribution();
		} else parametricRunAccordingToGivenModalSplit();

		new ConfigWriter(scenario.getConfig()).write(this.runDir+"/config.xml");
		new NetworkWriter(scenario.getNetwork()).write(this.runDir+"/network.xml");

		closeFile();
	}

	private void checkForConsistencyAndInitialize(){
		this.runDir = scenario.getConfig().controler().getOutputDirectory();
		if(runDir==null) throw new RuntimeException("Location to write data for FD is not set. Aborting...");

		createLogFile();

		if(reduceDataPointsByFactor != 1) {
			LOG.info("===============");
			LOG.warn("Number of modes for each mode type in FD will be reduced by a factor of "+reduceDataPointsByFactor+". This will not change the traffic dynamics.");
			if (scenario.getConfig().qsim().getTrafficDynamics()== QSimConfigGroup.TrafficDynamics.queue) LOG.warn("Make sure this is what you want because it will be more likely to have less or no points in congested regime in absence of queue model with holes.");
			LOG.info("===============");
		}

		if(isWritingEventsFileForEachIteration) Log.warn("This will write one event file corresponding to each iteration and thus ");

		Collection<String> mainModes = scenario.getConfig().qsim().getMainModes();
		travelModes = mainModes.toArray(new String[mainModes.size()]);

		if (scenario.getVehicles().getVehicleTypes().isEmpty()) {
			if (travelModes.length==1 && travelModes [0].equals("car")) {
				LOG.warn("No vehicle information is provided for "+this.travelModes[0]+". Using default vehicle (i.e. car) with maximum speed same as" +
						"allowed speed on the link.");

				VehicleType car = VehicleUtils.getFactory().createVehicleType(Id.create("car",VehicleType.class));
            	car.setPcuEquivalents(1.0);
            	car.setMaximumVelocity( fdNetworkGenerator.getLinkProperties().getLinkFreeSpeedMPS() );
            	scenario.getVehicles().addVehicleType(car);

			} else {
				throw new RuntimeException("Vehicle type information for modes "+ Arrays.toString(travelModes)+" is not provided. Aborting...");
			}
		}

		for (String travelMode : travelModes) {
			Id<VehicleType> vehicleTypeId = Id.create(travelMode, VehicleType.class);
			VehicleType vehicleType = scenario.getVehicles().getVehicleTypes().get(vehicleTypeId);
			mode2FlowData.put(travelMode, new TravelModesFlowDynamicsUpdator(vehicleType, travelModes.length,
					fdNetworkGenerator.getFirstLinkIdOfTrack(), fdNetworkGenerator.getLengthOfTrack()));
		}

		flowUnstableWarnCount = new int [travelModes.length];
		speedUnstableWarnCount = new int [travelModes.length];

		if (this.modalShareInPCU==null) {
			LOG.warn("No modal split is provided for mode(s) : " + Arrays.toString(this.travelModes)+". Using equla modal split in PCU.");
			this.modalShareInPCU = new Double[this.travelModes.length];
			for (int index = 0; index< this.travelModes.length; index++){
				this.modalShareInPCU[index] = 1.0;
			}
		} else if (this.modalShareInPCU.length != this.travelModes.length) {
			throw new RuntimeException("Number of modes is not equal to the provided modal share (in PCU). Aborting...");
		}
	}

	private void setUpConfig() {
		// required if using controler
		PlanCalcScoreConfigGroup.ActivityParams home = new PlanCalcScoreConfigGroup.ActivityParams("home");
		home.setScoringThisActivityAtAll(false);
		scenario.getConfig().planCalcScore().addActivityParams(home);

		PlanCalcScoreConfigGroup.ActivityParams work = new PlanCalcScoreConfigGroup.ActivityParams("work");
		work.setScoringThisActivityAtAll(false);
		scenario.getConfig().planCalcScore().addActivityParams(work);

		scenario.getConfig().controler().setLastIteration(0);
		scenario.getConfig().controler().setCreateGraphs(false);
		scenario.getConfig().controler().setDumpDataAtEnd(false);

		scenario.getConfig().qsim().setEndTime(100.0*3600.); // qsim should not go beyond 100 hrs it stability is not achieved.

		// following is necessary, in order to achieve the data points at high density
		if(this.travelModes.length==1 && this.travelModes[0].equals("car")) scenario.getConfig().qsim().setStuckTime(60.);
		else  if (this.travelModes.length==1 && this.travelModes[0].equals("truck")) scenario.getConfig().qsim().setStuckTime(180.);

		if ( scenario.getConfig().network().isTimeVariantNetwork() ) {
			Network netImpl = scenario.getNetwork();
			netImpl.getFactory().setLinkFactory(new VariableIntervalTimeVariantLinkFactory());
		}
	}

	public void setModalShareInPCU(Double[] modalShareInPCU) {
		this.modalShareInPCU = modalShareInPCU;
	}

	public void setReduceDataPointsByFactor(int reduceDataPointsByFactor) {
		this.reduceDataPointsByFactor = reduceDataPointsByFactor;
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
		List<Double> pcus = Arrays.stream(travelModes)
								  .mapToDouble(travelMode -> this.mode2FlowData.get(travelMode)
																			   .getVehicleType()
																			   .getPcuEquivalents())
								  .boxed()
								  .collect(Collectors.toList());

		List<Integer> minSteps = Arrays.stream(modalShareInPCU)
									   .mapToDouble(modalSplit -> modalSplit)
									   .mapToObj(modalSplit -> ((int) modalSplit * 100))
									   .collect(Collectors.toList());

		int commonMultiplier = 1;
		for (int i=0; i<travelModes.length; i++){
			double pcu = pcus.get(i);
			//heavy vehicles
			if ( (pcu>1) && (minSteps.get(i)%pcu != 0) ){
				double lcm = getLCM((int) pcu, minSteps.get(i));
				commonMultiplier *= lcm/minSteps.get(i);
			}
		}
		for (int i=0; i<travelModes.length; i++){
			minSteps.set(i, (int) (minSteps.get(i)*commonMultiplier/pcus.get(i)));
		}
		int pgcd = getGCDOfList(minSteps);
		for (int i=0; i<travelModes.length; i++){
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
		double cellSizePerPCU = scenario.getNetwork().getEffectiveCellSize();
		double networkDensity = fdNetworkGenerator.getLengthOfTrack() * fdNetworkGenerator.getLinkProperties().getNumberOfLanes() / cellSizePerPCU;
		double sumOfPCUInEachStep = 0;

		for(int index=0;index<travelModes.length;index++){
			sumOfPCUInEachStep +=  minSteps.get(index) * this.mode2FlowData.get(travelModes[index]).getVehicleType().getPcuEquivalents();
		}
		int numberOfPoints = (int) Math.ceil(networkDensity/sumOfPCUInEachStep) +5;

		List<List<Integer>> pointsToRun = new ArrayList<>();
		for (int m=1; m<numberOfPoints; m++){
			List<Integer> pointToRun = new ArrayList<>();
			for (int i=0; i<travelModes.length; i++){
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

		this.startingPoint = new Integer [travelModes.length];
		this.stepSize = new Integer [travelModes.length];

		for(int ii=0;ii<travelModes.length;ii++){
			this.startingPoint [ii] =0;
			this.stepSize [ii] = this.reduceDataPointsByFactor;
		}
		this.startingPoint = new Integer[] {1,1};

		maxAgentDistribution = new Integer [travelModes.length];
		double cellSizePerPCU = this.scenario.getNetwork().getEffectiveCellSize();
		double networkDensity = fdNetworkGenerator.getLengthOfTrack() * fdNetworkGenerator.getLinkProperties().getNumberOfLanes() / cellSizePerPCU;

		for(int ii=0;ii<maxAgentDistribution.length;ii++){
			double pcu = this.mode2FlowData.get(travelModes[ii]).getVehicleType().getPcuEquivalents();
			int maxNumberOfVehicle = (int) Math.floor(networkDensity/pcu)+1;
			maxAgentDistribution[ii] = maxNumberOfVehicle;
		}

		List<List<Integer>> pointsToRun = this.createPointsToRun();

		for (List<Integer> pointToRun : pointsToRun) {
			double density = 0;
			for (int jj = 0; jj < travelModes.length; jj++) {
				double pcu = this.mode2FlowData.get(travelModes[jj]).getVehicleType().getPcuEquivalents();
				density += pcu * pointToRun.get(jj);
			}

			if (density <= networkDensity + 5) {
				LOG.info("Going into run " + pointToRun);
				this.singleRun(pointToRun);
			}
		}
	}

	private List<List<Integer>> createPointsToRun() {

		int numberOfPoints = 1;

		for(int jj=0;jj<travelModes.length;jj++){
			numberOfPoints *= (int) Math.floor((maxAgentDistribution[jj]-startingPoint[jj])/stepSize[jj])+1;
		}

		if(numberOfPoints > 1000) LOG.warn("Total number of points to run is "+numberOfPoints+". This may take long time. "
				+ "For lesser time to get the data reduce data points by some factor.");

		//Actually going through the n-dimensional grid
		BinaryAdditionModule iterationModule = new BinaryAdditionModule(Arrays.asList(maxAgentDistribution), Arrays.asList(stepSize), startingPoint);
		List<List<Integer>> pointsToRun = new ArrayList<>();
		for (int i=0; i<numberOfPoints; i++){
			Integer[] newPoint = new Integer[maxAgentDistribution.length];
			System.arraycopy(iterationModule.getPoint(), 0, newPoint, 0, newPoint.length);
			pointsToRun.add(Arrays.asList(newPoint));
			String point = arraytostring(iterationModule.getPoint());
			LOG.info("Just added point "+point+" to the collection.");
			if (i<numberOfPoints-1){
				iterationModule.add1();
			}
		}
		return pointsToRun;
	}

	private void singleRun(List<Integer> pointToRun) {
		person2Mode.clear();
		for (int i=0; i<travelModes.length; i++){
			for (int ii = 0; ii < pointToRun.get(i); ii++){
				Id<Person> personId = Id.createPersonId(person2Mode.size());
				person2Mode.put(personId,travelModes[i]);
			}

			this.mode2FlowData.get(travelModes[i]).setnumberOfAgents(pointToRun.get(i).intValue());
		}

		EventsManager events = EventsUtils.createEventsManager();

		globalFlowDynamicsUpdator = new GlobalFlowDynamicsUpdator(
				this.mode2FlowData,
				fdNetworkGenerator.getFirstLinkIdOfTrack() ,
				fdNetworkGenerator.getLengthOfTrack());
		PassingEventsUpdator passingEventsUpdator = new PassingEventsUpdator(
				scenario.getConfig().qsim().getSeepModes(),
				fdNetworkGenerator.getFirstLinkIdOfTrack(),
				fdNetworkGenerator.getLastLinkIdOfTrack(),
				fdNetworkGenerator.getLengthOfTrack());

		events.addHandler(globalFlowDynamicsUpdator);
		if(travelModes.length > 1)	events.addHandler(passingEventsUpdator);

		EventWriterXML eventWriter = null;

		if(isWritingEventsFileForEachIteration){
			String eventsDir = runDir+"/events/";

			if (! new File(eventsDir).exists() ) new File(eventsDir).mkdir();

			eventWriter = new EventWriterXML(eventsDir+"/events"+pointToRun.toString()+".xml");
			events.addHandler(eventWriter);
		}

		Controler controler = new Controler( scenario ) ;

		final Netsim qSim = createModifiedQSim(this.scenario, events);
		controler.addOverridingModule(new AbstractModule(){
			@Override
			public void install() {
				this.bindMobsim().toInstance( qSim );
			}
		});

		controler.run();

		if(! scenario.getConfig().controler().getSnapshotFormat().isEmpty()) {
			//remove and renaming of the files which are generated from controler and not required.
			updateTransimFileNameAndDir(pointToRun);
		}
		cleanOutputDir();

		boolean stableState = true;
		for(int index=0;index<travelModes.length;index++){
			String veh = travelModes[index];
			if(!mode2FlowData.get(veh).isFlowStable())
			{
				stableState = false;
				int existingCount = flowUnstableWarnCount[index]; existingCount++;
				flowUnstableWarnCount[index] = existingCount;
				LOG.warn("Flow stability is not reached for travel mode "+ veh
						+" and simulation end time is reached. Output data sheet will have all zeros for such runs."
						+ "This is " + flowUnstableWarnCount[index]+ "th warning.");
			}
			if(!mode2FlowData.get(veh).isSpeedStable())
			{
				stableState = false;
				int existingCount = speedUnstableWarnCount[index]; existingCount++;
				speedUnstableWarnCount[index] = existingCount;
				LOG.warn("Speed stability is not reached for travel mode "+ veh
						+" and simulation end time is reached. Output data sheet will have all zeros for such runs."
						+ "This is " + speedUnstableWarnCount[index]+ "th warning.");
			}
		}
		if(!globalFlowDynamicsUpdator.isPermanent()) stableState=false;

		// sometimes higher density points are also executed (stuck time), to exclude them density check.
		double cellSizePerPCU = scenario.getNetwork().getEffectiveCellSize();
		double networkDensity = fdNetworkGenerator.getLengthOfTrack() * fdNetworkGenerator.getLinkProperties().getNumberOfLanes() / cellSizePerPCU;

		if(stableState){
			double globalLinkDensity = globalFlowDynamicsUpdator.getGlobalData().getPermanentDensity();
			if(globalLinkDensity > networkDensity / 3 + 10 ) stableState =false; //+10; since we still need some points at max density to show zero speed.
		}

		if( stableState ) {
			writer.format("%d\t",globalFlowDynamicsUpdator.getGlobalData().getnumberOfAgents());
			for (String travelMode : travelModes) {
				writer.format("%d\t", this.mode2FlowData.get(travelMode).getnumberOfAgents());
			}
			writer.format("%.2f\t", globalFlowDynamicsUpdator.getGlobalData().getPermanentDensity());
			for (String travelMode : travelModes) {
				writer.format("%.2f\t", this.mode2FlowData.get(travelMode).getPermanentDensity());
			}
			writer.format("%.2f\t", globalFlowDynamicsUpdator.getGlobalData().getPermanentFlow());
			for (String travelMode : travelModes) {
				writer.format("%.2f\t", this.mode2FlowData.get(travelMode).getPermanentFlow());
			}
			writer.format("%.2f\t", globalFlowDynamicsUpdator.getGlobalData().getPermanentAverageVelocity());
			for (String travelMode : travelModes) {
				writer.format("%.2f\t", this.mode2FlowData.get(travelMode).getPermanentAverageVelocity());
			}

			if( travelModes.length > 1 ) {

				writer.format("%.2f\t", passingEventsUpdator.getNoOfCarsPerKm());

				writer.format("%.2f\t", passingEventsUpdator.getAvgBikesPassingRate());
			}

			writer.print("\n");
		}

		if(isWritingEventsFileForEachIteration) {
			assert eventWriter != null;
			eventWriter.closeFile();
		}
	}

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

		if (this.scenario.getConfig().network().isTimeVariantNetwork()) {
			qSim.addMobsimEngine(new NetworkChangeEventsEngine());
		}

		//modification: Mobsim needs to know the different vehicle types (and their respective physical parameters)
		final Map<String, VehicleType> travelModesTypes =
				mode2FlowData
						.entrySet()
						.stream()
						.collect( Collectors.toMap(Map.Entry::getKey, e -> e.getValue().getVehicleType()) );

		AgentSource agentSource = new AgentSource() {
			@Override
			public void insertAgentsIntoMobsim() {

				for ( Id<Person> personId : person2Mode.keySet()) {
					String travelMode = person2Mode.get(personId);
					double randDouble = MatsimRandom.getRandom().nextDouble();
					double actEndTime = randDouble * MAX_ACT_END_TIME;

					MobsimAgent agent = new MySimplifiedRoundAndRoundAgent(personId, actEndTime, travelMode);
					qSim.insertAgentIntoMobsim(agent);

					final Vehicle vehicle = VehicleUtils.getFactory().createVehicle(Id.create(agent.getId(), Vehicle.class), travelModesTypes.get(travelMode));
					final Id<Link> linkId4VehicleInsertion = fdNetworkGenerator.getTripDepartureLinkId();
					qSim.createAndParkVehicleOnLink(vehicle, linkId4VehicleInsertion);
				}
			}
		};

		qSim.addAgentSource(agentSource);

		if ( isUsingLiveOTFVis ) {
			// otfvis configuration.  There is more you can do here than via file!
			final OTFVisConfigGroup otfVisConfig = ConfigUtils.addOrGetModule(qSim.getScenario().getConfig(), OTFVisConfigGroup.GROUP_NAME, OTFVisConfigGroup.class);
			otfVisConfig.setDrawTransitFacilities(false) ; // this DOES work
			OnTheFlyServer server = OTFVis.startServerAndRegisterWithQSim(sc.getConfig(), sc, events, qSim);
			OTFClientLive.run(sc.getConfig(), server);
		}
		return qSim;
	}

	private void updateTransimFileNameAndDir(List<Integer> runningPoint) {
		String outputDir = scenario.getConfig().controler().getOutputDirectory();
		//Check if Transim veh dir exists, if not create it
		if(! new File(outputDir+"/TransVeh/").exists() ) new File(outputDir+"/TransVeh/").mkdir();
		//first, move T.veh.gz file
		String sourceTVehFile = outputDir+"/ITERS/it.0/0.T.veh.gz";
		String targetTVehFilen = outputDir+"/TransVeh/T_"+runningPoint.toString()+".veh.gz";
		try {
			Files.move(new File(sourceTVehFile).toPath(), new File(targetTVehFilen).toPath(), StandardCopyOption.REPLACE_EXISTING);
		} catch (IOException e) {
			throw new RuntimeException("File not found.");
		}
	}

	private void cleanOutputDir(){
		String outputDir = scenario.getConfig().controler().getOutputDirectory();
		IOUtils.deleteDirectoryRecursively(new File(outputDir+"/ITERS/").toPath());
		IOUtils.deleteDirectoryRecursively(new File(outputDir+"/tmp/").toPath());
		new File(outputDir+"/logfile.log").delete();
		new File(outputDir+"/logfileWarningsErrors.log").delete();
		new File(outputDir+"/scorestats.txt").delete();
		new File(outputDir+"/modestats.txt").delete();
		new File(outputDir+"/stopwatch.txt").delete();
		new File(outputDir+"/traveldistancestats.txt").delete();
	}

	private void openFileAndWriteHeader(String dir) {
		try {
			writer = new PrintStream(dir);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		writer.print("n \t");
		for (String travelMode : travelModes) {
			String str = this.mode2FlowData.get(travelMode).getModeId().toString();
			String strn = "n_" + str;
			writer.print(strn + "\t");
		}
		writer.print("k \t");
		for (String travelMode : travelModes) {
			String str = this.mode2FlowData.get(travelMode).getModeId().toString();
			String strk = "k_" + str;
			writer.print(strk + "\t");
		}
		writer.print("q \t");
		for (String travelMode : travelModes) {
			String str = this.mode2FlowData.get(travelMode).getModeId().toString();
			String strq = "q_" + str;
			writer.print(strq + "\t");
		}
		writer.print("v \t");
		for (String travelMode : travelModes) {
			String str = this.mode2FlowData.get(travelMode).getModeId().toString();
			String strv = "v_" + str;
			writer.print(strv + "\t");
		}

		if( travelModes.length > 1 ) {
			writer.print("noOfCarsPerkm \t");

			writer.print("avgBikePassingRatePerkm \t");
		}

		writer.print("\n");
	}

	private void closeFile() {
		writer.close();
	}

	private static String arraytostring(Integer[] list){
		String str = "";
		for (Integer aList : list) {
			str += aList.intValue();
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
			appender = new FileAppender(layout, runDir+"/fdlogfile.log",false);
		} catch (IOException e1) {
			throw new RuntimeException("File not found.");
		}
		LOG.addAppender(appender);
	}

	static class MySimplifiedRoundAndRoundAgent implements MobsimAgent, MobsimDriverAgent {

		private static final Id<Link> FIRST_LINK_ID_OF_MIDDEL_BRANCH_OF_TRACK = fdNetworkGenerator.getFirstLinkIdOfMiddleLinkOfTrack();
		private static final Id<Link> LAST_LINK_ID_OF_BASE = fdNetworkGenerator.getLastLinkIdOfBase();
		private static final Id<Link> LAST_LINK_ID_OF_TRACK = fdNetworkGenerator.getLastLinkIdOfTrack();
		private static final Id<Link> FIRST_LINK_LINK_ID_OF_BASE =  fdNetworkGenerator.getFirstLinkIdOfTrack();
		private static final Id<Link> ORIGIN_LINK_ID = fdNetworkGenerator.getTripDepartureLinkId();
		private static final Id<Link> DESTINATION_LINK_ID = fdNetworkGenerator.getTripArrivalLinkId();

		private final Id<Person> personId;
		private final Id<Vehicle> plannedVehicleId;
		private final String mode;
		private final double actEndTime;

		private MobsimVehicle vehicle ;
		private boolean isArriving= false;

		MySimplifiedRoundAndRoundAgent(Id<Person> agentId, double actEndTime, String travelMode) {
			personId = agentId;
			mode = travelMode;
			this.actEndTime = actEndTime;
			this.plannedVehicleId = Id.create(agentId, Vehicle.class);
		}

		private Id<Link> currentLinkId = ORIGIN_LINK_ID;
		private State agentState= State.ACTIVITY;

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

			if (FundamentalDiagramDataGenerator.globalFlowDynamicsUpdator.isPermanent()){
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
				// TODO: if the link ids are not consecutive numbers, this will not work.
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
			return this.chooseNextLinkId()==null ;
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
			if(isArriving && this.agentState.equals(State.ACTIVITY)) {
				return Double.POSITIVE_INFINITY; // let agent go to sleep.
			}
			return this.actEndTime;
		}

		@Override
		public void endActivityAndComputeNextState(double now) {
			agentState= State.LEG;
		}

		@Override
		public void endLegAndComputeNextState(double now) {
			agentState= State.ACTIVITY;
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

		@Override
		public Facility<? extends Facility<?>> getCurrentFacility() {
			throw new RuntimeException("not implemented") ;
		}

		@Override
		public Facility<? extends Facility<?>> getDestinationFacility() {
			throw new RuntimeException("not implemented") ;
		}
	}
}