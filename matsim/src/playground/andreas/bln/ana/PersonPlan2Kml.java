package playground.andreas.bln.ana;

import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.population.PopulationReader;
import org.matsim.core.utils.geometry.transformations.AtlantisToWGS84;
import org.matsim.core.utils.geometry.transformations.CH1903LV03toWGS84;
import org.matsim.core.utils.geometry.transformations.GK4toWGS84;

import playground.andreas.bln.NewPopulation;


public class PersonPlan2Kml extends NewPopulation{
	
	String outputDir;
	NetworkLayer network;

	public PersonPlan2Kml(NetworkLayer network, PopulationImpl population, String outputDir) {
		super(population, "nofile.xml");
		this.outputDir = outputDir;
		this.network = network;
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Gbl.startMeasurement();

//		String networkFilename = "./examples/equil/network.xml";
//		String plansFilename = "./examples/equil/plans2.xml";
		String networkFilename = "E:/oev-test/output/network.multimodal.xml";
		String plansFilename = "E:\\oev-test\\server\\oevmatsim\\output\\ITERS\\it.100\\plan_17204263X3.txt";
		String outputDirectory = "E:\\oev-test\\GE";

		ScenarioImpl s = new ScenarioImpl();

		NetworkLayer network = s.getNetwork();
		new MatsimNetworkReader(network).readFile(networkFilename);

		PopulationImpl population = s.getPopulation();
		PopulationReader plansReader = new MatsimPopulationReader(population,
				network);
		plansReader.readFile(plansFilename);

		PersonPlan2Kml myPP2Kml = new PersonPlan2Kml(network, population, outputDirectory);
		myPP2Kml.run(population);
		myPP2Kml.writeEndPlans();

		Gbl.printElapsedTime();
	}

	protected void writePersonKML(PersonImpl person) {
		KMLPersonPlanWriter test = new KMLPersonPlanWriter(this.network, person);

		// set CoordinateTransformation
		test.setCoordinateTransformation(new GK4toWGS84());
//		test.setCoordinateTransformation(new AtlantisToWGS84());
//		test.setCoordinateTransformation(new CH1903LV03toWGS84());
		
//		String coordSys = this.config.global().getCoordinateSystem();
//		if (coordSys.equalsIgnoreCase("GK4")) test.setCoordinateTransformation(new GK4toWGS84());
//		if (coordSys.equalsIgnoreCase("Atlantis")) test.setCoordinateTransformation(new AtlantisToWGS84());
//		if (coordSys.equalsIgnoreCase("CH1903_LV03")) test.setCoordinateTransformation(new CH1903LV03toWGS84());

		// waiting for an implementation...
		// new WGS84toCH1903LV03();

//		test.writeActivityLinks(false);
		test.setOutputDirectory(this.outputDir);

		test.writeFile();
	}

	@Override
	public void run(PersonImpl person) {
//		if(person.getId().toString().equals("17204263X3")){//11100153
		writePersonKML(person);	
//		}
	}
	
}
