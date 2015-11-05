package playground.johannes.coopsim.analysis;

import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import playground.johannes.coopsim.pysical.Trajectory;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class TrajectoryUtils {

	public static Set<String> getTypes(Collection<Trajectory> trajectories) {
		Set<String> purposes = new HashSet<String>();
		for(Trajectory t : trajectories) {
			for(int i = 0; i < t.getElements().size(); i += 2) {
				purposes.add(((Activity)t.getElements().get(i)).getType());
			}
		}
		return purposes;
	}
	
	public static Set<String> getModes(Collection<Trajectory> trajectories) {
		Set<String> modes = new HashSet<String>();
		for(Trajectory t : trajectories) {
			for(int i = 1; i < t.getElements().size(); i += 2) {
				Leg leg = (Leg) t.getElements().get(i);
				modes.add(leg.getMode());
			}
		}
		
		return modes;
	}
}
