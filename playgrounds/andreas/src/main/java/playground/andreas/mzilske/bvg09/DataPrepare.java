/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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

package playground.andreas.mzilske.bvg09;

import java.util.Collection;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.contrib.otfvis.OTFVis;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.QSimUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.network.io.NetworkWriter;
import org.matsim.core.population.algorithms.PersonAlgorithm;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.router.PlanRouter;
import org.matsim.core.router.TripRouter;
import org.matsim.core.router.TripRouterFactoryBuilderWithDefaults;
import org.matsim.core.router.costcalculators.FreespeedTravelTimeAndDisutility;
import org.matsim.core.router.util.DijkstraFactory;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.Umlauf;
import org.matsim.pt.UmlaufInterpolator;
import org.matsim.pt.config.TransitConfigGroup;
import org.matsim.pt.router.TransitRouterConfig;
import org.matsim.pt.router.TransitRouterImpl;
import org.matsim.pt.transitSchedule.TransitScheduleWriterV1;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitScheduleWriter;
import org.matsim.pt.utils.CreatePseudoNetwork;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleCapacity;
import org.matsim.vehicles.VehicleCapacityImpl;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleWriterV1;
import org.matsim.vehicles.VehiclesFactory;
import org.matsim.vis.otfvis.OTFClientLive;
import org.matsim.vis.otfvis.OnTheFlyServer;
import org.matsim.visum.VisumNetwork;
import org.matsim.visum.VisumNetworkReader;

import playground.andreas.mzilske.pt.queuesim.GreedyUmlaufBuilderImpl;

import javax.inject.Provider;

public class DataPrepare {

	private static final Logger log = Logger.getLogger(DataPrepare.class);

	// INPUT FILES
	private static String InVisumNetFile = "/Users/michaelzilske/Desktop/visumnet_utf8woBOM.net";
	private static String InNetworkFile = "/Volumes/Data/VSP/svn/shared-svn/studies/schweiz-ivtch/baseCase/network/ivtch-osm.xml";
	private static String InInputPlansFileWithXY2Links = "/Volumes/Data/VSP/coding/eclipse35/thesis-data/application/plans.census2000ivtch1pct.dilZh30km.sample.xml.gz";

	// INTERMEDIARY FILES
	private static String IntermediateTransitNetworkFile = "../berlin-bvg09/pt/alles_mit_umlaeufen/test/transit_network.xml";
	private static String IntermediateTransitScheduleWithoutNetworkFile = "/Users/michaelzilske/Desktop/transit_schedule_without_network.xml";

	// OUTPUT FILES
	private static String OutTransitScheduleWithNetworkFile = "../berlin-bvg09/pt/alles_mit_umlaeufen/test/transitSchedule.networkOevModellBln.xml";
	private static String OutVehicleFile = "../berlin-bvg09/pt/alles_mit_umlaeufen/test/vehicles.oevModellBln.xml";
	private static String OutMultimodalNetworkFile = "../berlin-bvg09/pt/alles_mit_umlaeufen/test/network.multimodal.mini.xml";
	private static String OutRoutedPlanFile = "../berlin-bvg09/pt/alles_mit_umlaeufen/test/plan.routedOevModell.BVB344.moreLegPlan_Agent.xml";


	private final MutableScenario scenario;
	private final Config config;

	private Network pseudoNetwork;

	public DataPrepare() {
		this.scenario = (MutableScenario) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		this.config = this.scenario.getConfig();
	}

	protected void prepareConfig() {
		this.config.transit().setUseTransit(true);
	}

	protected void convertSchedule() {
		final VisumNetwork vNetwork = new VisumNetwork();
		log.info("reading visum network.");
		new VisumNetworkReader(vNetwork).read(InVisumNetFile);
		log.info("converting visum data to TransitSchedule.");
		Visum2TransitSchedule converter = new Visum2TransitSchedule(vNetwork, this.scenario.getTransitSchedule(), this.scenario.getTransitVehicles());

		// configure how transport modes must be converted
		// the ones for Berlin
		converter.registerTransportMode("B", "bus");
		converter.registerTransportMode("F", TransportMode.walk);
		converter.registerTransportMode("K", "bus");
		converter.registerTransportMode("L", "other");
		converter.registerTransportMode("P", TransportMode.car);
		converter.registerTransportMode("R", TransportMode.bike);
		converter.registerTransportMode("S", "train");
		converter.registerTransportMode("T", "tram");
		converter.registerTransportMode("U", "train");
		converter.registerTransportMode("V", "other");
		converter.registerTransportMode("W", "bus");
		converter.registerTransportMode("Z", "train");
		converter.convert();
	}

