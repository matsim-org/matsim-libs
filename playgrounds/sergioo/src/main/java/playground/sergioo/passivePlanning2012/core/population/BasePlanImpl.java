package playground.sergioo.passivePlanning2012.core.population;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.api.experimental.facilities.ActivityFacilities;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.population.routes.GenericRoute;
import org.matsim.core.router.TripRouter;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.router.TripStructureUtils.Trip;
import org.matsim.core.utils.misc.Time;
import org.matsim.population.algorithms.PlanAlgorithm;

import playground.sergioo.passivePlanning2012.api.population.BasePlan;
import playground.sergioo.passivePlanning2012.api.population.EmptyTime;
import playground.sergioo.passivePlanning2012.api.population.FloatActivity;
import playground.sergioo.singapore2012.transitLocationChoice.TransitActsRemover;

public class BasePlanImpl implements BasePlan {

	//Constant
	private final static String EMPTY = "empty"; 
	//Attributes
	private final List<PlanElement> planElements = new ArrayList<PlanElement>();
	private Double score;
	private Person person;
	private final Collection<FloatActivity> floatActivities = new ArrayList<FloatActivity>();

	//Static methods
	public static void createBasePlan(boolean fixedTypes, String[] types, BasePersonImpl newPerson, PlanImpl plan, TripRouter tripRouter, ActivityFacilities facilities) {
		PlanAlgorithm algorithm = new TransitActsRemover();
		algorithm.run(plan);
		BasePlanImpl newPlan = new BasePlanImpl(newPerson);
		Collection<String> typesC = Arrays.asList(types);
		Leg emptyActivity = null;
		Leg toBeAdded = null;
		double time = 0, prevTime = 0;
		for(PlanElement planElement:plan.getPlanElements())
			if(planElement instanceof Activity && fixedTypes==typesC.contains(((Activity)planElement).getType())) {
				if(emptyActivity!=null) {
					emptyActivity.setTravelTime(time-emptyActivity.getTravelTime());
					newPlan.addLeg(emptyActivity);
					emptyActivity = null;
				}
				else if(toBeAdded!=null) {
					newPlan.addLeg(toBeAdded);
					toBeAdded = null;
				}
				newPlan.addActivity((Activity) planElement);
				if(((Activity) planElement).getEndTime()!=Time.UNDEFINED_TIME)
					time = ((Activity) planElement).getEndTime();
				else if(((Activity) planElement).getMaximumDuration()!=Time.UNDEFINED_TIME)
					time += ((Activity) planElement).getMaximumDuration();
				else if(!planElement.equals(plan.getLastActivity()))
					throw new RuntimeException("Activity without time information");
			}
			else if(planElement instanceof Activity) {
				if(emptyActivity==null) {
					emptyActivity = new EmptyTimeImpl(toBeAdded.getRoute().getStartLinkId());
					emptyActivity.setTravelTime(prevTime);
				}
				if(toBeAdded!=null)
					toBeAdded = null;
				if(((Activity) planElement).getEndTime()!=Time.UNDEFINED_TIME)
					time = ((Activity) planElement).getEndTime();
				else if(((Activity) planElement).getMaximumDuration()!=Time.UNDEFINED_TIME)
					time += ((Activity) planElement).getMaximumDuration();
				else if(!planElement.equals(plan.getLastActivity()))
					throw new RuntimeException("Activity without time information");
			}
			else {
				toBeAdded = (Leg)planElement;
				prevTime = time;
				if(((Leg)planElement).getTravelTime()!=Time.UNDEFINED_TIME)
					time += ((Leg)planElement).getTravelTime();
				else
					throw new RuntimeException("Leg without time information");
			}
		if(emptyActivity!=null) {
			emptyActivity.setTravelTime(time-emptyActivity.getTravelTime());
			newPlan.addLeg(emptyActivity);
			emptyActivity = null;
		}
		for(PlanElement planElement:plan.getPlanElements())
			if(planElement instanceof Leg  && !(planElement instanceof EmptyTime) && /*TODO*/((Leg)planElement).getRoute() instanceof GenericRoute)
				((Leg)planElement).setRoute(null);
		for(PlanElement planElement:newPlan.getPlanElements())
			if(planElement instanceof Leg  && !(planElement instanceof EmptyTime) && /*TODO*/((Leg)planElement).getRoute() instanceof GenericRoute)
				((Leg)planElement).setRoute(null);
		List<Trip> trips = TripStructureUtils.getTrips( newPlan , tripRouter.getStageActivityTypes() );
		for (Trip trip : trips) {
			String mode = tripRouter.getMainModeIdentifier().identifyMainMode(trip.getTripElements());
			if(!mode.equals(EMPTY)) {
				final List<? extends PlanElement> newTrip = tripRouter.calcRoute(mode,
						facilities.getFacilities().get(trip.getOriginActivity().getFacilityId()),
						facilities.getFacilities().get(trip.getDestinationActivity().getFacilityId()),
						trip.getOriginActivity().getEndTime(),
						newPlan.getPerson());
				TripRouter.insertTrip(
						newPlan, 
						trip.getOriginActivity(),
						newTrip,
						trip.getDestinationActivity());
			}
		}
		trips = TripStructureUtils.getTrips( plan , tripRouter.getStageActivityTypes() );
		for (Trip trip : trips) {
			String mode = tripRouter.getMainModeIdentifier().identifyMainMode(trip.getTripElements());
			if(!mode.equals(EMPTY)) {
				final List<? extends PlanElement> newTrip = tripRouter.calcRoute(mode,
						facilities.getFacilities().get(trip.getOriginActivity().getFacilityId()),
						facilities.getFacilities().get(trip.getDestinationActivity().getFacilityId()),
						trip.getOriginActivity().getEndTime(),
						plan.getPerson());
				TripRouter.insertTrip(
						plan, 
						trip.getOriginActivity(),
						newTrip,
						trip.getDestinationActivity());
			}
		}
		newPerson.addPlan(newPlan);
		newPerson.setSelectedPlan(newPlan);
		BasePlanImpl copyPlan = new BasePlanImpl(newPerson);
		copyPlan.copyFrom(newPlan);
		newPerson.setBasePlan(copyPlan);
	}
	public static void convertToBasePlan(BasePersonImpl newPerson, Plan plan) {
		BasePlanImpl newPlan = new BasePlanImpl(newPerson);
		for(PlanElement planElement:plan.getPlanElements())
			if(planElement instanceof Activity)
				newPlan.addActivity((Activity) planElement);
			else {
				if(((Leg)planElement).getMode().equals(EMPTY)) {
					double travelTime = ((Leg)planElement).getTravelTime();
					planElement = new EmptyTimeImpl(((Leg)planElement).getRoute().getStartLinkId());
					((Leg)planElement).setTravelTime(travelTime);
					((Leg)planElement).getRoute().setTravelTime(travelTime);
				}
				newPlan.addLeg((Leg) planElement);
			}
		newPerson.addPlan(newPlan);
		newPerson.setSelectedPlan(newPlan);
		BasePlanImpl copyPlan = new BasePlanImpl(newPerson);
		copyPlan.copyFrom(newPlan);
		newPerson.setBasePlan(copyPlan);
	}

