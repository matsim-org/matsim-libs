package playground.jhackney.scoring;

import java.util.ArrayList;
import java.util.LinkedHashMap;

import org.matsim.core.api.population.Plan;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.scoring.ScoringFunctionFactory;



public class EventSocScoringFactory implements ScoringFunctionFactory {

	private String factype;
//	private TrackEventsOverlap teo;
	private LinkedHashMap<ActivityImpl,ArrayList<Double>> actStats;
	private playground.jhackney.scoring.CharyparNagelScoringFunctionFactory factory;

	public EventSocScoringFactory(String factype,LinkedHashMap<ActivityImpl,ArrayList<Double>> actStats) {
		this.factype=factype;
		this.actStats=actStats;

	}

	public EventSocScoringFunction getNewScoringFunction(final Plan plan) {
//		return new SNScoringMaxFriendFoeRatio(plan, this.factype, this.scorer);
		return new EventSocScoringFunction(plan, factype, actStats);
	}


}
