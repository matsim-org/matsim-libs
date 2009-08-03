package playground.dgrether.analysis.population;

import java.util.HashMap;
import java.util.Map;

import org.matsim.api.basic.v01.Id;
import org.matsim.core.population.ActivityImpl;
import org.matsim.households.basic.BasicIncome;

/**
 * @author dgrether
 *
 */
public class DgPersonData {
	
	private ActivityImpl homeActivity;

	private Map<Id, DgPlanData> planData;
	
	private Id personId;

	private BasicIncome income;
	
	public DgPersonData() {
		this.planData = new HashMap<Id, DgPlanData>();
	}
	
/**
	 * 
	 * @return the home location
	 */
	public ActivityImpl getActivity() {
		return homeActivity;
	}
	
	public void setActivity(ActivityImpl a) {
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

	public void setIncome(BasicIncome income) {
		this.income = income;
	}
	
	public BasicIncome getIncome() {
		return this.income;
	}
	
}