	private void writeScheduleAndVehicles() {
		log.info("writing TransitSchedule to file.");
		new TransitScheduleWriterV1(this.scenario.getTransitSchedule()).write(IntermediateTransitScheduleWithoutNetworkFile);
		log.info("writing vehicles to file.");
		new VehicleWriterV1(this.scenario.getTransitVehicles()).writeFile(OutVehicleFile);
		new NetworkWriter(this.pseudoNetwork).write(IntermediateTransitNetworkFile);
		new TransitScheduleWriter(this.scenario.getTransitSchedule()).writeFile(OutTransitScheduleWithNetworkFile);
	}

	protected void createNetworkFromSchedule() {
		this.pseudoNetwork = NetworkUtils.createNetwork();
		new CreatePseudoNetwork(this.scenario.getTransitSchedule(), this.pseudoNetwork, "tr_").createNetwork();
	}

	protected void mergeNetworks() {
		MutableScenario transitScenario = (MutableScenario) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		Network transitNetwork = transitScenario.getNetwork();
		MutableScenario streetScenario = (MutableScenario) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		Network streetNetwork = streetScenario.getNetwork();
		new MatsimNetworkReader(transitScenario.getNetwork()).readFile(IntermediateTransitNetworkFile);
		new MatsimNetworkReader(streetScenario.getNetwork()).readFile(InNetworkFile);
		MergeNetworks.merge(streetNetwork, "", transitNetwork, "", (Network) this.scenario.getNetwork());
		new NetworkWriter(this.scenario.getNetwork()).write(OutMultimodalNetworkFile);
	}

	protected void routePopulation() {
		Population pop = this.scenario.getPopulation();
		new PopulationReader(this.scenario).readFile(InInputPlansFileWithXY2Links);

		DijkstraFactory dijkstraFactory = new DijkstraFactory();
		FreespeedTravelTimeAndDisutility timeCostCalculator = new FreespeedTravelTimeAndDisutility(this.scenario.getConfig().planCalcScore());
		TransitConfigGroup transitConfig = new TransitConfigGroup();
		
		TransitRouterConfig transitRouterConfig = new TransitRouterConfig(this.scenario.getConfig().planCalcScore()
				, this.scenario.getConfig().plansCalcRoute(), this.scenario.getConfig().transitRouter(),
				this.scenario.getConfig().vspExperimental());

		log.warn( "please chack that this still does what you want. td, oct 2013");
		final TripRouterFactoryBuilderWithDefaults builder =
				new TripRouterFactoryBuilderWithDefaults();
		builder.setLeastCostPathCalculatorFactory( dijkstraFactory );
		final Provider<TripRouter> factory = builder.build( scenario );
		final PersonAlgorithm router =
				new PlanRouter(
						factory.get(
						));
		//PlansCalcTransitRoute router = new PlansCalcTransitRoute(this.scenario.getConfig().plansCalcRoute(),
		//		this.scenario.getNetwork(), timeCostCalculator, timeCostCalculator, dijkstraFactory, new ModeRouteFactory(),
		//		transitConfig, new TransitRouterImpl(transitRouterConfig, this.scenario.getTransitSchedule() ), this.scenario.getTransitSchedule());
		log.info("start pt-router");
		for ( Person p : pop.getPersons().values() ) {
			router.run(p);
		}
		log.info("write routed plans out.");
		new PopulationWriter(pop, this.scenario.getNetwork()).write(OutRoutedPlanFile);
	}

