package playground.sergioo.passivePlanning2012.core.population;

import java.util.Collection;
import java.util.Set;

import org.apache.commons.math3.distribution.NormalDistribution;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.router.TripRouter;
import org.matsim.core.utils.misc.Time;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.pt.PtConstants;

import playground.sergioo.passivePlanning2012.api.population.AgendaBasePerson;
import playground.sergioo.passivePlanning2012.core.population.agenda.Agenda;
import playground.sergioo.passivePlanning2012.core.population.decisionMakers.AgendaDecisionMaker;

public class AgendaBasePersonImpl extends BasePersonImpl implements	AgendaBasePerson {

	private final AgendaDecisionMaker agendaDecisionMaker;
	
	public AgendaBasePersonImpl(Id<Person> id, AgendaDecisionMaker agendaDecisionMaker) {
		super(id);
		this.agendaDecisionMaker = agendaDecisionMaker;
	}

	public static AgendaBasePersonImpl convertToAgendaBasePerson(PersonImpl person, ActivityFacilities facilities, Set<String> mainModes, Set<String> modes, double simulationEndTime) {
		AgendaDecisionMaker agendaDecisionMaker = new AgendaDecisionMaker(facilities, getCarAvailability(mainModes, person), modes, createAgenda(person), simulationEndTime);
		setInitialKnownPlaces(agendaDecisionMaker, person);
		AgendaBasePersonImpl newPerson = new AgendaBasePersonImpl(person.getId(), agendaDecisionMaker);
		newPerson.setAge(person.getAge());
		newPerson.setCarAvail(person.getCarAvail());
		newPerson.setEmployed(person.isEmployed());
		newPerson.setLicence(person.getLicense());
		newPerson.setSex(person.getSex());
		BasePlanImpl.convertToBasePlan(newPerson, person.getSelectedPlan());
		return newPerson;
	}
	
	public static AgendaBasePersonImpl createAgendaBasePerson(boolean fixedTypes, String[] types, PersonImpl person, TripRouter tripRouter, ActivityFacilities facilities, Set<String> mainModes, Set<String> modes, double simulationEndTime) {
		AgendaDecisionMaker agendaDecisionMaker = new AgendaDecisionMaker(facilities, getCarAvailability(mainModes, person), modes, createAgenda(person), simulationEndTime);
		setInitialKnownPlaces(agendaDecisionMaker, person);
		AgendaBasePersonImpl newPerson = new AgendaBasePersonImpl(person.getId(), agendaDecisionMaker);
		newPerson.setAge(person.getAge());
		newPerson.setCarAvail(person.getCarAvail());
		newPerson.setEmployed(person.isEmployed());
		newPerson.setLicence(person.getLicense());
		newPerson.setSex(person.getSex());
		newPerson.addPlan(person.getSelectedPlan());
		BasePlanImpl.createBasePlan(fixedTypes, types, newPerson, person.getSelectedPlan(), tripRouter, facilities);
		return newPerson;
	}
	
	@Override
	public AgendaDecisionMaker getAgendaDecisionMaker() {
		agendaDecisionMaker.reset();
		return agendaDecisionMaker;
	}
	public static void setInitialKnownPlaces(AgendaDecisionMaker agendaDecisionMaker, Person person) {
		for(Plan plan:person.getPlans())
			for(PlanElement planElement:plan.getPlanElements())
				if(planElement instanceof Activity && !((Activity)planElement).getType().equals(PtConstants.TRANSIT_ACTIVITY_TYPE))
					agendaDecisionMaker.addKnownPlace(((Activity)planElement).getFacilityId(), ((Activity)planElement).getType());
	}
	private static boolean getCarAvailability(Collection<String> mainModes, Person person) {
		boolean carAvailability = false;
		for(Plan plan:person.getPlans())
			for(PlanElement planElement:plan.getPlanElements())
				if(planElement instanceof Leg)
					if(mainModes.contains(((Leg)planElement).getMode()))
						carAvailability = true;
		return carAvailability;
	}
	private static Agenda createAgenda(Person person) {
		Agenda agenda = new Agenda();
		agenda.addElement("home", new NormalDistribution(0, 0.1), new NormalDistribution(10*3600, 2*3600));
		double time = 0;
		for(Plan plan:person.getPlans())
			for(PlanElement planElement:plan.getPlanElements()) {
				if(planElement instanceof Activity && !((Activity)planElement).getType().equals("home") && !((Activity)planElement).getType().equals(PtConstants.TRANSIT_ACTIVITY_TYPE)) {
					double duration = ((Activity)planElement).getEndTime()-time;
					if(duration<900)
						duration = 900;
					if(agenda.containsType(((Activity)planElement).getType()))
						agenda.addObservation(((Activity)planElement).getType(), duration);
					else
						agenda.addElement(((Activity)planElement).getType(), duration);
				}
				if(planElement instanceof Activity)
					if(((Activity)planElement).getEndTime()==Time.UNDEFINED_TIME)
						time += ((Activity)planElement).getMaximumDuration();
					else
						time = ((Activity)planElement).getEndTime();
				else
					time += ((Leg)planElement).getTravelTime();
			}
		return agenda;
	}

}
