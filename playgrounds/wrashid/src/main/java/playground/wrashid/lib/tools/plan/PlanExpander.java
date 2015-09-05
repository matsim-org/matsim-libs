package playground.wrashid.lib.tools.plan;

import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.contrib.parking.lib.GeneralLib;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PopulationWriter;
import org.matsim.population.Desires;

public class PlanExpander {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		String inputPlansFile = "H:/data/cvs/ivt/studies/switzerland/plans/teleatlas-ivtcheu/census2000v2_dilZh30km_10pct/plans.xml.gz";
		//String inputPlansFile = "H:/data/cvs/ivt/studies/switzerland/plans/teleatlas-ivtcheu/census2000v2_dilZh30km_10pct/plans.xml/plans.xml";
		
		String inputNetworkFile = "H:/data/cvs/ivt/studies/switzerland/network/teleatlas-ivtcheu/network.xml";
		String inputFacilitiesFile = "H:/data/cvs/ivt/studies/switzerland/facilities/facilities.xml";
		int populationExpansionFactor = 8;

		String outputPlansFile = "H:/data/experiments/msimoni/24Oct2014/plans_zurich_80pct.xml.gz";
		Scenario scenario = GeneralLib.readScenario(inputPlansFile,
				inputNetworkFile,inputFacilitiesFile);

		Population population = scenario.getPopulation();

		PopulationFactory factory = scenario.getPopulation().getFactory();

		LinkedList<Person> originalAgents = new LinkedList<Person>();

		for (Person p : population.getPersons().values()) {
			originalAgents.add(p);
		}

		for (Person p : originalAgents) {
			scenario.getPopulation().getPersons().remove(p.getId());
		}

		int pCounter = 1;

		for (int i = 0; i < populationExpansionFactor; i++) {
			for (Person origPerson : originalAgents) {
				PersonImpl originPersonImpl=(PersonImpl) origPerson;
				Desires originDesires = originPersonImpl.getDesires();
				
				Person newPerson = factory.createPerson(Id.create(String.valueOf(pCounter++), Person.class));
				newPerson.addPlan(originPersonImpl.createCopyOfSelectedPlanAndMakeSelected());
				
				//Desires newDesires = ((PersonImpl) newPerson).createDesires(originDesires.getDesc());
				//Map<String, Double> map = originDesires.getActivityDurations();
				//for (Entry<String, Double> entry : map.entrySet()) newDesires.putActivityDuration(entry.getKey(), entry.getValue());
				
				scenario.getPopulation().addPerson(newPerson);
			}
		}

//		new PopulationWriter(scenario.getPopulation(), scenario.getNetwork(),
//				1.0).write(outputPlansFile);

		// use V4 Format which also includes desires
		new PopulationWriter(scenario.getPopulation(), scenario.getNetwork(),
				1.0).writeFileV4(outputPlansFile);

	}

}
