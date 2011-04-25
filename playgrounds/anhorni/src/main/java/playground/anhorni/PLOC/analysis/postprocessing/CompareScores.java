package playground.anhorni.PLOC.analysis.postprocessing;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.List;
import java.util.TreeMap;
import java.util.Vector;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.basic.v01.IdImpl;

public class CompareScores {
	 
	private List<Double> averageScores = new Vector<Double>();
	private BufferedWriter bufferedWriterBestScores;
	private String outpath;
		
	public CompareScores(String outpath) {
		this.outpath = outpath;
	}
	
	public void handleScenario(Scenario scenario) {		
		double totalScore = 0.0;
		int numberOfEvaluatedPlans = 0;
		List<Double> scoresPerAgent = new Vector<Double>();
		List<Id> agentIds = new Vector<Id>();
		for (Person person : scenario.getPopulation().getPersons().values()) {
			Plan bestPlan = CompareScenarios.getBestPlan(person);
			totalScore += bestPlan.getScore();
			numberOfEvaluatedPlans++;
			scoresPerAgent.add(bestPlan.getScore());
			agentIds.add(person.getId());
		}
		this.averageScores.add(totalScore / numberOfEvaluatedPlans);
		this.append2ScoresFile(scoresPerAgent, agentIds);
	}
	
	private void append2ScoresFile(List<Double> scoresPerAgent, List<Id> agentIds) {
		DecimalFormat formatter = new DecimalFormat("0.00000000000000");
		for (int i = 0; i < scoresPerAgent.size(); i++) {
			try {
				bufferedWriterBestScores.write(agentIds.get(i).toString() + "\t" + formatter.format(scoresPerAgent.get(i)) +  "\t");
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		try {
			bufferedWriterBestScores.newLine();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
		
	private void printAverageScore() {
		DecimalFormat formatter = new DecimalFormat("0.00000000000000");
		try {
			BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(outpath + "/averageBestScores.txt")); 
			bufferedWriter.write("sim_run" + "\t" + "average_best_score" + "\n"); 
			int cnt = 0;
			for (Double score : averageScores) {
				bufferedWriter.write(cnt + "\t" + formatter.format(score) + "\n");
				cnt++;
			}
			bufferedWriter.newLine();
			
		    bufferedWriter.flush();
		    bufferedWriter.close();
		} 
		catch (IOException e) {
			e.printStackTrace();
		}
    }
	
	public void printScores() {
		this.printAverageScore();
	}
	
	public void openScoresFile(String bestScoresFile) {
		try {
			this.bufferedWriterBestScores = new BufferedWriter(new FileWriter(bestScoresFile));
			this.bufferedWriterBestScores.write("Agent_id" + "\t" + "score" + "\n");
		} 
		catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void closeScoresFile() {
		try {
			this.bufferedWriterBestScores.close(); 
		} 
		catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private void printBestScoresStatistics(TreeMap<Id, AgentsScores> agentsScores, String outfile) {
		try {
			BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(outfile)); 
			bufferedWriter.write("Agent_Id" + "\t" + "relativeStandardDeviation [%]" + "\n");
			for (AgentsScores scores : agentsScores.values()) {
				double relativeStandardDeviationinPercent = 100.0 * scores.getStandardDeviationScore() / Math.abs(scores.getAverageScore());
				bufferedWriter.write(scores.getAgentId() + "\t" + relativeStandardDeviationinPercent + "\n");
			}
			bufferedWriter.newLine();
			
		    bufferedWriter.flush();
		    bufferedWriter.close();
		} 
		catch (IOException e) {
			e.printStackTrace();
		}
		
	}
	
	public void compareBestScores(String bestScoresFile, String outfile) {
		TreeMap<Id, AgentsScores> agentsScores = new TreeMap<Id, AgentsScores>();	
		try {
	          BufferedReader bufferedReader = new BufferedReader(new FileReader(bestScoresFile));
	          String line = bufferedReader.readLine(); // skip header	          
	          while ((line = bufferedReader.readLine()) != null) {
	        	  String parts[] = line.split("\t");
	        	  for (int i = 0; i < parts.length; i+=2) {
	        		  Id agentId = new IdImpl(parts[i]);
	        		  double score = Double.parseDouble(parts[i + 1]);
	        		  if (agentsScores.get(agentId) == null) agentsScores.put(agentId, new AgentsScores(agentId));
	        		  agentsScores.get(agentId).addScore(score);
	        	  }
	          }
	  } // end try
      catch (IOException e) {
    	  e.printStackTrace();
      }
      this.printBestScoresStatistics(agentsScores, outfile);
	}
}
