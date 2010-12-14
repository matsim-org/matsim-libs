package playground.kai.ids;

import java.util.HashMap;
import java.util.Map;

public class Test4 {

	/**
	 * @param args
	 */
	public static void main(String[] args) {

		Map<PersonId,PersonId> map = new HashMap<PersonId,PersonId>() ;
		
		PersonId id1 = PopulationIdFactory.createPersonId("1") ;
		PersonId id2 = PopulationIdFactory.createPersonId("2") ;
		PersonId id3 = PopulationIdFactory.createPersonId("3") ;
		PersonId id4 = PopulationIdFactory.createPersonId("4") ;
		
		map.put( id1, id1 ) ;
		map.put( id2, id2 ) ;
		
		LinkId id = NetworkIdFactory.createLinkId("22") ;

		PersonId lid = map.get(id) ;
		
		
	}

}
