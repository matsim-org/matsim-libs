package tutorial.programming.example11PluggablePlanStrategyInCode;

import org.junit.Test;
import org.junit.Assert;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.population.PopulationUtils;

public class MyPlanSelectorTest {

	@Test
	public final void selectPlanTest()
	{
		//set up 
		Person person = PopulationUtils.createPerson(Id.create(1, Person.class));
		Plan plan0 = new PlanImpl(person);
		Plan plan1 = new PlanImpl(person);
		person.addPlan(plan0);
		person.addPlan(plan1);
		MyPlanSelector selector = new MyPlanSelector();
		
		//act
		Plan resultPlan = selector.selectPlan(person);
		
		//assert
		Assert.assertEquals(plan0, resultPlan);		
	}
}
