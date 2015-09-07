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
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.scenario.ScenarioUtils;

public class MainAdaptedDensityCalculationWithSpeed {

	public static void main(String[] args) {
	//	String networkFile = "C:/data/workspace3/matsim/output/equil/output_network.xml.gz";
	//	String eventsFile = "C:/data/workspace3/matsim/output/equil/ITERS/it.10/10.events.xml.gz";

		// String networkFile =
		// "C:/Users/simonimi/workspace_new/matsim/output/equil_JDEQSim/output_network.xml.gz";
		// String eventsFile =
		// "C:/Users/simonimi/workspace_new/matsim/output/equil_JDEQSim/ITERS/it.10/10.events.xml.gz";

		 String networkFile =
		 "C:/data/zhengn-backup-may-2013/zhengn/OUTPUT_JULY/1stmfdpricing_v2/output_network.xml.gz";
		 String eventsFile =
		 "C:/data/zhengn-backup-may-2013/zhengn/OUTPUT_JULY/1stmfdpricing_v2/ITERS/it.50/50.events.txt.gz";

		// String networkFile =
		// "C:/Users/simonimi/workspace_new/matsim/output2/berlin/output_network.xml.gz";
		// String eventsFile =
		// "C:/Users/simonimi/workspace_new/matsim/output2/berlin/ITERS/it.0/0.events.xml.gz";

		// String networkFile =
		// "H:/thesis/output_no_pricing_v5_subtours_bugfix/output_network.xml.gz";
		// String eventsFile =
		// "H:/thesis/output_no_pricing_v5_subtours_congested_withHole/ITERS/it.0/0.events.xml.gz";

		// String networkFile =
		// "\\\\kosrae.ethz.ch\\ivt-home\\simonimi/thesis/output_no_pricing_v5_subtours_bugfix/output_network.xml.gz";
		// String eventsFile =
		// "\\\\kosrae.ethz.ch\\ivt-home\\simonimi/thesis/output_no_pricing_v5_subtours_bugfix/ITERS/it.10/10.events.xml.gz";

		// String networkFile =
		// "H:/data/experiments/TRBAug2011/runs/run4/output/herbie.output_network.xml.gz";
		// String eventsFile =
		// "H:/data/experiments/TRBAug2011/runs/run4/output/ITERS/it.0/herbie.0.events.xml.gz";

		Coord center = null; // center=null means use all links
		double length = 50.0;

		Config config = ConfigUtils.createConfig();
		config.network().setInputFile(networkFile);
		Scenario scenario = ScenarioUtils.loadScenario(config);
		
		 int binSizeInSeconds = 300; 
		  double radiusInMeters =		 1000;
		center = new Coord(682548.0, 247525.5);
		 
		/**
		int binSizeInSeconds = 300; // 5 minute bins
		double radiusInMeters = 5000;
		center = scenario.createCoord(0, 0);
**/
		Map<Id<Link>, Link> links = LinkSelector.selectLinks(scenario.getNetwork(),
				center, radiusInMeters, length);

		InFlowInfoCollectorWithPt inflowHandler = new InFlowInfoCollectorWithPt(
				links, binSizeInSeconds);
		OutFlowInfoCollectorWithPt outflowHandler = new OutFlowInfoCollectorWithPt(
				links, binSizeInSeconds);
		AverageSpeedCalculator averageSpeedCalculator = new AverageSpeedCalculator(
				links, binSizeInSeconds);

		inflowHandler.reset(0);
		outflowHandler.reset(0);

		EventsManager events = EventsUtils.createEventsManager();

		events.addHandler(inflowHandler); // add handler
		events.addHandler(outflowHandler);
		events.addHandler(averageSpeedCalculator);

		//EventsReaderXMLv1 reader = new EventsReaderXMLv1(events);
		//reader.parse(eventsFile);
		MatsimEventsReader reader = new MatsimEventsReader(events);
		 reader.readFile(eventsFile);

		HashMap<Id, double[]> densities = calculateDensities(links,
				inflowHandler, outflowHandler,
				averageSpeedCalculator.getAverageSpeeds(), binSizeInSeconds);

		printDensityAndOutFlow(densities, links, outflowHandler,
				averageSpeedCalculator, binSizeInSeconds);
	}

