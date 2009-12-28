package playground.dgrether.analysis.population;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.population.ActivityImpl;
import org.matsim.households.Income;

/**
 * @author dgrether
 *
 */
public class DgPersonData {
	
	private static final Logger log = Logger.getLogger(DgPersonData.class);
	
	private ActivityImpl homeActivity;

	private Map<Id, DgPlanData> planData;
	
	private Id personId;

	private Income income;
	
	public DgPersonData() {
		this.planData = new HashMap<Id, DgPlanData>();
	}
	
/**
	 * 
	 * @return the home location
	 */
	public ActivityImpl getFirstActivity() {
		return homeActivity;
	}
	
	public void setFirstActivity(ActivityImpl a) {
		this.homeActivity = a;
	}

	
	public Map<Id, DgPlanData> getPlanData() {
		return planData;
	}

	public Id getPersonId() {
		return personId;
	}

	
	public void setPersonId(Id personId) {
		this.personId = personId;
	}

	public void setIncome(Income income) {
		this.income = income;
	}
	
	public Income getIncome() {
		return this.income;
	}

	/**
	 * Score difference plan runid 2 - runid 1
	 */
	public double getDeltaScore(Id runId1, Id runId2) {
		DgPlanData plan1 = this.planData.get(runId1);
		DgPlanData plan2 = this.planData.get(runId2);
		if ((plan1 != null) && (plan2 != null)) {
			return plan2.getScore() - plan1.getScore();
		}
		else if (plan1 == null) {
			log.error("Person id " + personId + " has no plan (null) for runId " + runId1); 
		}
		else if (plan2 == null) {
			log.error("Person id " + personId + " has no plan (null) for runId " + runId2); 
		}
		return 0.0;
	}
	
}