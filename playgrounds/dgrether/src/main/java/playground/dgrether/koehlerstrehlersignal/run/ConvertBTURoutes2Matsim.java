/**
 * 
 */
package playground.dgrether.koehlerstrehlersignal.run;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.api.internal.MatsimWriter;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.routes.LinkNetworkRouteImpl;
import org.matsim.core.scenario.ScenarioUtils;

import playground.dgrether.DgPaths;
import playground.dgrether.koehlerstrehlersignal.data.DgCommodities;
import playground.dgrether.koehlerstrehlersignal.data.DgCommodity;
import playground.dgrether.koehlerstrehlersignal.data.DgStreet;
import playground.dgrether.koehlerstrehlersignal.data.TtPath;
import playground.dgrether.koehlerstrehlersignal.ids.DgIdConverter;
import playground.dgrether.koehlerstrehlersignal.ids.DgIdPool;
import playground.dgrether.koehlerstrehlersignal.solutionconverter.KS2015RouteXMLParser;

/**
 * @author tthunig
 */
public class ConvertBTURoutes2Matsim {

	private static final Logger log = Logger
			.getLogger(ConvertBTURoutes2Matsim.class);

	private boolean createRouteChoice = true;

	private DgCommodities btuComsWithRoutes;
	private DgIdConverter idConverter;

	private Population population;
	private Network network;

	private double startTimeSec = 0.0 * 3600.0;
	private double endTimeSec = 23.99 * 3600.0;

	private void convertRoutes(String directory, String inputFile,
			double startTime, double endTime, String networkFile,
			String outputFile, boolean createRouteChoice) {

		this.createRouteChoice = createRouteChoice;

		DgIdPool idPool = DgIdPool.readFromFile(directory
				+ "id_conversions.txt");
		this.idConverter = new DgIdConverter(idPool);

		// parse btu routes from xml file
		KS2015RouteXMLParser routeParser = new KS2015RouteXMLParser(
				this.idConverter);
		routeParser.readFile(directory + inputFile);
		this.btuComsWithRoutes = routeParser.getComsWithRoutes();

		// some more preparation
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils
				.createConfig());
		(new MatsimNetworkReader(scenario)).readFile(directory + networkFile);
		// network saved in scenario
		this.network = scenario.getNetwork();
		this.population = ScenarioUtils.createScenario(
				ConfigUtils.createConfig()).getPopulation();
		this.startTimeSec = startTime;
		this.endTimeSec = endTime;

		// convert routes and write them into this.population
		createMatsimPlans();

