package playground.ciarif.retailers;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import org.matsim.basic.v01.Id;
import org.matsim.facilities.Facility;
import org.matsim.gbl.MatsimRandom;
import org.matsim.network.Link;
import org.matsim.network.NetworkLayer;
import org.matsim.population.Leg;
import org.matsim.population.Person;
import org.matsim.population.Plan;
import org.matsim.utils.geometry.Coord;

public class Retailer {
	private final Id id;
	private final Map<Id,Facility> facilities = new LinkedHashMap<Id,Facility>();
	
	protected Retailer(final Id id) {
		this.id = id;
	}

	public final Id getId() {
		return this.id;
	}

	public final boolean addFacility(Facility f) {
		if (f == null) { return false; }
		if (this.facilities.containsKey(f.getId())) { return false; }
		this.facilities.put(f.getId(),f);
		return true;
	}
	
	public final Facility getFacility(final Id facId) {
		return this.facilities.get(facId);
	}

	public final Map<Id,Facility> getFacilities() {
		return this.facilities;
	}
	
	public final Map<Id,Facility> moveFacilities(final NetworkLayer network) {
		for (Facility f : this.facilities.values()) {
			Object[] links = network.getLinks().values().toArray();
			int rd = MatsimRandom.random.nextInt(links.length);
			Link link = (Link)links[rd];
			Coord coord = link.getCenter();
			f.moveTo(coord);
		}
		return this.facilities;
	}
	
}
