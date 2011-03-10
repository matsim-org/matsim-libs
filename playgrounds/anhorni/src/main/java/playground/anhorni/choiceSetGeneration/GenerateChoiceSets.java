/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

package playground.anhorni.choiceSetGeneration;


import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.Config;
import org.matsim.core.controler.Controler;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.ConfigUtils;

import playground.anhorni.choiceSetGeneration.choicesetextractors.ExtractChoiceSetsRouting;
import playground.anhorni.choiceSetGeneration.filters.ActTypeAndAreaTripFilter;
import playground.anhorni.choiceSetGeneration.filters.SampleDrawer;
import playground.anhorni.choiceSetGeneration.filters.SampleDrawerFixedSizeRandom;
import playground.anhorni.choiceSetGeneration.filters.SampleDrawerFixedSizeTravelCosts;
import playground.anhorni.choiceSetGeneration.filters.TripFilter;
import playground.anhorni.choiceSetGeneration.helper.ChoiceSets;
import playground.anhorni.choiceSetGeneration.helper.ZHFacilities;
import playground.anhorni.choiceSetGeneration.io.CSShapeFileWriter;
import playground.anhorni.choiceSetGeneration.io.CSWriter;
import playground.anhorni.choiceSetGeneration.io.ChoiceSetWriterSimple;
import playground.anhorni.choiceSetGeneration.io.CompareTrips;
import playground.anhorni.choiceSetGeneration.io.NelsonTripReader;
import playground.anhorni.choiceSetGeneration.io.NelsonTripWriter;
import playground.anhorni.choiceSetGeneration.io.TripStats;
import playground.anhorni.choiceSetGeneration.io.ZHFacilitiesReader;
import playground.anhorni.choiceSetGeneration.io.ZHFacilitiesWriter;
import playground.balmermi.mz.PlansCreateFromMZ;

public class GenerateChoiceSets {

	//private static int idOffset = 20000000;
	//private final static double epsilon = 0.01;
	private Scenario scenario = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
	private Config config = null;
	private Population choiceSetPopulation = scenario.getPopulation();

	private final NetworkImpl network = (NetworkImpl) scenario.getNetwork();
	//private TreeMap<Id, ArrayList<ZHFacility>> zhFacilitiesByLink = new TreeMap<Id, ArrayList<ZHFacility>>();

	private ZHFacilities zhFacilities;

	/*
	private List<ChoiceSet> carChoiceSets = null;
	private List<ChoiceSet> walkChoiceSets = null;
	*/

	private final ChoiceSets choiceSets = new ChoiceSets();


	private final List<CSWriter> writers = new Vector<CSWriter>();
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
	private String reported;

	private final static Logger log = Logger.getLogger(GenerateChoiceSets.class);

	public static void main(final String[] args) {

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

	private void readInputFile(final String inputFile) {
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
			this.reported = bufferedReader.readLine();

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
			log.info("reported : " + this.reported);

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
		boolean folderGenerated = new File(this.outdir +"shapefiles").mkdir();
		folderGenerated = folderGenerated && new File(this.outdir +"shapefiles/singletrips").mkdir();
		folderGenerated = folderGenerated && new File(this.outdir +"shapefiles/singlechoicesets").mkdir();
		folderGenerated = folderGenerated && new File(this.outdir +"choicesets").mkdir();

		if (!folderGenerated) {
			log.info("Problem while generating output folders");
		}

		ChoiceSetWriterSimple writer = new ChoiceSetWriterSimple(this.zhFacilities);
		this.writers.add(writer);
		CSShapeFileWriter shpWriter = new CSShapeFileWriter();
		this.writers.add(shpWriter);

		TripStats tripStats = new TripStats(this.mode);
		this.writers.add(tripStats);



		// sampler
		SampleDrawer sampleDrawer;
		if (this.sampling.equals("random")) {
			sampleDrawer = new SampleDrawerFixedSizeRandom(this.choiceSetSize);
		}
		else {
			sampleDrawer = new SampleDrawerFixedSizeTravelCosts(this.choiceSetSize, true);
		}
		this.setSampleDrawer(sampleDrawer);
	}

	public void run() {

		this.config = ConfigUtils.loadConfig(this.matsimRunConfigFile);

		this.createChoiceSetFacilities();

		if (this.readNelson.equals("true")) {
			this.choiceSets.setCarChoiceSets(new NelsonTripReader(this.network, this.zhFacilities)
					.readFiles("input/MZ2005_Wege.dat", "input/810Trips.dat", "car"));
			this.choiceSets.setWalkChoiceSets(new NelsonTripReader(this.network, this.zhFacilities)
					.readFiles("input/MZ2005_Wege.dat", "input/810Trips.dat", "walk"));
		}
		else {
			this.choiceSetPopulation = this.createChoiceSetPopulationFromMZ();
			new PopulationWriter(this.choiceSetPopulation, this.network).write(this.outdir+"/MZPopulation.txt");

			// TODO: maybe also optimize area?
			this.choiceSets.setCarChoiceSets(this.filter.apply(this.choiceSetPopulation, "car"));
			this.choiceSets.setWalkChoiceSets(this.filter.apply(this.choiceSetPopulation, "walk"));
		}

		if (this.DEQSim.equals("true")) {
//			String [] args = {this.matsimRunConfigFile};
//			this.controler = new DEQSimControler(args);
			throw new RuntimeException("Support for C++ DEQSim has been removed.");
		}
		else {
			this.controler = new Controler(this.config);
		}

		int tt;
		if (this.reported.equals("reported")) {
			tt = 1;
		}
		else {
			tt = 2;
		}

		ExtractChoiceSetsRouting listenerCar = new ExtractChoiceSetsRouting(this.controler, this.zhFacilities,
				this.choiceSets.getCarChoiceSets(), "car", tt);

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
					this.choiceSets.getWalkChoiceSets(), "walk", tt);

		if (this.mode.equals("car")) {
			this.controler.addControlerListener(listenerCar);
		}
		else if (this.mode.equals("walk")) {
			this.controler.addControlerListener(listenerWalk);
		}
		else {
			log.error("No mode chosen");
		}
		log.info("Running controler: ...");
		this.controler.run();

		// sample the choice sets: not used at the moment:
		/*
		CSShapeFileWriter shpWriter = new CSShapeFileWriter(this.outdir);
		shpWriter.writeChoiceSets(this.outdir, "carBeforeSampling", this.carChoiceSets);
		shpWriter.writeChoiceSets(this.outdir, "walkBeforeSampling", this.walkChoiceSets);

		log.info("Sampling: ...");
		this.drawSample();
		*/

		log.info("Output: ...");
		this.output();
	}

