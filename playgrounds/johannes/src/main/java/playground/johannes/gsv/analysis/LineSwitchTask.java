package playground.johannes.gsv.analysis;

import java.util.Map;
import java.util.Set;

import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;

import playground.johannes.coopsim.analysis.TrajectoryAnalyzerTask;
import playground.johannes.coopsim.pysical.Trajectory;

public class LineSwitchTask extends TrajectoryAnalyzerTask {

	@Override
	public void analyze(Set<Trajectory> trajectories, Map<String, DescriptiveStatistics> results) {
		LineSwitch lineSwitch = new LineSwitch();
		DescriptiveStatistics stats = lineSwitch.statistics(trajectories);
		
		results.put("lineSwitch", stats);

	}

}
