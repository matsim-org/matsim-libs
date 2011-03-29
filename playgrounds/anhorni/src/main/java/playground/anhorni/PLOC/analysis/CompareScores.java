package playground.anhorni.PLOC.analysis;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.List;
import java.util.Vector;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;

public class CompareScores {

	private List<Double> averageScores = new Vector<Double>();

	public void handleScenario(Scenario scenario) {
		double totalScore = 0.0;
		int numberOfEvaluatedPlans = 0;
		for (Person person : scenario.getPopulation().getPersons().values()) {
			Plan bestPlan = this.getBestPlan(person);
			totalScore += bestPlan.getScore();
			numberOfEvaluatedPlans++;
		}
		averageScores.add(totalScore / numberOfEvaluatedPlans);
	}
	
	private Plan getBestPlan(Person person) {
		double highestScore = Double.MIN_VALUE;
		Plan bestPlan = person.getSelectedPlan();
		
		for (Plan plan : person.getPlans()) {
			if (plan.getScore() > highestScore) bestPlan = plan;
		}
		return bestPlan;
 	}
	
	private void printAverageScore(String outpath) {
		DecimalFormat formatter = new DecimalFormat("0.000");
		try {
			BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(outpath + "averageScores.txt")); 
			for (Double score : this.averageScores) {
				bufferedWriter.write(formatter.format(score) + "\n");
			}
			bufferedWriter.newLine();
			
		    bufferedWriter.flush();
		    bufferedWriter.close();
		} 
		catch (IOException e) {
			e.printStackTrace();
			}
    }    
	
	public void printStatistics(String outpath) {
		this.printAverageScore(outpath);
	}	
}
