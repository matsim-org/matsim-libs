package playground.anhorni.locationchoice.cs;


import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import org.apache.log4j.Logger;
import org.matsim.controler.Controler;
import org.matsim.gbl.Gbl;
import org.matsim.mobsim.cppdeqsim.DEQSimControler;
import org.matsim.network.MatsimNetworkReader;
import org.matsim.network.NetworkLayer;
import org.matsim.population.Population;
import org.matsim.population.PopulationWriter;

import playground.anhorni.locationchoice.cs.choicesetextractors.ExtractChoiceSetsRouting;
import playground.anhorni.locationchoice.cs.depr.filters.ActTypeAndAreaTripFilter;
import playground.anhorni.locationchoice.cs.depr.filters.SampleDrawer;
import playground.anhorni.locationchoice.cs.depr.filters.SampleDrawerFixedSizeRandom;
import playground.anhorni.locationchoice.cs.depr.filters.SampleDrawerFixedSizeTravelCosts;
import playground.anhorni.locationchoice.cs.depr.filters.TripFilter;
import playground.anhorni.locationchoice.cs.helper.ChoiceSet;
import playground.anhorni.locationchoice.cs.helper.ZHFacilities;
import playground.anhorni.locationchoice.cs.io.CSWriter;
import playground.anhorni.locationchoice.cs.io.ChoiceSetWriterSimple;
import playground.anhorni.locationchoice.cs.io.CSShapeFileWriter;
import playground.anhorni.locationchoice.cs.io.CompareTrips;
import playground.anhorni.locationchoice.cs.io.NelsonTripReader;
import playground.anhorni.locationchoice.cs.io.NelsonTripWriter;
import playground.anhorni.locationchoice.cs.io.TripStats;
import playground.anhorni.locationchoice.cs.io.ZHFacilitiesReader;
import playground.anhorni.locationchoice.cs.io.ZHFacilitiesWriter;
import playground.balmermi.mz.PlansCreateFromMZ;

public class GenerateChoiceSets {

	//private static int idOffset = 20000000;
	public static double epsilon = 0.01;	
	private Population choiceSetPopulation = new Population(false);
	
	private NetworkLayer network = new NetworkLayer();
	//private TreeMap<Id, ArrayList<ZHFacility>> zhFacilitiesByLink = new TreeMap<Id, ArrayList<ZHFacility>>();
	
	private ZHFacilities zhFacilities;
	
	private List<ChoiceSet> carChoiceSets = null;
	private List<ChoiceSet> walkChoiceSets = null;
	private List<CSWriter> writers = new Vector<CSWriter>();
	private TripFilter filter;
	private SampleDrawer sampleDrawer = null;
	private boolean isSetup = false;
	Controler controler = null;
	private int choiceSetSize = 20;
	
	// args
	private String choiceSetPopulationFile = null;	
	private String matsimRunConfigFile = null;
	private String zhFacilitiesFile = null;
	private String shapeFile = null;
	private double walkingSpeed = 5.0;
	private String outdir = null;
	private String sampling;
	private String readNelson;
	private String DEQSim;
	private String mode;
	
	private final static Logger log = Logger.getLogger(GenerateChoiceSets.class);
	
	public static void main(String[] args) {
		
		// for the moment hard-coding
		String inputFile = "./input/input.txt";
		/*
		String inputFile = args[0];
		if (args.length != 1) {
			System.out.println("Too few or too many arguments. Exit");
			System.exit(1);
		}
		*/
		
		Gbl.startMeasurement();

		GenerateChoiceSets generator = new GenerateChoiceSets();
		generator.readInputFile(inputFile);
		if (!generator.isSetup()) {
			generator.setup();
		}
		generator.run();
		
		Gbl.printElapsedTime();
	}
	
	private void readInputFile(String inputFile) {
		try {
			FileReader fileReader = new FileReader(inputFile);
			BufferedReader bufferedReader = new BufferedReader(fileReader);

			this.matsimRunConfigFile = bufferedReader.readLine();
			this.choiceSetPopulationFile = bufferedReader.readLine();
			this.outdir = bufferedReader.readLine();
			this.zhFacilitiesFile = bufferedReader.readLine();
			this.shapeFile = bufferedReader.readLine();
			this.walkingSpeed = Double.parseDouble(bufferedReader.readLine().trim())/3.6; 
			this.choiceSetSize = Integer.parseInt(bufferedReader.readLine().trim());
			this.sampling = bufferedReader.readLine();
			this.readNelson = bufferedReader.readLine();
			this.DEQSim = bufferedReader.readLine();
			this.mode = bufferedReader.readLine();
			
			log.info("MATSim config file: " + this.matsimRunConfigFile);
			log.info("choice set population file: " + this.choiceSetPopulationFile);
			log.info("out dir: " + this.outdir);
			log.info("zhFacilitiesFile" + this.zhFacilitiesFile);
			log.info("shape file:" + this.shapeFile);
			log.info("walkingSpeed = " + this.walkingSpeed + " m/s");
			log.info("choice set size: " + this.choiceSetSize);
			log.info("Sampling :" + this.sampling);
			log.info("readNelson: " + this.readNelson);
			log.info("DEQSim : " + this.DEQSim);
			log.info("mode : " + this.mode);

			bufferedReader.close();
			fileReader.close();
		
		} catch (IOException e) {
			Gbl.errorMsg(e);
		}			
	}
	
