package playground.dhosse.bachelorarbeit;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contrib.matsim4opus.constants.InternalConstants;
import org.matsim.contrib.matsim4opus.gis.GridUtils;
import org.matsim.contrib.matsim4opus.gis.SpatialGrid;
import org.matsim.contrib.matsim4opus.gis.ZoneLayer;
import org.matsim.contrib.matsim4opus.utils.network.NetworkBoundaryBox;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.facilities.ActivityFacilitiesImpl;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordImpl;

public class Main {
	
	public static void main(String args[]) {
		
//		String file1 = "C:/Users/Daniel/Dropbox/bsc/input/config.xml";
		String path = "C:/Users/Daniel/Dropbox/bsc/input";
		String file1 = "berlin_fis";
		String file2 = "berlin_osm";
		String file3 = "berlin_osm_main";
		
		Config config = ConfigUtils.createConfig();
		Scenario scenario = ScenarioUtils.createScenario(config);
		MatsimPopulationReader pr = new MatsimPopulationReader(scenario);
		MatsimNetworkReader nr = new MatsimNetworkReader(scenario);
		nr.readFile(path+"/"+file2+".xml");
		pr.readFile(path+"/"+"/test_population.xml");
		
		InternalConstants.setOpusHomeDirectory("C:/Users/Daniel/Dropbox/bsc");
		
//		NetworkBoundaryBox bbox = new NetworkBoundaryBox();
//		bbox.setDefaultBoundaryBox(scenario.getNetwork());
//		
//		ZoneLayer<Id> measuringPoints = GridUtils.createGridLayerByGridSizeByNetwork(200, 
//				   bbox.getBoundingBox());
//		SpatialGrid freeSpeedGrid = new SpatialGrid(bbox.getBoundingBox(), 200);
//		
//		ActivityFacilitiesImpl parcels = new ActivityFacilitiesImpl();
//		
//		int i=0;
//		for(Person p : scenario.getPopulation().getPersons().values()){
//			PlanElement pe = p.getSelectedPlan().getPlanElements().get(0);
//			PlanElement pe2 = p.getSelectedPlan().getPlanElements().get(2);
//			Coord coord = new CoordImpl(0, 0);
//			if(pe instanceof Activity)
//				coord = ((Activity)pe).getCoord();
//			Id id = new IdImpl("parcel"+i);
//			i++;
//			parcels.createAndAddFacility(id, coord);
//			if(pe2 instanceof Activity)
//				coord = ((Activity)pe).getCoord();
//			id = new IdImpl("parcel"+i);
//			i++;
//			parcels.createAndAddFacility(id, coord);
//		}
//		
//		AccessibilityCalc ac = new AccessibilityCalc(parcels, measuringPoints, freeSpeedGrid, (ScenarioImpl) scenario, file1);
//		ac.runAccessibilityComputation();
		
		NetworkInspector ni = new NetworkInspector(scenario);
//		if(ni.isRoutable())
//			System.out.println("Netzwerk ist routbar...");
//		else
//			System.out.println("Netzwerk ist nicht routbar");
		ni.checkLinkAttributes();
		ni.checkNodeAttributes();
		ni.shpExport();
		
		//TODO: methode isRoutable nochmal umschreiben (s.u.), unterscheidung bei node degree 1: einbahnstraße oder sackgasse?, dimension des untersuchungsgebiets ausgeben lassen
		//TODO: wenn sackgasse: sackgasse, weil se aus dem untersuchungsgebiet rausführt oder "echte" sackgasse
		//TODO: eigenen!!! controlerListener schreiben, ohne vererbung und pipapo,accessibility berechnung soll auch OHNE simulation, OHNE population möglich sein
		//TODO: WAS MACHT DER PARCELBASEDACCESSIBILITYCONTROLERLISTENER GENAU???
		
	}

}