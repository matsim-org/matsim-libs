package playground.mmoyo.algorithms;

import java.io.File;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.population.routes.RouteWRefs;
import org.matsim.core.scenario.ScenarioImpl;

import playground.mmoyo.utils.DataLoader;

public class PersonClonner{

	public Person run (final Person person, final Id newId){
		PersonImpl newPerson = new PersonImpl(newId);
		PersonImpl personImpl = (PersonImpl)person; //to avoid many castings
		newPerson.setAge(personImpl.getAge());
		newPerson.setCarAvail(personImpl.getCarAvail());
		newPerson.setEmployed(personImpl.isEmployed());
		newPerson.setLicence(personImpl.getLicense());
		newPerson.setSex(personImpl.getSex());
		for (Plan plan :person.getPlans()){
			PlanImpl newPlan = newPerson.createAndAddPlan(true);
			
			if (plan.isSelected()){
				newPerson.setSelectedPlan(newPlan);
			}
			
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
						if(leg.getRoute() instanceof RouteWRefs) { 
							newLeg.setRoute(((RouteWRefs) leg.getRoute()).clone());
						}
					}
					
					newPlan.getPlanElements().add(newLeg);
				}
			}
		} 
		return newPerson;
	}
	
	public static void main(String[] args) {
		String popFilePath = "../../berlin-bvg09/pt/nullfall_M44_344/test/pop.xml";
		String strPersonId = "passenger1";
		String netFilePath = "../../berlin-bvg09/pt/nullfall_M44_344/test/net.xml";
		
		DataLoader dataLoader = new DataLoader();
		Scenario scn = dataLoader.readNetwork_Population(netFilePath, popFilePath);

		Person newPerson = new PersonClonner().run(scn.getPopulation().getPersons().get(new IdImpl(strPersonId)), new IdImpl(strPersonId));

		//--> send this to a test case
		///erase routes from original person, the clon should retain the original routes
		Person person = scn.getPopulation().getPersons().get(new IdImpl(strPersonId));
		for (Plan plan : person.getPlans()){
			for (PlanElement pe : plan.getPlanElements()){
				if (pe instanceof LegImpl) {
					LegImpl leg = (LegImpl) pe;
					leg.setRoute(null);
				}
			}
		}

		Population testPop = new PopulationImpl((ScenarioImpl) dataLoader.createScenario());
		testPop.addPerson(newPerson);
		
		Network net = dataLoader.readNetwork(netFilePath);
		
		PopulationWriter popwriter = new PopulationWriter(testPop, net );
		popwriter.write(new File(popFilePath).getParent() + "/clonedPerson.xml") ;

		PopulationWriter popwriter2 = new PopulationWriter(scn.getPopulation(), net );
		popwriter2.write(new File(popFilePath).getParent() + "/originalPerson.xml") ;
		
		System.out.println("done");
	}
}
