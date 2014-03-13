package playground.wrashid.bsc.vbmh.vm_parking;

import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.scoring.SumScoringFunction.BasicScoring;

/**
 * Gets the parking related score (which is saved in money so far) out of the agents vm_scorekeeper and calculates 
 * a score out of if. 
 * 
 * 
 *
 * @author Valentin Bemetz & Moritz Hohenfellner
 *
 */


public class Park_Scoring implements BasicScoring  {
	double score =0;
	Plan plan = null;
	double beta_money=-0.1; //!! Kommt auch in Park_Control vor >> In config auslagern?
	VM_Score_Keeper scorekeeper = null;
	public Park_Scoring(Plan plan) {
		super();
		this.plan = plan;
		// TODO Auto-generated constructor stub
	}

	@Override
	public void finish() {
		// TODO Auto-generated method stub
		if(this.plan.getPerson().getCustomAttributes().get("VM_Score_Keeper")!=null){
			scorekeeper = (VM_Score_Keeper) this.plan.getPerson().getCustomAttributes().get("VM_Score_Keeper");
			this.score=beta_money*scorekeeper.get_score();
			System.out.println("Score Keeper geladen");
		}
		System.out.println("Park Scoring "+Double.toString(this.score));
	}

	@Override
	public double getScore() {
		// TODO Auto-generated method stub
		return this.score;
	}

}
