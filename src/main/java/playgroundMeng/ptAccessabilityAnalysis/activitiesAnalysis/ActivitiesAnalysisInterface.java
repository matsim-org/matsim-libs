package playgroundMeng.ptAccessabilityAnalysis.activitiesAnalysis;

import java.util.LinkedList;
import java.util.Map;

public interface ActivitiesAnalysisInterface {
	
	public Map<String, Map<Double, LinkedList<ActivityImp>>> getArea2time2activities();
}
