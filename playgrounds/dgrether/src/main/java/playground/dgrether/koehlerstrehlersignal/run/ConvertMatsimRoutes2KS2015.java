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
import playground.dgrether.koehlerstrehlersignal.data.DgProgram;
import playground.dgrether.koehlerstrehlersignal.data.DgStreet;
import playground.dgrether.koehlerstrehlersignal.data.KS2010ModelWriter;
import playground.dgrether.koehlerstrehlersignal.data.TtCrossingType;
import playground.dgrether.koehlerstrehlersignal.data.TtPath;
import playground.dgrether.koehlerstrehlersignal.ids.DgIdConverter;
import playground.dgrether.koehlerstrehlersignal.ids.DgIdPool;
import playground.dgrether.koehlerstrehlersignal.solutionconverter.KS2010CrossingSolution;
import playground.dgrether.koehlerstrehlersignal.solutionconverter.KS2014SolutionXMLParser;
import playground.dgrether.koehlerstrehlersignal.solutionconverter.KS2015NetworkXMLParser;

/**
 * Class to convert MATSim routes into BTU format.
 * Therefore all agents with the same route (same links, time irrelevant)
 * are merged as one commodity with this links as permitted streets.
 * 
 * The output is a full BTU scenario with all crossings, streets and commodities.
 * 
 * @author tthunig
 */
public class ConvertMatsimRoutes2KS2015 {

	private static final Logger log = Logger.getLogger(ConvertMatsimRoutes2KS2015.class);

	private Map<Id<Person>, List<Id<Link>>> matsimRoutes = new HashMap<>();
	private DgCommodities comsWithRoutes = new DgCommodities();

	private Network matsimNet;
	private DgKSNetwork ksNet;
	
	private DgIdConverter idConverter;
	
	/**
	 * Starts the conversion of MATSim routes into BTU format.
	 * 
	 * @param runNumber the number of the MATSim run, of which the routes should be taken
	 * @param lastIteration the iteration number, of which the routes should be taken
	 * @param ksModelDirectory the directory of the BTU scenario
	 * @param ksModelFile the name of the BTU model file
	 * (ksModelDirectory + ksModelFile should give the correct path)
	 * @param ksOptFile the name of the file with the BTU signal control
	 * (ksModelDirectory + ksOptFile should give the correct path)
	 * @param outputFile the name of the BTU output model with permitted streets 
	 * (ksModelDirectory + outputFile should give the correct path)
	 * @param description the description of the output model
	 */
	private void convertRoutes(String runNumber, Integer lastIteration,
			String ksModelDirectory, String ksModelFile, String ksOptFile, 
			String outputFile, String description) {

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
		
		// read offset of ks opt signals
		KS2014SolutionXMLParser solutionParser = new KS2014SolutionXMLParser();
		solutionParser.readFile(ksModelDirectory + ksOptFile);
		List<KS2010CrossingSolution> crossingSolutions = solutionParser.getCrossingSolutions();
		// write them into ksNet
		for (KS2010CrossingSolution solution : crossingSolutions){
			DgCrossing relevantCrossing = ksNet.getCrossings().get(solution.getCrossingId());
			for (DgProgram program : relevantCrossing.getPrograms().values()){
				int offset = solution.getProgramIdOffsetMap().get(program.getId());
				program.setOffset(offset);
			}
		}
		
		// read matsim routes from events
		EventsFilterManager eventsManager = new EventsFilterManagerImpl();
		ReadRoutesFromEvents readRoutes = new ReadRoutesFromEvents();
		eventsManager.addHandler(readRoutes);
		
		MatsimEventsReader reader = new MatsimEventsReader(eventsManager);
		reader.readFile(eventsFilename);
		
		this.matsimRoutes = readRoutes.getMatsimRoutes();

		// create commodities for this routes
		createCommodities(ksModelDirectory);

		// write matsim routes together with ks network and opt offsets as ks model
		new KS2010ModelWriter().write(ksNet, this.comsWithRoutes,
				outputFile.split("/")[1], description, ksModelDirectory
						+ outputFile);
	}

