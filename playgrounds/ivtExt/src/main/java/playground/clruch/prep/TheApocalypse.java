// code by jph
package playground.clruch.prep;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;

import ch.ethz.idsc.queuey.util.GlobalAssert;

/**
 * example use:
 * 
 * TheApocalypse.decimatesThe(population).toNoMoreThan(1000).people();
 */
public final class TheApocalypse {
    public static TheApocalypse decimatesThe(Population population) {
        return new TheApocalypse(population);
    }

    private final Population population;

    private TheApocalypse(Population population) {
        this.population = population;
    }

    public TheApocalypse toNoMoreThan(int capacityOfArk) {
        List<Id<Person>> list = new ArrayList<>(population.getPersons().keySet());
        Collections.shuffle(list, new Random(7582456789l));
        final int sizeAnte = list.size();
        list.stream() //
                .limit(Math.max(0, sizeAnte - capacityOfArk)) //
                .forEach(population::removePerson);
        final int sizePost = population.getPersons().size();
        GlobalAssert.that(sizePost <= capacityOfArk);
        return this;
    }

    public final void people() {
        System.out.println("Population size: " + population.getPersons().values().size());
    }
}
