package playground.mmoyo.algorithms;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PlanImpl;

import playground.mmoyo.utils.DataLoader;

public class PersonClonner{

	public Person run (final Person person, final Id<Person> newId){
		PersonImpl newPerson = new PersonImpl(newId);
		PersonImpl personImpl = (PersonImpl)person; //to avoid many castings
		newPerson.setAge(personImpl.getAge());
		newPerson.setCarAvail(personImpl.getCarAvail());
		newPerson.setEmployed(personImpl.isEmployed());
		newPerson.setLicence(personImpl.getLicense());
		newPerson.setSex(personImpl.getSex());
		for (Plan plan :person.getPlans()){
			PlanImpl newPlan = newPerson.createAndAddPlan(true);
			
			//if (plan.isSelected()){
			//	newPerson.setSelectedPlan(newPlan);
			//}
			newPlan.setType(((PlanImpl)plan).getType());
			
			for (PlanElement pe : plan.getPlanElements()) {
				if (pe instanceof ActivityImpl) {
					ActivityImpl act = (ActivityImpl) pe;
					Activity newAct = new ActivityImpl(act.getType(), act.getCoord(), act.getLinkId() );
					newAct.setEndTime(act.getEndTime());
					newAct.setMaximumDuration(act.getMaximumDuration());
					newAct.setStartTime(act.getStartTime());
					((ActivityImpl)newAct).setFacilityId(act.getFacilityId());
					newPlan.getPlanElements().add(newAct);
				} else if (pe instanceof LegImpl) {
					LegImpl leg = (LegImpl) pe;
					LegImpl newLeg = new LegImpl(leg.getMode());
					newLeg.setDepartureTime(leg.getDepartureTime());
					newLeg.setArrivalTime(leg.getArrivalTime());     			  
					newLeg.setTravelTime(leg.getTravelTime());       			 
					
					newLeg.setRoute(leg.getRoute());			     			
					if (leg.getRoute() != null){
							newLeg.setRoute(leg.getRoute().clone());
					}
					
					newPlan.getPlanElements().add(newLeg);
				}
			}
		} 
		
		int selInx = person.getPlans().indexOf(person.getSelectedPlan());
		newPerson.setSelectedPlan(newPerson.getPlans().get(selInx));
		return newPerson;
	}
	
	public static void main(String[] args) {
		String popFilePath = "../../";
		String netFilePath = "../../";
		
		DataLoader dataLoader = new DataLoader();
		Scenario scn = dataLoader.readNetwork_Population(netFilePath, popFilePath);
		Population pop= scn.getPopulation();
		
		String suf = "_2";
		PersonClonner personClonner = new PersonClonner();
		List<Person> clonsList = new ArrayList<Person>();
		for (Person person: pop.getPersons().values()){
			Id<Person> clonId = Id.create(person.getId() + suf, Person.class); 
			Person newPerson = personClonner.run(person, clonId);
			clonsList.add(newPerson);
		}
		for (Person clon : clonsList){
			pop.addPerson(clon);
		}
		
		PopulationWriter popwriter = new PopulationWriter(pop, scn.getNetwork());
		popwriter.write(new File(popFilePath).getParent() + "/clonedPopulation.xml.gz") ;

		System.out.println("done");
	}
}
