
package playground.pieter.distributed;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.replanning.selectors.RandomPlanSelector;
import org.matsim.population.Desires;
import playground.pieter.distributed.plans.PersonForPlanGenomes;

public class PersonSerializable implements Serializable {
    protected List<PlanSerializable> plans = new ArrayList<>(5);

    public PersonSerializable(PersonImpl person) {
        this.id = person.getId().toString();
        this.sex = person.getSex();
        this.age = person.getAge();
        this.hasLicense = person.getLicense();
        this.carAvail = person.getCarAvail();
        for (Plan plan : person.getPlans()) {
            PlanSerializable planSerializable = new PlanSerializable(plan);
            plans.add(planSerializable);
            if (plan.equals(person.getSelectedPlan()))
                this.selectedPlan = planSerializable;
        }

    }

    protected String id;
    private String sex;
    private int age = Integer.MIN_VALUE;
    private String hasLicense;
    private String carAvail;
    PlanSerializable selectedPlan = null;
    private TreeSet<String> travelcards = null;
    protected Desires desires = null;

    private Boolean isEmployed;

    public Person getPerson() {
        PersonForPlanGenomes person = new PersonForPlanGenomes(Id.createPersonId(id));
        person.setAge(age);
        person.setCarAvail(carAvail);

        person.setEmployed(isEmployed);
        person.setLicence(hasLicense);
        person.setSex(sex);
        for (PlanSerializable planSer : plans) {
            Plan plan = planSer.getPlan();
            person.addPlan(plan);
            if(planSer.equals(selectedPlan))
                person.setSelectedPlan(plan);

        }
        return person;
    }
}
