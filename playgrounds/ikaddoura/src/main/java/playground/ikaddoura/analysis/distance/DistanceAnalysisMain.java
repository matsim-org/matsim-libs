package playground.ikaddoura.analysis.distance;

import java.util.SortedMap;
import java.util.TreeMap;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.Config;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.config.ConfigUtils;

public class DistanceAnalysisMain {

	String netFile1 = "/Users/ihab/Documents/workspace/runs-svn/berlin_internalizationCar/output/baseCase_2/output_network.xml.gz";
	String plansFile1 = "/Users/ihab/Documents/workspace/runs-svn/berlin_internalizationCar/output/baseCase_2/output_plans.xml.gz";
	String outputFile1 = "/Users/ihab/Desktop/analysis_baseCase_2.txt";
	String outputFile_XYLineChart1 = "/Users/ihab/Desktop/analysis_baseCase_2_a.png";
	String outputFile_LineChart1 = "/Users/ihab/Desktop/analysis_baseCase_2_b.png";
	String outputFile_BarChart1 = "/Users/ihab/Desktop/analysis_baseCase_2_c.png";

	String netFile2 = "/Users/ihab/Documents/workspace/runs-svn/berlin_internalizationCar/output/internalization_2/output_network.xml.gz";
	String plansFile2 = "/Users/ihab/Documents/workspace/runs-svn/berlin_internalizationCar/output/internalization_2/output_plans.xml.gz";
	String outputFile2 = "/Users/ihab/Desktop/analysis_internalization_2.txt";
	String outputFile_XYLineChart2 = "/Users/ihab/Desktop/analysis_internalization_2_a.png";
	String outputFile_LineChart2 = "/Users/ihab/Desktop/analysis_internalization_2_b.png";
	String outputFile_BarChart2 = "/Users/ihab/Desktop/analysis_internalization_2_c.png";
	
	double basis = 10; 
		
	public static void main(String[] args) {
		DistanceAnalysisMain analyse = new DistanceAnalysisMain();
		analyse.run();
		}
	
	public void run() {
		
		Population population1 = getPopulation(netFile1, plansFile1);
		Population population2 = getPopulation(netFile2, plansFile2);

		SortedMap<String,ModusDistance> modiMap1 = getModiMap(population1);
		SortedMap<String,ModusDistance> modiMap2 = getModiMap(population2);
		
		double maxDistance1 = getMaximalDistanceAllModes(modiMap1);
		double maxDistance2 = getMaximalDistanceAllModes(modiMap2);
		double maxDistance = getMaximalDistanceBothPlanFiles(maxDistance1, maxDistance2);
		
		setLegsPerDistanceGroups(modiMap1, maxDistance);
		setLegsPerDistanceGroups(modiMap2, maxDistance);
		
		SortedMap<Double, Line> resultMap1 = putTogether(modiMap1);
		SortedMap<Double, Line> resultMap2 = putTogether(modiMap2);
		
		TextFileWriter writer = new TextFileWriter();
		writer.writeFile(plansFile1, outputFile1, resultMap1);
		writer.writeFile(plansFile2, outputFile2, resultMap2);
		
		ChartFileWriterDistance chartWriter = new ChartFileWriterDistance();
		chartWriter.writeXYLineChartFile("base case", modiMap1, outputFile_XYLineChart1);
		chartWriter.writeXYLineChartFile("policy case", modiMap2, outputFile_XYLineChart2);
		chartWriter.writeBarChartFile("base case", modiMap1, outputFile_BarChart1);
		chartWriter.writeBarChartFile("policy case", modiMap2, outputFile_BarChart2);
		chartWriter.writeLineChartFile("base case", modiMap1, outputFile_LineChart1);
		chartWriter.writeLineChartFile("policy case", modiMap2, outputFile_LineChart2);
	}

//------------------------------------------------------------------------------------------------------------------------
	
	private Population getPopulation(String netFile, String plansFile){
		
		Config config = ConfigUtils.createConfig();
		config.network().setInputFile(netFile);
		config.plans().setInputFile(plansFile);
		Scenario scenario = ScenarioUtils.loadScenario(config);
		Population population = scenario.getPopulation();
		return population;
	}
	
	private double getMaximalDistanceAllModes(SortedMap<String, ModusDistance> modiMap) {
		double maximalDistanceAllModes = 0.0;
		for (String modus : modiMap.keySet()){
			if (maximalDistanceAllModes > modiMap.get(modus).getMaximalDistance()){}
			else {
				maximalDistanceAllModes = modiMap.get(modus).getMaximalDistance();
			}
		}
		return maximalDistanceAllModes;
	}
	
	private double getMaximalDistanceBothPlanFiles(double maxDistance1, double maxDistance2){
		double maxDistance = 0.0;
		if (maxDistance1 > maxDistance2){
			maxDistance = maxDistance1;
		}
		if (maxDistance1 < maxDistance2){
			maxDistance = maxDistance2;
		}
		else {
			maxDistance = maxDistance1;
		}
		return maxDistance;
	}
	
	private SortedMap<String, ModusDistance> getModiMap(Population population) {
		SortedMap<String,ModusDistance> modiMap = new TreeMap<String, ModusDistance>();
		for(Person person : population.getPersons().values()){
			for (PlanElement pE : person.getSelectedPlan().getPlanElements()){
				if (pE instanceof Leg){
					Leg leg = (Leg) pE;
					String mode = leg.getMode();
					if (modiMap.containsKey(mode)){
						//nothing
					}
					else {
						ModusDistance modus = new ModusDistance(mode);
						modus.setDistances(population);
						modiMap.put(mode,modus);
					}
				}
			}
		}
		return modiMap;
	}
	
	private void setLegsPerDistanceGroups(SortedMap<String, ModusDistance> modiMap, double maxDistance) {
		for (ModusDistance modus : modiMap.values()){
			modus.setLegsPerDistanceGroups(basis, maxDistance);
		}
	}
	
	private SortedMap<Double, Line> putTogether(SortedMap<String, ModusDistance> modiMap) {
		
		SortedMap<Double, Line> resultMap = new TreeMap<Double, Line>();
		ModusDistance carModus = modiMap.get("car");
		SortedMap<Double, Integer> carLegsPerDistanceGroups = carModus.getLegsPerDistanceGroups(); // zum Durchiterieren
		for (Double distanceGroup : carLegsPerDistanceGroups.keySet()){
			Line line = new Line(distanceGroup);
			for (ModusDistance modus : modiMap.values()){
				String modeName = modus.getModeName();
				line.setLegs(modeName, modus.getLegsPerDistanceGroups().get(distanceGroup));
				resultMap.put(distanceGroup, line);
			}
		}
		return resultMap;		
	}

}
