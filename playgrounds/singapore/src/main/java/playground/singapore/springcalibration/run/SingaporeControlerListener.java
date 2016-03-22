package playground.singapore.springcalibration.run;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.controler.MatsimServices;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.replanning.strategies.SelectBestPlanStrategyProvider;
import org.matsim.core.router.MainModeIdentifierImpl;
import org.matsim.core.router.StageActivityTypesImpl;
import org.matsim.pt.PtConstants;

import playground.singapore.springcalibration.run.SingaporeDistributions.DistributionClass;

public class SingaporeControlerListener implements StartupListener {
	
	private final static Logger log = Logger.getLogger(SingaporeControlerListener.class);
	private String path = "/cluster/scratch/fouriep/calibration/input/20160225_0/validation/";
	private Population population;
	public static String [] activities = {"home", "work", "leisure", "pudo", "personal", "primaryschool", "secondaryschool", "tertiaryschool", "foreignschool"};
	public static String [] modes = {"car", "pt", "walk", "passenger", "taxi"}; // "other" removed

	@Override
	public void notifyStartup(StartupEvent event) {
		MatsimServices controler = event.getServices();
		this.population = controler.getScenario().getPopulation();
		
//		controler.getStrategyManager().addStrategy(
//				new SelectBestPlanStrategyProvider().get(), "low", 0.2);
		
		this.init(controler, event);
	}
	
	public void init(MatsimServices controler, StartupEvent event) {
		CountsControlerListenerSingapore countsListener = new CountsControlerListenerSingapore();
		controler.addControlerListener(countsListener);
		countsListener.notifyStartup(event);
		
		this.addDurationAnalyzers(controler);
		this.addDistanceAnalyzers(controler);
	}
	
	private void addDurationAnalyzers(MatsimServices controler) {
		SingaporeDistributions timeDistribution = new SingaporeDistributions(this.population, 
				new MainModeIdentifierImpl(), 
				new StageActivityTypesImpl(PtConstants.TRANSIT_ACTIVITY_TYPE),
				"time");
		controler.addControlerListener(timeDistribution);

		for (String activity_to: activities) {
			for (String mode : modes) {
				
				ArrayList<Double> times; 
				if (mode.equals("passenger")) times = this.readFile(path + "/time_" + "pass" + "-" + activity_to + ".txt");
				else times = this.readFile(path + "/time_" + mode + "-" + activity_to + ".txt");
				
				
				DistributionClass distanceDistributionClass = timeDistribution.createAndAddDistributionClass(mode + "-" + activity_to);
				timeDistribution.addMainMode(distanceDistributionClass, mode);
				for (String activity_from: activities) {							
					timeDistribution.addActivityCombination(distanceDistributionClass, activity_from, activity_to);								
				}
				timeDistribution.createAndAddBin(distanceDistributionClass, 0.0, 1.0, times.get(0));
				timeDistribution.createAndAddBin(distanceDistributionClass, 1.0, 5.0, times.get(1));
				timeDistribution.createAndAddBin(distanceDistributionClass, 5.0, 10.0, times.get(2));
				timeDistribution.createAndAddBin(distanceDistributionClass, 10.0, 20.0, times.get(3));
				timeDistribution.createAndAddBin(distanceDistributionClass, 20.0, 30.0, times.get(4));
				timeDistribution.createAndAddBin(distanceDistributionClass, 30.0, 45.0, times.get(5));
				timeDistribution.createAndAddBin(distanceDistributionClass, 45.0, 60.0, times.get(6));
				timeDistribution.createAndAddBin(distanceDistributionClass, 60.0, 90.0, times.get(7));
				timeDistribution.createAndAddBin(distanceDistributionClass, 90.0, 120.0, times.get(8));
				timeDistribution.createAndAddBin(distanceDistributionClass, 120.0, Double.MAX_VALUE, times.get(9));
			}
		}
	}
	
	private void addDistanceAnalyzers(MatsimServices controler) {		
		SingaporeDistributions distanceDistribution = new SingaporeDistributions(this.population, 
				new MainModeIdentifierImpl(), 
				new StageActivityTypesImpl(PtConstants.TRANSIT_ACTIVITY_TYPE),
				"distance");
		controler.addControlerListener(distanceDistribution);

		for (String activity_to: activities) {
			for (String mode : modes) {
				
				
				ArrayList<Double> distances = this.readFile(path + "/distance_" + mode + "-" + activity_to + ".txt");
				
				DistributionClass distanceDistributionClass = distanceDistribution.createAndAddDistributionClass(mode + "-" + activity_to);
				distanceDistribution.addMainMode(distanceDistributionClass, mode);
				for (String activity_from: activities) {							
					distanceDistribution.addActivityCombination(distanceDistributionClass, activity_from, activity_to);								
				}
				distanceDistribution.createAndAddBin(distanceDistributionClass, 0.0, 500.0, distances.get(0));
				distanceDistribution.createAndAddBin(distanceDistributionClass, 500.0, 1000.0, distances.get(1));
				distanceDistribution.createAndAddBin(distanceDistributionClass, 1000.0, 2000.0, distances.get(2));
				distanceDistribution.createAndAddBin(distanceDistributionClass, 2000.0, 5000.0, distances.get(3));
				distanceDistribution.createAndAddBin(distanceDistributionClass, 5000.0, 10000.0, distances.get(4));
				distanceDistribution.createAndAddBin(distanceDistributionClass, 10000.0, 20000.0, distances.get(5));
				distanceDistribution.createAndAddBin(distanceDistributionClass, 20000.0, 50000.0, distances.get(6));
				distanceDistribution.createAndAddBin(distanceDistributionClass, 50000.0, 100000.0, distances.get(7));
				distanceDistribution.createAndAddBin(distanceDistributionClass, 100000.0, Double.MAX_VALUE, distances.get(8));
			}
		}
	}
	
	private ArrayList<Double> readFile(String inputFile) {
		File file = new File(inputFile);
	    List<String> lines;
	    ArrayList<Double> distances = new ArrayList<Double>();
		try {
			lines = Files.readAllLines(file.toPath(), StandardCharsets.UTF_8);
			System.out.println(lines.size() + " lines");
			
			int cnt = 0;
			for (String line : lines) {
				if (cnt == 0) {
					cnt++;
					continue; // skip header
				}
		        String[] elements = line.split("\t");
		        
		        distances.add(Double.parseDouble(elements[2]));
		    }
		} catch (IOException e) {
			for (int i = 0; i < 9; i++) {
				distances.add(0.0);
			}
		}	
		return distances;
	}

}