	protected void visualizeRouterNetwork() {
		
		TransitRouterConfig transitRouterConfig = new TransitRouterConfig(this.scenario.getConfig().planCalcScore()
				, this.scenario.getConfig().plansCalcRoute(), this.scenario.getConfig().transitRouter(),
				this.scenario.getConfig().vspExperimental());
		
		TransitRouterImpl router = new TransitRouterImpl(transitRouterConfig, this.scenario.getTransitSchedule() );
		Network routerNet = router.getTransitRouterNetwork();

		log.info("create vis network");
		MutableScenario visScenario = (MutableScenario) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		Network visNet = visScenario.getNetwork();

		for (Node node : routerNet.getNodes().values()) {
			visNet.getFactory().createNode(node.getId(), node.getCoord());
			visNet.addNode(node);
		}
		for (Link link : routerNet.getLinks().values()) {
			Link l = visNet.getFactory().createLink(link.getId(), visNet.getNodes().get(link.getFromNode().getId()), visNet.getNodes().get(link.getToNode().getId()));
			l.setLength(link.getLength());
			l.setFreespeed(link.getFreespeed());
			l.setCapacity(link.getCapacity());
			l.setNumberOfLanes(link.getNumberOfLanes());
		}

		log.info("write routerNet.xml");
		new NetworkWriter(visNet).write("visNet.xml");

		log.info("start visualizer");
		EventsManager events = EventsUtils.createEventsManager();
		QSim otfVisQSim = QSimUtils.createDefaultQSim(visScenario, events);
		OnTheFlyServer server = OTFVis.startServerAndRegisterWithQSim(visScenario.getConfig(), visScenario, events, otfVisQSim);
		OTFClientLive.run(visScenario.getConfig(), server);
		otfVisQSim.run();
	}

	private void buildUmlaeufe() {
		Collection<TransitLine> transitLines = this.scenario.getTransitSchedule().getTransitLines().values();
		GreedyUmlaufBuilderImpl greedyUmlaufBuilder = new GreedyUmlaufBuilderImpl(new UmlaufInterpolator(this.scenario.getNetwork(), this.scenario.getConfig().planCalcScore()), transitLines);
		Collection<Umlauf> umlaeufe = greedyUmlaufBuilder.build();

		VehiclesFactory vb = this.scenario.getTransitVehicles().getFactory();
		VehicleType vehicleType = vb.createVehicleType(Id.create(
				"defaultTransitVehicleType", VehicleType.class));
		VehicleCapacity capacity = new VehicleCapacityImpl();
		capacity.setSeats(Integer.valueOf(101));
		capacity.setStandingRoom(Integer.valueOf(0));
		vehicleType.setCapacity(capacity);
		this.scenario.getTransitVehicles().addVehicleType(
				vehicleType);

		long vehId = 0;
		for (Umlauf umlauf : umlaeufe) {
			Vehicle veh = vb.createVehicle(Id.create("veh_"+ Long.toString(vehId++), Vehicle.class), vehicleType);
			this.scenario.getTransitVehicles().addVehicle( veh);
			umlauf.setVehicleId(veh.getId());
		}
	}


	private void emptyVehicles() {
		this.scenario.getTransitVehicles().getVehicles().clear();
	}


	public static void run(String inVisumFile, String inNetworkFile, String inInputPlansFile,
			String interTransitNetworkFile, String interTransitScheduleWithoutNetworkFile,
			String outTransitScheduleWithNetworkFile, String outVehicleFile, String outMultimodalNetworkFile,
			String outRoutedPlansFile){

		DataPrepare.InVisumNetFile = inVisumFile;
		DataPrepare.InNetworkFile = inNetworkFile;
		DataPrepare.InInputPlansFileWithXY2Links = inInputPlansFile;

		DataPrepare.IntermediateTransitNetworkFile = interTransitNetworkFile;
		DataPrepare.IntermediateTransitScheduleWithoutNetworkFile = interTransitScheduleWithoutNetworkFile;

		DataPrepare.OutTransitScheduleWithNetworkFile = outTransitScheduleWithNetworkFile;
		DataPrepare.OutVehicleFile = outVehicleFile;
		DataPrepare.OutMultimodalNetworkFile = outMultimodalNetworkFile;
		DataPrepare.OutRoutedPlanFile = outRoutedPlansFile;

		DataPrepare app = new DataPrepare();
		app.prepareConfig();
		app.convertSchedule();
		app.createNetworkFromSchedule();
		app.emptyVehicles();
		app.buildUmlaeufe();

		app.writeScheduleAndVehicles();


		app.mergeNetworks();
		app.routePopulation();
//		app.visualizeRouterNetwork();

		log.info("done.");
	}

	public static void main(final String[] args) {
		DataPrepare app = new DataPrepare();
		app.prepareConfig();
		app.convertSchedule();
		app.createNetworkFromSchedule();
		app.emptyVehicles();
		app.buildUmlaeufe();

		app.writeScheduleAndVehicles();


//		app.mergeNetworks();
//		app.routePopulation();
//		app.visualizeRouterNetwork();

		log.info("done.");
	}

}
