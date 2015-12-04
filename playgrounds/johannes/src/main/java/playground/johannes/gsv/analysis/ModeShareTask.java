package playground.johannes.gsv.analysis;

import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;
import org.matsim.api.core.v01.population.Leg;
import playground.johannes.coopsim.analysis.TrajectoryAnalyzerTask;
import playground.johannes.coopsim.pysical.Trajectory;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ModeShareTask extends TrajectoryAnalyzerTask {

	@Override
	public void analyze(Set<Trajectory> trajectories, Map<String, DescriptiveStatistics> results) {
		for(Trajectory t : trajectories) {
			Set<String> modes = new HashSet<String>();
			for(int i = 1; i < t.getElements().size(); i += 2) {
				Leg leg = (Leg) t.getElements().get(i);
				modes.add(leg.getMode());
			}
			
			for(String mode : modes) {
				DescriptiveStatistics stats = results.get(mode);
				if(stats == null) {
					stats = new DescriptiveStatistics();
					results.put(mode, stats);
				}
				stats.addValue(1);
			}
		}
	}

}
