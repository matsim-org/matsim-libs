package playground.mmoyo.algorithms;

import java.io.File;

import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.basic.v01.IdImpl;

import playground.mmoyo.utils.DataLoader;
import playground.mmoyo.utils.FirstPersonsExtractor;

/** adds a suffix to all persons id's*/
public class PersonIdSuffixAdder {
	final String affix;
	
	public PersonIdSuffixAdder (final String affix){
		this.affix = affix;
	}
	
	public void addSuffix(Population pop){
		for (Person person: pop.getPersons().values()){
			String newId = person.getId().toString()+ this.affix;
			person.setId(new IdImpl(newId));
		}
	}
	
	public void addPrefix(Population pop){
		for (Person person: pop.getPersons().values()){
			String newId = this.affix+ person.getId().toString();
			person.setId(new IdImpl(newId));
		}
	}
	
	public static void main(String[] args) {
		String netFilePath = "../../berlin-bvg09/pt/nullfall_berlin_brandenburg/input/network_multimodal.xml.gz";
		String popFilePath= "../../input/rou_mut/ro_mut10_0_240__8_5_720__6_0_1200.xml.gz";
		String output = "../../input/rou_mut/ro_mut10_0_240__8_5_720__6_0_1200.xml.gz";
		String suffix = "_2";
		
		DataLoader dataLoader = new DataLoader();
		Population pop= dataLoader.readPopulation(popFilePath);
		new PersonIdSuffixAdder(suffix).addSuffix(pop);

		//write
		Network net = dataLoader.readNetwork(netFilePath);
		PopulationWriter popWriter = new PopulationWriter(pop, net);
		popWriter.write(output);
		
		//write a sample
		popWriter = new PopulationWriter(new FirstPersonsExtractor().run(pop,4), net);
		File file = new File (output);
		popWriter.write(file.getParent() + "/samplePlans.xml") ;
		
	}

}
