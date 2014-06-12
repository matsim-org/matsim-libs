package herbie.running.analysis;

import herbie.running.controler.listeners.CalcLegTimesHerbieListener;
import herbie.running.population.algorithms.AbstractClassifiedFrequencyAnalysis.CrosstabFormat;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.scenario.ScenarioUtils;

import java.io.FileNotFoundException;
import java.io.PrintStream;

public class AnalyseEventFile {
	
	private String eventFilePath;
	private CalcLegTimesKTI standardHandler;
	private PrintStream out;
	private Population pop;
	private String configFile = "P:/Projekte/herbie/output/Report/AdditionalAnalysis/configForAnalysis.xml";
	private String outputfolder;
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		if(args.length != 2){
			System.out.println("Please enter path for the event file, optionally zipped (ending .gz), " +
					"and the path for the output folder.");
		}
		else{
			AnalyseEventFile runAnalysis = new AnalyseEventFile();
			runAnalysis.run(args);
		}
	}

	private void run(String[] args) {
		
		readArguments(args);
		
		readPopulatin();
		
		for(int i = 0; i < 24; i++){
			
			double j = (double)i * 3600d + 3600d;
			if(i == 23) j = Double.MAX_VALUE;
			
			readEventFile((double)i * 3600d, j);
			analyseEventFile();
		}
		
		close();
	}

	private void close() {
		
		out.close();
		
		System.out.println("AnalyseEventFile.java finished.");
	}

	private void readPopulatin() {
		
		System.out.println("Starts reading Plans file...");
		
		Config config = ConfigUtils.loadConfig(configFile);
		Scenario scenario = ScenarioUtils.createScenario(config);

		pop = scenario.getPopulation();
		new MatsimPopulationReader(scenario).readFile(config.plans().getInputFile());
		
	}

	private void analyseEventFile() {
		
		this.standardHandler.printClasses(CrosstabFormat.ABSOLUTE, false, CalcLegTimesHerbieListener.timeBins, out);
		
		System.out.println(standardHandler.getAverageOverallTripDuration());
	}

	private void readEventFile(double startTime, double endTime) {
		
		System.out.println("Starts reading Events file...");
		
		EventsManager eventsManager = EventsUtils.createEventsManager();
		
		this.standardHandler = new CalcLegTimesKTI(pop, out);
		this.standardHandler.setTimeWindow(startTime, endTime);
		
		eventsManager.addHandler(standardHandler);
		
		MatsimEventsReader reader = new MatsimEventsReader(eventsManager);
		
		reader.readFile(this.eventFilePath);

		System.out.println("Events and plans file read!");
	}

	private void readArguments(String[] args) {
		
		this.eventFilePath = args[0];
		this.outputfolder = args[1];
		
		out = null;
		try {
			out = new PrintStream(outputfolder + "detailedTimeAnalysis.txt");
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}
}
