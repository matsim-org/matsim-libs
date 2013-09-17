package playground.singapore.scoring;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.population.routes.GenericRoute;
import org.matsim.core.scoring.ScoringFunctionAccumulator.LegScoring;
import org.matsim.core.utils.misc.RouteUtils;
import org.matsim.pt.transitSchedule.api.TransitSchedule;

public class SingaporeFareScoring implements LegScoring {

	public static double DISTANCE_FARE_RATE = -7.0/100000;
	protected double score;
	private TransitSchedule transitSchedule;
	private Network network;
	private Plan plan;
	private int numPtLegsScored = 0;
	
	public SingaporeFareScoring(Plan plan, TransitSchedule transitSchedule, Network network) {
		this.plan = plan;
		this.transitSchedule = transitSchedule;
		this.network = network;
	}

	@Override
	public void finish() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public double getScore() {
		return score;
	}

	@Override
	public void reset() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void startLeg(double time, Leg leg) {
		if(leg.getMode().equals(TransportMode.pt))
			score += getDistance()*DISTANCE_FARE_RATE;
	}
	
	protected double getDistance() {
		int num = numPtLegsScored++;
		for(PlanElement planElement:plan.getPlanElements())
			if(planElement instanceof Leg && ((Leg)planElement).getMode().equals(TransportMode.pt))
				if(num==0) {
					String[] parts = ((GenericRoute)((Leg)planElement).getRoute()).getRouteDescription().split("===");
					Id fromLinkId = transitSchedule.getFacilities().get(new IdImpl(parts[1])).getLinkId();
					Id toLinkId = transitSchedule.getFacilities().get(new IdImpl(parts[4])).getLinkId();
					return RouteUtils.calcDistance(transitSchedule.getTransitLines().get(new IdImpl(parts[2])).getRoutes().get(new IdImpl(parts[3])).getRoute().getSubRoute(fromLinkId, toLinkId), network);
				}
				else
					num--;
		return 0;
	}

	@Override
	public void endLeg(double time) {
		
	}

}
