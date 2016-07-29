package playground.sergioo.passivePlanning2012.core.population;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.algorithms.PlanAlgorithm;
import org.matsim.core.router.TripRouter;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.router.TripStructureUtils.Trip;
import org.matsim.core.utils.misc.Time;
import org.matsim.facilities.ActivityFacilities;

import playground.sergioo.passivePlanning2012.api.population.BasePlan;
import playground.sergioo.passivePlanning2012.api.population.EmptyTime;
import playground.sergioo.singapore2012.transitLocationChoice.TransitActsRemover;

public class BasePlanImpl implements BasePlan {

	//Constant
	private final static String EMPTY = "empty"; 
	//Attributes
	private final List<PlanElement> planElements = new ArrayList<PlanElement>();
	private Double score;
	private Person person;

	//Static methods
	public static void createBasePlan(boolean fixedTypes, String[] types, BasePersonImpl newPerson, Plan plan, TripRouter tripRouter, ActivityFacilities facilities) {
		PlanAlgorithm algorithm = new TransitActsRemover();
		algorithm.run(plan);
		BasePlanImpl newPlan = new BasePlanImpl(newPerson);
		Collection<String> typesC = Arrays.asList(types);
		Leg emptyTime = null;
		Leg toBeAdded = null;
		double time = 0, prevTime = 0;
		for(PlanElement planElement:plan.getPlanElements())
			if(planElement instanceof Activity && fixedTypes==typesC.contains(((Activity)planElement).getType())) {
				if(emptyTime!=null) {
					emptyTime.setTravelTime(time-emptyTime.getTravelTime());
					newPlan.addLeg(emptyTime);
					emptyTime = null;
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
				else if(!planElement.equals(PopulationUtils.getLastActivity(((Plan)plan))))
					throw new RuntimeException("Activity without time information");
			}
			else if(planElement instanceof Activity) {
				if(emptyTime==null)
					emptyTime = new EmptyTimeImpl(toBeAdded.getRoute().getStartLinkId(), prevTime);
				if(toBeAdded!=null)
					toBeAdded = null;
				if(((Activity) planElement).getEndTime()!=Time.UNDEFINED_TIME)
					time = ((Activity) planElement).getEndTime();
				else if(((Activity) planElement).getMaximumDuration()!=Time.UNDEFINED_TIME)
					time += ((Activity) planElement).getMaximumDuration();
				else if(!planElement.equals(PopulationUtils.getLastActivity(((Plan)plan))))
					throw new RuntimeException("Activity without time information");
			}
			else {
				toBeAdded = PopulationUtils.createLeg((Leg)planElement);
				prevTime = time;
				if(((Leg)planElement).getTravelTime()!=Time.UNDEFINED_TIME)
					time += ((Leg)planElement).getTravelTime();
				else
					throw new RuntimeException("Leg without time information");
			}
		if(emptyTime!=null) {
			emptyTime.setTravelTime(time-emptyTime.getTravelTime());
			newPlan.addLeg(emptyTime);
			emptyTime = null;
		}
		for(PlanElement planElement:plan.getPlanElements())
			if(planElement instanceof Leg  && !(planElement instanceof EmptyTime))
				((Leg)planElement).setRoute(null);
		for(PlanElement planElement:newPlan.getPlanElements())
			if(planElement instanceof Leg  && !(planElement instanceof EmptyTime))
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
		Plan newPlan = PopulationUtils.createPlan(newPerson);
		EmptyTime time = null;
		for(PlanElement planElement:plan.getPlanElements())
			if(planElement instanceof Activity) {
				newPlan.addActivity((Activity) planElement);
				if(time!=null) {
					time.getRoute().setEndLinkId(((Activity)planElement).getLinkId());
					time = null;
				}
			}
			else {
				if(((Leg)planElement).getMode().equals(EMPTY)) {
					double travelTime = ((Leg)planElement).getTravelTime();
					planElement = new EmptyTimeImpl(((Leg)planElement).getRoute().getStartLinkId(), travelTime);
					((Leg)planElement).getRoute().setTravelTime(travelTime);
					time = (EmptyTime) planElement;
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
    public String getType() {
        return null;
    }

    @Override
    public void setType(String type) {

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
		for(PlanElement pe : in.getPlanElements()) {
			if (pe instanceof Activity)
				addActivity(PopulationUtils.createActivity((Activity) pe));
			else if (pe instanceof Leg)
				if (pe instanceof EmptyTime || ((Leg)pe).getMode().equals(EMPTY)) {
					EmptyTime emptyTime = new EmptyTimeImpl(((Leg)pe).getRoute().getStartLinkId(), ((EmptyTime)pe).getTravelTime());
					addLeg(emptyTime);
				}
				else
					addLeg(PopulationUtils.createLeg((Leg)pe));
			else
				throw new IllegalArgumentException("unrecognized plan element type discovered");
		}
	}
	/**
	 * Removes the specified act from the plan as well as a leg according to the following rule:
	 * <ul>
	 * <li>first act: removes the act and the following leg</li>
	 * <li>last act: removes the act and the previous leg</li>
	 * <li>in-between act: removes the act, removes the previous leg's route, and removes the following leg.
	 * </ul>
	 *
	 * @param index
	 */
	public final void removeActivity(final int index) {
		if ((index % 2 != 0) || (index < 0) || (index > getPlanElements().size()-1));
		else if (getPlanElements().size() == 1);
		else {
			if (index == 0) {
				// remove first act and first leg
				getPlanElements().remove(index+1); // following leg
				getPlanElements().remove(index); // act
			}
			else if (index == getPlanElements().size()-1) {
				// remove last act and last leg
				getPlanElements().remove(index); // act
				getPlanElements().remove(index-1); // previous leg
			}
			else {
				// remove an in-between act
				Leg prev_leg = (Leg)getPlanElements().get(index-1); // prev leg;
				prev_leg.setDepartureTime(Time.UNDEFINED_TIME);
				prev_leg.setTravelTime(Time.UNDEFINED_TIME);
				prev_leg.setTravelTime( Time.UNDEFINED_TIME - prev_leg.getDepartureTime() );
				prev_leg.setRoute(null);

				getPlanElements().remove(index+1); // following leg
				getPlanElements().remove(index); // act
			}
		}
	}

	/**
	 * Removes the specified leg <b>and</b> the following act, too! If the following act is not the last one,
	 * the following leg will be emptied to keep consistency (i.e. for the route)
	 *
	 * @param index
	 */
	public final void removeLeg(final int index) {
		if ((index % 2 == 0) || (index < 1) || (index >= getPlanElements().size()-1));
		else {
			if (index != getPlanElements().size()-2) {
				// not the last leg
				Leg next_leg = (Leg)getPlanElements().get(index+2);
				next_leg.setDepartureTime(Time.UNDEFINED_TIME);
				next_leg.setTravelTime(Time.UNDEFINED_TIME);
				next_leg.setTravelTime( Time.UNDEFINED_TIME - next_leg.getDepartureTime() );
				next_leg.setRoute(null);
			}
			getPlanElements().remove(index+1); // following act
			getPlanElements().remove(index); // leg
		}
	}

}
