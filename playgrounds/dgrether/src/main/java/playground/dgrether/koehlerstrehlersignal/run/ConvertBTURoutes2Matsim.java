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
 * Class to convert commodities with paths given by the BTU model to MATSim 
 * agents with routes.
 * 
 * The routes are expected in the format
 * 
 * <commodityPaths>
 *   <commodity drain="???" id="???" source="???">
 *     <path flow="???">
 *       <edge id="???"/>     
 *       <edge id="???"/>
 *       ...
 *     </path>
 *     ...
 *   </commodity> 
 *   ...
 * </commodityPaths> 
 * 
 * The BTU model splits the commodities into different paths which might have 
 * fractional flow values. To solve the assignment problem of BTU paths and
 * MATSim agents, this class assigns all the paths of a commodity to its agents
 * as a route choice set. The idea is to switch of rerouting and let the agents
 * find the best of this routes.
 * 
 * To be able to compare the simulation results of this route choice set with a 
 * free route choice, the same agent departure times are used. Therefore the 
 * conversion takes a while to look for the corresponding agent and his 
 * departure time in the former MATSim population.
 * 
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

	/**
	 * Starts the conversion of BTU paths into MATSim routes.
	 * 
	 * @param directory the directory of the BTU scenario
	 * @param inputFile the filename of the BTU paths 
	 * (directory + inputFile should give the correct path)
	 * @param networkFile the filename of the MATSim network 
	 * (directory + networkFile should give the correct path)
	 * @param populationFile the filename of the MATSim population 
	 * (directory + networkFile should give the correct path)
	 * @param outputFile the filename for the output MATSim population file 
	 * (directory + networkFile should give the correct path)
	 */
	private void startConversion(String directory, String inputFile,
			String networkFile,	String populationFile, String outputFile) {

		// prepare id conversation between btu and matsim model
		this.idPool = DgIdPool.readFromFile(directory + "id_conversions.txt");
		this.idConverter = new DgIdConverter(idPool);

		// parse btu paths from xml file
		KS2015RouteXMLParser routeParser = new KS2015RouteXMLParser(
				this.idConverter);
		routeParser.readFile(directory + inputFile);
		this.btuComsWithRoutes = routeParser.getComsWithRoutes();

		// read the matsim files
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils
				.createConfig());
		// save network in scenario
		(new MatsimNetworkReader(scenario)).readFile(directory + networkFile);
		this.network = scenario.getNetwork();
		// save former population (without routes) in scenario
		(new MatsimPopulationReader(scenario)).readFile(directory
				+ populationFile);
		this.population = scenario.getPopulation();
		// create container for the population with routes
		this.popWithRoutes = ScenarioUtils.createScenario(
				ConfigUtils.createConfig()).getPopulation();
		
		// convert routes from btu to matsim 
		// and write them into this.popWithRoutes
		convertAndAddRoutesToPlans();

		// write population with routes as plans file
		MatsimWriter popWriter = new PopulationWriter(this.popWithRoutes,
				this.network);
		popWriter.write(outputFile);
		log.info("plans file with btu routes written to " + outputFile);
	}

	/**
	 * Converts the BTU paths into MATSim links, looks for the corresponding
	 * agent in the MATSim population (an arbitrary one with the same start
	 * and end link) and gives him all paths of his commodity as route choice set.
	 * 
	 * Note: BTU paths contain lights and streets. In the MATSim route all lights
	 * are skipped.
	 */
	private void convertAndAddRoutesToPlans() {
		// determine the maximal number of plans as additional information output
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
			if (legs.size() > maxNumberOfPlans)
				maxNumberOfPlans = legs.size();

			// commodity flow values should be integer because of conversion.
			// regarding to rounding issues of single path flow values in the btu model, 
			// the sum of all path flow values might be non integer, though.
			// -> round it to the next integer.
			int roundedFlow = (int) Math.round(com.getFlow());
				
			// create a route choice set for each agent of the commodity			
			for (int i = 0; i < roundedFlow; i++) {
				
				// look for a matsim agent with the same start and end link
				// (remove him from this.population)
				Person correspondingPerson = getCorrespondingMatsimAgent(
					matsimStartLinkId, matsimEndLinkId);
				// remove his former plan
				correspondingPerson.getPlans().clear();

				// add all btu routes of the commodity as plans
				for (Leg leg : legs) {
					Plan plan = this.population.getFactory().createPlan();
					Activity start = this.population.getFactory().
						createActivityFromLinkId("dummy", matsimStartLinkId);
					start.setEndTime(this.currentDepartureTime);
					plan.addActivity(start);
					plan.addLeg(leg);
					Activity end = this.population.getFactory().
						createActivityFromLinkId("dummy", matsimEndLinkId);
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
			throw new RuntimeException("There are " + this.population.
				getPersons().size() + " persons left with no route.");
		}
	}

	/**
	 * Finds the corresponding MATSim link, which ends in the given 
	 * crossing node. If the crossing node is expanded, the unique 
	 * MATSim link ID is given by the crossing node ID. If not, this
	 * method returns the ingoing link of the crossing, if there is
	 * only one. If there are more than one ingoing links, it return 
	 * the one which corresponds to the given commodity ID (which 
	 * contains the start and end link ID). Therefore the additional 
	 * information is needed, whether the MATSim link should be a 
	 * start or an end link.
	 * 
	 * @param toCrossingNodeId the ID of the crossing node, where the 
	 * MATSim link ends
	 * @param comId the ID of the commodity, of which the MATSim link 
	 * is either the start or the end link
	 * @param startLink true, if the MATSim link is the start link of 
	 * the commodity. false, if it is the end link
	 * @return the ID of the corresponding MATSim link, where the 
	 * commodity ends or starts at
	 */
	private Id<Link> createMatsimLink(Id<DgCrossingNode> toCrossingNodeId,
			Id<DgCommodity> comId, boolean startLink) {
		
		Id<Link> matsimLinkId = null;
		
		// try to get the link from the crossing node
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
				for (Id<Link> linkId : inLinks.keySet()) {
					matsimLinkId = linkId;
				}
			} else {
				// use the inLink which corresponds to the commodity id
	
				for (Id<Link> linkId : inLinks.keySet()) {
					Integer comIntId = Integer.parseInt(comId.toString());
					String comStringId = this.idPool.getStringId(comIntId);
	
					int checkMatches = 0;
					if ((startLink && comStringId.startsWith(linkId.toString()))
							|| (!startLink && comStringId.endsWith(linkId
									.toString()))) {
						matsimLinkId = linkId;
						checkMatches++;
					}
					if (checkMatches > 1) {
						throw new RuntimeException(
								"Number of inLinks that corresponds to the commodity id "
										+ comIntId + " = " + comStringId
										+ " should not be greater than one here!");
					}
					if (checkMatches == 0) {
						throw new RuntimeException(
								"No matsim link for commodity " + comIntId
										+ " = " + comStringId + " was found.");
					}
				}
			}
		}
		return matsimLinkId;
	}

	/**
	 * Converts the BTU paths of the given commodity into MATSim legs.
	 * 
	 * @param com the commodity
	 * @param matsimStartLinkId the MATSim start link of the commodity
	 * @param matsimEndLinkId the MATSim end link of the commodity
	 * @return a list of MATSim legs which contain all BTU paths of 
	 * the commodity
	 */
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

	/**
	 * Looks for a MATSim agent with the given start and end link.
	 * 
	 * Note: This method takes a long time... (depending on the 
	 * number of agents in the population)
	 * 
	 * @param matsimStartLinkId
	 * @param matsimEndLinkId
	 * @return an agent with this start and end link
	 */
	private Person getCorrespondingMatsimAgent(Id<Link> matsimStartLinkId,
			Id<Link> matsimEndLinkId) {

		Person correspondingPerson = null;
		for (Person person : this.population.getPersons().values()) {
			
			int checkPlan = 0;
			Activity startAct = null;
			Activity endAct = null;
				
			// persons in this population were created with only one plan
			for (Plan plan : person.getPlans()) {
				int checkStartAct = 0;
				int checkEndAct = 0;
					
				for (PlanElement plEl : plan.getPlanElements()) {
					// plans were created with exactly two activities
					if (plEl instanceof Activity) {
						if (((Activity) plEl).getEndTime() == Time.UNDEFINED_TIME) {
							// plEl is end activity (without end time)
							checkEndAct++;
							endAct = (Activity) plEl;
						} else {
							// plEl is start activity (with end time)
							checkStartAct++;
							startAct = (Activity) plEl;
						}
					}
				}
				checkPlan++;
				if (!(checkStartAct == 1) || !(checkEndAct == 1)) {
					throw new RuntimeException("Number of activities should be 2 here.");
				}
			}
			if (!(checkPlan == 1)) {
				throw new RuntimeException("Number of plans should be 1 here.");
			}

			// check correspondence
			if (startAct.getLinkId().equals(matsimStartLinkId)
					&& endAct.getLinkId().equals(matsimEndLinkId)) {
				// remove person from the population without routes 
				// to touch every person only once	
				correspondingPerson = this.population.getPersons().remove(person.getId());
				this.currentDepartureTime = startAct.getEndTime();
				break;
			}
		}
		if (correspondingPerson == null) {
			throw new RuntimeException("No agent with start link " + matsimStartLinkId 
				+ " and end link " + matsimEndLinkId + " was found.");
		}
		return correspondingPerson;
	}

	/**
	 * Main method to run the conversion.
	 * 
	 * @param args is not used
	 */
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
		
		new ConvertBTURoutes2Matsim().startConversion(directory,
				btuRoutesFilename, networkFilename, populationFile, 
				outputFilename);
	}

}
