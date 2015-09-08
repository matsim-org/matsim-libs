package playground.wrashid.msimoni.analyses;

import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.parking.lib.GeneralLib;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsReaderXMLv1;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.scenario.ScenarioUtils;

import playground.wrashid.fd.InstantaniousOutflowCollector;

public class MainAdaptedDensityCalculationWithSpeedInstantanious {

		public static void main(String[] args) {
			//String networkFile = "C:/Users/simonimi/workspace_new/matsim/output/equil_JDEQSim/output_network.xml.gz";
			//String eventsFile =  "C:/Users/simonimi/workspace_new/matsim/output/equil_JDEQSim/ITERS/it.10/10.events.xml.gz";
			
			String networkFile = "C:/data/workspace3/matsim/output/equil/output_network.xml.gz";
			String eventsFile = "C:/data/workspace3/matsim/output/equil/ITERS/it.10/10.events.xml.gz";
			
			//String networkFile = "C:/Users/simonimi/workspace_new/matsim/output2/berlin/output_network.xml.gz";
			//String eventsFile =  "C:/Users/simonimi/workspace_new/matsim/output2/berlin/ITERS/it.0/0.events.xml.gz";
		
			
			//String networkFile = "H:/thesis/output_no_pricing_v5_subtours_bugfix/output_network.xml.gz";
			//String eventsFile =  "H:/thesis/output_no_pricing_v5_subtours_congested_withHole/ITERS/it.0/0.events.xml.gz";

			//String networkFile = "\\\\kosrae.ethz.ch\\ivt-home\\simonimi/thesis/output_no_pricing_v5_subtours_bugfix/output_network.xml.gz";
			//String eventsFile =  "\\\\kosrae.ethz.ch\\ivt-home\\simonimi/thesis/output_no_pricing_v5_subtours_bugfix/ITERS/it.10/10.events.xml.gz";

			
			//String networkFile = "H:/data/experiments/TRBAug2011/runs/run4/output/herbie.output_network.xml.gz";
			//String eventsFile =	"H:/data/experiments/TRBAug2011/runs/run4/output/ITERS/it.0/herbie.0.events.xml.gz";	
			
			
			Coord center = null; // center=null means use all links
			int binSizeInSeconds = 30;	// 5 minute bins

			double radiusInMeters = 10000;
			double length = 50.0;

			Config config = ConfigUtils.createConfig();
			config.network().setInputFile(networkFile);
			Scenario scenario = ScenarioUtils.loadScenario(config);
			//center = scenario.createCoord(682548.0, 247525.5);
			center = new Coord((double) 0, (double) 0);
			
			Map<Id<Link>, Link> links = LinkSelector.selectLinks(scenario.getNetwork(), center, radiusInMeters, length);
			
			InFlowInfoCollectorWithPt inflowHandler = new InFlowInfoCollectorWithPt(links, binSizeInSeconds);
			InstantaniousOutflowCollector outflowHandler = new InstantaniousOutflowCollector(links, binSizeInSeconds);
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
			
			printDensityAndOutFlow(densities,links, outflowHandler, averageSpeedCalculator);
		}
		