	private static HashMap<Id, double[]> calculateDensities(
			Map<Id<Link>, Link> links, InFlowInfoCollectorWithPt inflowHandler,
			OutFlowInfoCollectorWithPt outflowHandler,
			HashMap<Id, double[]> averageSpeeds, int binSizeInSeconds) {

		HashMap<Id, double[]> density = new HashMap<Id, double[]>();
		HashMap<Id, int[]> linkInFlow = inflowHandler.getLinkInFlow();
		HashMap<Id<Link>, int[]> linkOutFlow = outflowHandler.getLinkOutFlow();

		for (Link link : links.values()) {
			double[] bins = new double[getNumberOfBins(binSizeInSeconds)];

			if (inflowHandler.getFlow(link.getId()) == null
					|| outflowHandler.getFlow(link.getId()) == null) {
				continue;
			}

			int[] outFlowAccumulated = inflowHandler.getAccumulatedFlow(link
					.getId());
			int[] inFlowAccumulated = outflowHandler.getAccumulatedFlow(link
					.getId());

			// if(outFlowAccumulated[0]-inFlowAccumulated[0]==0){
			bins[0] = linkOutFlow.get(link.getId())[0]
					* (3600 / binSizeInSeconds)
					/ (averageSpeeds.get(link.getId())[0] * 3.6);
			// } else {
			// bins[0]=linkInFlow.get(link.getId())[0]-linkOutFlow.get(link.getId())[0]/(link.getLength()*link.getNumberOfLanes()/1000);
			// }

			for (int i = 1; i < getNumberOfBins(binSizeInSeconds); i++) {
				// if(outFlowAccumulated[i]-inFlowAccumulated[i]==0){
				bins[i] = linkOutFlow.get(link.getId())[i]
						* (3600 / binSizeInSeconds)
						/ (averageSpeeds.get(link.getId())[i] * 3.6);
				// } else {
				// bins[i]=linkInFlow.get(link.getId())[i]-linkOutFlow.get(link.getId())[i]/(link.getLength()*link.getNumberOfLanes()/1000)+bins[i-1];
				// }
			}

			density.put(link.getId(), bins);
		}

		return density;
	}

	private static int getNumberOfBins(int binSizeInSeconds) {
		return (86400 / binSizeInSeconds) + 1;
	}

	public static void printDensityAndOutFlow(HashMap<Id, double[]> density,
			Map<Id<Link>, ? extends Link> links,
			OutFlowInfoCollectorWithPt outflowHandler,
			AverageSpeedCalculator averageSpeedCalculator, int binSizeInSeconds) { // print

		for (Id linkId : density.keySet()) {
			double[] bins = averageSpeedCalculator.getAverageSpeeds().get(
					linkId);

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
		System.out
				.println("outflow: ==============================================");

		for (Id linkId : density.keySet()) {
			int[] bins = outflowHandler.getFlow(linkId);

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

		System.out
				.println("density: ==============================================");

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

		double[] cordonDensity = new double[getNumberOfBins(binSizeInSeconds)];
		double[] cordonOutflow = new double[getNumberOfBins(binSizeInSeconds)];
		double lengthOfLinksInCordon = 0;

		for (Link link : links.values()) {
			lengthOfLinksInCordon += link.getLength() * link.getNumberOfLanes();
		}

		for (Id linkId : density.keySet()) {
			double[] bins = density.get(linkId);
			Link link = links.get(linkId);
			for (int i = 0; i < bins.length; i++) {
				cordonDensity[i]+= bins[i] * link.getLength()
						* link.getNumberOfLanes();
			}
		}

		for (Id linkId : outflowHandler.getLinkOutFlow().keySet()) {
			int[] bins = outflowHandler.getFlow(linkId);
			Link link = links.get(linkId);
			for (int i = 0; i < bins.length; i++) {
				cordonOutflow[i] += bins[i] * link.getLength()
						* link.getNumberOfLanes();
			}
		}

		for (int i = 0; i < cordonOutflow.length; i++) {
			cordonDensity[i]/=lengthOfLinksInCordon;
			cordonOutflow[i]/=lengthOfLinksInCordon;
		}

		System.out.println("cordon density:");
		for (int i = 0; i < cordonDensity.length; i++) {
			System.out.print(cordonDensity[i] + "\t");
		}
		System.out.println();

		System.out.println("cordon outflow:");
		for (int i = 0; i < cordonOutflow.length; i++) {
			System.out.print(cordonOutflow[i] + "\t");
		}
		System.out.println();
		
		GeneralLib.generateXYScatterPlot("c:/tmp/density_2.png", cordonDensity, cordonOutflow, "cordon density vs. flow",
				"density", "outflow");

	}

}
