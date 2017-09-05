package playground.clruch.trb18.scenario.stages;

import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;

public class TRBPopulationDecimiser {
    final private Random random;

    public TRBPopulationDecimiser(Random random) {
        this.random = random;
    }

    public void decimise(Population population, long maximumNumberOfAgents) {
        List<Id<Person>> idList = population.getPersons().values().stream().map(p -> p.getId()).collect(Collectors.toList());

        while (population.getPersons().size() > maximumNumberOfAgents) {
            int index = random.nextInt(population.getPersons().size());
            Id<Person> personId = idList.remove(index);
            population.getPersons().remove(personId);
            population.getPersonAttributes().removeAllAttributes(personId.toString());
        }
    }
}
