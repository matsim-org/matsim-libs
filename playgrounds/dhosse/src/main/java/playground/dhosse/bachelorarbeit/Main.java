package playground.dhosse.bachelorarbeit;

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
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;

public class Main {
	
	public static void main(String args[]) {
		
//		String file1 = "C:/Users/Daniel/Dropbox/bsc/input/config.xml";
		
		String file1 = "C:/Users/Daniel/Dropbox/bsc/input/network_bridge.xml";
		
		Config config = ConfigUtils.createConfig();
		Scenario scenario = ScenarioUtils.createScenario(config);
//		MatsimPopulationReader pR = new MatsimPopulationReader(scenario);
//		pR.readFile("C:/Users/Daniel/Dropbox/bsc/input/population.xml");
		
		MatsimNetworkReader nr = new MatsimNetworkReader(scenario);
		nr.readFile(file1);
		
		InternalConstants.setOpusHomeDirectory("C:/Users/Daniel/Dropbox/bsc");
		
		NetworkBoundaryBox bbox = new NetworkBoundaryBox();
		bbox.setDefaultBoundaryBox(scenario.getNetwork());
		
		ZoneLayer<Id> measuringPoints = GridUtils.createGridLayerByGridSizeByNetwork(200, 
				   bbox.getBoundingBox());
		SpatialGrid freeSpeedGrid = new SpatialGrid(bbox.getBoundingBox(), 200);
		
		ActivityFacilitiesImpl parcels = new ActivityFacilitiesImpl();
		
		long i = 0;
		
		for(Person p : scenario.getPopulation().getPersons().values()){
				PlanElement pe1 = p.getSelectedPlan().getPlanElements().get(0);
				PlanElement pe2 = p.getSelectedPlan().getPlanElements().get(2);
				if(pe1 instanceof Activity){
					Id id = new IdImpl("cell"+i);
					parcels.createAndAddFacility(id, ((Activity)pe1).getCoord());
					i++;
				}
				if(pe2 instanceof Activity){
					Id id = new IdImpl("cell"+i);
					parcels.createAndAddFacility(id, ((Activity)pe2).getCoord());
					i++;
				}
		}
		
		AccessibilityCalc ac = new AccessibilityCalc(parcels, measuringPoints, freeSpeedGrid, (ScenarioImpl) scenario);
		ac.runAccessibilityComputation();
		
//		NetworkInspector ni = new NetworkInspector(scenario);
//		if(ni.isRoutable())
//			System.out.println("Netzwerk ist routbar...");
//		ni.checkLinkAttributes();
//		ni.checkNodeAttributes();
		
		//TODO: methode isRoutable nochmal umschreiben (s.u.), unterscheidung bei node degree 1: einbahnstraße oder sackgasse?, dimension des untersuchungsgebiets ausgeben lassen
		//TODO: wenn sackgasse: sackgasse, weil se aus dem untersuchungsgebiet rausführt oder "echte" sackgasse
		//TODO: eigenen!!! controlerListener schreiben, ohne vererbung und pipapo,accessibility berechnung soll auch OHNE simulation, OHNE population möglich sein
		//TODO: WAS MACHT DER PARCELBASEDACCESSIBILITYCONTROLERLISTENER GENAU???
		
	}

}