package playground.wrashid.msimoni.analyses;

import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsReaderXMLv1;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.scenario.ScenarioUtils;

public class MainAdaptedDensityCalculationWithSpeed {

		public static void main(String[] args) {
			String networkFile = "H:/thesis/output_no_pricing_v3_subtours_bugfix/output_network.xml.gz";
			String eventsFile =  "H:/thesis/output_no_pricing_v3_subtours_bugfix/ITERS/it.50/50.events.xml.gz";
			
			//String networkFile = "H:/data/experiments/TRBAug2011/runs/run4/output/herbie.output_network.xml.gz";
			//String eventsFile =	"H:/data/experiments/TRBAug2011/runs/run4/output/ITERS/it.0/herbie.0.events.xml.gz";	
			
			
			Coord center = null; // center=null means use all links
			int binSizeInSeconds = 60;	// 5 minute bins

			double radiusInMeters = 1500;
			double length = 50.0;

			Config config = ConfigUtils.createConfig();
			config.network().setInputFile(networkFile);
			Scenario scenario = ScenarioUtils.loadScenario(config);
			center = scenario.createCoord(682548.0, 247525.5);
			
			Map<Id, Link> links = LinkSelector.selectLinks(scenario.getNetwork(), center, radiusInMeters, length);
			
			InFlowInfoCollectorWithPt inflowHandler = new InFlowInfoCollectorWithPt(links, binSizeInSeconds);
			OutFlowInfoCollectorWithPt outflowHandler = new OutFlowInfoCollectorWithPt(links, binSizeInSeconds);
			AverageSpeedCalculator averageSpeedCalculator=new AverageSpeedCalculator(links, binSizeInSeconds);
			
			inflowHandler.reset(0);
			outflowHandler.reset(0);

			EventsManager events = EventsUtils.createEventsManager();

			events.addHandler(inflowHandler); // add handler
			events.addHandler(outflowHandler);
			events.addHandler(averageSpeedCalculator);
			
			EventsReaderXMLv1 reader = new EventsReaderXMLv1(events);
			reader.parse(eventsFile);

			HashMap<Id, double[]> densities=calculateDensities(links,inflowHandler,outflowHandler,averageSpeedCalculator.getAverageSpeeds(),binSizeInSeconds);
			
			printDensity(densities,links);
		}
		
		private static HashMap<Id, double[]> calculateDensities(
				Map<Id, Link> links, InFlowInfoCollectorWithPt inflowHandler,
				OutFlowInfoCollectorWithPt outflowHandler,
				HashMap<Id, double[]> averageSpeeds, int binSizeInSeconds) {
			
			HashMap<Id, double[]> density = new HashMap<Id, double[]>();
			HashMap<Id, int[]> linkInFlow = inflowHandler.getLinkInFlow();
			HashMap<Id, int[]> linkOutFlow = outflowHandler.getLinkOutFlow();
			
			for (Link link : links.values()) {
				double[] bins=new double[getNumberOfBins(binSizeInSeconds)];
				int[] outFlowAccumulated = inflowHandler.getAccumulatedFlow(link.getId());
				int[] inFlowAccumulated = outflowHandler.getAccumulatedFlow(link.getId());
				
				if(outFlowAccumulated[0]-inFlowAccumulated[0]==0){
					if (linkOutFlow.get(link.getId())==null){
						System.out.println();
					}
					
					if (averageSpeeds.get(link.getId())==null){
						System.out.println();
					}
					
					bins[0]=linkOutFlow.get(link.getId())[0]*(3600/binSizeInSeconds)/(averageSpeeds.get(link.getId())[0]*3.6);
				} else {
					bins[0]=linkInFlow.get(link.getId())[0]-linkOutFlow.get(link.getId())[0]/(link.getLength()*link.getNumberOfLanes()/1000);
				}
				
				for (int i=1;i<getNumberOfBins(binSizeInSeconds);i++){
					if(outFlowAccumulated[i]-inFlowAccumulated[i]==0){
						bins[i]=linkOutFlow.get(link.getId())[i]*(3600/binSizeInSeconds)/(averageSpeeds.get(link.getId())[i]*3.6);
					} else {
						bins[i]=linkInFlow.get(link.getId())[i]-linkOutFlow.get(link.getId())[i]/(link.getLength()*link.getNumberOfLanes()/1000)+bins[i-1];
					}
				}
				
				density.put(link.getId(), bins);
			}
			
			return density;
		}

		public static HashMap<Id, double[]> calculateDensities(HashMap<Id, int[]> deltaFlow, Map<Id, ? extends Link> links) {

			HashMap<Id, double[]> density = new HashMap<Id, double[]>();

			for (Id linkId : deltaFlow.keySet()) {
				density.put(linkId, null);
			}

			for (Id linkId : density.keySet()) {

				int[] deltaflowBins = deltaFlow.get(linkId);
				double[] densityBins = new double[deltaflowBins.length];
				Link link = links.get(linkId);
				
				for (int i = 0; i < densityBins.length; i++) {
					densityBins[i] = deltaflowBins[i] / (link.getLength() * link.getNumberOfLanes() / 1000);
				}

				density.put(linkId, densityBins);
				deltaFlow.remove(linkId);
			}

			return density;
		}
		
		private static int getNumberOfBins(int binSizeInSeconds) {
			return (86400 / binSizeInSeconds) + 1;
		}
		
		public static void printDensity(HashMap<Id, double[]> density,
				Map<Id, ? extends Link> links) { // print
			for (Id linkId : density.keySet()) {
				double[] bins = density.get(linkId);

				Link link = links.get(linkId);

				boolean hasTraffic = false;
				for (int i = 0; i < bins.length; i++) {
					if (bins[i] != 0.0) {
						hasTraffic = true;
						break;
					}
				}

				if (hasTraffic) {
					Coord coord = link.getCoord();
					System.out.print(linkId.toString() + " : \t");
					System.out.print(coord.getX() + "\t");
					System.out.print(coord.getY() + "\t");
					
					for (int i = 0; i < bins.length; i++) {
						System.out.print(bins[i] + "\t");
					}

					System.out.println();
				}
			}
		}
	
}