	//Methods
	public BasePlanImpl(final Person person) {
		this.person = person;
	}
	@Override
	public List<PlanElement> getPlanElements() {
		return planElements;
	}
	@Override
	public void addLeg(Leg leg) {
		planElements.add(leg);
	}
	@Override
	public void addActivity(Activity act) {
		planElements.add(act);
	}
	@Override
	public boolean isSelected() {
		return this.getPerson().getSelectedPlan() == this;
	}
	@Override
	public void setScore(Double score) {
		this.score = score;
	}
	@Override
	public Double getScore() {
		return score;
	}
	@Override
	public Person getPerson() {
		return person;
	}
	@Override
	public void setPerson(Person person) {
		this.person = person;
	}
	@Override
	public Map<String, Object> getCustomAttributes() {
		return null;
	}
	@Override
	public Collection<FloatActivity> getFloatActivities() {
		return floatActivities ;
	}
	@Override
	public void addFloatActivity(FloatActivity floatActivity) {
		floatActivities.add(floatActivity);
	}
	@Override
	public Plan getAndSelectPlan() {
		Plan plan = new BasePlanImpl(person);
		for(int i=0; i<planElements.size(); i++) {
			PlanElement planElement = planElements.get(i);
			if(planElement instanceof Activity)
				plan.addActivity((Activity) planElement);
			else if(planElement instanceof Leg)
				plan.addLeg((Leg) planElement);
		}
		if(person.addPlan(plan)) {
			((PersonImpl)person).setSelectedPlan(plan);
			return plan;
		}
		else
			return null;
	}
	public int getPlanElementIndex(double time) {
		double timeRef = 0;
		if(time>=0) {
			for(int i=0; i<this.getPlanElements().size(); i++) {
				PlanElement planElement = this.getPlanElements().get(i);
				if(planElement instanceof Activity)
					if(((Activity)planElement).getEndTime()!=Time.UNDEFINED_TIME)
						timeRef = ((Activity)planElement).getEndTime();
					else if(((Activity)planElement).getMaximumDuration()!=Time.UNDEFINED_TIME)
						timeRef += ((Activity)planElement).getMaximumDuration();
				if(planElement instanceof Leg)
					timeRef += ((Leg)planElement).getTravelTime();
				if(time<timeRef)
					return i;
			}
		}
		return -1;
	}
	public void copyFrom(final Plan in) {
		for (PlanElement pe : in.getPlanElements()) {
			if (pe instanceof Activity)
				getPlanElements().add(new ActivityImpl((Activity) pe));
			else if (pe instanceof Leg) {
				Leg l = (Leg) pe;
				LegImpl l2 = null;
				if(l.getMode().equals(EMPTY))
					l2 = new EmptyTimeImpl(l.getRoute().getStartLinkId());
				else
					l2 = new LegImpl(l.getMode());
				addLeg(l2);
				l2.setDepartureTime(l.getDepartureTime());
				l2.setTravelTime(l.getTravelTime());
				if (pe instanceof LegImpl) {
					// get the arrival time information only if available
					l2.setArrivalTime(((LegImpl) pe).getArrivalTime());
				}
				if (l.getRoute() != null) {
					l2.setRoute(l.getRoute().clone());
				}
			} else {
				throw new IllegalArgumentException("unrecognized plan element type discovered");
			}
		}
	}

}
