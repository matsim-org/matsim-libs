package playground.anhorni.surprice;

import java.util.List;
import java.util.Vector;

import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.PlanImpl;

public class AgentMemory {
	
	private List<Plan> plans = new Vector<Plan>();
	
	private int nShoppingActs = 0;
	private int nLeisureActs = 0;
	private int nWorkActs = 0;
	private int nEducActs = 0;
	
	public void addPlan(Plan plan) {
		this.plans.add(plan);		
		this.countActs(plan);
	}
		
	public List<String> getModeOfLastTripsWithSamePurpose(String purpose) {
		List<String> modes = new Vector<String>();
		
		for (Plan plan : this.plans) {
			for (PlanElement pe : plan.getPlanElements()) {
				if (pe instanceof Activity) {
					ActivityImpl act = (ActivityImpl)pe;
					
					if (act.getType().equals(purpose)) {
						PlanImpl pl = (PlanImpl)plan;
						String mode = pl.getPreviousLeg(act).getMode();
						modes.add(mode);
					}
				}
			}			
		}		
		return modes;
	}
	
	// act frequency -------------------------------------------------------------
	
	private void countActs(Plan plan) {
		for (PlanElement pe : plan.getPlanElements()) {
			if (pe instanceof Activity) {
				ActivityImpl act = (ActivityImpl)pe;
				String type = act.getType();
				
				if (type.startsWith("s")) {
					this.nShoppingActs++;
				}
				else if (type.startsWith("l")) {
					this.nLeisureActs++;
				}
				else if (type.startsWith("w")) {
					this.nWorkActs++;
				}
				else if (type.startsWith("e")) {
					this.nEducActs++;
				}			
			}
		}
	}
	
	public int getnShoppingActs() {
		return nShoppingActs;
	}
	public int getnLeisureActs() {
		return nLeisureActs;
	}
	public int getnWorkActs() {
		return nWorkActs;
	}
	public int getnEducActs() {
		return nEducActs;
	}
}