	/**
	 * Creates a commodity for each new path and saves it in this.comsWithRoutes.
	 * This converts the matsim routes into ks model format.
	 * 
	 * @param ksModelDirectory
	 *            the directory with the ks model files 
	 */
	private void createCommodities(String ksModelDirectory) {

		DgIdPool idPool = DgIdPool.readFromFile(ksModelDirectory
				+ "id_conversions.txt");
		this.idConverter = new DgIdConverter(idPool);

		for (Id<Person> personId : matsimRoutes.keySet()) {
			// id preparations
			List<Id<Link>> matsimRoute = matsimRoutes.get(personId);
			Id<Link> matsimFirstLink = matsimRoute.get(0);
			Id<Link> matsimLastLink = matsimRoute.get(matsimRoute.size() - 1);
			Id<Node> matsimSourceNodeId = this.matsimNet.getLinks()
					.get(matsimFirstLink).getToNode().getId();
			Id<Node> matsimDrainNodeId = this.matsimNet.getLinks()
					.get(matsimLastLink).getToNode().getId();
			
			Id<DgCommodity> comId = idConverter
					.convertLinkToLinkPair2CommodityId(matsimFirstLink,
							matsimLastLink);
			
			Id<DgCrossingNode> ksSourceNodeId = getCrossingNodeIdFromNodeAndLink(
					matsimSourceNodeId, matsimFirstLink);
			Id<DgCrossingNode> ksDrainNodeId = getCrossingNodeIdFromNodeAndLink(
					matsimDrainNodeId, matsimLastLink);

			// convert matsim route into ks path
			List<Id<DgStreet>> ksPath = new ArrayList<>();
			for (Id<Link> linkId : matsimRoute) {
				Id<DgStreet> streetId = idConverter
						.convertLinkId2StreetId(linkId);
				ksPath.add(streetId);
			}
			// delete the first link of the matsim route, because a path in
			// the btu format starts at the end node of the link
			ksPath.remove(0);

			// create the path id which depends on the street ids
			Id<TtPath> pathId = idConverter.convertPathInfo2PathId(
					ksPath, ksSourceNodeId, ksDrainNodeId);
			
			addRouteToCommodity(ksSourceNodeId, ksDrainNodeId, comId, ksPath, pathId);
		}
	}

	/**
	 * Returns the ID of the crossing node corresponding to the given MATSim
	 * node and link pair. If the node is expanded in the ks format, this
	 * crossing node comes from the link. If not, it comes from the node.
	 * 
	 * @param matsimNodeId
	 * @param matsimLinkId
	 * @return the corresponding crossing node ID
	 */
	private Id<DgCrossingNode> getCrossingNodeIdFromNodeAndLink(Id<Node> matsimNodeId, Id<Link> matsimLinkId) {
		
		Id<DgCrossing> ksCrossingId = idConverter.convertNodeId2CrossingId(matsimNodeId);
		DgCrossing ksCrossing = this.ksNet.getCrossings().get(ksCrossingId);
		
		Id<DgCrossingNode> crossingNodeId;
		if (ksCrossing.getType().equals(TtCrossingType.NOTEXPAND)){
			// source crossing isn't expanded and contains only one crossing node
			crossingNodeId = this.idConverter.convertNodeId2NotExpandedCrossingNodeId(matsimNodeId);
		} 
		else{ 
			// source crossing is expanded and contains different crossing nodes
			crossingNodeId = this.idConverter.convertLinkId2ToCrossingNodeId(matsimLinkId);
		}
		return crossingNodeId;
	}

	/**
	 * Adds a new commodity to comsWithRoutes or increases its flow value, 
	 * if it already exists.
	 * 
	 * @param sourceNodeId
	 *            the source node ID of the commodity in the ks format
	 * @param drainNodeId
	 *            the drain node ID of the commodity in the ks format
	 * @param comId
	 *            the commodity ID
	 * @param ksRoute
	 *            the route of the specific agent in the ks format
	 */
	private void addRouteToCommodity(Id<DgCrossingNode> sourceNodeId,
			Id<DgCrossingNode> drainNodeId, Id<DgCommodity> comId,
			List<Id<DgStreet>> ksRoute, Id<TtPath> pathId) {

		// check whether the commodity already exists
		if (!this.comsWithRoutes.getCommodities().containsKey(comId)){
			this.comsWithRoutes.addCommodity(new DgCommodity(comId, sourceNodeId, drainNodeId, 0.0));
		}
		DgCommodity currentCom = this.comsWithRoutes.getCommodities().get(comId);
		
		// add the route to the commodity routes
		if (!currentCom.containsPath(pathId)){
			currentCom.addPath(pathId, ksRoute, 0.0);
		}
		// increase the flow values (total flow and specific path flow)
		currentCom.increaseFlowOfPath(pathId, 1.0);
	}
	

	/**
	 * Main method to run the conversion.
	 * 
	 * @param args is not used
	 */
	public static void main(String[] args) {
		
		String runNumber = "2038";
		Integer lastIteration = 1400;
		String BTUsignalControl = "new_optimum";
		
		String ksModelDirectory = DgPaths.REPOS
				+ "shared-svn/projects/cottbus/data/optimization/cb2ks2010/"
				+ "2015-02-06_minflow_50.0_morning_peak_speedFilter15.0_SP_tt_cBB50.0_sBB500.0/";
		String ksModelFile = "ks2010_model_50.0_19800.0_50.0.xml";
		String ksOptFile = "btu/" + BTUsignalControl + ".xml";
		String outputFile = "routeComparison/2015-03-10_matsimRoutes_" + BTUsignalControl + ".xml";
		
		String description = "matsim routes with " + BTUsignalControl + " offsets";

		new ConvertMatsimRoutes2KS2015().convertRoutes(runNumber,
				lastIteration, ksModelDirectory, ksModelFile, ksOptFile, outputFile,
				description);
	}

}
