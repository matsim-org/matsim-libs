package playground.ciarif.retailers;

import java.util.LinkedHashMap;
import java.util.Map;

import org.matsim.controler.Controler;
import org.matsim.facilities.Facility;
import org.matsim.gbl.MatsimRandom;
import org.matsim.interfaces.basic.v01.Coord;
import org.matsim.interfaces.basic.v01.Id;
import org.matsim.network.Link;
import org.matsim.network.NetworkLayer;

public class Retailer {
	private final Id id;
	private final Map<Id,Facility> facilities = new LinkedHashMap<Id,Facility>();
	private final RetailerStrategy strategy;
		
	protected Retailer(final Id id) {
		this.id = id;
		// TODO balmermi: implement different strategies and instantiate them here
		this.strategy = null; // implementation of the strategy
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

	public final Map<Id,Facility> runStrategy() {
		strategy.moveFacilities(this.facilities);
		return this.facilities;
	}
	
	// strategy: Random Mutation
	public final Map<Id,Facility> moveFacilitiesRandom(final NetworkLayer network) {
		for (Facility f : this.facilities.values()) {
			Object[] links = network.getLinks().values().toArray();
			int rd = MatsimRandom.random.nextInt(links.length);
			Link link = (Link)links[rd];
			Coord coord = link.getCenter();
			f.moveTo(coord);
		}
		return this.facilities;
	}
	
	// strategy: Random choice and compare
	public final Map<Id, Facility> moveFacilitiesMaxLink(Controler controler) {
		for (Facility f : facilities.values()) {
			Object[] links = controler.getNetwork().getLinks().values().toArray();
			int rd = MatsimRandom.random.nextInt(links.length);
			Link link = (Link)links[rd];
			controler.getLinkStats().addData(controler.getVolumes(), controler.getTravelTimeCalculator());
			double[] currentlink_volumes = controler.getLinkStats().getAvgLinkVolumes(f.getLink().getId().toString());
			double[] newlink_volumes = controler.getLinkStats().getAvgLinkVolumes(link.getId().toString());
			System.out.println ("currentlink_volumes = " + currentlink_volumes);
			double currentlink_volume =0;
			double newlink_volume =0;
			for (int j=0; j<currentlink_volumes.length;j=j+1) {
				currentlink_volume = currentlink_volume + currentlink_volumes[j];
				
			}
			for (int j=0; j<newlink_volumes.length;j=j+1) {
				newlink_volume = newlink_volume + newlink_volumes[j];
				
			}
			System.out.println ("currentlink_volume = " + currentlink_volume);
			System.out.println ("newlink_volume = " + newlink_volume);
			if (newlink_volume >= currentlink_volume) {
				Coord coord = link.getCenter();
				f.moveTo(coord);
			}
		}
		return this.facilities;
	}
}
