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
	 
	private List<Double> averageBestScores = new Vector<Double>();
	private List<Double> averageExecutedScores = new Vector<Double>();
	private BufferedWriter bufferedWriterScores;
	private String outpath;
		
	public CompareScores(String outpath) {
		this.outpath = outpath;
	}
	
	public void handleScenario(Scenario scenario) {		
		double totalBestScore = 0.0;
		double totalExecutedScore = 0.0;
		int numberOfEvaluatedPlans = 0;
		List<Double> bestScoresPerAgent = new Vector<Double>();
		List<Double> executedScoresPerAgent = new Vector<Double>();
		List<Id> agentIds = new Vector<Id>();
		for (Person person : scenario.getPopulation().getPersons().values()) {
			Plan bestPlan = CompareScenarios.getBestPlan(person);
			totalBestScore += bestPlan.getScore();
			totalExecutedScore += person.getSelectedPlan().getScore();
			numberOfEvaluatedPlans++;
			bestScoresPerAgent.add(bestPlan.getScore());
			executedScoresPerAgent.add(person.getSelectedPlan().getScore());
			agentIds.add(person.getId());
		}
		this.averageBestScores.add(totalBestScore / numberOfEvaluatedPlans);
		this.averageExecutedScores.add(totalExecutedScore / numberOfEvaluatedPlans);
		this.append2ScoresFile(bestScoresPerAgent, executedScoresPerAgent, agentIds);
	}
	
	private void append2ScoresFile(List<Double> bestScoresPerAgent, List<Double> executedScoresPerAgent, List<Id> agentIds) {
		DecimalFormat formatter = new DecimalFormat("0.00000000000000");
		for (int i = 0; i < bestScoresPerAgent.size(); i++) {
			try {
				this.bufferedWriterScores.write(agentIds.get(i).toString() + "\t" 
						+ formatter.format(bestScoresPerAgent.get(i)) +  "\t" 
						+ formatter.format(executedScoresPerAgent.get(i)) + "\t");
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		try {
			bufferedWriterScores.newLine();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
		
	private void printAverageScore() {
		DecimalFormat formatter = new DecimalFormat("0.00000000000000");
		try {
			BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(outpath + "/averageScores.txt")); 
			bufferedWriter.write("sim_run" + "\t" + "average_best_score" + "\t" + "average_executed_score" + "\n"); 
			int cnt = 0;
			for (Double score : this.averageBestScores) {
				bufferedWriter.write(cnt + "\t" + 
						formatter.format(score) + "\t" + 
						formatter.format(this.averageExecutedScores.get(cnt)) + 
						"\n");
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
			this.bufferedWriterScores = new BufferedWriter(new FileWriter(bestScoresFile));
			this.bufferedWriterScores.write("Agent_id" + "\t" + "bestScore" + "\t" + "executedScore" + "\n");
		} 
		catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void closeScoresFile() {
		try {
			this.bufferedWriterScores.close(); 
		} 
		catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private void printScoresStatistics(TreeMap<Id, AgentsScores> agentsBestScores, TreeMap<Id, AgentsScores> agentsExecutedScores, String outfile) {
		try {
			BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(outfile)); 
			bufferedWriter.write("Agent_Id" + "\t" + 
					"bestScore" + "\t" +
					"executedScore"
					+ "\n");
			
			for (AgentsScores bestScores : agentsBestScores.values()) {
				double bestScoreRelativeStandardDeviationinPercent = 
					100.0 * bestScores.getStandardDeviationScore() / Math.abs(bestScores.getAverageScore());
				
				double executedScoreRelativeStandardDeviationinPercent = 
					100.0 * agentsExecutedScores.get(bestScores.getAgentId()).getStandardDeviationScore() / 
					Math.abs(agentsExecutedScores.get(bestScores.getAgentId()).getAverageScore());
				
				bufferedWriter.write(bestScores.getAgentId() + "\t" + 
						bestScoreRelativeStandardDeviationinPercent + "\t" +  
						executedScoreRelativeStandardDeviationinPercent +
						"\n");
				
			}
			bufferedWriter.newLine();
			
		    bufferedWriter.flush();
		    bufferedWriter.close();
		} 
		catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void compareScores(String scoresFile, String outfile) {
		TreeMap<Id, AgentsScores> agentsBestScores = new TreeMap<Id, AgentsScores>();
		TreeMap<Id, AgentsScores> agentsExecutedScores = new TreeMap<Id, AgentsScores>();
		try {
	          BufferedReader bufferedReader = new BufferedReader(new FileReader(scoresFile));
	          String line = bufferedReader.readLine(); // skip header	          
	          while ((line = bufferedReader.readLine()) != null) {
	        	  String parts[] = line.split("\t");
	        	  for (int i = 0; i < parts.length; i+=3) {
	        		  Id agentId = new IdImpl(parts[i]);
	        		  double bestScore = Double.parseDouble(parts[i + 1]);
	        		  double executedScore = Double.parseDouble(parts[i + 2]);
	        		  if (agentsBestScores.get(agentId) == null) agentsBestScores.put(agentId, new AgentsScores(agentId));
	        		  if (agentsExecutedScores.get(agentId) == null) agentsExecutedScores.put(agentId, new AgentsScores(agentId));
	        		  agentsBestScores.get(agentId).addScore(bestScore);
	        		  agentsExecutedScores.get(agentId).addScore(executedScore);
	        	  }
	          }
	  } // end try
      catch (IOException e) {
    	  e.printStackTrace();
      }
      this.printScoresStatistics(agentsBestScores, agentsExecutedScores, outfile);
	}
}