	/* Setting of trip filter, writers and the sampler 
	 * Overwrite this method or use getters and setters and set this.setup to true
	 */
	
	public void setup() {
		
		this.zhFacilities = new ZHFacilities();
		
		//filters
		ActTypeAndAreaTripFilter filterAreaAndType = new ActTypeAndAreaTripFilter(this.shapeFile, "s");
		this.filter = filterAreaAndType;
		
		//writers
		new File(this.outdir +"shapefiles").mkdir();
		new File(this.outdir +"shapefiles/singletrips").mkdir();
		new File(this.outdir +"shapefiles/singlechoicesets").mkdir();
		
		ChoiceSetWriterSimple writer = new ChoiceSetWriterSimple(this.zhFacilities);
		this.writers.add(writer);	
		CSShapeFileWriter shpWriter = new CSShapeFileWriter();
		this.writers.add(shpWriter);
		
		TripStats tripStats = new TripStats(this.mode);
		this.writers.add(tripStats);
		
		
		
		// sampler
		SampleDrawer sampleDrawer;
		if (sampling.equals("random")) {
			sampleDrawer = new SampleDrawerFixedSizeRandom(this.choiceSetSize);
		}
		else {
			sampleDrawer = new SampleDrawerFixedSizeTravelCosts(this.choiceSetSize, true);
		}
		this.setSampleDrawer(sampleDrawer);
	}
	
	public void run() {
			
		String configArgs [] = {this.matsimRunConfigFile};	
		Gbl.createConfig(configArgs);
								
		this.createChoiceSetFacilities();
				
		if (this.readNelson.equals("true")) {
			this.carChoiceSets = new NelsonTripReader(this.network, this.zhFacilities).readFiles("input/MZ2005_Wege.dat", "input/810Trips.dat", "car");
			this.walkChoiceSets = new NelsonTripReader(this.network, this.zhFacilities).readFiles("input/MZ2005_Wege.dat", "input/810Trips.dat", "walk");				
		}
		else {
			this.choiceSetPopulation = this.createChoiceSetPopulationFromMZ();
			new PopulationWriter(choiceSetPopulation, this.outdir+"/MZPopulation.txt", "v4").write();
			
			// TODO: maybe also optimize area?
			this.carChoiceSets = this.filter.apply(this.choiceSetPopulation, "car");
			this.walkChoiceSets = this.filter.apply(this.choiceSetPopulation, "walk");	
		}
		
		if (this.DEQSim.equals("true")) {
			String [] args = {this.matsimRunConfigFile};
			this.controler = new DEQSimControler(args);
		}
		else {
			this.controler = new Controler(this.matsimRunConfigFile);
		}		
		
		ExtractChoiceSetsRouting listenerCar = new ExtractChoiceSetsRouting(this.controler, this.zhFacilities, 
				this.carChoiceSets, "car");
		
		/*
		 * This does NOT work at the moment:
		 *
		// set free speed to walking speed
		if (this.walkingSpeed > 0.0) {	
			Iterator<Link> link_it = this.network.getLinks().values().iterator();
			while (link_it.hasNext()) {		
				link_it.next().setFreespeed(this.walkingSpeed);
			}
		}
		*/
				
		ExtractChoiceSetsRouting listenerWalk = new ExtractChoiceSetsRouting(this.controler, this.zhFacilities, 
					this.walkChoiceSets, "walk");
		
		if (this.mode.equals("car")) {
			controler.addControlerListener(listenerCar);
		}
		else if (this.mode.equals("walk")) {
			controler.addControlerListener(listenerWalk);
		}
		else {
			log.error("No mode chosen");
		}
		
		
		log.info("Running controler: ...");
		controler.run();
		
		/*
		CSShapeFileWriter shpWriter = new CSShapeFileWriter(this.outdir);
		shpWriter.writeChoiceSets(this.outdir, "carBeforeSampling", this.carChoiceSets);
		shpWriter.writeChoiceSets(this.outdir, "walkBeforeSampling", this.walkChoiceSets);
		*/
		
		// sample the choice sets
		
		log.info("Sampling: ...");
		this.drawSample();
		
		log.info("Output: ...");
		this.output();	
	}
		