	private Population createChoiceSetPopulationFromMZ() {

		Population temporaryPopulation = ((ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig())).getPopulation();

		try {
			new PlansCreateFromMZ(this.choiceSetPopulationFile,this.outdir+"/output_wegeketten.dat",1,7).run(temporaryPopulation);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return temporaryPopulation;
	}

	private void createChoiceSetFacilities() {
		MatsimNetworkReader networkReader = new MatsimNetworkReader(this.scenario);
		networkReader.readFile(this.config.network().getInputFile());

		ZHFacilitiesReader zhFacilitiesReader = new ZHFacilitiesReader(this.network);
		zhFacilitiesReader.readFile(this.zhFacilitiesFile, this.zhFacilities);

		new ZHFacilitiesWriter().write(this.outdir, this.zhFacilities);
	}

	private void output() {

		this.choiceSets.finish();
		this.zhFacilities.finish();

		Iterator<CSWriter> writer_it = this.writers.iterator();
		while (writer_it.hasNext()) {
			CSWriter writer = writer_it.next();
			if (this.mode.equals("car")) {
				writer.write(this.outdir, "car", this.choiceSets.getCarChoiceSets());
			}
			else if (this.mode.equals("walk")) {
				writer.write(this.outdir, "walk", this.choiceSets.getWalkChoiceSets());
			}
		}

		NelsonTripWriter nelsonWriter = new NelsonTripWriter();
		nelsonWriter.write(this.outdir, "car",  this.choiceSets.getCarChoiceSets());
		nelsonWriter.write(this.outdir, "walk", this.choiceSets.getWalkChoiceSets());


		CompareTrips compareTripsCar = new CompareTrips(this.outdir, "car");
		compareTripsCar.compare("input/ttbcar.dat", this.choiceSets.getCarChoiceSets());

		CompareTrips compareTripsWalk = new CompareTrips(this.outdir, "walk");
		compareTripsWalk.compare("input/ttbwalk.dat", this.choiceSets.getWalkChoiceSets());

	}

	/* not used at the moment
	private void drawSample() {
		if (this.sampleDrawer != null) {
			sampleDrawer.drawSample(this.choiceSets.getCarChoiceSets());
			sampleDrawer.drawSample(this.choiceSets.getWalkChoiceSets());
		}
	}
	*/



	// getters and setters: --------------------------------------------------------------------------------
	public String getSpssfile() {
		return this.choiceSetPopulationFile;
	}
	public void setSpssfile(final String spssfile) {
		this.choiceSetPopulationFile = spssfile;
	}
	public String getOutdir() {
		return this.outdir;
	}
	public void setOutdir(final String outdir) {
		this.outdir = outdir;
	}
	public String getConfigFile() {
		return this.matsimRunConfigFile;
	}
	public void setConfigFile(final String configfile) {
		this.matsimRunConfigFile = configfile;
	}
	public String getZhFacilitiesFile() {
		return this.zhFacilitiesFile;
	}
	public void setZhFacilitiesFile(final String zhFacilitiesFile) {
		this.zhFacilitiesFile = zhFacilitiesFile;
	}
	public TripFilter getFilter() {
		return this.filter;
	}
	public void setFilter(final TripFilter filter) {
		this.filter = filter;
	}
	public String getShapeFile() {
		return this.shapeFile;
	}
	public void setShapeFile(final String shapeFile) {
		this.shapeFile = shapeFile;
	}
	public SampleDrawer getSampleDrawer() {
		return this.sampleDrawer;
	}
	public void setSampleDrawer(final SampleDrawer sampleDrawer) {
		this.sampleDrawer = sampleDrawer;
	}
	public boolean isSetup() {
		return this.isSetup;
	}
	public void setSetup(final boolean isSetup) {
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


