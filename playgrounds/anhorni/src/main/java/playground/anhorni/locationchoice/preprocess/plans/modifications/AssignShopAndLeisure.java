package playground.anhorni.locationchoice.preprocess.plans.modifications;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.core.facilities.ActivityFacilitiesImpl;
import org.matsim.core.facilities.FacilitiesReaderMatsimV1;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.population.PopulationReader;
import org.matsim.core.population.PopulationWriter;

public class AssignShopAndLeisure {
	private final ScenarioImpl scenario = new ScenarioImpl();
	private PopulationImpl plans = scenario.getPopulation();
	private ActivityFacilitiesImpl facilities = scenario.getActivityFacilities();
	private NetworkImpl network = scenario.getNetwork();
	
	private String plansfilePath;
	private String facilitiesfilePath;
	private String networkfilePath;
	private String outpath = "output/plans/";
	
	private final static Logger log = Logger.getLogger(AssignShopAndLeisure.class);
	
	public static void main(final String[] args) {
		Gbl.startMeasurement();
		final AssignShopAndLeisure assigner = new AssignShopAndLeisure();
		assigner.run(args[0]);
		Gbl.printElapsedTime();
	}	
	
	public void run(String variant) {
		this.init();
		if (variant.equals("0")) {
			ActivityDifferentiationShop differentiator = new ActivityDifferentiationShop(this.scenario);
			differentiator.run();
		}
		// handle leisure
		// ...
		
		this.writePlans(variant);
	}
	
	private void readInputFile(final String inputFile) {
		try {
			FileReader fileReader = new FileReader(inputFile);
			BufferedReader bufferedReader = new BufferedReader(fileReader);

			this.networkfilePath = bufferedReader.readLine();
			this.facilitiesfilePath = bufferedReader.readLine();
			this.plansfilePath = bufferedReader.readLine();

			bufferedReader.close();
			fileReader.close();

		} catch (IOException e) {
			Gbl.errorMsg(e);
		}
	}
	
	private void init() {
		
		String pathsFile = "./input/trb/valid/paths.txt";
		this.readInputFile(pathsFile);
						
		log.info("reading the facilities ...");
		new FacilitiesReaderMatsimV1(this.facilities).readFile(facilitiesfilePath);
			
		log.info("reading the network ...");
		new MatsimNetworkReader(this.network).readFile(networkfilePath);
		
		log.info("  reading file " + plansfilePath);
		final PopulationReader plansReader = new MatsimPopulationReader(this.scenario);
		plansReader.readFile(plansfilePath);				
	}
	
	private void writePlans(String variant) {
		if (variant.equals("0")) {
			new PopulationWriter(this.plans).writeFile(this.outpath + "plans0.xml.gz");
		}
		else {
			new PopulationWriter(this.plans).writeFile(this.outpath + "plans1.xml.gz");
		}
	}
}
