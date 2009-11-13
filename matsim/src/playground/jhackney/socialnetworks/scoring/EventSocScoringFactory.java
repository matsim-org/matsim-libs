package playground.jhackney.socialnetworks.scoring;

import java.util.ArrayList;
import java.util.LinkedHashMap;

import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionFactory;



public class EventSocScoringFactory implements ScoringFunctionFactory {

	private String factype;
	private LinkedHashMap<ActivityImpl,ArrayList<Double>> actStats;
	private ScoringFunctionFactory factory;

	public EventSocScoringFactory(String factype, ScoringFunctionFactory sf, LinkedHashMap<ActivityImpl,ArrayList<Double>> actStats) {
		this.factype=factype;
		this.actStats=actStats;
		this.factory=sf;

	}

	public ScoringFunction getNewScoringFunction(final Plan plan) {
//		return new SNScoringMaxFriendFoeRatio(plan, this.factype, this.scorer);
		return new EventSocScoringFunction(plan, this.factory.getNewScoringFunction(plan), factype, actStats);
	}


}