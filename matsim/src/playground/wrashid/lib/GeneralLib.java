package playground.wrashid.lib;

import org.matsim.api.basic.v01.BasicScenarioImpl;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.facilities.ActivityFacilitiesImpl;
import org.matsim.core.facilities.ActivityFacilitiesImpl;
import org.matsim.core.facilities.FacilitiesWriter;
import org.matsim.core.facilities.MatsimFacilitiesReader;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.population.PopulationReader;
import org.matsim.core.population.PopulationReaderMatsimV4;
import org.matsim.core.population.PopulationWriter;
import org.matsim.world.World;

public class GeneralLib {

	/*
	 * Reads the population from the plans file. TODO: is it possible to remove
	 * the networkFile parameter (it was added, because else some error is
	 * caused).
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
	 * Reads the population from the plans file. TODO: is it possible to remove
	 * the networkFile parameter (it was added, because else some error is
	 * caused).
	 */
	public static Population readPopulation(String plansFile, String networkFile, String facilititiesPath) {

		Population population = new PopulationImpl();

		NetworkLayer network = new NetworkLayer();
		new MatsimNetworkReader(network).readFile(networkFile);

		PopulationReader popReader = new PopulationReaderMatsimV4(population,	network, readActivityFacilities(facilititiesPath) ,null);
		popReader.readFile(plansFile);

		return population;
	}

	/*
	 * Write the population to the specified file.
	 */
	public static void writePopulation(Population population, String plansFile) {
		// these two lines are needed, because else there would be an error...
		BasicScenarioImpl sc = new BasicScenarioImpl();
		Gbl.setConfig(sc.getConfig());
		
		PopulationWriter populationWriter = new PopulationWriter(population, plansFile, "v4");
		
		populationWriter.write();
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
