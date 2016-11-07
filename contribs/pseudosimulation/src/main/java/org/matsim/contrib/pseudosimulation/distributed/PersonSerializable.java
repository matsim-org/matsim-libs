
package org.matsim.contrib.pseudosimulation.distributed;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.population.PersonUtils;
import org.matsim.core.population.PopulationUtils;

public class PersonSerializable implements Serializable {
    protected List<PlanSerializable> plans = new ArrayList<>(5);

    public PersonSerializable(Person p) {
        this.id = p.getId().toString();
        Person person = p;

        for (Plan plan : person.getPlans()) {
            PlanSerializable planSerializable = new PlanSerializable(plan);
            plans.add(planSerializable);
            if (plan.equals(person.getSelectedPlan()))
                this.selectedPlan = planSerializable;
        }
    }

    protected String id;

    PlanSerializable selectedPlan = null;
    private TreeSet<String> travelcards = null;

    private Boolean isEmployed;

    public Person getPerson() {
        Person person = PopulationUtils.getFactory().createPerson(Id.createPersonId(id));

        for (PlanSerializable planSer : plans) {
            Plan plan = planSer.getPlan(person);
            person.addPlan(plan);
            if(planSer.equals(selectedPlan))
                person.setSelectedPlan(plan);

        }
        return person;
    }
}
