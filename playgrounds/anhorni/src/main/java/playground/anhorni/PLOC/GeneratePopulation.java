package playground.anhorni.PLOC;

import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PopulationImpl;

public class GeneratePopulation {
			
	public GeneratePopulation() {
	}
	
	public void generatePopulation(int populationSize, ExpenditureAssigner expenditureAssigner,  PopulationImpl staticPopulation) {
		
		for (int i = 0; i < populationSize; i++) {
			PersonImpl p = new PersonImpl(new IdImpl(i));

			// assign home town
			int townId = 0;
			if (i >= (populationSize / 2)) {
				townId = 1;
			}
			p.getCustomAttributes().put("townId", townId);
			staticPopulation.addPerson(p);
		}
		expenditureAssigner.assignExpenditures(staticPopulation);
	}
}
