package playground.acmarmol.microcensus2010;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.households.Households;
import org.matsim.households.HouseholdsWriterV10;
import org.matsim.utils.objectattributes.ObjectAttributes;
import org.matsim.utils.objectattributes.ObjectAttributesXmlWriter;
import org.matsim.vehicles.VehicleWriterV1;
import org.matsim.vehicles.Vehicles;

public class Mz2010ToPlans {

	//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////

	private final static Logger log = Logger.getLogger(Mz2010ToPlans.class);
	
//	public static void createMZ2Plans(Config config) throws Exception {
//		
//	System.out.println("MATSim-DB: create Population based on micro census 2010 data.");
//
//	
//	System.out.println("  creating plans object... ");
//	Population plans = ((ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig())).getPopulation();
//	System.out.println("  done.");
//		
//	throw new RuntimeException("the following line did not compile; commented it out.  kai, 30/jul/2012") ;
////	new HouseholdsFromMZ("input/Microcensus2010/haushalte.dat").run();
//	
//	
//	}
	
	
	
	//////////////////////////////////////////////////////////////////////
	// main
	//////////////////////////////////////////////////////////////////////

	public static void main(String[] args) throws Exception {
		
		// you do not need a config.xml file for the data conversion process
		// do something like this:
		
		// from local directory
		String inputBase = "D:/balmermi/documents/data/mz/2010/3_DB_SPSS/dat files/";
		args = new String[] {
				inputBase+"haushalte.dat",
				inputBase+"haushaltspersonen.dat",
				inputBase+"fahrzeuge.dat",
				inputBase+"zielpersonen.dat",
				inputBase+"wege.dat",
				inputBase+"ausgaenge.dat",
				"D:/balmermi/documents/eclipse/output/mz/"
		};
		
		// from your directory
//		String inputBase = "P:/x/y/z/";
//		args = new String[] {
//				inputBase+"haushalte.dat",
//				inputBase+"haushaltspersonen.dat",
//				inputBase+"fahrzeuge.dat",
//				inputBase+"zielpersonen.dat",
//				inputBase+"wege.dat",
//				inputBase+"ausgaenge.dat",
//				"D:/balmermi/documents/eclipse/output/mz/"
//		};
		
		if (args.length != 7) {
			log.error("createMZ2Plans haushalteFile haushaltspersonenFile fahrzeugeFile zielpersonenFile wegeFile ausgaengeFile outputBase");
			System.exit(-1);
		}

		Gbl.startMeasurement();

		// store input parameters
		String haushalteFile = args[0];
		String haushaltspersonenFile = args[1];
		String fahrzeugeFile = args[2];
		String zielpersonenFile = args[3];
		String wegeFile = args[4];
		String ausgaengeFile = args[5];
		String outputBase = args[6];

		// print input parameters
		log.info("haushalteFile: "+haushalteFile);
		log.info("haushaltspersonenFile: "+haushaltspersonenFile);
		log.info("fahrzeugeFile: "+fahrzeugeFile);
		log.info("zielpersonenFile: "+zielpersonenFile);
		log.info("wegeFile: "+wegeFile);
		log.info("ausgaengeFile: "+ausgaengeFile);
		log.info("outputBase: "+outputBase);
		
		// you will need to create households, persons and vehicles, incl. additional object attributes
		ScenarioImpl scenario = (ScenarioImpl)ScenarioUtils.createScenario(ConfigUtils.createConfig());
		Population population = scenario.getPopulation();
		ObjectAttributes personAttributes = new ObjectAttributes();
		Households households = scenario.getHouseholds();
		ObjectAttributes householdAttributes = new ObjectAttributes();
		Vehicles vehicles = scenario.getVehicles();
		ObjectAttributes vehiclesAttributes = new ObjectAttributes();
		
		Gbl.printElapsedTime();

		// use the logger to print messages

		log.info("parsing haushalteFile...");
		// first, parse the household file to create the household database
		// it will fill up the household and the attributes
//		new MZHouseholdParser(households,householdAttributes).parse(haushalteFile);
		log.info("done. (parsing haushalteFile)");
		
		Gbl.printElapsedTime();

		// write intemediate results
		log.info("writing intermediate files...");
		new HouseholdsWriterV10(households).writeFile(outputBase+"/households.00.xml.gz");
		new ObjectAttributesXmlWriter(householdAttributes).writeFile(outputBase+"/householdAttributes.00.xml.gz");
		log.info("done. (writing)");
		
		Gbl.printElapsedTime();

		log.info("parsing fahrzeugeFile...");
		// next fill up the vehicles. For that you need to doublecheck consistency with the households (as given in the MZ database structure)
		// and probably add additional data to the households too
//		new MZVehicleParser(vehicles,vehiclesAttributes,households,householdAttributes).parse(fahrzeugeFile);
		log.info("done. (parsing fahrzeugeFile)");
		
		Gbl.printElapsedTime();

		// write intemediate results
		log.info("writing intermediate files...");
		new HouseholdsWriterV10(households).writeFile(outputBase+"/households.01.xml.gz");
		new ObjectAttributesXmlWriter(householdAttributes).writeFile(outputBase+"/householdAttributes.01.xml.gz");
		new VehicleWriterV1(vehicles).writeFile(outputBase+"vehicles.01.xml.gz");
		new ObjectAttributesXmlWriter(vehiclesAttributes).writeFile(outputBase+"/vehiclesAttributes.01.xml.gz");
		log.info("done. (writing)");

		// and so on
		
		// and you do not need to instantiate this.
		// use it only for the main routine
//		createMZ2Plans(config);
		
		// that whole thing makes it a little bit more modular, so that in future just some of the input files will change the spec
		// you do not need to change the whole process but just the classes that are involved
		
		// hope that helped a bit.
		
		// have fun
	}
}
