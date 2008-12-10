package playground.ciarif.retailers;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import org.matsim.basic.v01.Id;
import org.matsim.gbl.MatsimRandom;
import org.matsim.network.Link;

public class NewRetailersLocation {

	private Map<Id, Link> links;
	private Vector<Id> link_ids = new Vector<Id>();
	
	NewRetailersLocation (Map<Id,Link> links){
		this.links = links;
		Set<Id> keySet = this.links.keySet();
		Iterator<Id> iter_id = keySet.iterator();
		while (iter_id.hasNext()){
			Id id = iter_id.next();
			link_ids.add(id);
		}
		
	}
	
	public Link findLocation (){
		int rd = MatsimRandom.random.nextInt(links.size());
		Link l =  links.get(link_ids.elementAt(rd));
		return l;
	}
}
