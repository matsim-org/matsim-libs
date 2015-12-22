package playground.tschlenther.lanes;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.vehicles.Vehicle;

public class LanesTestComparator implements Comparator<Id<Vehicle>> {

	Map<Id<Vehicle>, Double> map = new HashMap<>();

	public LanesTestComparator(Map<Id<Vehicle>, Double> map) {
		this.map = map;
	}

	@Override
	public int compare(Id<Vehicle> o1, Id<Vehicle> o2) {
		double dd1 = this.map.get(o1);
		double dd2 = this.map.get(o2);
		return (dd1 > dd2) ? 1 : (dd1 < dd2) ? -1 : o2.compareTo(o1);
	}

}
