/**
 * 
 */
package playground.dgrether.koehlerstrehlersignal.run;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.events.MatsimEventsReader;

import playground.dgrether.DgPaths;
import playground.dgrether.analysis.RunResultsLoader;
import playground.dgrether.events.EventsFilterManager;
import playground.dgrether.events.EventsFilterManagerImpl;
import playground.dgrether.koehlerstrehlersignal.analysis.ReadRoutesFromEvents;
import playground.dgrether.koehlerstrehlersignal.data.DgCommodities;
import playground.dgrether.koehlerstrehlersignal.data.DgCommodity;
import playground.dgrether.koehlerstrehlersignal.data.DgCrossingNode;
import playground.dgrether.koehlerstrehlersignal.data.DgKSNetwork;
import playground.dgrether.koehlerstrehlersignal.data.DgStreet;
import playground.dgrether.koehlerstrehlersignal.data.KS2010ModelWriter;
import playground.dgrether.koehlerstrehlersignal.ids.DgIdConverter;
import playground.dgrether.koehlerstrehlersignal.ids.DgIdPool;
import playground.dgrether.koehlerstrehlersignal.solutionconverter.KS2015NetworkXMLParser;

/**
 * @author tthunig
 */
public class ConvertMatsimRoutes2KS2015 {

	private Map<Id<DgCommodity>, Integer> numberOfDifferentPathsPerCommodity = new HashMap<>();

	private Map<Id<Person>, List<Id<Link>>> matsimRoutes = new HashMap<>();
	private DgCommodities comsWithRoutes = new DgCommodities();

	private void convertRoutes(String runNumber, Integer lastIteration,
			String ksModelDirectory, String ksModelFile, String outputFile,
			String description) {

		// init
		String runsDirectory = DgPaths.REPOS + "runs-svn/cottbus/run"
				+ runNumber + "/";
		RunResultsLoader runDir = new RunResultsLoader(runsDirectory, runNumber);
		String eventsFilename = runDir.getEventsFilename(lastIteration);
		
		// read matsim routes from events
		EventsFilterManager eventsManager = new EventsFilterManagerImpl();
		ReadRoutesFromEvents readRoutes = new ReadRoutesFromEvents();
		eventsManager.addHandler(readRoutes);
		
		MatsimEventsReader reader = new MatsimEventsReader(eventsManager);
		reader.readFile(eventsFilename);
		
		this.matsimRoutes = readRoutes.getMatsimRoutes();

		// create commodities for this routes
		createCommodities(ksModelDirectory);

		// read ks network with crossings and streets
		KS2015NetworkXMLParser ksNetworkReader = new KS2015NetworkXMLParser();
		ksNetworkReader.readFile(ksModelDirectory + ksModelFile);
		DgKSNetwork ksNet = ksNetworkReader.getKsNet();
		// write matsim routes together with ks network as ks model
		new KS2010ModelWriter().write(ksNet, this.comsWithRoutes,
				outputFile.split("/")[1], description, ksModelDirectory
						+ outputFile);
	}

	/**
	 * create a commodity for each new path and save them in comsWithRoutes.
	 * this converts the matsim routes into ks model format.
	 * 
	 * @param ksModelDirectory
	 *            the directory with the ks model files like the file
	 *            'id_conversions.txt'
	 */
	private void createCommodities(String ksModelDirectory) {

		DgIdPool idPool = DgIdPool.readFromFile(ksModelDirectory
				+ "id_conversions.txt");
		DgIdConverter idConverter = new DgIdConverter(idPool);

		for (Id<Person> personId : matsimRoutes.keySet()) {
			List<Id<Link>> matsimRoute = matsimRoutes.get(personId);
			Id<Link> matsimFirstLink = matsimRoute.get(0);
			Id<Link> matsimEndLink = matsimRoute.get(matsimRoute.size() - 1);
			Id<DgCrossingNode> sourceNode = idConverter
					.convertLinkId2ToCrossingNodeId(matsimFirstLink);
			Id<DgCrossingNode> drainNode = idConverter
					.convertLinkId2ToCrossingNodeId(matsimEndLink);
			Id<DgCommodity> comId = idConverter
					.createCommodityId4LinkToLinkPair(matsimFirstLink,
							matsimEndLink);

			// convert matsim route into ks format
			List<Id<DgStreet>> ksRoute = new ArrayList<>();
			for (Id<Link> linkId : matsimRoute) {
				Id<DgStreet> streetId = idConverter
						.convertLinkId2StreetId(linkId);
				ksRoute.add(streetId);
			}
			// delete the first link of the matsim route.
			// in ks format the route starts at the end node of the link
			ksRoute.remove(0);

			addNewCommodityOrIncreaseFlow(sourceNode, drainNode, comId, ksRoute);
		}
	}

