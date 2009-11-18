package playground.kai.test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

interface BasicPlan {
}

class BasicPlanImpl implements BasicPlan {
}

interface Plan extends BasicPlan {
}

class PlanImpl extends BasicPlanImpl implements Plan {
}

interface BasicPerson<T extends BasicPlan> {
	List<? extends T> getPlans() ;
}

class BasicPersonImpl<T extends BasicPlan> implements BasicPerson<T> {
	protected List<? extends T> plans = new ArrayList<T>() ;
	public List<? extends T> getPlans() {
		return plans ;
	}
}	

interface Person extends BasicPerson<Plan> {
}

class PersonImpl extends BasicPersonImpl<Plan> implements Person {
	@Override
	public List<PlanImpl> getPlans() {
		return (List<PlanImpl>) plans ;
	}
}

interface Population {
	Map<Integer, PersonImpl> getPersons() ;
}

class PopulationImpl implements Population {
	private Map<Integer,PersonImpl> persons = new HashMap<Integer,PersonImpl>() ;
	
	public Map<Integer,PersonImpl> getPersons() {
		return persons ;
	}
	
}




public class OurNewTest {
	
	public static void main( String[] args ) {
		
		Population pop = new PopulationImpl() ;
		
		Person person = new PersonImpl() ;
		
		Plan plan = new PlanImpl() ;
		
		List<Plan> plans = (List<Plan>) person.getPlans() ;
		
		plans.add( plan ) ;


		
	}


}
