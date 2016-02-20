package playground.sergioo.passivePlanning2012.core.population.decisionMakers;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.routes.GenericRouteImpl;
import org.matsim.core.router.TripRouter;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.misc.Time;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.ActivityOption;
import org.matsim.facilities.OpeningTime;
import org.matsim.pt.PtConstants;

import playground.sergioo.passivePlanning2012.api.population.EmptyTime;
import playground.sergioo.passivePlanning2012.core.population.EmptyTimeImpl;
import playground.sergioo.passivePlanning2012.core.population.PlacesSharer;
import playground.sergioo.passivePlanning2012.core.population.agenda.Agenda;
import playground.sergioo.passivePlanning2012.core.population.decisionMakers.types.RouteDecisionMaker;
import playground.sergioo.passivePlanning2012.population.parallelPassivePlanning.PassivePlannerManager.CurrentTime;
import playground.sergioo.passivePlanning2012.population.parallelPassivePlanning.PassivePlannerManager.MobsimStatus;
import playground.sergioo.scheduling2013.SchedulingNetwork;
import playground.sergioo.scheduling2013.SchedulingNetwork.ActivitySchedulingLink;
import playground.sergioo.scheduling2013.SchedulingNetwork.JourneySchedulingLink;
import playground.sergioo.scheduling2013.SchedulingNetwork.SchedulingLink;
import playground.sergioo.scheduling2013.SchedulingNetwork.SchedulingNode;

public class AgendaDecisionMaker extends PlacesSharer implements RouteDecisionMaker {
	
	private static final int PLAN_STEP = 900;
	private static final double WALK_FACTOR = 3.4;
	private static final double WALK_SPEED = 4*1000/3600;
	private static final double TIME_WINDOW_SIZE = 12*3600;
	//Attributes
	private final ActivityFacilities facilities;
	private final Agenda agenda;
	private double futureActivityStartTime;
	private Set<String> modes;
	private final boolean carAvailability;
	private List<Tuple<String, Tuple<Double, Double>>> previousActivities;
	private List<Tuple<String, Tuple<Double, Double>>> followingActivities;
	private MobsimStatus mobsimStatus;
	private CurrentTime now;
	private double simulationEndTime;
	
	//Methods
	public AgendaDecisionMaker(ActivityFacilities facilities, boolean carAvailability, Set<String> modes, Agenda agenda, double simulationEndTime) {
		super();
		this.facilities = facilities;
		this.carAvailability = carAvailability;
		this.agenda = agenda;
		this.modes = modes;
		this.simulationEndTime = simulationEndTime;
	}
	
