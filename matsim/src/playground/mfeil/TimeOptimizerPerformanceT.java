package playground.mfeil;


import org.matsim.planomat.PlanOptimizeTimes;
import org.matsim.planomat.costestimators.LegTravelTimeEstimator;
import org.matsim.population.algorithms.PlanAlgorithm;
import org.matsim.scoring.*;
import org.matsim.population.Plan;
import org.matsim.controler.Controler;
import org.matsim.gbl.MatsimRandom;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import org.matsim.population.Act;


public class TimeOptimizerPerformanceT implements org.matsim.population.algorithms.PlanAlgorithm {
	
	private final PlanAlgorithm 	timeOptAlgorithm;
	private final PlanScorer		scorer;
	
	public TimeOptimizerPerformanceT (LegTravelTimeEstimator estimator, PlanScorer scorer, ScoringFunctionFactory factory){
		this.timeOptAlgorithm = new TimeOptimizer14 (estimator, scorer);
		//this.timeOptAlgorithm = new PlanOptimizeTimes (estimator, factory);
		this.scorer			  = scorer;
	}
	
	public void run (Plan plan){
		
		String outputfile = Controler.getOutputFilename("TimeOptimizerTest.xls");
		PrintStream stream;
		try {
			stream = new PrintStream (new File(outputfile));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return;
		}
		stream.print(plan.getScore()+"\t");
		for (int z= 0;z<plan.getActsLegs().size();z=z+2){
		Act act = (Act)plan.getActsLegs().get(z);
			stream.print(act.getType()+"\t");
		}
		stream.println();
		stream.print("\t");
		for (int z= 0;z<plan.getActsLegs().size();z=z+2){
			stream.print(((Act)(plan.getActsLegs()).get(z)).getDuration()+"\t");
		}
		stream.println();
		
		
		// Variation of plan
		PlanomatXPlan [] variation = new PlanomatXPlan [50];
		double [][] statistics 	 = new double [variation.length][2];
		for (int i=0;i<variation.length;i++){
			variation[i] = new PlanomatXPlan (plan.getPerson());
			variation[i].copyPlan(plan);
		}
		for (int i = 0;i<variation.length;i++){
			double time = 70000;
			((Act)variation[i].getActsLegs().get(0)).setDuration(MatsimRandom.random.nextDouble()*time);
			((Act)variation[i].getActsLegs().get(0)).setEndTime(((Act)variation[i].getActsLegs().get(0)).getDuration());
			time -=((Act)variation[i].getActsLegs().get(0)).getDuration();
			for (int j=2; j<variation[i].getActsLegs().size()-2;j+=2){
				((Act)variation[i].getActsLegs().get(j)).setStartTime(((Act)variation[i].getActsLegs().get(j-2)).getEndTime());
				((Act)variation[i].getActsLegs().get(j)).setDuration(MatsimRandom.random.nextDouble()*time);
				((Act)variation[i].getActsLegs().get(j)).setEndTime(((Act)variation[i].getActsLegs().get(j)).getDuration()+((Act)variation[i].getActsLegs().get(j)).getStartTime());
				time -= ((Act)variation[i].getActsLegs().get(j)).getDuration();
			}
			((Act)variation[i].getActsLegs().get(variation[i].getActsLegs().size()-1)).setStartTime(((Act)variation[i].getActsLegs().get(variation[i].getActsLegs().size()-3)).getEndTime());
			((Act)variation[i].getActsLegs().get(variation[i].getActsLegs().size()-1)).setDuration(86400-((Act)variation[i].getActsLegs().get(variation[i].getActsLegs().size()-1)).getStartTime());
			
			stream.print("\t");
			for (int z= 0;z<plan.getActsLegs().size();z=z+2){
				stream.print(((Act)(variation[i].getActsLegs()).get(z)).getDuration()+"\t");
			}
			stream.println();
		}
		
		stream.println();
		long average=0;
		double mean=0;
		for (int i = 0;i<variation.length;i++){
			long runtime = System.currentTimeMillis();
			timeOptAlgorithm.run(variation[i]);
			statistics[i][1] = System.currentTimeMillis()-runtime;
			average+=statistics[i][1];
			
			
			variation[i].setScore(scorer.getScore(variation[i]));
			statistics[i][0] = variation[i].getScore();
			mean+=statistics[i][0];
			
			stream.print(variation[i].getScore()+"\t");
			for (int z= 0;z<plan.getActsLegs().size();z=z+2){
				stream.print(((Act)(variation[i].getActsLegs()).get(z)).getDuration()+"\t");
			}
			stream.println(statistics[i][1]);
		}
		mean = mean/statistics.length;
		double varianz=0;
		for (int i=0;i<statistics.length;i++){
			//varianz += Math.exp(statistics[i][0]-mean);
			varianz += (statistics[i][0]-mean)*(statistics[i][0]-mean);
		}
		stream.println(mean+"\t\t\t\t\t\t"+average/statistics.length);
		stream.println(varianz/statistics.length);
	}
}
