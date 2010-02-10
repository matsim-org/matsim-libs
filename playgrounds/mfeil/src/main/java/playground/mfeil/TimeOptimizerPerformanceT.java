package playground.mfeil;


import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;

import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.ControlerIO;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.router.PlansCalcRoute;
import org.matsim.core.router.costcalculators.FreespeedTravelTimeCost;
import org.matsim.core.router.util.AStarLandmarksFactory;
import org.matsim.core.scoring.PlanScorer;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.planomat.Planomat;
import org.matsim.planomat.costestimators.LegTravelTimeEstimatorFactory;
import org.matsim.population.algorithms.PlanAlgorithm;


public class TimeOptimizerPerformanceT implements org.matsim.population.algorithms.PlanAlgorithm {
	
	private final PlanAlgorithm 	timeOptAlgorithm;
	private final PlanScorer		scorer;
	private final PlansCalcRoute router;
  private ControlerIO controlerIO;
	
	public TimeOptimizerPerformanceT (Controler controler, LegTravelTimeEstimatorFactory estimatorFactory, PlanScorer scorer, ScoringFunctionFactory scoringFunctionFactory){

		//this.timeOptAlgorithm 		= new TimeOptimizer (controler, estimator, scorer);
		this.scorer			  		= scorer;
		FreespeedTravelTimeCost ttCost = new FreespeedTravelTimeCost(controler.getConfig().charyparNagelScoring());
		this.router 				= new PlansCalcRoute(controler.getConfig().plansCalcRoute(), controler.getNetwork(), controler.getTravelCostCalculator(), controler.getTravelTimeCalculator(), new AStarLandmarksFactory(controler.getNetwork(), ttCost));
		this.timeOptAlgorithm 	= new Planomat (estimatorFactory, scoringFunctionFactory, controler.getConfig().planomat(), this.router, controler.getNetwork());
		this.controlerIO = controler.getControlerIO();
	}
	
	public void run (Plan plan){
		
		if (plan.getPerson().getId().toString().equals("2")){
		
			String outputfile = this.controlerIO.getOutputFilename("TimeOptimizerTest.xls");
			PrintStream stream;
			try {
				stream = new PrintStream (new File(outputfile));
			} catch (FileNotFoundException e) {
				e.printStackTrace();
				return;
			}
			stream.print(plan.getScore()+"\t");
			for (int z= 0;z<plan.getPlanElements().size();z=z+2){
			ActivityImpl act = (ActivityImpl)plan.getPlanElements().get(z);
				stream.print(act.getType()+"\t");
			}
			stream.println();
			stream.print("\t");
			for (int z= 0;z<plan.getPlanElements().size();z=z+2){
				stream.print(((ActivityImpl)(plan.getPlanElements()).get(z)).getDuration()+"\t");
			}
			stream.println();
			
			// Routing
			this.router.run(plan);
			
			
			// Variation of plan
			PlanomatXPlan [] variation = new PlanomatXPlan [50];
			double [][] statistics 	 = new double [variation.length][2];
			for (int i=0;i<variation.length;i++){
				variation[i] = new PlanomatXPlan (plan.getPerson());
				variation[i].copyPlan(plan);
			}
			for (int i = 0;i<variation.length;i++){
				double time = 70000;
				((ActivityImpl)variation[i].getPlanElements().get(0)).setDuration(MatsimRandom.getRandom().nextDouble()*time);
				((ActivityImpl)variation[i].getPlanElements().get(0)).setEndTime(((ActivityImpl)variation[i].getPlanElements().get(0)).getDuration());
				time -=((ActivityImpl)variation[i].getPlanElements().get(0)).getDuration();
				for (int j=2; j<variation[i].getPlanElements().size()-2;j+=2){
					((ActivityImpl)variation[i].getPlanElements().get(j)).setStartTime(((ActivityImpl)variation[i].getPlanElements().get(j-2)).getEndTime());
					((ActivityImpl)variation[i].getPlanElements().get(j)).setDuration(MatsimRandom.getRandom().nextDouble()*time);
					((ActivityImpl)variation[i].getPlanElements().get(j)).setEndTime(((ActivityImpl)variation[i].getPlanElements().get(j)).getDuration()+((ActivityImpl)variation[i].getPlanElements().get(j)).getStartTime());
					time -= ((ActivityImpl)variation[i].getPlanElements().get(j)).getDuration();
				}
				((ActivityImpl)variation[i].getPlanElements().get(variation[i].getPlanElements().size()-1)).setStartTime(((ActivityImpl)variation[i].getPlanElements().get(variation[i].getPlanElements().size()-3)).getEndTime());
				((ActivityImpl)variation[i].getPlanElements().get(variation[i].getPlanElements().size()-1)).setDuration(86400-((ActivityImpl)variation[i].getPlanElements().get(variation[i].getPlanElements().size()-1)).getStartTime());
				
				stream.print("\t");
				for (int z= 0;z<plan.getPlanElements().size();z=z+2){
					stream.print(((ActivityImpl)(variation[i].getPlanElements()).get(z)).getDuration()+"\t");
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
				statistics[i][0] = variation[i].getScore().doubleValue();
				mean+=statistics[i][0];
				
				stream.print(variation[i].getScore()+"\t");
				for (int z= 0;z<plan.getPlanElements().size();z=z+2){
					stream.print(((ActivityImpl)(variation[i].getPlanElements()).get(z)).getDuration()+"\t");
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
}