	public void prepareScheduling(Activity activity, Plan plan, CurrentTime now) {
		this.now = now;
		followingActivities = new ArrayList<Tuple<String, Tuple<Double, Double>>>();
		while(followingActivities.size()==0) {
			previousActivities = new ArrayList<Tuple<String, Tuple<Double, Double>>>();
			double time = 0;
			modes = new HashSet<String>();
			modes.add("pt");
			if(carAvailability)
				modes.add("car");
			Id<ActivityFacility> carLocation = ((Activity)plan.getPlanElements().get(0)).getFacilityId();
			boolean addFollowingActivities = false;
			int emptyTimeIndex = -1;
			for(int i=0; i<plan.getPlanElements().size(); i++) {
				PlanElement element = plan.getPlanElements().get(i);
				if(element instanceof Activity && !((Activity)element).getType().equals(PtConstants.TRANSIT_ACTIVITY_TYPE)) {
					if(Math.abs(activity.getEndTime()-((Activity)element).getEndTime())<TIME_WINDOW_SIZE || Math.abs(activity.getEndTime()-time)<TIME_WINDOW_SIZE)	
						if(!addFollowingActivities) {
							Tuple<Double, Double> times = new Tuple<Double, Double>(((Activity)element).getEndTime(), ((Activity)element).getEndTime()-time);
							previousActivities.add(new Tuple<String, Tuple<Double, Double>>(((Activity)element).getType(), times));
							if(element == activity) {
								addFollowingActivities = true;
								emptyTimeIndex = i+1;
								futureActivityStartTime = ((Activity)element).getEndTime() + ((Leg)plan.getPlanElements().get(emptyTimeIndex)).getTravelTime();
								if(!carLocation.equals(((Activity)element).getFacilityId()))
									modes.remove("car");
							}
						}
						else {
							double endTime = (((Activity)element).getEndTime()==Time.UNDEFINED_TIME?simulationEndTime:((Activity)element).getEndTime());
							followingActivities.add(new Tuple<String, Tuple<Double, Double>>(((Activity)element).getType(), new Tuple<Double, Double>(endTime,endTime-time)));
						}
					time = ((Activity)element).getEndTime();
				}
				else if(element instanceof Leg) {
					if(!addFollowingActivities && ((Leg)element).getMode().equals("car"))
						carLocation = ((Activity)plan.getPlanElements().get(i+1)).getFacilityId();
					time += ((Leg)element).getTravelTime();
				}
			}
			if(followingActivities.size()==0) {
				EmptyTime emptyTime = (EmptyTime) plan.getPlanElements().get(emptyTimeIndex);
				Activity nextActivity = (Activity) plan.getPlanElements().get(emptyTimeIndex+1);
				Activity newActivity = new ActivityImpl(nextActivity);
				futureActivityStartTime-=emptyTime.getTravelTime()/2;
				newActivity.setStartTime(futureActivityStartTime);
				newActivity.setEndTime(futureActivityStartTime+PLAN_STEP);
				plan.getPlanElements().add(emptyTimeIndex+1, new EmptyTimeImpl(newActivity.getLinkId(), -PLAN_STEP+emptyTime.getTravelTime()/2));
				plan.getPlanElements().add(emptyTimeIndex+1, newActivity);
				emptyTime.setTravelTime(emptyTime.getTravelTime()/2);
			}
		}
	}
	@Override
	public List<? extends PlanElement> decideRoute(double time,	Id<ActivityFacility> startFacilityId, Id<ActivityFacility> endFacilityId, String mode,
			TripRouter tripRouter) {
		List<PlanElement> planElements = new ArrayList<PlanElement>();
		if(Math.abs(futureActivityStartTime-followingActivities.get(0).getSecond().getFirst()+followingActivities.get(0).getSecond().getSecond())>0.001)
			throw new RuntimeException();
		List<SchedulingLink> path = new SchedulingNetwork().createNetwork(now, facilities, startFacilityId, endFacilityId,
				PLAN_STEP, modes, this, agenda, previousActivities, followingActivities, mobsimStatus);
		if(mobsimStatus.isMobsimEnds())
			return null;
		Id<ActivityFacility> currentFacilityId = startFacilityId;
		if(path!=null) {
			for(SchedulingLink link:path)
				if(link instanceof ActivitySchedulingLink) {
					ActivitySchedulingLink activityLink = (ActivitySchedulingLink)link;
					ActivityImpl activity = new ActivityImpl(activityLink.getActivityType(), activityLink.getCoord());
					activity.setEndTime(((SchedulingNode)link.getToNode()).getTime());
					activity.setLinkId(facilities.getFacilities().get(activityLink.getFacilityId()).getLinkId());
					activity.setFacilityId(activityLink.getFacilityId());
					if(planElements.size()>0 && planElements.get(planElements.size()-1) instanceof Activity)
						if(((Activity)planElements.get(planElements.size()-1)).getCoord().equals(activity.getCoord())) {
							Leg leg = new LegImpl("transit_walk");
							leg.setTravelTime(0);
							Route route = new GenericRouteImpl(facilities.getFacilities().get(currentFacilityId).getLinkId(),
									facilities.getFacilities().get(currentFacilityId).getLinkId());
							route.setDistance(0);
							leg.setRoute(route);
							planElements.add(leg);
						}
						else
							throw new RuntimeException("Two consecutive activities can not be planned at different locations");
					planElements.add(activity);
					currentFacilityId = Id.create(link.getToNode().getId().toString().split("\\(")[0], ActivityFacility.class);
				}
				else {
					JourneySchedulingLink journeyLink = (JourneySchedulingLink)link;
					Id<ActivityFacility> nextFacilityId = Id.create(link.getToNode().getId().toString().split("\\(")[0], ActivityFacility.class);
					if(facilities.getFacilities().get(currentFacilityId).getLinkId().equals(facilities.getFacilities().
							get(nextFacilityId).getLinkId())) {
						Leg leg = new LegImpl("transit_walk");
						leg.setTravelTime(0);
						Route route = new GenericRouteImpl(facilities.getFacilities().get(currentFacilityId).getLinkId(),
								facilities.getFacilities().get(currentFacilityId).getLinkId());
						route.setDistance(0);
						leg.setRoute(route);
						planElements.add(leg);
					}
					else {
						List<? extends PlanElement> routeElements = tripRouter.calcRoute(journeyLink.getMode(), facilities.getFacilities().
								get(currentFacilityId), facilities.getFacilities().get(nextFacilityId),
								((SchedulingNode)link.getFromNode()).getTime(), null);
						if(routeElements!=null && routeElements.size()>0)
							planElements.addAll(routeElements);
						else {
							Leg leg = new LegImpl("transit_walk");
							double distance = WALK_FACTOR*CoordUtils.calcEuclideanDistance(facilities.getFacilities().get(currentFacilityId).getCoord(), facilities.getFacilities().get(nextFacilityId).getCoord()); 
							leg.setTravelTime((int)(distance/WALK_SPEED));
							Route route = new GenericRouteImpl(facilities.getFacilities().get(currentFacilityId).getLinkId(),
									facilities.getFacilities().get(nextFacilityId).getLinkId());
							route.setDistance(distance);
							leg.setRoute(route);
							planElements.add(leg);
						}
					}
				}
		}
		else
			if(facilities.getFacilities().get(currentFacilityId).getLinkId().equals(facilities.getFacilities().
					get(endFacilityId).getLinkId())) {
				Leg leg = new LegImpl("transit_walk");
				leg.setTravelTime(0);
				Route route = new GenericRouteImpl(facilities.getFacilities().get(currentFacilityId).getLinkId(),
						facilities.getFacilities().get(currentFacilityId).getLinkId());
				route.setDistance(0);
				leg.setRoute(route);
				planElements.add(leg);
			}
			else {
				List<? extends PlanElement> routeElements = tripRouter.calcRoute(modes.iterator().next(), facilities.getFacilities().
						get(currentFacilityId), facilities.getFacilities().get(endFacilityId), time, null);
				if(routeElements!=null && routeElements.size()>0)
					planElements.addAll(routeElements);
				else {
					Leg leg = new LegImpl("transit_walk");
					double distance = WALK_FACTOR*CoordUtils.calcEuclideanDistance(facilities.getFacilities().get(currentFacilityId).getCoord(), facilities.getFacilities().get(endFacilityId).getCoord()); 
					leg.setTravelTime((int)(distance/WALK_SPEED));
					Route route = new GenericRouteImpl(facilities.getFacilities().get(currentFacilityId).getLinkId(),
							facilities.getFacilities().get(endFacilityId).getLinkId());
					route.setDistance(distance);
					leg.setRoute(route);
					planElements.add(leg);
				}
			}
		return planElements;
	}
	public void addKnownPlace(Id<ActivityFacility> facilityId, double startTime, String typeOfActivity) {
		if(agenda.containsType(typeOfActivity))
			super.addKnownPlace(facilityId, startTime, typeOfActivity);
	}
	public void addKnownPlace(Id<ActivityFacility> facilityId, double startTime, double endTime, String typeOfActivity) {
		if(agenda.containsType(typeOfActivity))
			super.addKnownPlace(facilityId, startTime, endTime, typeOfActivity);
	}
	public void addKnownPlace(Id<ActivityFacility> facilityId, String typeOfActivity) {
		ActivityFacility facility = facilities.getFacilities().get(facilityId);
		if(facility.getActivityOptions().get(typeOfActivity)!=null && agenda.containsType(typeOfActivity))
			if(facility.getActivityOptions().get(typeOfActivity).getOpeningTimes().isEmpty())
				super.addKnownPlace(facilityId, 0, 24*3600, typeOfActivity);
			else
				for(OpeningTime openingTime:facility.getActivityOptions().get(typeOfActivity).getOpeningTimes())
					super.addKnownPlace(facilityId, openingTime.getStartTime(), openingTime.getEndTime(), typeOfActivity);
	}
	public void addKnownPlace(Id<ActivityFacility> facilityId) {
		ActivityFacility facility = facilities.getFacilities().get(facilityId);
		for(Entry<String, ActivityOption> entry:facility.getActivityOptions().entrySet())
			if(agenda.containsType(entry.getKey()))
				if(entry.getValue().getOpeningTimes().isEmpty())
					super.addKnownPlace(facilityId, 0, 24*3600, entry.getKey());
				else
					for(OpeningTime openingTime:entry.getValue().getOpeningTimes())
						super.addKnownPlace(facilityId, openingTime.getStartTime(), openingTime.getEndTime(), entry.getKey());
	}
	public void setMobsimEnds(final MobsimStatus mobsimStatus) {
		this.mobsimStatus = mobsimStatus;
	}
	public void reset() {
		agenda.reset();
	}

}