		// write population as plans file
		MatsimWriter popWriter = new PopulationWriter(this.population,
				this.network);
		popWriter.write(outputFile);
		log.info("plans file with btu routes written to " + outputFile);
	}

	private void createMatsimPlans() {
		log.info("lights will be skipped in the matsim route");

		int maxNumberOfPlans = Integer.MIN_VALUE;
		for (DgCommodity com : this.btuComsWithRoutes.getCommodities().values()) {
			// note: btu routes don't contain the first link of the matsim
			// route, because they were converted to the end-node of the link

			// collect common information for all routes
			Id<Link> matsimSourceLinkId;
			try {
				// choose the link ending in the source node as start
				matsimSourceLinkId = this.idConverter
						.convertToCrossingNodeId2LinkId(com.getSourceNodeId());
			} catch (IllegalStateException e) {
				log.warn("Source crossing "
						+ com.getSourceNodeId()
						+ "is not expanded. The matsim source link may not be unique.");
				// choose an arbitrary inLink of the source crossing as start
				Id<Node> matsimNodeId = this.idConverter
						.convertNotExpandedCrossingNodeId2NodeId(com
								.getSourceNodeId());
				matsimSourceLinkId = this.network.getNodes().get(matsimNodeId)
						.getInLinks().get(0).getId();
			}

			List<Leg> legs = new ArrayList<>();
			Id<Link> matsimEndLinkId = null;
			
			for (TtPath path : com.getPaths().values()) {
				List<Id<Link>> matsimLinks = new ArrayList<>();

				// add the first link to the route which is missing in
				// the btu route
				matsimLinks.add(matsimSourceLinkId);

				// add all links to the route that are used in the btu path,
				// but no lights
				for (Id<DgStreet> street : path.getPath()) {
					Id<Link> linkId = null;
					boolean lights = false;
					try {
						linkId = this.idConverter
								.convertStreetId2LinkId(street);
					} catch (IllegalStateException e) {
						// most light id's can't be converted and create
						// exceptions
						lights = true;
					}
					if (!this.network.getLinks().containsKey(linkId)) {
						// lights ending with "88" seem like links and don't
						// create an exception.
						// with this request we detect them.
						lights = true;
					}
					if (!lights) {
						// add all links but no lights to the matsim route
						matsimLinks.add(linkId);
					}
				}				

				// save the last link of the route as the end link
				// (it's the same for every path because of conversion)
				matsimEndLinkId = matsimLinks.get(matsimLinks.size()-1);
				
				// remove first and last link because route should not
				// contain start and end link
				if (!matsimLinks.isEmpty())
					matsimLinks.remove(0);
				if (!matsimLinks.isEmpty())
					matsimLinks.remove(matsimLinks.size() - 1);

				// create leg with this route
				Route route = new LinkNetworkRouteImpl(matsimSourceLinkId,
						matsimLinks, matsimEndLinkId);
				Leg leg = this.population.getFactory().createLeg(
						TransportMode.car);
				leg.setRoute(route);
				legs.add(leg);

				if (!this.createRouteChoice) {
					// create agents with one single leg
					maxNumberOfPlans = 1;

					if ((path.getFlow() % 1.0) == 0.0) {
						log.warn("Some flow values are non integer. To many agents were created. "
								+ "Please use 'createRouteChoice = true'.");
					}
					// create a dummy-dummy trip for each flow unit
					for (int i = 0; i < path.getFlow(); i++) {
						Person person = this.population.getFactory()
								.createPerson(
										Id.create(path.getId().toString() + "_"
												+ i, Person.class));
						Plan plan = this.population.getFactory().createPlan();
						Activity start = this.population.getFactory().createActivityFromLinkId(
								"dummy", matsimSourceLinkId);
						start.setEndTime(createEndTime());
						plan.addActivity(start);
						plan.addLeg(leg);
						Activity end = this.population.getFactory().createActivityFromLinkId(
								"dummy", matsimEndLinkId);
						plan.addActivity(end);
						person.addPlan(plan);
						this.population.addPerson(person);
					}
				}
			}
			if (this.createRouteChoice) {
				// create different routes for each agent as a choice set
				
				if (legs.size() > maxNumberOfPlans)
					maxNumberOfPlans = legs.size();
				
				for (int i = 0; i < com.getFlow(); i++) {
					// note: com.getFlow() is integer in contrast to
					// path.getFlow() (because of conversion)

					Person person = this.population.getFactory().createPerson(
							Id.create(com.getId().toString() + "_" + i,
									Person.class));
					Activity start = this.population.getFactory().createActivityFromLinkId(
							"dummy", matsimSourceLinkId);
					start.setEndTime(createEndTime());
					Activity end = this.population.getFactory().createActivityFromLinkId(
							"dummy", matsimEndLinkId);

					for (Leg leg : legs) {
						Plan plan = this.population.getFactory().createPlan();
						plan.addActivity(start);
						plan.addLeg(leg);
						plan.addActivity(end);
						person.addPlan(plan);
					}
					this.population.addPerson(person);

				}
			}
		}
		log.info("The maximal number of plans per agent is " + maxNumberOfPlans);
	}

	// create uniformly distributed activity end time in the given time interval
	private double createEndTime() {
		double r = Math.random();
		return this.startTimeSec + r * (this.endTimeSec - this.startTimeSec);
	}

	public static void main(String[] args) {

		// if true, all agents of the same commodity have the same route choice
		// set. if false, the converter creates agents with exactly one route
		// depending on the flow value in the btu model
		// note: if this flow value is non integer, it rounds up to the next
		// integer. i.e. you'll get a different number of agents in total!
		boolean createRouteChoice = true;

		String directory = DgPaths.REPOS
				+ "shared-svn/projects/cottbus/data/optimization/cb2ks2010/"
				+ "2015-02-25_minflow_50.0_morning_peak_speedFilter15.0_SP_tt_cBB50.0_sBB500.0/";

		String btuRoutesFilename = "routeComparison/paths.xml";
		String networkFilename = "network_small_simplified.xml.gz";

		// TODO change this? create same activity end time for all agents and enable time choice
		double startTimeSec = 5.5 * 3600.0;
		double endTimeSec = 9.5 * 3600.0;

		String[] filenameAttributes = btuRoutesFilename.split("/");
		String outputFilename = directory + "routeComparison/2015-03-10_";
		if (createRouteChoice)
			outputFilename += "ksOptRouteChoice_";
		else
			outputFilename += "ksOptTripPlans_";
		outputFilename += filenameAttributes[filenameAttributes.length - 1];

		new ConvertBTURoutes2Matsim().convertRoutes(directory,
				btuRoutesFilename, startTimeSec, endTimeSec, networkFilename,
				outputFilename, createRouteChoice);
	}

}
