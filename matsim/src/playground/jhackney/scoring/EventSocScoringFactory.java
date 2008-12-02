package playground.jhackney.scoring;

import java.util.ArrayList;
import java.util.Hashtable;

import org.matsim.population.Act;
import org.matsim.population.Plan;
import org.matsim.scoring.ScoringFunction;
import org.matsim.scoring.ScoringFunctionFactory;



public class EventSocScoringFactory implements ScoringFunctionFactory {

	private String factype;
//	private TrackEventsOverlap teo;
	private Hashtable<Act,ArrayList<Double>> actStats;
	private playground.jhackney.scoring.CharyparNagelScoringFunctionFactory factory;

	public EventSocScoringFactory(String factype,Hashtable<Act,ArrayList<Double>> actStats) {
		this.factype=factype;
		this.actStats=actStats;

	}

	public EventSocScoringFunction getNewScoringFunction(final Plan plan) {
//		return new SNScoringMaxFriendFoeRatio(plan, this.factype, this.scorer);
		return new EventSocScoringFunction(plan, factype, actStats);
	}


}
