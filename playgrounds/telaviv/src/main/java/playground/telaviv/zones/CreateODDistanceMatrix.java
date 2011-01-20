package playground.telaviv.zones;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.TravelMinCost;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.misc.Counter;

import playground.telaviv.locationchoice.FullNetworkDijkstra;
import playground.telaviv.locationchoice.FullNetworkDijkstraFactory;

public class CreateODDistanceMatrix {

	private static final Logger log = Logger.getLogger(CreateODDistanceMatrix.class);
	private static String networkFile = "../../matsim/mysimulations/telaviv/network/network.xml";
	
	protected Scenario scenario;
	protected ZoneMapping zoneMapping;
	protected Map<Integer, Integer> tazMapping;	// index in Array, TAZ
	protected double distances[][];
	protected Set<Id> nodeIds;
	protected int numOfThreads = 6;
	
	public static void main(String[] args) {
		Scenario scenario = new ScenarioImpl();
		new MatsimNetworkReader(scenario).readFile(networkFile);
		ZoneMapping zoneMapping = new ZoneMapping(scenario, TransformationFactory.getCoordinateTransformation("EPSG:2039", "WGS84"));
		new CreateODDistanceMatrix(scenario, zoneMapping).calculateODMatrix();
	}
	
	public CreateODDistanceMatrix(Scenario scenario, ZoneMapping zoneMapping) {
		this.scenario = scenario;
		this.zoneMapping = zoneMapping;
		
//		TravelTimeCost travelTimeCost = new TravelTimeCost();
//		leastCostPathCalculator = new AStarLandmarksFactory(scenario.getNetwork(), travelTimeCost).createPathCalculator(scenario.getNetwork(), travelTimeCost, travelTimeCost);
		
		getConnectorNodes();
	}
	
	public double[][] getODDistanceMatrix() {
		return distances;
	}
	
	public Map<Integer, Integer> getTAZMapping() {
		return tazMapping;
	}
	
	public Map<Integer, Integer> getInvertedTAZMapping() {
		Map<Integer, Integer> map = new HashMap<Integer, Integer>();
		
		for (Entry<Integer, Integer> entry : tazMapping.entrySet()) {
			map.put(entry.getValue(), entry.getKey());
		}
		return map;
	}
	
	public void calculateODMatrix() {
		log.info("Calculate ZoneMatrix with size " + nodeIds.size() + "x" + nodeIds.size());
		Counter counter = new Counter("Calculated OD Distance Pairs: ");
		distances = new double[nodeIds.size()][];
		tazMapping = new HashMap<Integer, Integer>();
		
		TravelTimeCost travelTimeCost = new TravelTimeCost();
		
		Thread[] threads = new Thread[numOfThreads];
		for (int j = 0; j < numOfThreads; j++) {
			Thread thread = new ParallelThread();
			thread.setDaemon(true);
			thread.setName("ParallelCreateODDistanceMatrixThread" + j);
			((ParallelThread) thread).scenario = scenario;
			((ParallelThread) thread).counter = counter;
//			((ParallelThread) thread).leastCostPathCalculator = new AStarLandmarksFactory(scenario.getNetwork(), travelTimeCost).createPathCalculator(scenario.getNetwork(), travelTimeCost, travelTimeCost);
			((ParallelThread) thread).leastCostPathCalculator = new FullNetworkDijkstraFactory().createPathCalculator(scenario.getNetwork(), travelTimeCost, travelTimeCost);
			((ParallelThread) thread).nodeIds = this.nodeIds;
			((ParallelThread) thread).data = new ArrayList<Data>();
			threads[j] = thread;
		}
		
		int i = 0;
		for (Id fromId : nodeIds) {
			distances[i] = new double[nodeIds.size()];
			Node fromNode = scenario.getNetwork().getNodes().get(fromId);
			
			tazMapping.put(Integer.valueOf(fromId.toString()), i);
			
			Data data = new Data();
			data.array = distances[i];
			data.fromNode = fromNode;
			((ParallelThread) threads[i % numOfThreads]).data.add(data);
						
			i++;
		}
		
		for (Thread thread : threads) {
			thread.start();
		}
		
		// wait until each thread is finished
		try {
			for (Thread thread : threads) {
				thread.join();
			}
		} catch (InterruptedException e) {
			Gbl.errorMsg(e);
		}
		
		/*
		 *  Now update trips within a single zone. We set the distance to
		 *  half of the smallest distance of the neighbors.
		 */
		for (int m = 0; m < nodeIds.size(); m++) {
			
			double min = Double.MAX_VALUE;
			
			for (int n = 0; n < nodeIds.size(); n++) {
				if (n == m) continue;
				if (distances[n][m] < min) min = distances[n][m];
				if (distances[m][n] < min) min = distances[m][n];
			}
			
			distances[m][m] = min/2;
		}
		
		log.info("done.");
	}
	
	/*
	 * Find Connector Nodes. Each Zone Connector Node has the same Id as the TAZ of its zone!
	 */
	private void getConnectorNodes () {
		nodeIds = new TreeSet<Id>();
		for (Integer zoneId : zoneMapping.getParsedZones().keySet()) {
			nodeIds.add(scenario.createId(zoneId.toString()));
		}

	}
	
	private static class Data {
		Node fromNode;
		double[] array;
	}
	
	private static class ParallelThread extends Thread {
		
		Scenario scenario;
		Counter counter;
		LeastCostPathCalculator leastCostPathCalculator;
		List<Data> data;
		Set<Id> nodeIds;
		
		@Override
		public void run() {
			for (Data d :data) {
				int i = 0;
				
				if (leastCostPathCalculator instanceof FullNetworkDijkstra) {
					((FullNetworkDijkstra)leastCostPathCalculator).calcLeastCostTree(d.fromNode, 0.0);
				}
				
				for (Id toId : nodeIds) {
					Node toNode = scenario.getNetwork().getNodes().get(toId);
					Path path = leastCostPathCalculator.calcLeastCostPath(d.fromNode, toNode, 0.0);
					d.array[i] = path.travelCost;
					counter.incCounter();
					i++;
				}
			}
		}
	}
	
	private static class TravelTimeCost implements TravelTime, TravelMinCost {

		@Override
		public double getLinkTravelTime(Link link, double time) {
			return link.getLength()/link.getFreespeed();
		}

		@Override
		public double getLinkGeneralizedTravelCost(Link link, double time) {
//			return getLinkTravelTime(link, time);
			return link.getLength();
		}

		@Override
		public double getLinkMinimumTravelCost(Link link) {
			return getLinkGeneralizedTravelCost(link, 0.0);
		}
		
	}
}
