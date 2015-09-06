package playground.wrashid.fd;

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

import playground.wrashid.msimoni.analyses.LinkSelector;

public class MainFundamentalDiagram {

	

	public static void main(String[] args) {
		//String networkFile = "C:/data/workspace3/matsim/output/equil_jdeq/output_network.xml.gz";
		//String eventsFile = "C:/data/workspace3/matsim/output/equil_jdeq/ITERS/it.0/0.events.xml.gz";
		
	//	String networkFile = "C:/data/Dropbox/eigene/ETHZ/matsim/output/equil_jdeq/output_network.xml.gz";
	//	String eventsFile = "C:/data/Dropbox/eigene/ETHZ/matsim/output/equil_jdeq/ITERS/it.10/10.events.xml.gz";
		
	//	String networkFile = "\\\\kosrae.ethz.ch\\ivt-home\\simonimi\\thesis\\output_no_pricing_v5_subtours_JDEQSim_working\\output_network.xml.gz";
	//	String eventsFile =  "\\\\kosrae.ethz.ch\\ivt-home\\simonimi\\thesis\\output_no_pricing_v5_subtours_JDEQSim_working\\ITERS\\it.50\\50.events.xml.gz";
		
		String networkFile = "\\\\kosrae.ethz.ch\\ivt-home\\simonimi\\thesis\\output_no_pricing_v5_subtours_JDEQSim_working\\output_network.xml.gz";
		String eventsFile =  "H:/data/experiments/msimoni/22July2013/output_no_pricing_v5_subtours_JDEQSim_gap5/ITERS/it.10/10.events.xml.gz";
		
		
		
		
		// String networkFile =
		// "H:/data/experiments/TRBAug2011/runs/ktiRun24/output/output_network.xml.gz";
		// String eventsFile =
		// "H:/data/experiments/TRBAug2011/runs/ktiRun24/output/ITERS/it.50/50.events.xml.gz";
		boolean doConsoleOutput = true;
		Coord center = null; // center=null means use all links
		int binSizeInSeconds = 300; // 5 minute bins
		boolean isJDEQSim=true;

		double radiusInMeters = 1000;
		double length = 50.0;
		double sampleSize=0.1;

		Config config = ConfigUtils.createConfig();
		config.network().setInputFile(networkFile);
		Scenario scenario = ScenarioUtils.loadScenario(config);
		center = new Coord(682548.0, 247525.5);
		//center = scenario.createCoord(0, 0);

		Map<Id<Link>, Link> links = LinkSelector.selectLinks(scenario.getNetwork(),
				center, radiusInMeters, length);

		DensityInfoCollectorDualSim densityHandler = new DensityInfoCollectorDualSim(
				links, binSizeInSeconds,isJDEQSim);
		OutFlowInfoCollectorDualSim outflowHandler = new OutFlowInfoCollectorDualSim(
				links, binSizeInSeconds,isJDEQSim);

		densityHandler.reset(0);
		outflowHandler.reset(0);

		EventsManager events = EventsUtils.createEventsManager();

		events.addHandler(densityHandler); // add handler
		events.addHandler(outflowHandler);

		EventsReaderXMLv1 reader = new EventsReaderXMLv1(events);
		reader.parse(eventsFile);

		//HashMap<Id, double[]> densities = calculateDensities(links,
		//		densityHandler, binSizeInSeconds);
		
		HashMap<Id, double[]> densities = densityHandler.getLinkDensities();
		
		System.out.println(densityHandler.getNumberOfProcessedVehicles()); 

		printDensityAndOutFlow(densities, links, outflowHandler, doConsoleOutput,0,"",binSizeInSeconds,sampleSize);
	}

	public static HashMap<Id, double[]> calculateDensities(
			Map<Id, Link> links, DensityInfoCollectorDualSim densityHandler,
			int binSizeInSeconds) {

		HashMap<Id, double[]> density = new HashMap<Id, double[]>();

		for (Id linkId : densityHandler.density.keySet()) {
			Link link = links.get(linkId);

			double[] bins = new double[getNumberOfBins(binSizeInSeconds)];

			for (int i = 1; i < getNumberOfBins(binSizeInSeconds); i++) {
				bins[i] = densityHandler.density.get(linkId)[i]
						/ (link.getLength() * link.getNumberOfLanes());
			}

			density.put(linkId, bins);
		}

		return density;
	}

	private static int getNumberOfBins(int binSizeInSeconds) {
		return (86400 / binSizeInSeconds) + 1;
	}

	public static void printDensityAndOutFlow(HashMap<Id, double[]> density,
			Map<Id<Link>, ? extends Link> links,
			OutFlowInfoCollectorDualSim outflowHandler, boolean doConsoleOutput, int runId, String caption, int binSizeInSeconds, double sampleSize) { // print

		for (Id linkId : density.keySet()) {
			int[] tempBin = outflowHandler.linkOutFlow.get(linkId);

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
				GeneralLib.generateXYScatterPlot("c:/tmp/runId-" + runId +"-link-" + linkId
						+ ".png", densityBins, outFlowBins, caption,
						"density", "outflow");
			}
		}

		System.out
				.println("outflow: ==============================================");

		for (Id linkId : density.keySet()) {
			int[] bins = outflowHandler.linkOutFlow.get(linkId);

			Link link = links.get(linkId);

			boolean hasTraffic = false;
			for (int i = 0; i < bins.length; i++) {
				if (bins[i] != 0.0) {
					hasTraffic = true;
					break;
				}
			}

			if (hasTraffic) {
				if (doConsoleOutput) {
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
				if (doConsoleOutput) {
					Coord coord = link.getCoord();
					System.out.print(linkId.toString() + " : \t");
					System.out.print(coord.getX() + "\t");
					System.out.print(coord.getY() + "\t");
					System.out.print(link.getLength() + "\t");
					System.out.print(link.getNumberOfLanes() + "\t");

					for (int i = 0; i < bins.length; i++) {
						System.out.print(bins[i] + "\t");
					}
				}

				System.out.println();
			}
		}
		
		printCordonDensityFlowGraph(density,outflowHandler,binSizeInSeconds,links,sampleSize);
	}

	private static void printCordonDensityFlowGraph(
			HashMap<Id, double[]> density,
			OutFlowInfoCollectorDualSim outflowHandler, int binSizeInSeconds, Map<Id<Link>, ? extends Link> links, double sampleSize) {
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
			int[] bins = outflowHandler.getLinkOutFlow().get(linkId);
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
		
		GeneralLib.generateXYScatterPlot("c:/tmp/density_2.png", cordonDensity, cordonOutflow, "cordon density vs. flow (sample size:" + sampleSize + ")",
				"density", "outflow");
	}

}
