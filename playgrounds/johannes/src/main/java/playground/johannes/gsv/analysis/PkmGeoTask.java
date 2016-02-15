package playground.johannes.gsv.analysis;

import gnu.trove.iterator.TObjectDoubleIterator;
import gnu.trove.map.hash.TObjectDoubleHashMap;
import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;
import org.matsim.facilities.ActivityFacilities;
import playground.johannes.coopsim.analysis.LegModeCondition;
import playground.johannes.coopsim.analysis.TrajectoryAnalyzerTask;
import playground.johannes.coopsim.analysis.TrajectoryUtils;
import playground.johannes.coopsim.analysis.TripDistanceTotal;
import playground.johannes.coopsim.pysical.Trajectory;

import java.util.Map;
import java.util.Set;

public class PkmGeoTask extends TrajectoryAnalyzerTask {

	private final ActivityFacilities facilities;
	
	public PkmGeoTask(ActivityFacilities facilities) {
		this.facilities = facilities;
	}
	
	@Override
	public void analyze(Set<Trajectory> trajectories, Map<String, DescriptiveStatistics> results) {
		Set<String> modes = TrajectoryUtils.getModes(trajectories);
		modes.add(null);
		
		TripDistanceTotal dist = new TripDistanceTotal(facilities);
		for(String mode : modes) {
			if(mode != null) {
				dist.setCondition(new LegModeCondition(mode));
			}
			TObjectDoubleHashMap<Trajectory> dists = dist.values(trajectories);
			TObjectDoubleIterator<Trajectory> it = dists.iterator();
			double sum = 0;
			for(int i = 0; i < dists.size(); i++) {
				it.advance();
				sum += it.value();
			}
			
			if(mode == null) {
				mode = "all";
			}
			
			DescriptiveStatistics stats = new DescriptiveStatistics();
			stats.addValue(sum);
			results.put(String.format("pkm.%s", mode), stats);
		}
	}

}
