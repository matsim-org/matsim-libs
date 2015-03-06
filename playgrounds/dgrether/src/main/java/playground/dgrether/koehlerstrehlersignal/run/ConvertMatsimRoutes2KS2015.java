/**
 * 
 */
package playground.dgrether.koehlerstrehlersignal.run;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.events.MatsimEventsReader;

import playground.dgrether.DgPaths;
import playground.dgrether.analysis.RunResultsLoader;
import playground.dgrether.events.EventsFilterManager;
import playground.dgrether.events.EventsFilterManagerImpl;
import playground.dgrether.koehlerstrehlersignal.analysis.ReadRoutesFromEvents;
import playground.dgrether.koehlerstrehlersignal.data.DgCommodities;
import playground.dgrether.koehlerstrehlersignal.data.DgCommodity;
import playground.dgrether.koehlerstrehlersignal.data.DgCrossing;
import playground.dgrether.koehlerstrehlersignal.data.DgCrossingNode;
import playground.dgrether.koehlerstrehlersignal.data.DgKSNetwork;
import playground.dgrether.koehlerstrehlersignal.data.DgStreet;
import playground.dgrether.koehlerstrehlersignal.data.KS2010ModelWriter;
import playground.dgrether.koehlerstrehlersignal.data.TtCrossingType;
import playground.dgrether.koehlerstrehlersignal.data.TtPath;
import playground.dgrether.koehlerstrehlersignal.ids.DgIdConverter;
import playground.dgrether.koehlerstrehlersignal.ids.DgIdPool;
import playground.dgrether.koehlerstrehlersignal.solutionconverter.KS2015NetworkXMLParser;

/**
 * @author tthunig
 */
public class ConvertMatsimRoutes2KS2015 {

	private static final Logger log = Logger.getLogger(ConvertMatsimRoutes2KS2015.class);

	private Map<Id<Person>, List<Id<Link>>> matsimRoutes = new HashMap<>();
	private DgCommodities comsWithRoutes = new DgCommodities();

	private Network matsimNet;
	private DgKSNetwork ksNet;
	
	private void convertRoutes(String runNumber, Integer lastIteration,
			String ksModelDirectory, String ksModelFile, String outputFile,
			String description) {

		// init
		String runsDirectory = DgPaths.REPOS + "runs-svn/cottbus/run"
				+ runNumber + "/";
		RunResultsLoader runDir = new RunResultsLoader(runsDirectory, runNumber);
		String eventsFilename = runDir.getEventsFilename(lastIteration);
		this.matsimNet = runDir.getNetwork();
		
		// read ks network with crossings and streets
		KS2015NetworkXMLParser ksNetworkReader = new KS2015NetworkXMLParser();
		ksNetworkReader.readFile(ksModelDirectory + ksModelFile);
		this.ksNet = ksNetworkReader.getKsNet();
		
		// read matsim routes from events
		EventsFilterManager eventsManager = new EventsFilterManagerImpl();
		ReadRoutesFromEvents readRoutes = new ReadRoutesFromEvents();
		eventsManager.addHandler(readRoutes);
		
		MatsimEventsReader reader = new MatsimEventsReader(eventsManager);
		reader.readFile(eventsFilename);
		
		this.matsimRoutes = readRoutes.getMatsimRoutes();

		// create commodities for this routes
		createCommodities(ksModelDirectory);

		// write matsim routes together with ks network as ks model
		new KS2010ModelWriter().write(ksNet, this.comsWithRoutes,
				outputFile.split("/")[1], description, ksModelDirectory
						+ outputFile);
	}

