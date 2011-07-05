package city2000w;

import gis.arcgis.NutsRegionShapeReader;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import kid.KiDDataReader;
import kid.KiDPlanAgentCreator;
import kid.KiDUtils;
import kid.ScheduledVehicles;
import kid.filter.And;
import kid.filter.BerlinFilter;
import kid.filter.BusinessSectorFilter;
import kid.filter.GeoRegionFilterV2;
import kid.filter.LogicVehicleFilter;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.matsim.core.config.Config;
import org.matsim.core.controler.Controler;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.network.NetworkWriter;
import org.matsim.core.router.PlansCalcRoute;
import org.matsim.core.router.costcalculators.FreespeedTravelTimeCost;
import org.matsim.core.router.util.DijkstraFactory;
import org.matsim.core.router.util.TravelCost;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.ConfigUtils;
import org.opengis.feature.simple.SimpleFeature;

import utils.LinkMerger;
import utils.MergingConstraint;
import utils.NetworkFuser;

import city2000w.KiDDataGeoCoder.MobileVehicleFilter;


public class KiDPopulationGenerator {
	
	private static Logger logger = Logger.getLogger(KiDPopulationGenerator.class);
	
	public static void main(String[] args) throws FileNotFoundException, IOException {
		logger.info("create scheduled vehicles from KiD");
		Logger.getRootLogger().setLevel(Level.INFO);
		ScheduledVehicles scheduledVehicles = new ScheduledVehicles();
		KiDDataReader kidReader = new KiDDataReader(scheduledVehicles);
		String directory = "/Volumes/projekte/2000-Watt-City/Daten/KiD/";
		kidReader.setVehicleFile(directory + "KiD_2002_Fahrzeug-Datei.txt");
		kidReader.setTransportChainFile(directory + "KiD_2002_Fahrtenketten-Datei.txt");
		kidReader.setTransportLegFile(directory + "KiD_2002_(Einzel)Fahrten-Datei.txt");
		LogicVehicleFilter andFilter = new And();
		andFilter.addFilter(new MobileVehicleFilter());
//		andFilter.addFilter(new LkwKleiner3Punkt5TFilter());
//		andFilter.addFilter(new BusinessSectorFilter());
		kidReader.setVehicleFilter(andFilter);
		List<SimpleFeature> regions = new ArrayList<SimpleFeature>();
		NutsRegionShapeReader regionReader = new NutsRegionShapeReader(regions, new BerlinFilter(), null);
		regionReader.read(directory + "regions_europe_wgsUtm32N.shp");
		kidReader.setScheduledVehicleFilter(new GeoRegionFilterV2(regions));
		kidReader.run();
		
		Config config = ConfigUtils.createConfig();
		config.addCoreModules();
		ScenarioImpl scen = (ScenarioImpl)ScenarioUtils.createScenario(config);
		
		new MatsimNetworkReader(scen).readFile("networks/berlinRoads.xml");
		
		KiDPlanAgentCreator planAgentCreator = new KiDPlanAgentCreator(scheduledVehicles);
		planAgentCreator.setTransformation(KiDUtils.createTransformation_WGS84ToWGS84UTM33N());
		planAgentCreator.setNetwork((NetworkImpl)scen.getNetwork());
		PlansCalcRoute router = new PlansCalcRoute(null, scen.getNetwork(), 
				new FreespeedTravelTimeCost(-1.0,0.0,0.0), new FreespeedTravelTimeCost(-1.0,0.0,0.0));
		planAgentCreator.setRouter(router);
		planAgentCreator.createPlanAgents();
		planAgentCreator.writePlans("output/berlinPlans.xml");
		
	}

}
