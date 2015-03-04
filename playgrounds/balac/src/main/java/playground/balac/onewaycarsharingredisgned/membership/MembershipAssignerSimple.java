package playground.balac.onewaycarsharingredisgned.membership;

import java.util.Map;
import java.util.TreeMap;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PopulationReader;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.utils.objectattributes.ObjectAttributes;
import org.matsim.utils.objectattributes.ObjectAttributesXmlReader;
import org.matsim.utils.objectattributes.ObjectAttributesXmlWriter;

public class MembershipAssignerSimple {

	
final private int numMembers = 20850;
	
	private static double[] ageShares = {0.03 , 0.68 , 0.98, 1.0};
	
	private static double menShare = 0.44;
	public void run (String[] args) {
		
		ObjectAttributes bla = new ObjectAttributes();
		
		new ObjectAttributesXmlReader(bla).parse(args[0]);		
		
		ScenarioImpl scenario = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		PopulationReader populationReader = new MatsimPopulationReader(scenario);
		MatsimNetworkReader networkReader = new MatsimNetworkReader(scenario);
		networkReader.readFile(args[1]);
		populationReader.readFile(args[2]);
		
		//randomly choose a person and run the simple model
		
		Object[] personArray = scenario.getPopulation().getPersons().values().toArray();
		
		int numberOfPersons = personArray.length;
		
		Map<Id, Person> addedMembers = new TreeMap<Id, Person>();
		
		int i = 0;
		
		Map<Id, Person> men = new TreeMap<Id, Person>();

		Map<Id, Person> women = new TreeMap<Id, Person>();

		
		//split the agents into men and women groups
		
		for (Person p: scenario.getPopulation().getPersons().values()) {
			
			if (((PersonImpl)p).getSex().equals("m")) 
				men.put(p.getId(), p);
			else
				women.put(p.getId(), p);
		}
		
		Object[] mArray =  men.values().toArray();
		
		Object[] fArray =  women.values().toArray();


		
		while(i < numMembers) {
			
			
			
			double randomDouble = MatsimRandom.getRandom().nextDouble();
			
			if (randomDouble < menShare) {
				
				randomDouble = MatsimRandom.getRandom().nextDouble();
				
				if (randomDouble < ageShares[0]) {
					
					Person addP = findPerson(0, addedMembers, mArray);
				}
				else if (randomDouble < ageShares[1]) {
					Person addP = findPerson(1, addedMembers, mArray);

				}
				else if (randomDouble < ageShares[2]) {
					Person addP = findPerson(1, addedMembers, mArray);

				}
				else if (randomDouble < ageShares[3]) {
					Person addP = findPerson(1, addedMembers, mArray);

				}
				
				
				
			}
			
			else {
				
				randomDouble = MatsimRandom.getRandom().nextDouble();

				
				if (randomDouble < ageShares[0]) {
					
					Person addP = findPerson(0, addedMembers, fArray);
				}
				else if (randomDouble < ageShares[1]) {
					Person addP = findPerson(1, addedMembers, fArray);

				}
				else if (randomDouble < ageShares[2]) {
					Person addP = findPerson(1, addedMembers, fArray);

				}
				else if (randomDouble < ageShares[3]) {
					Person addP = findPerson(1, addedMembers, fArray);

				}
				
			}
			
			i++;
			
		}
		
		for (Person p : scenario.getPopulation().getPersons().values())
			bla.putAttribute(p.getId().toString(), "OW_CARD", "false");

		
		for (Person p: addedMembers.values()){
			
			bla.putAttribute(p.getId().toString(), "OW_CARD", "true");
			
		}
		new PopulationWriter(scenario.getPopulation(), scenario.getNetwork()).writeFileV5(args[3] + "/plans_100perc_noride.xml.gz");		
		ObjectAttributesXmlWriter betaWriter = new ObjectAttributesXmlWriter(bla);
		betaWriter.writeFile(args[3] + "/desires_ow_memb_100perc.xml.gz");
		
	}

	private Person findPerson (int index, Map<Id, Person> addedMembers, Object[] potentialMembers) {
		
		boolean notFound = true;
		
		while (notFound) {
			
			int randomInt = MatsimRandom.getRandom().nextInt(potentialMembers.length);
			Person p = (Person)potentialMembers[randomInt];
			
			if (!addedMembers.containsKey(p.getId())) {
				
				switch(index) {
				
					case 0: if (18 <= ((PersonImpl)p).getAge() && ((PersonImpl)p).getAge() <= 24) {
					
								addedMembers.put(p.getId(), p);
								notFound = false;
							}
					case 1: if (25 <= ((PersonImpl)p).getAge() && ((PersonImpl)p).getAge() <= 44) {
						
						addedMembers.put(p.getId(), p);
						notFound = false;
					}
				
					case 2: if (45 <= ((PersonImpl)p).getAge() && ((PersonImpl)p).getAge() <= 64) {
						
						addedMembers.put(p.getId(), p);
						notFound = false;
					}
					case 3: if (55 <= ((PersonImpl)p).getAge() && ((PersonImpl)p).getAge() <= 64) {
						
						addedMembers.put(p.getId(), p);
						notFound = false;
					}
				}
				
			}
			
			
		}
		
		
		return null;
		
	}
		
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		MembershipAssignerSimple mem = new MembershipAssignerSimple();
		mem.run(args);
		

	}

}
