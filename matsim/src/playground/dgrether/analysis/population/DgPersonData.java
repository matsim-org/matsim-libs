package playground.dgrether.analysis.population;

import java.util.HashMap;
import java.util.Map;

import org.matsim.api.basic.v01.Id;
import org.matsim.core.population.ActivityImpl;
import org.matsim.households.Income;

/**
 * @author dgrether
 *
 */
public class DgPersonData {
	
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

	
	public void setPlanData(Map<Id, DgPlanData> planData) {
		this.planData = planData;
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
	
}