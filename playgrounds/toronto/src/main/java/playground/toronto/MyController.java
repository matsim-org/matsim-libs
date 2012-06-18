package playground.toronto;

import java.util.HashMap;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.config.ConfigUtils;
import org.matsim.pt.router.TransitRouterConfig;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.utils.objectattributes.ObjectAttributesXmlReader;
import org.matsim.utils.objectattributes.ObjectAttributes;
import org.matsim.core.utils.collections.Tuple;

//import playground.toronto.transitfares.FareBasedTransitRouter;
import playground.toronto.transitfares.FareBasedTransitRouterFactory;
import playground.toronto.transitfares.FareLookupTableXmlReader;

/**
 * My test controller for running a fare-based transit assignment. 
 * 
 * @author pkucirek
 *
 */
public class MyController{
	
	public static void main(final String[] args){
		
		//Load the config file. FILEPATH is temporary (ie, for debugging)
		final String FILEPATH = "C:\\Documents and Settings\\peter\\Desktop\\Fares\\config\\config.xml";
		Config config = ConfigUtils.loadConfig(FILEPATH);
		
		//Load the fare lookup table
		HashMap<String, HashMap<Tuple<String,String>, Double>> farelookuptable = new HashMap<String, HashMap<Tuple<String,String>,Double>>();
		String filename = config.getParam("transit_fares", "input_fareLookupTable_file");
		FareLookupTableXmlReader reader_1 = new FareLookupTableXmlReader(farelookuptable);
		reader_1.parse(filename);
		
		//Load the add'l stop_facility attribute "Zone" -> the corresponding fare zone for each transit stop
		ObjectAttributes StopZoneMap = new ObjectAttributes();
		ObjectAttributesXmlReader reader_2 = new ObjectAttributesXmlReader(StopZoneMap);
		reader_2.parse(config.getParam("transit_fares", "input_NodeZoneMap_file"));
		
		//Load the add'l agent attribute "FareClass" -> the fare class of each agent
		ObjectAttributes PersonFareClassMap = new ObjectAttributes();
		reader_2 = new ObjectAttributesXmlReader(PersonFareClassMap);
		reader_2.parse(config.getParam("transit_fares", "input_PersonFareClass_file"));
		
		//Load the scenario & various configs
		Scenario myscenario = ScenarioUtils.loadScenario(config);
		
		TransitSchedule trschedule = null;
		trschedule = myscenario.getScenarioElement(trschedule.getClass());
		
		TransitRouterConfig trconfig = new TransitRouterConfig(config.planCalcScore(),
				config.plansCalcRoute(),
				config.transitRouter(),
				config.vspExperimental());
		
		//Create the controller
		Controler controller = new Controler(myscenario);

		//Sets the controller's TransitRouterFactory to use the modified FBTRf for general-cost routing.
		controller.setTransitRouterFactory(new FareBasedTransitRouterFactory(
				trschedule,
				trconfig,
				farelookuptable,
				StopZoneMap,
				PersonFareClassMap,
				Double.parseDouble(config.getParam("transit_fares", "tvm"))));
		
		/*
		 * TODO 
		 * - Load population of agents (ie, plans)
		 * - Disable the plan mutator (or whatever it's called)
		 * - Should I add an event handler to make note of when an agent pays a fare?
		 */
		
		
	
		controller.run();
		
	}
}

