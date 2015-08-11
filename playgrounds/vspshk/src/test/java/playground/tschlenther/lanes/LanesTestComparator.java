package playground.tschlenther.lanes;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;

public class LanesTestComparator implements Comparator<Id<Person>> {

	Map<Id<Person>,Double> map = new HashMap<Id<Person>, Double>();

	public LanesTestComparator(Map<Id<Person>,Double> map){
		this.map = map;
	}
	
	
	@Override
	public int compare(Id<Person> o1, Id<Person> o2) {
		 double dd1 = this.map.get( o1 );
		 double dd2 = this.map.get( o2 );
		    return ( dd1 > dd2 ) ?  1 :
		           ( dd1 < dd2 ) ? -1 :
		           o2.compareTo(o1);
	}

}
