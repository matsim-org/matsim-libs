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
package org.matsim.integration.weekly.fundamentaldiagram;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Stream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.AxisLocation;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.CombinedDomainXYPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.QSimConfigGroup.LinkDynamics;
import org.matsim.core.config.groups.QSimConfigGroup.TrafficDynamics;
import org.matsim.core.config.groups.VspExperimentalConfigGroup.VspDefaultsCheckingLevel;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.mobsim.framework.AgentSource;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.framework.MobsimDriverAgent;
import org.matsim.core.mobsim.qsim.PopulationModule;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.QSimBuilder;
import org.matsim.core.mobsim.qsim.interfaces.MobsimVehicle;
import org.matsim.core.mobsim.qsim.qnetsimengine.QVehicle;
import org.matsim.core.mobsim.qsim.qnetsimengine.QVehicleImpl;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.misc.OptionalTime;
import org.matsim.facilities.Facility;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;

/**
 * Generates fundamental diagrams for the all combination of link and traffic dynamics for car/bike and car/truck groups.
 * Also generates car/bike FDs using fast capacity update method.
 * Results are archived at @see <a href="https://svn.vsp.tu-berlin.de/testresults/">https://svn.vsp.tu-berlin.de/testresults/</a>
 * @author amit
 */

public class CreateAutomatedFDTest {
	private final Map<Id<Person>,String> person2Mode = new HashMap<>();

	public static Stream<Arguments> arguments() {
		List<Arguments> result = new LinkedList<>();
		for (LinkDynamics ld : LinkDynamics.values()) {
			for (TrafficDynamics td : TrafficDynamics.values()) {
				result.add(Arguments.of(ld, td));
			}
		}
		return result.stream();

	}

	@ParameterizedTest
	@MethodSource("arguments")
	void fdsCarTruck(LinkDynamics linkDynamics, TrafficDynamics trafficDynamics){
		this.travelModes = new String [] {"car","truck"};
		run(false, linkDynamics, trafficDynamics);
	}

	@ParameterizedTest
	@MethodSource("arguments")
	void fdsCarBike(LinkDynamics linkDynamics, TrafficDynamics trafficDynamics){
		run(false,linkDynamics, trafficDynamics);
	}

	@ParameterizedTest
	@MethodSource("arguments")
	void fdsCarBikeFastCapacityUpdate(LinkDynamics linkDynamics, TrafficDynamics trafficDynamics){
		run(true, linkDynamics, trafficDynamics);
	}

	@ParameterizedTest
	@MethodSource("arguments")
	void fdsCarOnly(LinkDynamics linkDynamics, TrafficDynamics trafficDynamics){
		this.travelModes = new String [] {"car"};
		run(false, linkDynamics, trafficDynamics);
	}

	@RegisterExtension private MatsimTestUtils helper = new MatsimTestUtils();

	private String [] travelModes = new String [] {"car","bike"};
	public final Id<Link> flowDynamicsMeasurementLinkId = Id.createLinkId(0);
	private Map<String, VehicleType> modeVehicleTypes;
	private Map<Id<VehicleType>, TravelModesFlowDynamicsUpdator> mode2FlowData;
	GlobalFlowDynamicsUpdator globalFlowDynamicsUpdator;

	private final static Logger LOG = LogManager.getLogger(CreateAutomatedFDTest.class);

