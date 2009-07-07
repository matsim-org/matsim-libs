package playground.kai.test;

import java.util.HashMap;
import java.util.Map;

interface Person {
}

class PersonImpl implements Person {
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
		
//		pop.getPersons().put( 1, person ) ;


		
	}


}
