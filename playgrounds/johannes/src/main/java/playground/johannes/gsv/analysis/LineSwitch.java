package playground.johannes.gsv.analysis;

import gnu.trove.map.hash.TObjectDoubleHashMap;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.pt.PtConstants;
import playground.johannes.coopsim.analysis.AbstractTrajectoryProperty;
import playground.johannes.coopsim.pysical.Trajectory;

import java.util.Set;

public class LineSwitch extends AbstractTrajectoryProperty {

	@Override
	public TObjectDoubleHashMap<Trajectory> values(Set<? extends Trajectory> trajectories) {
		TObjectDoubleHashMap<Trajectory> values = new TObjectDoubleHashMap<Trajectory>();
		for(Trajectory t : trajectories) {
			int count = 0;
			for(int i = 0; i < t.getElements().size(); i += 2) {
				Activity act = (Activity) t.getElements().get(i);
				if(act.getType().equalsIgnoreCase(PtConstants.TRANSIT_ACTIVITY_TYPE)) {
					count++;
				}
			}
			values.put(t, count);
		}
		
		return values;
	}

	
}
