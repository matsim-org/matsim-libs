package playground.sergioo.passivePlanning2012.core.population.decisionMakers;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.router.TripRouter;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.misc.Time;
import org.matsim.pt.PtConstants;

import playground.sergioo.passivePlanning2012.api.population.EmptyTime;
import playground.sergioo.passivePlanning2012.core.population.PlaceSharer;
import playground.sergioo.passivePlanning2012.core.population.agenda.Agenda;
import playground.sergioo.passivePlanning2012.core.population.decisionMakers.types.RouteDecisionMaker;
import playground.sergioo.scheduling2013.SchedulingNetwork;
import playground.sergioo.scheduling2013.SchedulingNetwork.ActivitySchedulingLink;
import playground.sergioo.scheduling2013.SchedulingNetwork.JourneySchedulingLink;
import playground.sergioo.scheduling2013.SchedulingNetwork.SchedulingLink;
import playground.sergioo.scheduling2013.SchedulingNetwork.SchedulingNode;

public class AgendaDecisionMaker extends PlaceSharer implements RouteDecisionMaker {
	
	//Attributes
	private final Scenario scenario;
	private final Agenda agenda;
	private String futureActivityType;
	private double futureActivityStartTime;
	private Set<String> modes;
	private final boolean carAvailability;
	private final Plan plan;
	private List<Tuple<String, Double>> previousActivities;
	
	//Methods
	public AgendaDecisionMaker(Scenario scenario, boolean carAvailability, Set<String> modes, Plan plan, Agenda agenda) {
		super();
		this.scenario = scenario;
		this.carAvailability = carAvailability;
		this.plan = plan;
		this.agenda = agenda;
		this.modes = modes;
		double time = 0;
		for(PlanElement planElement:plan.getPlanElements()) {
			if(planElement instanceof Activity && !((Activity)planElement).getType().equals(PtConstants.TRANSIT_ACTIVITY_TYPE))
				this.addKnownPlace(((Activity)planElement).getFacilityId(), time, ((Activity) planElement).getEndTime(), ((Activity)planElement).getType());
			if(planElement instanceof Activity)
				if(((Activity)planElement).getEndTime()==Time.UNDEFINED_TIME)
					time += ((Activity)planElement).getMaximumDuration();
				else
					time = ((Activity)planElement).getEndTime();
			else
				time += ((Leg)planElement).getTravelTime();
		}
	}
	
	public void setLastActivity(Activity activity) {
		previousActivities = new ArrayList<Tuple<String,Double>>();
		double time = 0;
		modes = new HashSet<String>();
		modes.add("pt");
		if(carAvailability)
			modes.add("car");
		Id carLocation = ((Activity)plan.getPlanElements().get(0)).getFacilityId();
		for(int i=0; i<plan.getPlanElements().size()-2; i++) {
			PlanElement element = plan.getPlanElements().get(i);
			if(element instanceof Activity && !((Activity)element).getType().equals(PtConstants.TRANSIT_ACTIVITY_TYPE)) {
				previousActivities.add(new Tuple<String, Double>(((Activity)element).getType(),((Activity)element).getEndTime()-time));
				time = ((Activity)element).getEndTime();
				Leg nextLeg = (Leg)plan.getPlanElements().get(i+1);
				if(((Activity)element).getEndTime()==activity.getEndTime() && nextLeg instanceof EmptyTime) {
					Activity nextActivity = (Activity)plan.getPlanElements().get(i+2);
					futureActivityType = nextActivity.getType();
					futureActivityStartTime = time + nextLeg.getTravelTime();
					if(!carLocation.equals(((Activity)element).getFacilityId()))
						modes.remove("car");
				}
				if(element == activity)
					return;
			}
			else if(element instanceof Leg)
				if(((Leg)element).getMode().equals("car"))
					carLocation = ((Activity)plan.getPlanElements().get(i+1)).getFacilityId();
		}
	}
	@Override
	public List<? extends PlanElement> decideRoute(double time,
			Id startFacilityId, Id endFacilityId, String mode, TripRouter tripRouter) {
		List<PlanElement> planElements = new ArrayList<PlanElement>();
		List<SchedulingLink> path = new SchedulingNetwork().createNetwork(((ScenarioImpl) scenario).getActivityFacilities(),
				startFacilityId, endFacilityId, futureActivityType, futureActivityStartTime, 900, modes, this, agenda, previousActivities);
		Id currentFacilityId = startFacilityId;
		int numPrevious = 0;
		if(path!=null)
			for(SchedulingLink link:path) {
				if(link instanceof ActivitySchedulingLink) {
					if(numPrevious<previousActivities.size()) {
						numPrevious++;
						continue;
					}
					ActivitySchedulingLink activityLink = (ActivitySchedulingLink)link;
					ActivityImpl activity = new ActivityImpl(activityLink.getActivityType(), activityLink.getCoord());
					activity.setEndTime(((SchedulingNode)link.getToNode()).getTime());
					planElements.add(activity);
					currentFacilityId = new IdImpl(link.getToNode().getId().toString().split("\\(")[0]);
				}
				else {
					JourneySchedulingLink journeyLink = (JourneySchedulingLink)link;
					Id nextFacilityId = new IdImpl(link.getToNode().getId().toString().split("\\(")[0]);
					planElements.addAll(tripRouter.calcRoute(journeyLink.getMode(), scenario.getActivityFacilities().getFacilities().get(currentFacilityId), scenario.getActivityFacilities().getFacilities().get(nextFacilityId), ((SchedulingNode)link.getFromNode()).getTime(), null));
				}
			}
		else
			planElements.addAll(tripRouter.calcRoute(modes.iterator().next(), scenario.getActivityFacilities().getFacilities().get(currentFacilityId), scenario.getActivityFacilities().getFacilities().get(endFacilityId), time, null));
		return planElements;
	}
	public Agenda getAgenda() {
		return agenda;
	}

}
