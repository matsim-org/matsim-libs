package playground.andreas.bln.pop;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.Config;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.population.PopulationReader;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.scenario.ScenarioLoaderImpl;
import org.matsim.core.utils.geometry.CoordImpl;

import playground.mzilske.bvg09.DataPrepare;

public class DeriveSmallScenarioFromBigOne {
	private static final Logger log = Logger.getLogger(DeriveSmallScenarioFromBigOne.class);
	
	public static void main(final String[] args) {
		Gbl.startMeasurement();

		Config config = new Config();
		config.addCoreModules();
		Gbl.setConfig(config);
		
		String bigNetworkFile = "D:/Berlin/BVG/berlin-bvg09/pt/baseplan_900s_bignetwork/network.multimodal.xml.gz";
		String smallNetworkFile = "D:/Berlin/BVG/berlin-bvg09/net/miv_small/m44_344_small_ba.xml.gz";
		String unroutedWholePlansFile = "D:/Berlin/BVG/berlin-bvg09/pop/baseplan_900s.xml.gz";
		String wholeRoutedPlansFile = "D:/Berlin/BVG/berlin-bvg09/pt/baseplan_900s_bignetwork/baseplan_900s.routedOevModell.xml.gz";
		String ptLinesToKeep = "D:/Berlin/BVG/berlin-bvg09/net/pt/linien_im_untersuchungsgebiet.txt";
		
		String popGeoFilterOut = "./baseplan_900s_subset.xml.gz";
		String setBoundingBoxOut = "./baseplan_900s_subset_bb.xml.gz";
		String xy2linksOut = "./baseplan_900s_subset_bb_xy2links_ba.xml.gz";
		
		//DataPrepare
		String inVisumFile = "D:/Berlin/BVG/berlin-bvg09/net/pt/visumnet_utf8woBOM.net";
		String interTransitNetworkFile = "./inter.network.oevModellBln.xml";
		String interTransitScheduleWithoutNetworkFile = "./inter.OevModellBln.xml";
		String outTransitScheduleWithNetworkFile = "./transitSchedule.xml.gz";
		String outVehicleFile = "./vehicles.xml.gz";
		String outMultimodalNetworkFile = "./network.multimodal.xml.gz";
		String outRoutedPlansFile = "./plan.routedOevModell.xml.gz";
		
		Coord minXY = new CoordImpl(4590999.0, 5805999.0);
		Coord maxXY = new CoordImpl(4606021.0, 5822001.0);

		
		log.info("Start DeriveSmallScenarioFromBigOne");
		
		log.info("Start PopGeoFilter");
		
		ScenarioImpl bigNetScenario = new ScenarioImpl();
		log.info("Reading network " + bigNetworkFile);
		NetworkLayer bigNet = bigNetScenario.getNetwork();
		new MatsimNetworkReader(bigNet).readFile(bigNetworkFile);
		
		ScenarioImpl smallNetScenario = new ScenarioImpl();
		log.info("Reading network " + smallNetworkFile);
		NetworkLayer smallNet = smallNetScenario.getNetwork();
		new MatsimNetworkReader(smallNet).readFile(smallNetworkFile);

		log.info("Reading routed population: " + wholeRoutedPlansFile);
		PopulationImpl wholeRoutedPop = new PopulationImpl();
		PopulationReader popReader = new MatsimPopulationReader(new SharedNetScenario(bigNetScenario, wholeRoutedPop));
		popReader.readFile(wholeRoutedPlansFile);
		
		log.info("Reading unrouted population: " + unroutedWholePlansFile);
		PopulationImpl unroutedWholePop = new PopulationImpl();
		PopulationReader origPopReader = new MatsimPopulationReader(new SharedNetScenario(bigNetScenario, unroutedWholePop));
		origPopReader.readFile(unroutedWholePlansFile);

		PopGeoFilter popGeoFilter = new PopGeoFilter(wholeRoutedPop, popGeoFilterOut, unroutedWholePop, minXY, maxXY);
		popGeoFilter.readFile(ptLinesToKeep);
		log.info("Filtering...");
		popGeoFilter.run(wholeRoutedPop);
		popGeoFilter.printStatistics();
		log.info("Finished... plans written to " + popGeoFilterOut);
		System.out.println();
		popGeoFilter.writeEndPlans();
		
		log.info("End PopGeoFilter");
		
		log.info("Start SetPersonCoordsToBoundingBox");

		PopulationImpl inPop = smallNetScenario.getPopulation();
		popReader = new MatsimPopulationReader(smallNetScenario);
		popReader.readFile(popGeoFilterOut);

		SetPersonCoordsToBoundingBox setPersonCoordsToBoundingBox = new SetPersonCoordsToBoundingBox(inPop, setBoundingBoxOut, minXY, maxXY);
		setPersonCoordsToBoundingBox.run(inPop);
		setPersonCoordsToBoundingBox.printStatistics();
		log.info("Finished... plans written to " + setBoundingBoxOut);
		setPersonCoordsToBoundingBox.writeEndPlans();
		
		log.info("End SetPersonCoordsToBoundingBox");
		
		log.info("Start XY2Links");
		
		config.setParam("network", "inputNetworkFile", smallNetworkFile);
		config.setParam("plans", "inputPlansFile", setBoundingBoxOut);
		config.setParam("plans", "outputPlansFile", xy2linksOut);
		
		ScenarioLoaderImpl sl = new ScenarioLoaderImpl(config);
		sl.loadNetwork();
		Network network = sl.getScenario().getNetwork();
		config = sl.getScenario().getConfig();

		final PopulationImpl plans = sl.getScenario().getPopulation();
		plans.setIsStreaming(true);
		final PopulationReader plansReader = new MatsimPopulationReader(sl.getScenario());
		final PopulationWriter plansWriter = new PopulationWriter(plans);
		plansWriter.startStreaming(config.plans().getOutputFile());
		plans.addAlgorithm(new org.matsim.population.algorithms.XY2Links((NetworkLayer) network));
		plans.addAlgorithm(plansWriter);
		plansReader.readFile(config.plans().getInputFile());
		plans.printPlansCount();
		plansWriter.closeStreaming();
				
		log.info("End XY2Links");
		
		log.info("Start DataPrepare");
		
		DataPrepare.run(inVisumFile, smallNetworkFile, xy2linksOut,
				interTransitNetworkFile, interTransitScheduleWithoutNetworkFile,
				outTransitScheduleWithNetworkFile, outVehicleFile, outMultimodalNetworkFile, outRoutedPlansFile);
				
		log.info("End DataPrepare");		
		
		log.info("End DeriveSmallScenarioFromBigOne");
		
		log.info("Please rerun your simulation with: " + outMultimodalNetworkFile + " as networkFile, "
				+ outRoutedPlansFile + " as plans file, " + outVehicleFile + " as pt vehicles file and "
				+ outTransitScheduleWithNetworkFile + " as the pt schedule file.");
		
//		log.info("Please rerun DataPrepare with: " + smallNetworkFile + " as networkFile, "
//				+ xy2linksOut + " as plans file and the corresponding visum file");

		Gbl.printElapsedTime();
	}

}