	private void run(final boolean isUsingFastCapacityUpdate, LinkDynamics linkDynamics, TrafficDynamics trafficDynamics) {

		MatsimRandom.reset();

		final Config config = ConfigUtils.createConfig();
		config.qsim().setMainModes(Arrays.asList(travelModes));
		config.qsim().setEndTime(14*3600);
		config.qsim().setLinkDynamics(linkDynamics);

		if(linkDynamics.equals(LinkDynamics.SeepageQ)){
			config.qsim().setSeepModes(Arrays.asList("bike"));
			config.qsim().setSeepModeStorageFree(false);
			config.qsim().setRestrictingSeepage(true);
		}

		config.vspExperimental().setVspDefaultsCheckingLevel( VspDefaultsCheckingLevel.abort );
		config.qsim().setTrafficDynamics(trafficDynamics);

		config.qsim().setUsingFastCapacityUpdate(isUsingFastCapacityUpdate);

		// ---

		Scenario scenario = ScenarioUtils.loadScenario(config);
		createNetwork(scenario);

		storeVehicleTypeInfo();


		double networkDensity = 3.*(1000./7.5);

		double sumOfPCUInEachStep = 0.;

		//equal modal split run
		for (String mode : travelModes) {
			sumOfPCUInEachStep += modeVehicleTypes.get(mode).getPcuEquivalents() *  getMinNumberOfAgentAtStart(mode) ;
		};

		int reduceNoOfDataPointsInPlot = 4; // 1--> will generate all possible data points;
		if( sumOfPCUInEachStep >=3 ) reduceNoOfDataPointsInPlot = 1 ;

		int numberOfPoints = (int) Math.ceil( networkDensity/ (reduceNoOfDataPointsInPlot * sumOfPCUInEachStep) ) + 5;

		List<Map<String,Integer>> points2Run = new ArrayList<>();

		for (int m=1; m<numberOfPoints; m++){
			Map<String,Integer> pointToRun = new HashMap<>();
			for (String mode:travelModes){
				pointToRun.put(mode,  getMinNumberOfAgentAtStart(mode)   * m * reduceNoOfDataPointsInPlot);
			}

			double density =0 ;
			for (String mode :pointToRun.keySet()){
				double pcu = this.modeVehicleTypes.get(mode).getPcuEquivalents();
				density += pcu * pointToRun.get(mode);
			}

			if(density <= networkDensity+10){
				System.out.println("Number of Agents - \t"+pointToRun.toString());
				points2Run.add(pointToRun);
			}
		}

		Map<Double, Map<String, Tuple<Double, Double>>> outData = new HashMap<>();

		for(Map<String, Integer> point2run : points2Run){

			System.out.println("\n \n \t \t Running points "+point2run.toString()+"\n \n");

			int count = 0;
			person2Mode.clear();

			for(String mode : point2run.keySet()){
				for(int ii =0; ii < point2run.get(mode); ii++){
					person2Mode.put(Id.createPersonId(count++), mode);
				}
				this.mode2FlowData.get(modeVehicleTypes.get(mode).getId()).setnumberOfAgents(point2run.get(mode));
			}

			EventsManager events = EventsUtils.createEventsManager();
			globalFlowDynamicsUpdator = new GlobalFlowDynamicsUpdator(mode2FlowData);
			events.addHandler(globalFlowDynamicsUpdator);

			final QSim qSim = new QSimBuilder(config) //
					.useDefaults()
					.configureQSimComponents( components -> {
						components.removeNamedComponent(PopulationModule.COMPONENT_NAME);
					} )
					.build(scenario, events);

			final Map<String, VehicleType> travelModesTypes = new HashMap<>();

			for(String mode :travelModes){
				travelModesTypes.put(mode, modeVehicleTypes.get(mode));
			}

			AgentSource agentSource = new AgentSource(){
				@Override
				public void insertAgentsIntoMobsim() {
					for ( Id<Person> personId : person2Mode.keySet()) {
						String travelMode = person2Mode.get(personId);
						double actEndTime = (MatsimRandom.getRandom().nextDouble())*900;

						MobsimAgent agent = new MySimplifiedRoundAndRoundAgent(personId, actEndTime, travelMode);
						qSim.insertAgentIntoMobsim(agent);

						final Vehicle vehicle = VehicleUtils.getFactory().createVehicle(Id.create(agent.getId(), Vehicle.class), travelModesTypes.get(travelMode));
						final Id<Link> linkId4VehicleInsertion = Id.createLinkId("home");

//						qSim.createAndParkVehicleOnLink(vehicle, linkId4VehicleInsertion);
						QVehicle qVehicle = new QVehicleImpl( vehicle ) ; // yyyyyy should use factory. kai, nov'18
						qSim.addParkedVehicle( qVehicle, linkId4VehicleInsertion );

					}
				}
			};

			qSim.addAgentSource(agentSource);

			qSim.run();

			Map<String, Tuple<Double, Double>> mode2FlowSpeed = new HashMap<>();
			for(int i=0; i < travelModes.length; i++){

				Tuple<Double, Double> flowSpeed =
						new Tuple<>(this.mode2FlowData.get(Id.create(travelModes[i],VehicleType.class)).getPermanentFlow(),
								this.mode2FlowData.get(Id.create(travelModes[i],VehicleType.class)).getPermanentAverageVelocity());
				mode2FlowSpeed.put(travelModes[i], flowSpeed);
				outData.put(globalFlowDynamicsUpdator.getGlobalData().getPermanentDensity(), mode2FlowSpeed);
			}
		}

		/*
		 *	Basically overriding the helper.getOutputDirectory() method, such that,
		 *	if file directory does not exists or same file already exists, remove and re-creates the whole dir hierarchy so that
		 *	all existing files are re-written
		 *	else, just keep adding files in the directory.
		 *	This is necessary in order to allow writing different tests results from JUnit parameterization.
		 */

		String outDir  = "test/output/" + CreateAutomatedFDTest.class.getCanonicalName().replace('.', '/') + "/" + helper.getMethodName() + "/";
		String fileName = linkDynamics+"_"+trafficDynamics+".png";

		String outFile ;
		//TODO : what if, there exists some different directory (or files with old filename) => changing method name will keep collecting the old data.
		if(!new File(outDir).exists() || new File(outDir+fileName).exists()){
			outFile = helper.getOutputDirectory()+fileName;
		} else {
			outFile = outDir+fileName;
		}

		//plotting data
		scatterPlot(outData,outFile);
	}

