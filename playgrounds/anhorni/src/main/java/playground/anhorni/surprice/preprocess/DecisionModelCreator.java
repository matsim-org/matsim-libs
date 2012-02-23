package playground.anhorni.surprice.preprocess;

import org.matsim.core.population.PersonImpl;

import playground.anhorni.surprice.AgentMemory;
import playground.anhorni.surprice.DecisionModel;

public class DecisionModelCreator {
	
	public DecisionModel createDecisionModelForAgent(PersonImpl person, AgentMemory memory) {
		DecisionModel model = new DecisionModel();
		model.setMemory(memory);
		
		model.setFrequency("work", "Mon-Fri", 1);
		model.setFrequency("shop", "Mon-Fri", 0.2);
		model.setFrequency("leisure", "Mon-Fri", 0.2);
		
		model.setFrequency("work", "Sat", 0);
		model.setFrequency("shop", "Sat", 1);
		model.setFrequency("leisure", "Sat", 0);
		
		model.setFrequency("work", "Sun", 0);
		model.setFrequency("shop", "Sun", 0);
		model.setFrequency("leisure", "Sun", 1);
		
		return model;
	}
}
