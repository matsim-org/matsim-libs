package others.sergioo.mains;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.facilities.MatsimFacilitiesReader;

public class MergePlans {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Scenario base25 = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new MatsimNetworkReader(base25).readFile("./input/network/singapore7.xml");
		new MatsimFacilitiesReader((ScenarioImpl)base25).readFile("C:\\Users\\sergioo\\Documents\\2011\\Work\\FCL\\Operations\\Data\\MATSimXMLCurrentData\\SpecialTrips\\completeFacilities.xml");
		new MatsimPopulationReader(base25).readFile("C:\\Users\\sergioo\\Documents\\2011\\Work\\FCL\\Operations\\Data\\MATSimXMLCurrentData\\SpecialTrips\\population25.xml.gz");
		Scenario specialTrips = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new MatsimFacilitiesReader((ScenarioImpl)specialTrips).readFile("C:\\Users\\sergioo\\Documents\\2011\\Work\\FCL\\Operations\\Data\\MATSimXMLCurrentData\\SpecialTrips\\StFacilitiesST.xml");
		new MatsimPopulationReader(specialTrips).readFile("C:\\Users\\sergioo\\Documents\\2011\\Work\\FCL\\Operations\\Data\\MATSimXMLCurrentData\\SpecialTrips\\plansCheckpointwtST.xml");
		for(Person person:specialTrips.getPopulation().getPersons().values())
			if(!mcycle(person) && Math.random()<0.25)
				base25.getPopulation().addPerson(person);
		specialTrips = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new MatsimFacilitiesReader((ScenarioImpl)specialTrips).readFile("C:\\Users\\sergioo\\Documents\\2011\\Work\\FCL\\Operations\\Data\\MATSimXMLCurrentData\\SpecialTrips\\StFacilitiesST.xml");
		new MatsimPopulationReader(specialTrips).readFile("C:\\Users\\sergioo\\Documents\\2011\\Work\\FCL\\Operations\\Data\\MATSimXMLCurrentData\\SpecialTrips\\plansGVwtST.xml");
		for(Person person:specialTrips.getPopulation().getPersons().values())
			if(Math.random()<0.25)
				base25.getPopulation().addPerson(person);
		specialTrips = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new MatsimFacilitiesReader((ScenarioImpl)specialTrips).readFile("C:\\Users\\sergioo\\Documents\\2011\\Work\\FCL\\Operations\\Data\\MATSimXMLCurrentData\\SpecialTrips\\StFacilitiesST.xml");
		new MatsimPopulationReader(specialTrips).readFile("C:\\Users\\sergioo\\Documents\\2011\\Work\\FCL\\Operations\\Data\\MATSimXMLCurrentData\\SpecialTrips\\plansTouristwtST.xml");
		for(Person person:specialTrips.getPopulation().getPersons().values())
			if(Math.random()<0.25)
				base25.getPopulation().addPerson(person);
		new PopulationWriter(base25.getPopulation(), base25.getNetwork()).write("C:\\Users\\sergioo\\Documents\\2011\\Work\\FCL\\Operations\\Data\\MATSimXMLCurrentData\\SpecialTrips\\population25ST.xml.gz");
	}

	private static boolean mcycle(Person person) {
		for(Plan p:person.getPlans())
			for(PlanElement e:p.getPlanElements())
				if(e instanceof Leg)
					if(((Leg)e).getMode().contains("mcycle"))
						return true;
		return false;
	}

	//Attributes

	//Methods

}
