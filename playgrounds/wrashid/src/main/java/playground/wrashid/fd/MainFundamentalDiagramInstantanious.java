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

public class MainFundamentalDiagramInstantanious {

	static boolean doConsoleOutput = false;

	public static void main(String[] args) {
		String networkFile = "C:/data/workspace3/matsim/output/equil_jdeq/output_network.xml.gz";
		String eventsFile = "C:/data/workspace3/matsim/output/equil_jdeq/ITERS/it.10/10.events.xml.gz";
		// String networkFile =
		// "H:/data/experiments/TRBAug2011/runs/ktiRun24/output/output_network.xml.gz";
		// String eventsFile =
		// "H:/data/experiments/TRBAug2011/runs/ktiRun24/output/ITERS/it.50/50.events.xml.gz";

		Coord center = null; // center=null means use all links
		int binSizeInSeconds = 20; // 5 minute bins
		boolean isJDEQSim=true;

		double radiusInMeters = 100000;
		double length = 50.0;

		Config config = ConfigUtils.createConfig();
		config.network().setInputFile(networkFile);
		Scenario scenario = ScenarioUtils.loadScenario(config);
		// center = scenario.createCoord(682548.0, 247525.5);
		center = new Coord((double) 0, (double) 0);

		Map<Id<Link>, Link> links = LinkSelector.selectLinks(scenario.getNetwork(),
				center, radiusInMeters, length);

		DensityInfoCollectorDualSim densityHandler = new DensityInfoCollectorDualSim(
				links, binSizeInSeconds,isJDEQSim);
		InstantaniousOutflowCollector outflowHandler = new InstantaniousOutflowCollector(
				links, binSizeInSeconds);

		densityHandler.reset(0);
		outflowHandler.reset(0);

		EventsManager events = EventsUtils.createEventsManager();

		events.addHandler(densityHandler); // add handler
		events.addHandler(outflowHandler);

		EventsReaderXMLv1 reader = new EventsReaderXMLv1(events);
		reader.parse(eventsFile);

		HashMap<Id<Link>, double[]> densities = calculateDensities(links,
				densityHandler, binSizeInSeconds);

		printDensityAndOutFlow(densities, links, outflowHandler);
	}

	private static HashMap<Id<Link>, double[]> calculateDensities(
			Map<Id<Link>, Link> links, DensityInfoCollectorDualSim densityHandler,
			int binSizeInSeconds) {

		HashMap<Id<Link>, double[]> density = new HashMap<Id<Link>, double[]>();

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

	public static void printDensityAndOutFlow(HashMap<Id<Link>, double[]> density,
			Map<Id<Link>, ? extends Link> links,
			InstantaniousOutflowCollector outflowHandler) { // print

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
				GeneralLib.generateXYScatterPlot("c:/tmp/links" + linkId
						+ ".png", densityBins, outFlowBins, "outflow",
						"density", "outflow");
			}
		}

		System.out
				.println("outflow: ==============================================");

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
	}

}
