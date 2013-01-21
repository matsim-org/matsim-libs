package playground.sergioo.passivePlanning2012.core.population;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PlanImpl;

import playground.sergioo.passivePlanning2012.api.population.BasePlan;
import playground.sergioo.passivePlanning2012.api.population.EmptyActivity;
import playground.sergioo.passivePlanning2012.api.population.FloatActivity;

public class BasePlanImpl implements BasePlan {

	//Attributes
	private final List<PlanElement> planElements = new ArrayList<PlanElement>();
	private Double score;
	private Person person;
	private final Collection<FloatActivity> floatActivities = new ArrayList<FloatActivity>();

	//Methods
	public BasePlanImpl(final Person person) {
		this.person = person;
	}
	@Override
	public List<PlanElement> getPlanElements() {
		return planElements;
	}
	@Override
	public void addLeg(Leg leg) {
		planElements.add(leg);
	}
	@Override
	public void addActivity(Activity act) {
		planElements.add(act);
	}
	@Override
	public boolean isSelected() {
		return this.getPerson().getSelectedPlan() == this;
	}
	@Override
	public void setScore(Double score) {
		this.score = score;
	}
	@Override
	public Double getScore() {
		return score;
	}
	@Override
	public Person getPerson() {
		return person;
	}
	@Override
	public void setPerson(Person person) {
		this.person = person;
	}
	@Override
	public Map<String, Object> getCustomAttributes() {
		return null;
	}
	@Override
	public Collection<FloatActivity> getFloatActivities() {
		return floatActivities ;
	}
	@Override
	public void addFloatActivity(FloatActivity floatActivity) {
		floatActivities.add(floatActivity);
	}
	@Override
	public Plan getAndSelectPlan() {
		Plan plan = new PlanImpl(person);
		double time=0;
		for(int i=0; i<planElements.size(); i++) {
			PlanElement planElement = planElements.get(i);
			if(planElement instanceof Activity && ((Activity)planElement).getStartTime()==time) {
					plan.addActivity((Activity) planElement);
					time = ((Activity)planElement).getEndTime()+1;
			}
			else if(planElement instanceof Leg && ((Leg)planElement).getDepartureTime()==time) {
				plan.addLeg((Leg) planElement);
				time = ((Leg)planElement).getDepartureTime()+((Leg)planElement).getTravelTime()+1;
			}
			else {
				EmptyActivity emptyActivity = new EmptyActivityImpl();
				emptyActivity.setStartTime(time);
				PlanElement nextPlanElement = null;
				if(i+1<planElements.size())
					nextPlanElement = planElements.get(i+1);
				if(nextPlanElement == null)
					time = Double.MAX_VALUE;
				else if(nextPlanElement instanceof Activity)
					time = ((Activity)nextPlanElement).getStartTime()-1;
				else
					time = ((Leg)nextPlanElement).getDepartureTime()-1;
				emptyActivity.setEndTime(time);
				plan.addActivity(emptyActivity);
				time++;
				i--;
			}
		}
		if(person.addPlan(plan)) {
			((PersonImpl)person).setSelectedPlan(plan);
			return plan;
		}
		else
			return null;
	}

}
