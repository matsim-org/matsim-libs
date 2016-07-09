package patryk.populationgeneration;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.algorithms.XY2Links;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import patryk.utils.LinksRemover;

public class ActToLinks {
	final static String networkFile = "./networks/network_v09.xml";
	final static String plansFile = "./prep_01_april/plans_10sample_v03.xml";
	final static String activityLocations = "./prep_01_april/activityLocations_links.txt";
	final static String plansOutputFile = "./prep_01_april/plans_10sample_v03_links.xml";
	
	public static void main(String[] args) {
		Config config = ConfigUtils.createConfig();
        config.network().setInputFile(networkFile);
        config.plans().setInputFile(plansFile);
        Scenario scenario = ScenarioUtils.loadScenario(config);
        
        CoordinateReferenceSystem crs = MGC.getCRS("EPSG:3857");
        LinksRemover linkRem = new LinksRemover(scenario.getNetwork());
        linkRem.run();
        
        
        XY2Links xy2links = new XY2Links(scenario);
             
        for(Person person : scenario.getPopulation().getPersons().values()) {
        	xy2links.run(person);
        	List<PlanElement> planElements = person.getSelectedPlan().getPlanElements();
        	Coord linkCoord = null;
        	
        	for (PlanElement planelement : planElements) {
        		if (planelement instanceof Activity) {
        			Activity activity = (Activity) planelement;
        			Network network = scenario.getNetwork();
        			linkCoord = network.getLinks().get(activity.getLinkId()).getCoord();
        			writeCoordinates(linkCoord);
        		}
 
        	}
        	
        }
        
        PopulationWriter popWr = new PopulationWriter(scenario.getPopulation(), scenario.getNetwork());
        popWr.write(plansOutputFile);

	}
	
	private static void writeCoordinates(Coord coord) {
		try(PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(activityLocations, true)))) {
		    out.println(String.valueOf(coord.getX()) + ";" + String.valueOf(coord.getY()));
		}catch (IOException e) {
		    // ...
		}
	}

}
