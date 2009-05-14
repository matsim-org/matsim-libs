package playground.anhorni.locationchoice.preprocess.plans.modifications;

import org.apache.log4j.Logger;
import org.matsim.core.api.facilities.ActivityFacilities;
import org.matsim.core.api.population.Population;
import org.matsim.core.facilities.ActivityFacilitiesImpl;
import org.matsim.core.facilities.FacilitiesReaderMatsimV1;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.population.PopulationReader;
import org.matsim.core.population.PopulationWriter;

public class AssignShopAndLeisure {
	private Population plans = new PopulationImpl();
	String plansFile = "./input/plans/plans.xml.gz";
	private String facilitiesFile = "./input/facilities/facilities.xml.gz";
	private String networkFile = "./input/networks/ivtch.xml.gz";
	private String outpath = "output/valid/plans/";
	
	private final static Logger log = Logger.getLogger(AssignShopAndLeisure.class);
	
	public static void main(final String[] args) {
		Gbl.startMeasurement();
		final AssignShopAndLeisure assigner = new AssignShopAndLeisure();
		assigner.run();
		Gbl.printElapsedTime();
	}	
	
	public void run() {
		this.init();
		this.writePlans();
		
		ActivityDifferentiationShop differentiator = new ActivityDifferentiationShop(this.plans);
		differentiator.run();
	}
	
	private void init() {
		ActivityFacilities facilities = new ActivityFacilitiesImpl();
		NetworkLayer network = new NetworkLayer();
			
		log.info("reading the facilities ...");
		new FacilitiesReaderMatsimV1(facilities).readFile(this.facilitiesFile);
			
		log.info("reading the network ...");
		new MatsimNetworkReader(network).readFile(this.networkFile);
		
		log.info("  reading file " + this.plansFile);
		final PopulationReader plansReader = new MatsimPopulationReader(this.plans, network);
		plansReader.readFile(this.plansFile);	
	}
	
	private void writePlans() {
		PopulationWriter writer = new PopulationWriter(this.plans, this.outpath + "plans.xml.gz");
		writer.write();
	}
}
