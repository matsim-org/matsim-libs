package playground.balac.utils;

import java.util.Map;
import java.util.TreeMap;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.*;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.utils.objectattributes.ObjectAttributes;
import org.matsim.utils.objectattributes.ObjectAttributesXmlReader;
import org.matsim.utils.objectattributes.ObjectAttributesXmlWriter;

public class MembershipGenerationFreeFloating {
	
	final private int numMembers = 6000;
	
	private static double[] ageShares = {0.107 , 0.614 , 0.89, 0.987 , 1.0};
	
	private static double menShare = 0.8;
	public void run (String[] args) {
		
		MutableScenario scenario = (MutableScenario) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		PopulationReader populationReader = new MatsimPopulationReader(scenario);
		MatsimNetworkReader networkReader = new MatsimNetworkReader(scenario.getNetwork());
		networkReader.readFile(args[0]);
		populationReader.readFile(args[1]);
		ObjectAttributes bla = new ObjectAttributes();
		
		new ObjectAttributesXmlReader(bla).parse(args[2]);
		int h = 0;
		for(Person p : scenario.getPopulation().getPersons().values()) {
			if (withinBorders(p))
				h++;
			if ( ((String) bla.getAttribute(p.getId().toString(), "FF_CARD")).equals("true")) {
				bla.putAttribute(p.getId().toString(), "FF_CARD", "false");

			}
			
		}
		ObjectAttributesXmlWriter betaWriterAll = new ObjectAttributesXmlWriter(bla);
	//	betaWriterAll.writeFile("C:/Users/balacm/Desktop/personAttrinutesAllFFMembers.xml.gz");
		//randomly choose a person and run the simple model
		
		Object[] personArray = scenario.getPopulation().getPersons().values().toArray();
		
		int numberOfPersons = personArray.length;
		
		Map<Id, Person> addedMembers = new TreeMap<Id, Person>();
		
		int i = 0;
		
		Map<Id, Person> men = new TreeMap<Id, Person>();

		Map<Id, Person> women = new TreeMap<Id, Person>();

		
		//split the agents into men and women groups
		
		for (Person p: scenario.getPopulation().getPersons().values()) {
			if (withinBorders(p)){
				if (PersonUtils.getSex(p).equals("m"))
					men.put(p.getId(), p);
				else
					women.put(p.getId(), p);
			}
		}
		
		Object[] mArray =  men.values().toArray();
		
		Object[] fArray =  women.values().toArray();


		
		while(i < numMembers) {
			
			System.out.println(i);
			
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
				else {
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
				else {
					Person addP = findPerson(1, addedMembers, fArray);
				}
				
			}
			
			i++;
			
		}
		
		int j = 0;
		
		for (Person p: addedMembers.values()){
			
			System.out.println(j++);
			bla.putAttribute(p.getId().toString(), "FF_CARD", "true");
		}
		
		ObjectAttributesXmlWriter betaWriter = new ObjectAttributesXmlWriter(bla);
		betaWriter.writeFile("C:/Users/balacm/Desktop/personAttrinutes10kmZoneFF_1xmemb.xml.gz");
		
	}

	private boolean withinBorders(Person p) {
		double centerX = 683217.0; 
		double centerY = 247300.0;
		Coord coord = new Coord(centerX, centerY);

		boolean insideBorders = false;
		
		Activity a = (Activity) p.getSelectedPlan().getPlanElements().get(0);
		if (CoordUtils.calcEuclideanDistance(a.getCoord(), coord) < 10000)
			insideBorders = true;
		return insideBorders;
	}

	private Person findPerson (int index, Map<Id, Person> addedMembers, Object[] potentialMembers) {
		
		boolean notFound = true;
		
		while (notFound) {
			
			int randomInt = MatsimRandom.getRandom().nextInt(potentialMembers.length);
			Person p = (Person)potentialMembers[randomInt];
			
			if (!addedMembers.containsKey(p.getId())) {
				
				switch(index) {
				
					case 0: if (18 <= PersonUtils.getAge(p) && PersonUtils.getAge(p) <= 24) {
					
								addedMembers.put(p.getId(), p);
								notFound = false;
							}
					case 1: if (25 <= PersonUtils.getAge(p) && PersonUtils.getAge(p) <= 34) {
						
						addedMembers.put(p.getId(), p);
						notFound = false;
					}
					case 2: if (35 <= PersonUtils.getAge(p) && PersonUtils.getAge(p) <= 44) {
						
						addedMembers.put(p.getId(), p);
						notFound = false;
					}
					case 3: if (45 <= PersonUtils.getAge(p) && PersonUtils.getAge(p) <= 54) {
						
						addedMembers.put(p.getId(), p);
						notFound = false;
					}
					case 4: if (55 <= PersonUtils.getAge(p) && PersonUtils.getAge(p) <= 64) {
						
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
				
		MembershipGenerationFreeFloating mgff = new MembershipGenerationFreeFloating();
		mgff.run(args);

	}

}
