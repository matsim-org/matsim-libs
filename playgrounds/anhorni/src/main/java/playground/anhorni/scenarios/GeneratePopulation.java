package playground.anhorni.scenarios;

import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PopulationImpl;

public class GeneratePopulation {
	
	public void generatePopulation(int populationSize, ExpenditureAssigner expenditureAssigner,  PopulationImpl staticPopulation,
			boolean expenditureFixed, int offset) {
		for (int i = 0; i < populationSize; i++) {
			PersonImpl p = new PersonImpl(new IdImpl(offset + i));

			int townId = 0;
			if (i >= (populationSize / 2)) {
				townId = 1;
			}
			p.getCustomAttributes().put("townId", townId);
			if (!expenditureFixed) {
				expenditureAssigner.assignExpenditureGaussian(p);
			}
			else {
				expenditureAssigner.assignExpenditureFixed(p);
			}
			staticPopulation.addPerson(p);
		}
	}

}
