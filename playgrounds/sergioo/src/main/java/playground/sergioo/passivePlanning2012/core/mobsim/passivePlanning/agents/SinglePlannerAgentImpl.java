package playground.sergioo.passivePlanning2012.core.mobsim.passivePlanning.agents;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.routes.GenericRouteImpl;
import org.matsim.core.router.TripRouter;
import org.matsim.core.utils.misc.Time;
import org.matsim.facilities.ActivityFacility;
import org.matsim.pt.PtConstants;

import playground.sergioo.passivePlanning2012.api.population.EmptyTime;
import playground.sergioo.passivePlanning2012.core.mobsim.passivePlanning.definitions.SinglePlannerAgent;
import playground.sergioo.passivePlanning2012.core.population.EmptyTimeImpl;
import playground.sergioo.passivePlanning2012.core.population.decisionMakers.types.DecisionMaker;
import playground.sergioo.passivePlanning2012.population.parallelPassivePlanning.PassivePlannerManager.CurrentTime;
import playground.sergioo.passivePlanning2012.population.parallelPassivePlanning.PassivePlannerManager.MobsimStatus;

public abstract class SinglePlannerAgentImpl implements SinglePlannerAgent {

	//Attributes
	protected final DecisionMaker[] decisionMakers;
	protected AtomicInteger currentElementIndex = new AtomicInteger();
	protected PassivePlannerDriverAgent agent;
	protected TripRouter tripRouter;
	private boolean useCurrentElementIndex = true;
	
	//Constructors
	public SinglePlannerAgentImpl(DecisionMaker[] decisionMakers, PassivePlannerDriverAgent agent) {
		this.decisionMakers = decisionMakers;
		this.agent = agent;
	}

	//Methods
	@Override
	public Plan getPlan() {
		return agent.getCurrentPlan();
	}
	@Override
	public int getPlanElementIndex() {
		while(!useCurrentElementIndex){getPlan();}
		return currentElementIndex.get();
	}
	@Override
	public void incrementPlanElementIndex() {
		while(!useCurrentElementIndex){getPlan();}
		currentElementIndex.incrementAndGet();
	}
	@Override
	public void setRouter(TripRouter tripRouter) {
		this.tripRouter = tripRouter;
	}
	@Override
	public int planLegActivityLeg(double startTime, CurrentTime now, Id<ActivityFacility> startFacilityId, double endTime, Id<ActivityFacility> endFacilityId, final MobsimStatus mobsimStatus) {
		List<? extends PlanElement> legActLeg = getLegActivityLeg(startTime, now, startFacilityId, endTime, endFacilityId, mobsimStatus);
		if(legActLeg == null || legActLeg.size()==0)
			return 2;
		else {
			Plan plan = agent.getCurrentPlan();
			Activity previous = (Activity) plan.getPlanElements().get(currentElementIndex.get()-1);
			double previousEnd = previous.getEndTime();
			EmptyTime old = (EmptyTime) plan.getPlanElements().get(currentElementIndex.get());
			int index=currentElementIndex.get()+1;
			boolean emptySpace = false;
			Leg empty = null;
			if(startTime-previousEnd>3600) {
				empty = new EmptyTimeImpl(old.getRoute().getStartLinkId(), startTime-previousEnd);
				plan.getPlanElements().add(index++, empty);
				emptySpace = true;
			}
			double finalTime = startTime, firstLegTime=0;
			boolean firstLeg = true;
			for(PlanElement planElement:legActLeg) {
				if(planElement instanceof Leg) {
					finalTime += ((Leg)planElement).getTravelTime();
					if(firstLeg && emptySpace)
						firstLegTime += ((Leg)planElement).getTravelTime();
				}
				else
					if(((Activity)planElement).getEndTime()!=Time.UNDEFINED_TIME)
						finalTime = ((Activity)planElement).getEndTime();
					else
						finalTime += ((Activity)planElement).getMaximumDuration();
				if(firstLeg && planElement instanceof Activity && !((Activity)planElement).getType().equals(PtConstants.TRANSIT_ACTIVITY_TYPE)) {
					firstLeg = false;
					if(!((Activity)planElement).getType().equals(previous.getType()) && !(plan.getPlanElements().get(index-1) instanceof Leg)) {
						Leg leg = new LegImpl(TransportMode.transit_walk);
						leg.setTravelTime(0);
						leg.setRoute(new GenericRouteImpl(previous.getLinkId(), ((Activity)planElement).getLinkId()));
						plan.getPlanElements().add(index++, leg);
					}
				}
				if(!(firstLeg && emptySpace))
					plan.getPlanElements().add(index++, planElement);
			}
			if(emptySpace)
				empty.setTravelTime(empty.getTravelTime()+firstLegTime);
			if(legActLeg.get(legActLeg.size()-1) instanceof Activity && previousEnd+old.getTravelTime()-finalTime>3600) {
				Id<Link> finalLinkId = ((Activity)legActLeg.get(legActLeg.size()-1)).getLinkId();
				empty = new EmptyTimeImpl(finalLinkId, previousEnd+old.getTravelTime()-finalTime);
				plan.getPlanElements().add(index++, empty);
			}
			else if(plan.getPlanElements().get(index-1) instanceof Activity) {
				plan.getPlanElements().remove(index-1);
			}
			PlanElement nextElement = plan.getPlanElements().get(currentElementIndex.get()+1);
			PlanElement prevElement = plan.getPlanElements().get(currentElementIndex.get()-1);
			if(nextElement instanceof Activity) {
				if(((Activity)nextElement).getType().equals(((Activity)prevElement).getType())) {
					useCurrentElementIndex = false;
					currentElementIndex.decrementAndGet();
					plan.getPlanElements().remove(currentElementIndex.get());
					useCurrentElementIndex = true;
					agent.initializeLastActivity((Activity)nextElement, startTime);
				}
				else if(((Activity)nextElement).getCoord().equals(((Activity)prevElement).getCoord())) {
					Leg leg = new LegImpl(TransportMode.transit_walk);
					leg.setTravelTime(0);
					leg.setRoute(new GenericRouteImpl(previous.getLinkId(), previous.getLinkId()));
					plan.getPlanElements().add(currentElementIndex.get()+1, leg);
				}
				else
					throw new RuntimeException("Two consecutive activities can not be planned at different locations");
			}
			if(currentElementIndex.get()+1==plan.getPlanElements().size()-1) {
				agent.initializeLastActivity((Activity)plan.getPlanElements().get(plan.getPlanElements().size()-1), startTime);
			}
			plan.getPlanElements().remove(old);
			return emptySpace?1:0;
		}
	}
	protected abstract List<? extends PlanElement> getLegActivityLeg(double startTime, CurrentTime now, Id<ActivityFacility> startFacilityId, double endTime, Id<ActivityFacility> endFacilityId, final MobsimStatus mobsimStatus);
	@Override
	public void advanceToNextActivity(double now, double penalty) {
		agent.advanceToNextActivity(now, penalty);
		while(!useCurrentElementIndex){getPlan();}
		currentElementIndex.incrementAndGet();
	}

}