	/**
	 * add a new commodity to comsWithRoutes or increase its flow value, if it
	 * already exists.
	 * 
	 * @param sourceNode
	 *            the source node of the commodity in the ks format
	 * @param drainNode
	 *            the drain node of the commodity in the ks format
	 * @param comId
	 *            the commodity id
	 * @param ksRoute
	 *            the route of the specific agent in the ks format
	 */
	private void addNewCommodityOrIncreaseFlow(Id<DgCrossingNode> sourceNode,
			Id<DgCrossingNode> drainNode, Id<DgCommodity> comId,
			List<Id<DgStreet>> ksRoute) {

		// check whether route exists already
		if (!(ksRoute == null) && !ksRoute.isEmpty()) {
			boolean routesEqual = false;
			for (DgCommodity comWithRoute : this.comsWithRoutes
					.getCommodities().values()) {
				List<Id<DgStreet>> comRoute = comWithRoute.getRoute();
				// routes can't be equal with different sizes
				if (comRoute.size() == ksRoute.size()) {
					routesEqual = true;
					for (int i = 0; i < comRoute.size(); i++) {
						// switch boolean if different streetIds found
						if (!comRoute.get(i).equals(ksRoute.get(i))) {
							routesEqual = false;
							break;
						}
					}
					if (routesEqual) {
						// same route exists already. increase its flow value
						comWithRoute.setFlow(comWithRoute.getFlow() + 1.0);
						break;
					}
				}
			}
			if (!routesEqual) {
				// the route doesn't exist already
				this.comsWithRoutes.addCommodity(new DgCommodity(
						createId(comId), sourceNode, drainNode, 1.0, ksRoute));
			}
		}
	}

	/**
	 * creates an id for the sub-commodity of comId, where all agents travel
	 * along the same path
	 * 
	 * @param comId
	 *            the remaining commodity id
	 * @return an unique id for the sub-commodity where all agents travel along
	 *         the same path
	 */
	private Id<DgCommodity> createId(Id<DgCommodity> comId) {
		if (!this.numberOfDifferentPathsPerCommodity.containsKey(comId))
			this.numberOfDifferentPathsPerCommodity.put(comId, -1);
		// increase the number of different paths per commodity by one
		this.numberOfDifferentPathsPerCommodity.put(comId,
				this.numberOfDifferentPathsPerCommodity.get(comId) + 1);

		return Id.create(comId.toString()
				+ this.numberOfDifferentPathsPerCommodity.get(comId),
				DgCommodity.class);
	}

	public static void main(String[] args) {
		
		String runNumber = "2039";
		String runDescription = "optimized";
		Integer lastIteration = 1400;
		
		String ksModelDirectory = DgPaths.REPOS
				+ "shared-svn/projects/cottbus/cb2ks2010/"
				+ "2015-02-25_minflow_50.0_morning_peak_speedFilter15.0_SP_tt_cBB50.0_sBB500.0/";
		String ksModelFile = "ks2010_model_50.0_19800.0_50.0.xml";
		String outputFile = "routeComparison/2015-03-03_matsimRoutes_" + runNumber + ".xml";
		
		String description = "matsim routes with " + runDescription + " offsets";

		new ConvertMatsimRoutes2KS2015().convertRoutes(runNumber,
				lastIteration, ksModelDirectory, ksModelFile, outputFile,
				description);
	}

}
