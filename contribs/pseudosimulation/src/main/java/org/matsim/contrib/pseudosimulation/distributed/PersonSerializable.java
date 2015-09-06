
package org.matsim.contrib.pseudosimulation.distributed;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.population.PersonImpl;
import org.matsim.population.Desires;

public class PersonSerializable implements Serializable {
    protected List<PlanSerializable> plans = new ArrayList<>(5);

    public PersonSerializable(Person p) {
        this.id = p.getId().toString();
        Person person = p;
        this.sex = PersonUtils.getSex(person);
        this.age = PersonUtils.getAge(person);
        this.hasLicense = PersonUtils.getLicense(person);
        this.carAvail = PersonUtils.getCarAvail(person);
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

    private Boolean isEmployed;

    public Person getPerson() {
        Person person = PersonImpl.createPerson(Id.createPersonId(id));
        PersonUtils.setAge(person, age);
        PersonUtils.setCarAvail(person, carAvail);

        PersonUtils.setEmployed(person, isEmployed);
        PersonUtils.setLicence(person, hasLicense);
        PersonUtils.setSex(person, sex);
        for (PlanSerializable planSer : plans) {
            Plan plan = planSer.getPlan(person);
            person.addPlan(plan);
            if(planSer.equals(selectedPlan))
                person.setSelectedPlan(plan);

        }
        return person;
    }
}
