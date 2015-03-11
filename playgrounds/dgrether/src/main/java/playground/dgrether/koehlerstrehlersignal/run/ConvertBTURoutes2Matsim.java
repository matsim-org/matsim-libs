/**
 * 
 */
package playground.dgrether.koehlerstrehlersignal.run;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.api.internal.MatsimWriter;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.routes.LinkNetworkRouteImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.Time;

import playground.dgrether.DgPaths;
import playground.dgrether.koehlerstrehlersignal.data.DgCommodities;
import playground.dgrether.koehlerstrehlersignal.data.DgCommodity;
import playground.dgrether.koehlerstrehlersignal.data.DgCrossingNode;
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

	private DgCommodities btuComsWithRoutes;
	private double currentDepartureTime;
	
	private DgIdConverter idConverter;
	private DgIdPool idPool;

	private Population population;
	private Population popWithRoutes;
	private Network network;

	
	private void convertRoutes(String directory, String inputFile,
			String networkFile,	String outputFile, String populationFile) {

		this.idPool = DgIdPool.readFromFile(directory + "id_conversions.txt");
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
		(new MatsimPopulationReader(scenario)).readFile(directory
				+ populationFile);
		// population (without routes) saved in scenario
		this.population = scenario.getPopulation();
		// create container for the population with routes
		this.popWithRoutes = ScenarioUtils.createScenario(
				ConfigUtils.createConfig()).getPopulation();
		
		// convert routes and write them into this.population
		addRoutesToPlans();

		// write population as plans file
		MatsimWriter popWriter = new PopulationWriter(this.popWithRoutes,
				this.network);
		popWriter.write(outputFile);
		log.info("plans file with btu routes written to " + outputFile);
	}

	// add the btu routes to the orignal matsim agents
	private void addRoutesToPlans() {
		log.info("lights will be skipped in the matsim routes");

		int maxNumberOfPlans = Integer.MIN_VALUE;
		
		for (DgCommodity com : this.btuComsWithRoutes.getCommodities().values()) {

			// reconstruct matsim start and end link
			Id<Link> matsimStartLinkId = createMatsimLink(
					com.getSourceNodeId(), com.getId(), true);
			Id<Link> matsimEndLinkId = createMatsimLink(com.getDrainNodeId(),
					com.getId(), false);

			// convert btu paths into matsim legs
			List<Leg> legs = convertPathsToLegs(com, matsimStartLinkId,
					matsimEndLinkId);

				// create route choice set for each agent with btu routes

				if (legs.size() > maxNumberOfPlans)
					maxNumberOfPlans = legs.size();

				// commodity flow values should be integer because of conversion.
				// regarding to rounding issues of single path flow values in the btu model, 
				// the sum of all path flow values might be non integer, though.
				// -> round it to the next integer.
				int roundedFlow = (int) Math.round(com.getFlow());
				
				for (int i = 0; i < roundedFlow; i++) {
					// note: com.getFlow() is integer in contrast to
					// path.getFlow() (because of conversion)

					// look for a matsim agent with the same start and end link
					// (remove's it from this.population)
					Person correspondingPerson = getCorrespondingMatsimAgent(
							matsimStartLinkId, matsimEndLinkId);
					// remove it's former plan
					correspondingPerson.getPlans().clear();

					// add all btu routes of the commodity as plans
					for (Leg leg : legs) {
						Plan plan = this.population.getFactory().createPlan();
						Activity start = this.population.getFactory().
								createActivityFromLinkId("dummy", 
										matsimStartLinkId);
						start.setEndTime(this.currentDepartureTime);
						plan.addActivity(start);
						plan.addLeg(leg);
						Activity end = this.population.getFactory().
								createActivityFromLinkId("dummy", 
										matsimEndLinkId);
						plan.addActivity(end);
						correspondingPerson.addPlan(plan);
					}
					
					// add the agent to this.popWithRoutes
					this.popWithRoutes.addPerson(correspondingPerson);
				}
			
		}
		log.info("The maximal number of plans per agent is " + maxNumberOfPlans);

		// check whether all agents have routes
		if (!this.population.getPersons().isEmpty()) {
				log.error("There are " + this.population.getPersons().size() + " persons left with no route.");
		}
	}

	private List<Leg> convertPathsToLegs(DgCommodity com,
			Id<Link> matsimStartLinkId, Id<Link> matsimEndLinkId) {
		
		List<Leg> legs = new ArrayList<>();
		for (TtPath path : com.getPaths().values()) {
			List<Id<Link>> matsimLinks = new ArrayList<>();

			// note: the matsim start link is missing in the btu route,
			// because routes were converted to the end-node of the link
			// but this doesn't matter here, because routes of legs only
			// contain all links between start and end link

			// add all links to the route that are used in the btu path,
			// except the matsim end link and all lights
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
				if (!lights && !linkId.equals(matsimEndLinkId)) {
					matsimLinks.add(linkId);
				}
			}

			// create leg with this route
			Route route = new LinkNetworkRouteImpl(matsimStartLinkId,
					matsimLinks, matsimEndLinkId);
			Leg leg = this.population.getFactory().createLeg(
					TransportMode.car);
			leg.setRoute(route);
			legs.add(leg);
		}
		return legs;
	}

	private Person getCorrespondingMatsimAgent(Id<Link> matsimStartLinkId,
			Id<Link> matsimEndLinkId) {

		Person correspondingPerson = null;
		for (Person person : this.population.getPersons().values()) {
			
				int checkerPlan = 0;
				Activity startAct = null;
				Activity endAct = null;
				
				// persons in this population were created with only one plan
				for (Plan plan : person.getPlans()) {
					int checkerStartAct = 0;
					int checkerEndAct = 0;
					
					for (PlanElement plEl : plan.getPlanElements()) {
						// plans were created with exactly two activities
						if (plEl instanceof Activity) {
							if (((Activity) plEl).getEndTime() == Time.UNDEFINED_TIME) {
								// plEl is end activity (without end time)
								checkerEndAct++;
								endAct = (Activity) plEl;
							} else {
								// plEl is start activity (with end time)
								checkerStartAct++;
								startAct = (Activity) plEl;
							}
						}
					}
					checkerPlan++;
					if (!(checkerStartAct == 1) || !(checkerEndAct == 1)) {
						throw new RuntimeException(
								"Number of activities should be 2 here.");
					}
				}
				if (!(checkerPlan == 1)) {
					throw new RuntimeException(
							"Number of plans should be 1 here.");
				}

				// remove person from the population without routes 
				// to touch every person only once
				if (startAct.getLinkId().equals(matsimStartLinkId)
						&& endAct.getLinkId().equals(matsimEndLinkId)) {
					
					correspondingPerson = this.population.getPersons().remove(person.getId());
					this.currentDepartureTime = startAct.getEndTime();
					break;
				}
		}
		if (correspondingPerson == null) {
			throw new RuntimeException("No agent with start link "
					+ matsimStartLinkId + " and end link " + matsimEndLinkId
					+ " was found.");
		}
		return correspondingPerson;
	}

	private Id<Link> createMatsimLink(Id<DgCrossingNode> toCrossingNodeId,
			Id<DgCommodity> comId, boolean startLink) {
		Id<Link> matsimLinkId = null;
		// try to get the fromLink from the source crossingNode
		// (only works, if it is expanded)
		try {
			matsimLinkId = this.idConverter
					.convertToCrossingNodeId2LinkId(toCrossingNodeId);
		} catch (IllegalStateException e) {
			// the source crossing node is not expanded

			// use the inLink if there is only one
			Id<Node> matsimNodeId = this.idConverter
					.convertNotExpandedCrossingNodeId2NodeId(toCrossingNodeId);
			Map<Id<Link>, ? extends Link> inLinks = this.network.getNodes()
					.get(matsimNodeId).getInLinks();
			if (inLinks.size() == 1) {
				int checker = 0;
				for (Id<Link> linkId : inLinks.keySet()) {
					matsimLinkId = linkId;
					checker++;
				}
				if (checker > 1) {
					throw new RuntimeException(
							"Number of inLinks should not be greater than one here!");
				}
			} else {
				// use the inLink which corresponds to the commodity id

				for (Id<Link> linkId : inLinks.keySet()) {
					Integer comIntId = Integer.parseInt(comId.toString());
					String comStringId = this.idPool.getStringId(comIntId);

					int checker = 0;
					if ((startLink && comStringId.startsWith(linkId.toString()))
							|| (!startLink && comStringId.endsWith(linkId
									.toString()))) {
						matsimLinkId = linkId;
						checker++;
					}
					if (checker > 1) {
						throw new RuntimeException(
								"Number of inLinks that corresponds to the commodity id "
										+ comIntId
										+ " = "
										+ comStringId
										+ " should not be greater than one here!");
					}
					if (checker == 0) {
						throw new RuntimeException(
								"No matsim link for commodity " + comIntId
										+ " = " + comStringId + " was found.");
					}
				}
			}
		}
		return matsimLinkId;
	}

	public static void main(String[] args) {

		String directory = DgPaths.REPOS
				+ "shared-svn/projects/cottbus/data/optimization/cb2ks2010/"
				+ "2015-02-25_minflow_50.0_morning_peak_speedFilter15.0_SP_tt_cBB50.0_sBB500.0/";

		String btuRoutesFilename = "routeComparison/paths.xml";
		String networkFilename = "network_small_simplified.xml.gz";
		String populationFile = "trip_plans_from_morning_peak_ks_commodities_minFlow50.0.xml";

		String[] filenameAttributes = btuRoutesFilename.split("/");
		String outputFilename = directory
				+ "routeComparison/2015-03-10_sameEndTimes_ksOptTripPlans_" 
				+ filenameAttributes[filenameAttributes.length - 1];
		
		new ConvertBTURoutes2Matsim().convertRoutes(directory,
				btuRoutesFilename, networkFilename,
				outputFilename, populationFile);
	}

}