	static int getMinNumberOfAgentAtStart(final String mode) {//equal modal split run
		// only three different modes (and pcus) are used in this test ie -- car(1), truck(3), bike(0.25)
		switch (mode) {
		case "car": return 1;
		case "truck": return 3;
		case "motorbike":
		case "bike": return 4;
		default : throw new RuntimeException("The test is not designed for this "+ mode + "yet.");
		}
	}

	class MySimplifiedRoundAndRoundAgent implements MobsimAgent, MobsimDriverAgent {

		private final Id<Link> ORIGIN_LINK_ID = Id.createLinkId("home");
		private final Id<Link> BASE_LINK_ID = Id.createLinkId(0);
		private final Id<Link> MIDDEL_LINK_ID_OF_TRACK = Id.createLinkId(1);
		private final Id<Link> LAST_LINK_ID_OF_TRACK = Id.createLinkId(2);
		private final Id<Link> DESTINATION_LINK_ID = Id.createLinkId("work");

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
			if (globalFlowDynamicsUpdator.isPermanent()){
				isArriving = true;
			}

			if( ORIGIN_LINK_ID.equals(this.currentLinkId ) ){
				return BASE_LINK_ID;
			} else if( BASE_LINK_ID.equals( this.currentLinkId ) ) {
				if ( isArriving) {
					return DESTINATION_LINK_ID ;
				} else {
					return MIDDEL_LINK_ID_OF_TRACK ;
				}
			} else if(MIDDEL_LINK_ID_OF_TRACK.equals(this.currentLinkId )){
				return LAST_LINK_ID_OF_TRACK;
			} else if(LAST_LINK_ID_OF_TRACK.equals(this.currentLinkId )){
				return BASE_LINK_ID;
			} else return null; // returning null so that agent will arrive.
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
			if(isArriving  && agentState.equals(MobsimAgent.State.ACTIVITY)){
				return Double.POSITIVE_INFINITY;
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
		public OptionalTime getExpectedTravelTime() {
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
		public Facility getCurrentFacility() {
			throw new RuntimeException("not implemented") ;
		}

		@Override
		public Facility getDestinationFacility() {
			throw new RuntimeException("not implemented") ;
		}
	}

	private void storeVehicleTypeInfo() {
		modeVehicleTypes = new HashMap<>();
		mode2FlowData = new HashMap<>();

		VehicleType car = VehicleUtils.getFactory().createVehicleType(Id.create("car", VehicleType.class));
		car.setMaximumVelocity(16.667);
		car.setPcuEquivalents(1.0);
		modeVehicleTypes.put("car", car);

		VehicleType bike = VehicleUtils.getFactory().createVehicleType(Id.create("bike", VehicleType.class));
		bike.setMaximumVelocity(4.167);
		bike.setPcuEquivalents(0.25);
		modeVehicleTypes.put("bike", bike);

		VehicleType truck = VehicleUtils.getFactory().createVehicleType(Id.create("truck",VehicleType.class));
		truck.setMaximumVelocity(8.33);
		truck.setPcuEquivalents(3.);
		modeVehicleTypes.put("truck", truck);

		for (String mode :travelModes){
			TravelModesFlowDynamicsUpdator modeUpdator = new TravelModesFlowDynamicsUpdator(modeVehicleTypes.get(mode));
			mode2FlowData.put(modeVehicleTypes.get(mode).getId(), modeUpdator);
		}
	}

	private void createNetwork(Scenario scenario){
		Network network = scenario.getNetwork();

		double x = -50;
		Node nodeHome = NetworkUtils.createAndAddNode(network, Id.createNodeId("home"), new Coord(x, (double) 0));
		Node node1 = NetworkUtils.createAndAddNode(network, Id.createNodeId(0), new Coord((double) 0, (double) 0));
		Node node2 = NetworkUtils.createAndAddNode(network, Id.createNodeId(1), new Coord((double) 1000, (double) 0));
		Node node3 = NetworkUtils.createAndAddNode(network, Id.createNodeId(2), new Coord((double) 500, 866.0));
		Node nodeWork = NetworkUtils.createAndAddNode(network, Id.createNodeId("work"), new Coord((double) 1050, (double) 0));

		double freeSpeedOnLink = 60/3.6;
		final Node fromNode = nodeHome;
		final Node toNode = node1;
		final double freespeed = freeSpeedOnLink;
		NetworkUtils.createAndAddLink(network,Id.createLinkId("home"), fromNode, toNode, (double) 25, freespeed, (double) 7200, (double) 1 );
		final Node fromNode1 = node1;
		final Node toNode1 = node2;
		final double freespeed1 = freeSpeedOnLink;
		NetworkUtils.createAndAddLink(network,Id.createLinkId(0), fromNode1, toNode1, (double) 1000, freespeed1, (double) 1600, (double) 1 );
		final Node fromNode2 = node2;
		final Node toNode2 = node3;
		final double freespeed2 = freeSpeedOnLink;
		NetworkUtils.createAndAddLink(network,Id.createLinkId(1), fromNode2, toNode2, (double) 1000, freespeed2, (double) 1600, (double) 1 );
		final Node fromNode3 = node3;
		final Node toNode3 = node1;
		final double freespeed3 = freeSpeedOnLink;
		NetworkUtils.createAndAddLink(network,Id.createLinkId(2), fromNode3, toNode3, (double) 1000, freespeed3, (double) 1600, (double) 1 );
		final Node fromNode4 = node2;
		final Node toNode4 = nodeWork;
		final double freespeed4 = freeSpeedOnLink;
		NetworkUtils.createAndAddLink(network,Id.createLinkId("work"), fromNode4, toNode4, (double) 25, freespeed4, (double) 7200, (double) 1 );

		Set<String> allowedModes = new HashSet<>();
		allowedModes.addAll(Arrays.asList(travelModes));

		for(Link l:network.getLinks().values()){
			l.setAllowedModes(allowedModes);
		}
	}

	private void scatterPlot (Map<Double, Map<String, Tuple<Double, Double>>> inputData, String outFile){

		String mode1 = travelModes[0];
		XYSeries carFlow = new XYSeries(mode1+" flow");
		XYSeries carSpeed = new XYSeries(mode1+" speed");

		XYSeries bikeFlow = null;
		XYSeries bikeSpeed = null;

		if(travelModes.length==2) {
			bikeFlow = new XYSeries(travelModes[1]+" flow");
			bikeSpeed = new XYSeries(travelModes[1]+" speed");
		}

		for(double d :inputData.keySet()){
			carFlow.add(d, inputData.get(d).get(mode1).getFirst());
			carSpeed.add(d, inputData.get(d).get(mode1).getSecond());

			if(travelModes.length == 2) {
				bikeFlow.add(d, inputData.get(d).get(travelModes[1]).getFirst());
				bikeSpeed.add(d, inputData.get(d).get(travelModes[1]).getSecond());
			}
		}

		// flow vs density
		XYSeriesCollection flowDataset = new XYSeriesCollection();
		flowDataset.addSeries(carFlow);

		NumberAxis flowAxis = new NumberAxis("Flow (PCU/h)");
		flowAxis.setRange(0.0, 1700.0);

		XYPlot plot1 = new XYPlot(flowDataset, null, flowAxis, new XYLineAndShapeRenderer(false,true));
		plot1.setRangeAxisLocation(AxisLocation.BOTTOM_OR_LEFT);

		// speed vs density
		XYSeriesCollection speedDataset = new XYSeriesCollection();
		speedDataset.addSeries(carSpeed);

		if ( travelModes.length==2 ) {
			flowDataset.addSeries(bikeFlow);
			speedDataset.addSeries(bikeSpeed);
		}

		NumberAxis speedAxis = new NumberAxis("Speed (m/s)");
		speedAxis.setRange(0.0, 17.0);

		XYPlot plot2 = new XYPlot(speedDataset, null, speedAxis, new XYLineAndShapeRenderer(false,true));
		plot2.setRangeAxisLocation(AxisLocation.TOP_OR_LEFT);

		NumberAxis densityAxis = new NumberAxis("Overall density (PCU/km)");
		densityAxis.setRange(0.0,140.00);

		CombinedDomainXYPlot plot = new CombinedDomainXYPlot(densityAxis);
		plot.setGap(10.);
		plot.add(plot1);
		plot.add(plot2);
		plot.setOrientation(PlotOrientation.VERTICAL);

		JFreeChart chart = new JFreeChart("Fundamental diagrams", JFreeChart.DEFAULT_TITLE_FONT, plot, true);

		try {
			ChartUtils.saveChartAsPNG(new File(outFile), chart, 800, 600);
		} catch (IOException e) {
			throw new RuntimeException("Data is not plotted. Reason "+e);
		}
	}

	//==============================================

	class TravelModesFlowDynamicsUpdator {

		private final int NUMBER_OF_MEMORIZED_FLOWS = 10;
		private Id<VehicleType> modeId;
		private VehicleType vehicleType=null;//      Maybe keeping global data in the EventHandler can be smart (ssix, 25.09.13)
		//	     So far programmed to contain also global data, i.e. data without a specific vehicleType (ssix, 30.09.13)
		public int numberOfAgents;
		private double permanentDensity;
		private double permanentAverageVelocity;
		private double permanentFlow;

		private Map<Id<Vehicle>,Double> lastSeenOnStudiedLinkEnter;//records last entry time for every person, but also useful for getting actual number of people in the simulation
		private int speedTableSize;
		private List<Double> speedTable;
		private Double flowTime;
		private List<Double> flowTable900;
		private List<Double> lastXFlows900;;//recording a number of flows to ensure stability
		private boolean speedStability;
		private boolean flowStability;

		public TravelModesFlowDynamicsUpdator(){}

		public TravelModesFlowDynamicsUpdator(VehicleType vT){
			this.vehicleType = vT;
			this.modeId = this.vehicleType.getId();
		}

		public void handle(LinkEnterEvent event){
			if (event.getLinkId().equals(flowDynamicsMeasurementLinkId)){
//				Id<Person> personId = Id.createPersonId(event.getDriverId());
				double nowTime = event.getTime();

				this.updateFlow900(nowTime, this.vehicleType.getPcuEquivalents());
//				this.updateSpeedTable(nowTime, personId);
				this.updateSpeedTable(nowTime, event.getVehicleId() ) ;

				//Checking for stability
				//Making sure all agents are on the track before testing stability
				//Also waiting half an hour to let the database build itself.

				if ((this.getNumberOfDrivingAgents() == this.numberOfAgents) && (nowTime > 1800)){//TODO empirical factor
					if (!(this.speedStability)){
						this.checkSpeedStability();
					}
					if (!(this.flowStability)){
						this.checkFlowStability900();
					}
				}
			}
		}

		private void updateFlow900(double nowTime, double pcuVeh){
			if (nowTime == this.flowTime.doubleValue()){//Still measuring the flow of the same second
				Double nowFlow = this.flowTable900.get(0);
				this.flowTable900.set(0, nowFlow.doubleValue()+pcuVeh);
			} else {//Need to offset the new flow table from existing flow table.
				int timeDifference = (int) (nowTime-this.flowTime.doubleValue());
				if (timeDifference<900){
					for (int i=899-timeDifference; i>=0; i--){
						this.flowTable900.set(i+timeDifference, this.flowTable900.get(i).doubleValue());
					}
					if (timeDifference > 1){
						for (int i = 1; i<timeDifference; i++){
							this.flowTable900.set(i, 0.);
						}
					}
					this.flowTable900.set(0, pcuVeh);
				} else {
					flowTableReset();
				}
				this.flowTime = nowTime;
			}
			updateLastXFlows900();
		}

		private void updateLastXFlows900(){
			Double nowFlow = this.getCurrentHourlyFlow();
			for (int i=NUMBER_OF_MEMORIZED_FLOWS-2; i>=0; i--){
				this.lastXFlows900.set(i+1, this.lastXFlows900.get(i).doubleValue());
			}
			this.lastXFlows900.set(0, nowFlow);
		}

		private void updateSpeedTable(double nowTime, Id<Vehicle> vehicleId){
			if (this.lastSeenOnStudiedLinkEnter.containsKey(vehicleId)){
				double lastSeenTime = lastSeenOnStudiedLinkEnter.get(vehicleId);
				double speed = 1000 * 3 / (nowTime-lastSeenTime);//in m/s!!
				for (int i=speedTableSize-2; i>=0; i--){
					this.speedTable.set(i+1, this.speedTable.get(i).doubleValue());
				}
				this.speedTable.set(0, speed);

				this.lastSeenOnStudiedLinkEnter.put(vehicleId,nowTime);
			} else {
				this.lastSeenOnStudiedLinkEnter.put(vehicleId, nowTime);
			}
			//this.numberOfDrivingAgents = this.lastSeenOnStudiedLinkEnter.size();
		}

		private void checkSpeedStability(){
			double relativeDeviances = 0.;
			double averageSpeed = 0;
			for (int i=0; i<this.speedTableSize; i++){
				averageSpeed += this.speedTable.get(i).doubleValue();
			}
			averageSpeed /= this.speedTableSize;
			for (int i=0; i<this.speedTableSize; i++){
				relativeDeviances += Math.pow( ((this.speedTable.get(i).doubleValue() - averageSpeed) / averageSpeed) , 2);
			}
			relativeDeviances /= travelModes.length;//taking dependence on number of modes away
			if (relativeDeviances < 0.0005){
				this.speedStability = true;
			} else {
				this.speedStability = false;
			}
		}

		private void checkFlowStability900(){
			double absoluteDeviances = this.lastXFlows900.get(this.lastXFlows900.size()-1) - this.lastXFlows900.get(0);
			if (Math.abs(absoluteDeviances) < 1){
				this.flowStability = true;
				if(modeId==null) LOG.info("========== Reaching a certain flow stability for global flow.");
				else LOG.info("========== Reaching a certain flow stability in mode: "+modeId.toString());
			} else {
				this.flowStability = false;
			}
		}

		private void initDynamicVariables() {
			//numberOfAgents for each mode should be initialized at this point
			this.decideSpeedTableSize();
			this.speedTable = new LinkedList<>();
			for (int i=0; i<this.speedTableSize; i++){
				this.speedTable.add(0.);
			}
			this.flowTime = 0.;
			this.flowTable900 = new LinkedList<>();

			flowTableReset();

			this.lastXFlows900 = new LinkedList<>();
			for (int i=0; i<NUMBER_OF_MEMORIZED_FLOWS; i++){
				this.lastXFlows900.add(0.);
			}
			this.speedStability = false;
			this.flowStability = false;
			this.lastSeenOnStudiedLinkEnter = new TreeMap<>();
			this.permanentDensity = 0.;
			this.permanentAverageVelocity =0.;
			this.permanentFlow = 0.;
		}

		private void reset(){
			this.speedTable.clear();
			this.speedStability = false;
			this.flowStability = false;
		}

		private void decideSpeedTableSize() {
			//Ensures a significant speed sampling for every mode size
			//Is pretty empirical and can be changed if necessary (ssix, 16.10.13)
			if (this.numberOfAgents >= 500) {
				this.speedTableSize = 50;
			} else if (this.numberOfAgents >= 100) {
				this.speedTableSize = 20;
			} else if (this.numberOfAgents >= 10) {
				this.speedTableSize = 10;
			} else if (this.numberOfAgents >  0) {
				this.speedTableSize = this.numberOfAgents;
			} else { //case no agents in mode
				this.speedTableSize = 1;
			}
		}

		private void flowTableReset() {
			for (int i=0; i<900; i++){
				this.flowTable900.add(0.);
			}
		}

		private void saveDynamicVariables(){
			//NB: Should not be called upon a modeData without a vehicleType, as this.vehicleType will be null and will throw an exception.
			this.permanentDensity = this.numberOfAgents / (1000.*3) *1000. * this.vehicleType.getPcuEquivalents();
			this.permanentAverageVelocity = this.getActualAverageVelocity();
			LOG.info("Calculated permanent Speed from "+modeId+"'s lastXSpeeds : "+speedTable+"\nResult is : "+this.permanentAverageVelocity);
			this.permanentFlow = this.getSlidingAverageLastXFlows900();
			LOG.info("Calculated permanent Flow from "+modeId+"'s lastXFlows900 : "+lastXFlows900+"\nResult is :"+this.permanentFlow);
		}

		//Getters/Setters
		public double getActualAverageVelocity(){
			double nowSpeed = 0.;
			for (int i=0; i<this.speedTableSize; i++){
				nowSpeed += this.speedTable.get(i);
			}
			nowSpeed /= this.speedTableSize;
			return nowSpeed;
		}

		public double getCurrentHourlyFlow(){
			double nowFlow = 0.;
			for (int i=0; i<900; i++){
				nowFlow += this.flowTable900.get(i);
			}
			return nowFlow*4;
		}

		public double getSlidingAverageLastXFlows900(){
			double average = 0;
			for (double flow : this.lastXFlows900){ average += flow; }
			return average / NUMBER_OF_MEMORIZED_FLOWS;
		}

		public boolean isSpeedStable(){
			return this.speedStability;
		}

		public boolean isFlowStable(){
			return this.flowStability;
		}

		public void setnumberOfAgents(int n){
			this.numberOfAgents = n;
		}

		public double getPermanentDensity(){
			return this.permanentDensity;
		}

		public void setPermanentDensity(double permanentDensity) {
			this.permanentDensity = permanentDensity;
		}

		public double getPermanentAverageVelocity(){
			return this.permanentAverageVelocity;
		}

		public void setPermanentAverageVelocity(double permanentAverageVelocity) {
			this.permanentAverageVelocity = permanentAverageVelocity;
		}

		public double getPermanentFlow(){
			return this.permanentFlow;
		}

		public void setPermanentFlow(double permanentFlow) {
			this.permanentFlow = permanentFlow;
		}

		public int getNumberOfDrivingAgents() {
			return this.lastSeenOnStudiedLinkEnter.size();
		}
	}

	//=======================================

	class GlobalFlowDynamicsUpdator implements LinkEnterEventHandler {

		private Map<Id<VehicleType>, TravelModesFlowDynamicsUpdator> travelModesFlowData;
		private TravelModesFlowDynamicsUpdator globalFlowData;

		private boolean permanentRegime;

		/**
		 * container to store static properties of vehicles and dynamic flow properties during simulation
		 */
		public GlobalFlowDynamicsUpdator(Map<Id<VehicleType>, TravelModesFlowDynamicsUpdator> travelModeFlowDataContainer){
			this.travelModesFlowData = travelModeFlowDataContainer;
			for (Id<VehicleType> vehTyp : this.travelModesFlowData .keySet()){
				this.travelModesFlowData.get(vehTyp).initDynamicVariables();
			}
			this.globalFlowData = new TravelModesFlowDynamicsUpdator();
			this.globalFlowData.setnumberOfAgents(person2Mode.size());
			this.globalFlowData.initDynamicVariables();
			this.permanentRegime = false;
		}

		@Override
		public void reset(int iteration) {
			for (Id<VehicleType> vehTyp : this.travelModesFlowData .keySet()){
				this.travelModesFlowData.get(vehTyp).reset();
			}
			this.globalFlowData.reset();
			this.permanentRegime = false;
		}

		@Override
		public void handleEvent(LinkEnterEvent event) {
			if (!(permanentRegime)){
//				Id<Person> personId = Id.createPersonId(event.getDriverId());

				//Disaggregated data updating methods
				String travelMode = person2Mode.get(event.getVehicleId());

				Id<VehicleType> transportMode = modeVehicleTypes.get(travelMode).getId();
				this.travelModesFlowData.get(transportMode).handle(event);
				double pcuVeh = modeVehicleTypes.get(travelMode).getPcuEquivalents();

				//Aggregated data update
				double nowTime = event.getTime();
				if (event.getLinkId().equals(flowDynamicsMeasurementLinkId)){
					this.globalFlowData.updateFlow900(nowTime, pcuVeh);
					this.globalFlowData.updateSpeedTable(nowTime, event.getVehicleId());
					//Waiting for all agents to be on the track before studying stability
					if ((this.globalFlowData.getNumberOfDrivingAgents() == this.globalFlowData.numberOfAgents) && (nowTime>1800)){	//TODO parametrize this correctly
						/*//Taking speed check out, as it is not reliable on the global speed table
						 *  Maybe making a list of moving averages could be smart,
						 *  but there is no reliable converging process even in that case. (ssix, 25.10.13)
						 * if (!(this.globalData.isSpeedStable())){
							this.globalData.checkSpeedStability();
							System.out.println("Checking speed stability in global data for: "+this.globalData.getSpeedTable());
						}*/
						if (!(this.globalFlowData.isFlowStable())){
							this.globalFlowData.checkFlowStability900();
						}

						//Checking modes stability
						boolean modesStable = true;
						for (Id<VehicleType> vehTyp : this.travelModesFlowData .keySet()){
							if (this.travelModesFlowData.get(vehTyp).numberOfAgents != 0){
								if (! this.travelModesFlowData.get(vehTyp).isSpeedStable() || !(this.travelModesFlowData.get(vehTyp).isFlowStable())) {
									modesStable = false;
									break;
								}
							}
						}
						if (modesStable){
							//Checking global stability
							if ( /*this.globalData.isSpeedStable() &&*/ this.globalFlowData.isFlowStable() ){
								LOG.info("========== Global permanent regime is attained");
								for (Id<VehicleType> vehTyp : this.travelModesFlowData .keySet()){
									this.travelModesFlowData.get(vehTyp).saveDynamicVariables();
								}
								this.globalFlowData.setPermanentAverageVelocity(this.globalFlowData.getActualAverageVelocity());
								//this.permanentFlow = this.getActualFlow();
								this.globalFlowData.setPermanentFlow(this.globalFlowData.getCurrentHourlyFlow());
								double globalDensity = 0.;
								for (TravelModesFlowDynamicsUpdator mode : this.travelModesFlowData.values()){
									globalDensity += mode.getPermanentDensity();
								}
								this.globalFlowData.setPermanentDensity(globalDensity);
								this.permanentRegime = true;
							}
						}
					}
				}
			}
		}

		public boolean isPermanent(){
			return permanentRegime;
		}

		public TravelModesFlowDynamicsUpdator getGlobalData(){
			return this.globalFlowData;
		}
	}
}
