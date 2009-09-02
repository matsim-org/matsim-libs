package playground.wrashid.lib;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.api.internal.MatsimWriter;
import org.matsim.core.facilities.ActivityFacilitiesImpl;
import org.matsim.core.facilities.FacilitiesWriter;
import org.matsim.core.facilities.MatsimFacilitiesReader;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.population.PopulationReader;
import org.matsim.core.scenario.ScenarioLoader;

public class GeneralLib {

	/*
	 * Reads the population from the plans file. 
	 * 
	 * Note: use the other method with the same name, if this poses problems.
	 */
	public static Population readPopulation(String plansFile, String networkFile) {
		Population population = new PopulationImpl();

		NetworkLayer network = new NetworkLayer();
		new MatsimNetworkReader(network).readFile(networkFile);

		PopulationReader popReader = new MatsimPopulationReader(population,	network);
		popReader.readFile(plansFile);

		return population;
	}
	
	/*
	 * Reads the population from the plans file.
	 */
	public static Population readPopulation(String plansFile, String networkFile, String facilititiesPath) {
		Scenario sc=new ScenarioImpl(); 
		
		sc.getConfig().setParam("plans", "inputPlansFile", plansFile);
		sc.getConfig().setParam("network", "inputNetworkFile", networkFile);
		sc.getConfig().setParam("facilities", "inputFacilitiesFile", facilititiesPath);
		
		ScenarioLoader sl=new ScenarioLoader((ScenarioImpl) sc);
		
		sl.loadScenario();
		
		return sc.getPopulation();
	}

	/*
	 * Write the population to the specified file.
	 */
	public static void writePopulation(Population population, String plansFile) {
		MatsimWriter populationWriter = new PopulationWriter(population);
		
		populationWriter.write(plansFile);
	}

	public static ActivityFacilitiesImpl readActivityFacilities(String facilitiesFile){		
		ActivityFacilitiesImpl facilities = new ActivityFacilitiesImpl();
		new MatsimFacilitiesReader(facilities).readFile(facilitiesFile);
		return facilities;	
	}
	
	/*
	 * Write the facilities to the specified file.
	 */
	public static void writeActivityFacilities(ActivityFacilitiesImpl facilities, String facilitiesFile) {
		FacilitiesWriter facilitiesWriter=new FacilitiesWriter(facilities, facilitiesFile);
		facilitiesWriter.write();
	}
	 
}   
 