	/**
	 * create a commodity for each new path and save them in this.comsWithRoutes.
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
			// id preparations
			List<Id<Link>> matsimRoute = matsimRoutes.get(personId);
			Id<Link> matsimFirstLink = matsimRoute.get(0);
			Id<Link> matsimLastLink = matsimRoute.get(matsimRoute.size() - 1);
			Id<Node> matsimSourceNodeId = this.matsimNet.getLinks().get(matsimFirstLink).getToNode().getId();
			Id<Node> matsimDrainNodeId = this.matsimNet.getLinks().get(matsimLastLink).getToNode().getId();
			
			Id<DgCommodity> comId = idConverter
					.convertLinkToLinkPair2CommodityId(matsimFirstLink,
							matsimLastLink);
			
			Id<DgCrossing> ksSourceCrossingId = idConverter.convertNodeId2CrossingId(matsimSourceNodeId);
			DgCrossing ksSourceCrossing = this.ksNet.getCrossings().get(ksSourceCrossingId);
			Id<DgCrossingNode> ksSourceNodeId;
			if (ksSourceCrossing.getType().equals(TtCrossingType.NOTEXPAND)){
				// source crossing isn't expanded and contains only one crossing node
				ksSourceNodeId = idConverter.convertNodeId2NotExpandedCrossingNodeId(matsimSourceNodeId);
			} 
			else{ 
				// source crossing is expanded and contains different crossing nodes
				ksSourceNodeId = idConverter.convertLinkId2ToCrossingNodeId(matsimFirstLink);
			}
			
			Id<DgCrossing> ksDrainCrossingId = idConverter.convertNodeId2CrossingId(matsimDrainNodeId);
			DgCrossing ksDrainCrossing = this.ksNet.getCrossings().get(ksDrainCrossingId);
			Id<DgCrossingNode> ksDrainNodeId;
			if (ksDrainCrossing.getType().equals(TtCrossingType.NOTEXPAND)){
				// drain crossing isn't expanded and contains only one crossing node
				ksDrainNodeId = idConverter.convertNodeId2NotExpandedCrossingNodeId(matsimDrainNodeId);
			} 
			else{ 
				// drain crossing is expanded and contains different crossing nodes
				ksDrainNodeId = idConverter.convertLinkId2ToCrossingNodeId(matsimLastLink);
			}

			// convert matsim route into ks path
			List<Id<DgStreet>> ksPath = new ArrayList<>();
			for (Id<Link> linkId : matsimRoute) {
				Id<DgStreet> streetId = idConverter
						.convertLinkId2StreetId(linkId);
				ksPath.add(streetId);
			}
			// delete the first link of the matsim route.
			// in ks format the path starts at the end node of the link
			ksPath.remove(0);

			// create the path id which depends on the street ids
			Id<TtPath> pathId = idConverter.convertPathInfo2PathId(ksPath, ksSourceNodeId, ksDrainNodeId);
			
			addRouteToCommodity(ksSourceNodeId, ksDrainNodeId, comId, ksPath, pathId);
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
	private void addRouteToCommodity(Id<DgCrossingNode> sourceNode,
			Id<DgCrossingNode> drainNode, Id<DgCommodity> comId,
			List<Id<DgStreet>> ksRoute, Id<TtPath> pathId) {

		// check whether the commodity already exists
		if (!this.comsWithRoutes.getCommodities().containsKey(comId)){
			this.comsWithRoutes.addCommodity(new DgCommodity(comId, sourceNode, drainNode, 0.0));
		}
		DgCommodity currentCom = this.comsWithRoutes.getCommodities().get(comId);
		
		// add the route to the commodity routes
		if (!currentCom.containsPath(pathId)){
			currentCom.addPath(pathId, ksRoute, 0.0);
		}
		// increase the flow values (total flow and specific path flow)
		currentCom.increaseFlowOfPath(pathId, 1.0);
	}
	

	public static void main(String[] args) {
		
		String runNumber = "2034";
		String runDescription = "new2_optimum"; // please use btu name here
		Integer lastIteration = 1400;
		
		String ksModelDirectory = DgPaths.REPOS
				+ "shared-svn/projects/cottbus/data/optimization/cb2ks2010/"
				+ "2015-02-06_minflow_50.0_morning_peak_speedFilter15.0_SP_tt_cBB50.0_sBB500.0/";
		String ksModelFile = "ks2010_model_50.0_19800.0_50.0.xml"; // TODO include opt signals
		String outputFile = "routeComparison/2015-03-03_matsimRoutes_" + runNumber + ".xml";
		
		String description = "matsim routes with " + runDescription + " offsets";

		new ConvertMatsimRoutes2KS2015().convertRoutes(runNumber,
				lastIteration, ksModelDirectory, ksModelFile, outputFile,
				description);
	}

}
