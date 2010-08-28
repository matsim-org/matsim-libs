package playground.kai.test.test2;

import java.util.ArrayList;
import java.util.List;

interface Plan {
}

class PlanImpl implements Plan {
}

interface Person {
	public List<? extends Plan> getPlans() ;
}

class PersonImpl implements Person {
	List<PlanImpl> plans = new ArrayList<PlanImpl>() ;
	
	public List<PlanImpl> getPlans() {
		return (List<PlanImpl>) plans ;
	}
}



public class OurNewTest {
	
	public static void main( String[] args ) {
		
		Person person = new PersonImpl() ;
		
		Plan plan = new PlanImpl() ;
		
		// this does NOT work:
//		person.getPlans().add( plan ) ;
	
		// this works:
		List<Plan> plans = (List<Plan>) person.getPlans() ;
		plans.add( plan ) ;


		
	}


}
