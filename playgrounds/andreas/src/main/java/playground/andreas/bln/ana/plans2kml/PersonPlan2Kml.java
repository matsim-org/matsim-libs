package playground.andreas.bln.ana.plans2kml;

import java.util.TreeSet;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.population.PopulationReader;
import org.matsim.core.utils.geometry.transformations.GK4toWGS84;

import playground.andreas.bln.pop.NewPopulation;


public class PersonPlan2Kml extends NewPopulation{
	
	private static final Logger log = Logger.getLogger(PersonPlan2Kml.class);
	
	boolean warningPrinted = false;
	String outputDir;
	NetworkLayer network;
	TreeSet<String> agentIds;
	
	/**
	 * Converts the selected plan of an agent to a kml-file.
	 * 
	 * @param network The corresponding network file
	 * @param population The population to be read
	 * @param outputDir Directory for kml output
	 * @param agentIds Ids of agents to be converted. Be careful: If <code>null</code> every agent will be converted.
	 */
	public PersonPlan2Kml(NetworkLayer network, PopulationImpl population, String outputDir, TreeSet<String> agentIds) {
		super(population, "nofile.xml");
		this.outputDir = outputDir;
		this.network = network;
		this.agentIds = agentIds;
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Gbl.startMeasurement();

		String networkFilename = null;
		String plansFilename = null;
		String outputDirectory = null;
		TreeSet<String> agentIds = null;
		
		if(args.length < 3){
			System.err.println("Need at least three arguments: networkfile, plansfile, outputdirectory");
			System.exit(1);

		} else {
			networkFilename = args[0];
			plansFilename = args[1];
			outputDirectory = args[2];
			
			if(args.length > 3){
				agentIds = new TreeSet<String>();
				for (int i = 3; i < args.length; i++) {
					agentIds.add(args[i]);					
				}				
			}
		}


		ScenarioImpl s = new ScenarioImpl();

		NetworkLayer network = s.getNetwork();
		new MatsimNetworkReader(network).readFile(networkFilename);

		PopulationImpl population = s.getPopulation();
		PopulationReader plansReader = new MatsimPopulationReader(population, network);
		plansReader.readFile(plansFilename);

		PersonPlan2Kml myPP2Kml = new PersonPlan2Kml(network, population, outputDirectory, agentIds);
		myPP2Kml.run(population);
		myPP2Kml.writeEndPlans();

		Gbl.printElapsedTime();
	}

	protected void writePersonKML(Person person) {
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
	public void run(Person person) {
		if(this.agentIds == null){
			writePersonKML(person);
			if(!this.warningPrinted){
				log.info("Will write every agent to kml");
				this.warningPrinted = true;
			}

		} else if(this.agentIds.contains(person.getId().toString())){
			writePersonKML(person);
			if(!this.warningPrinted){
				log.info("Will write " + this.agentIds.size() + " agents to kml");
				this.warningPrinted = true;
			}
			log.info(" " + person.getId() + " written to kml");
		}
	}
	
}