		private static HashMap<Id, double[]> calculateDensities(
				Map<Id<Link>, Link> links, InFlowInfoCollectorWithPt inflowHandler,
				InstantaniousOutflowCollector outflowHandler,
				HashMap<Id, double[]> averageSpeeds, int binSizeInSeconds) {
			
			HashMap<Id, double[]> density = new HashMap<Id, double[]>();
			HashMap<Id, int[]> linkInFlow = inflowHandler.getLinkInFlow();
			HashMap<Id, int[]> linkOutFlow = outflowHandler.getLinkOutFlow();
			
			for (Link link : links.values()) {
				double[] bins=new double[getNumberOfBins(binSizeInSeconds)];
				
				if (inflowHandler.getFlow(link.getId())==null || outflowHandler.getLinkOutFlow().get(link.getId())==null){
					continue;
				}
				
				//if(outFlowAccumulated[0]-inFlowAccumulated[0]==0){
					bins[0]=linkOutFlow.get(link.getId())[0]*(3600/binSizeInSeconds)/(averageSpeeds.get(link.getId())[0]*3.6);
				//} else {
				//	bins[0]=linkInFlow.get(link.getId())[0]-linkOutFlow.get(link.getId())[0]/(link.getLength()*link.getNumberOfLanes()/1000);
				//}
				
				for (int i=1;i<getNumberOfBins(binSizeInSeconds);i++){
					//if(outFlowAccumulated[i]-inFlowAccumulated[i]==0){
						bins[i]=linkOutFlow.get(link.getId())[i]*(3600/binSizeInSeconds)/(averageSpeeds.get(link.getId())[i]*3.6);
					//} else {
					//	bins[i]=linkInFlow.get(link.getId())[i]-linkOutFlow.get(link.getId())[i]/(link.getLength()*link.getNumberOfLanes()/1000)+bins[i-1];
					//}
				}
				
				density.put(link.getId(), bins);
			}
			
			return density;
		}

		
		
		private static int getNumberOfBins(int binSizeInSeconds) {
			return (86400 / binSizeInSeconds) + 1;
		}
		
		public static void printDensityAndOutFlow(HashMap<Id, double[]> density,
				Map<Id<Link>, ? extends Link> links, InstantaniousOutflowCollector outflowHandler, AverageSpeedCalculator averageSpeedCalculator) { // print
			
			for (Id linkId : density.keySet()) {
				double[] averageSpeed = averageSpeedCalculator.getAverageSpeeds().get(linkId);

				double[] densityBins = density.get(linkId);

				boolean hasTraffic = false;
				for (int i = 0; i < averageSpeed.length; i++) {
					if (averageSpeed[i] != 0.0) {
						hasTraffic = true;
						break;
					}
				}

				if (hasTraffic) {
					GeneralLib.generateXYScatterPlot("c:/tmp/links-speed" + linkId
							+ ".png", densityBins, averageSpeed, "speed vs. density",
							"density", "speed");
				}
			}
			
			for (Id linkId : density.keySet()) {
				int[] tempBin = outflowHandler.getLinkOutFlow().get(linkId);

				double[] outFlowBins = new double[tempBin.length];

				for (int i = 0; i < tempBin.length; i++) {
					outFlowBins[i] = tempBin[i];
				}

				double[] densityBins = density.get(linkId);

				boolean hasTraffic = false;
				for (int i = 0; i < tempBin.length; i++) {
					if (tempBin[i] != 0.0) {
						hasTraffic = true;
						break;
					}
				}

				if (hasTraffic) {
					GeneralLib.generateXYScatterPlot("c:/tmp/links-outflow" + linkId
							+ ".png", densityBins, outFlowBins, "outfow vs. density",
							"density", "outfow");
				}
			}
			
		
			
			
			for (Id linkId : density.keySet()) {
				double[] bins = averageSpeedCalculator.getAverageSpeeds().get(linkId);

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
					System.out.print(link.getLength() + "\t");
					System.out.print(link.getNumberOfLanes() + "\t");
					
					for (int i = 0; i < bins.length; i++) {
						System.out.print(bins[i] + "\t");
					}

					System.out.println();
				}
			}
			System.out.println("outflow: ==============================================");
			
			for (Id linkId : density.keySet()) {
				int[] bins = outflowHandler.getLinkOutFlow().get(linkId);

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
					System.out.print(link.getLength() + "\t");
					System.out.print(link.getNumberOfLanes() + "\t");
					
					for (int i = 0; i < bins.length; i++) {
						System.out.print(bins[i] + "\t");
					}

					System.out.println();
				}
			}
			
			System.out.println("density: ==============================================");
			
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
					System.out.print(link.getLength() + "\t");
					System.out.print(link.getNumberOfLanes() + "\t");
					
					for (int i = 0; i < bins.length; i++) {
						System.out.print(bins[i] + "\t");
					}

					System.out.println();
				}
			}
		}
	
}
