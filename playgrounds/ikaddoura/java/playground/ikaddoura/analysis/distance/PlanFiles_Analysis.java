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
import org.matsim.core.utils.misc.ConfigUtils;

public class PlanFiles_Analysis {

	String netFile1 = "input/output_network.xml";
	String plansFile1 = "input/0.plans.xml";
	String outputFile1 = "output_planFileAnalysis/analyse_it.0_Basis2.txt";
	String outputFile_XYLineChart1 = "output_planFileAnalysis/analyse_it.0_Basis2_xyLineChart.png";
	String outputFile_LineChart1 = "output_planFileAnalysis/analyse_it.0_Basis2_LineChart.png";
	String outputFile_BarChart1 = "output_planFileAnalysis/analyse_it.0_Basis2_BarChart.png";

	String netFile2 = "input/output_network.xml";
	String plansFile2 = "input/500.plans.xml";
	String outputFile2 = "output_planFileAnalysis/analyse_it.500_Basis2.txt";
	String outputFile_XYLineChart2 = "output_planFileAnalysis/analyse_it.500_Basis2_xyLineChart.png";
	String outputFile_LineChart2 = "output_planFileAnalysis/analyse_it.500_Basis2_LineChart.png";
	String outputFile_BarChart2 = "output_planFileAnalysis/analyse_it.500_Basis2_BarChart.png";
	
	double basis = 2; 
		
	public static void main(String[] args) {
		PlanFiles_Analysis analyse = new PlanFiles_Analysis();
		analyse.run();
		}
	
	public void run() {
		
		Population population1 = getPopulation(netFile1, plansFile1);
		Population population2 = getPopulation(netFile2, plansFile2);

		SortedMap<String,Modus> modiMap1 = getModiMap(population1);
		SortedMap<String,Modus> modiMap2 = getModiMap(population2);
		
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
		
		ChartFileWriter chartWriter = new ChartFileWriter();
		chartWriter.writeXYLineChartFile("PlanFile_Iteration_0", modiMap1, outputFile_XYLineChart1);
		chartWriter.writeXYLineChartFile("PlanFile_Iteration_500", modiMap2, outputFile_XYLineChart2);
		chartWriter.writeBarChartFile("PlanFile_Iteration_0", modiMap1, outputFile_BarChart1);
		chartWriter.writeBarChartFile("PlanFile_Iteration_500", modiMap2, outputFile_BarChart2);
		chartWriter.writeLineChartFile("PlanFile_Iteration_0", modiMap1, outputFile_LineChart1);
		chartWriter.writeLineChartFile("PlanFile_Iteration_500", modiMap2, outputFile_LineChart2);
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
	
	private double getMaximalDistanceAllModes(SortedMap<String, Modus> modiMap) {
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
	
	private SortedMap<String, Modus> getModiMap(Population population) {
		SortedMap<String,Modus> modiMap = new TreeMap<String, Modus>();
		for(Person person : population.getPersons().values()){
			for (PlanElement pE : person.getSelectedPlan().getPlanElements()){
				if (pE instanceof Leg){
					Leg leg = (Leg) pE;
					String mode = leg.getMode();
					if (modiMap.containsKey(mode)){
						//nothing
					}
					else {
						Modus modus = new Modus(mode);
						modus.setDistances(population);
						modiMap.put(mode,modus);
					}
				}
			}
		}
		return modiMap;
	}
	
	private void setLegsPerDistanceGroups(SortedMap<String, Modus> modiMap, double maxDistance) {
		for (Modus modus : modiMap.values()){
			modus.setLegsPerDistanceGroups(basis, maxDistance);
		}
	}
	
	private SortedMap<Double, Line> putTogether(SortedMap<String, Modus> modiMap) {
		
		SortedMap<Double, Line> resultMap = new TreeMap<Double, Line>();
		Modus carModus = modiMap.get("car");
		SortedMap<Double, Integer> carLegsPerDistanceGroups = carModus.getLegsPerDistanceGroups(); // zum Durchiterieren
		for (Double distanceGroup : carLegsPerDistanceGroups.keySet()){
			Line line = new Line(distanceGroup);
			for (Modus modus : modiMap.values()){
				String modeName = modus.getModeName();
				line.setLegs(modeName, modus.getLegsPerDistanceGroups().get(distanceGroup));
				resultMap.put(distanceGroup, line);
			}
		}
		return resultMap;		
	}

}