	private Population createChoiceSetPopulationFromMZ() {	
		
		Population temporaryPopulation = new Population(false);
		
		try {
			new PlansCreateFromMZ(this.choiceSetPopulationFile,this.outdir+"/output_wegeketten.dat",1,7).run(temporaryPopulation);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return temporaryPopulation;
	}
			
	private void createChoiceSetFacilities() {		
		MatsimNetworkReader networkReader = new MatsimNetworkReader(this.network);
		networkReader.readFile(Gbl.getConfig().network().getInputFile());
				
		ZHFacilitiesReader zhFacilitiesReader = new ZHFacilitiesReader(this.network);
		zhFacilitiesReader.readFile(this.zhFacilitiesFile, this.zhFacilities);
		
		new ZHFacilitiesWriter().write(this.outdir, this.zhFacilities);
	}
						
	private void output() {			
		Iterator<CSWriter> writer_it = this.writers.iterator();
		while (writer_it.hasNext()) {
			CSWriter writer = writer_it.next();
			if (this.mode.equals("car")) {
				writer.write(this.outdir, "car", this.carChoiceSets);
			}
			else if (this.mode.equals("walk")) {
				writer.write(this.outdir, "walk", this.walkChoiceSets);
			}	
		}
		
		NelsonTripWriter nelsonWriter = new NelsonTripWriter();
		nelsonWriter.write(this.outdir, "car", this.carChoiceSets);
		nelsonWriter.write(this.outdir, "walk", this.walkChoiceSets);
		
		
		CompareTrips compareTripsCar = new CompareTrips(this.outdir, "car");
		compareTripsCar.compare("input/ttbcar.dat", this.carChoiceSets);
		
		CompareTrips compareTripsWalk = new CompareTrips(this.outdir, "walk");
		compareTripsWalk.compare("input/ttbwalk.dat", this.walkChoiceSets);
		
	}
	
	private void drawSample() {
		if (this.sampleDrawer != null) {
			sampleDrawer.drawSample(this.carChoiceSets);
			sampleDrawer.drawSample(this.walkChoiceSets);
		}
	}
	
	
	
	// getters and setters: --------------------------------------------------------------------------------
	public String getSpssfile() {
		return choiceSetPopulationFile;
	}
	public void setSpssfile(String spssfile) {
		this.choiceSetPopulationFile = spssfile;
	}
	public String getOutdir() {
		return outdir;
	}
	public void setOutdir(String outdir) {
		this.outdir = outdir;
	}
	public String getConfigFile() {
		return matsimRunConfigFile;
	}
	public void setConfigFile(String configfile) {
		this.matsimRunConfigFile = configfile;
	}
	public List<ChoiceSet>  getChoiceSets() {
		return carChoiceSets;
	}
	public void setChoiceSets(List<ChoiceSet>  choiceSets) {
		this.carChoiceSets = choiceSets;
	}
	public String getZhFacilitiesFile() {
		return zhFacilitiesFile;
	}
	public void setZhFacilitiesFile(String zhFacilitiesFile) {
		this.zhFacilitiesFile = zhFacilitiesFile;
	}
	public TripFilter getFilter() {
		return this.filter;
	}
	public void setFilter(TripFilter filter) {
		this.filter = filter;
	}
	public String getShapeFile() {
		return shapeFile;
	}
	public void setShapeFile(String shapeFile) {
		this.shapeFile = shapeFile;
	}
	public SampleDrawer getSampleDrawer() {
		return sampleDrawer;
	}
	public void setSampleDrawer(SampleDrawer sampleDrawer) {
		this.sampleDrawer = sampleDrawer;
	}
	public boolean isSetup() {
		return isSetup;
	}
	public void setSetup(boolean isSetup) {
		this.isSetup = isSetup;
	}
}


/* unused:
 * 
 * private void extractWalkChoiceSetsEllipse(List<ChoiceSet> choiceSets) {	
		// create a list of all zhFacilities to give to ExtractWalkChoiceSets()
		List<ZHFacility> zhFacilities = new Vector<ZHFacility>();		
		Iterator<ArrayList<ZHFacility>> zhFacilitiesList_it = this.zhFacilitiesByLink.values().iterator();
		while (zhFacilitiesList_it.hasNext()) {
			List<ZHFacility> list = zhFacilitiesList_it.next();
			zhFacilities.addAll(list);
		}		
		ExtractWalkChoiceSetsEllipse extractor = new ExtractWalkChoiceSetsEllipse(
				this.controler, zhFacilities, this.walkingSpeed, choiceSets);
		extractor.run();
	}
 * 
 */


