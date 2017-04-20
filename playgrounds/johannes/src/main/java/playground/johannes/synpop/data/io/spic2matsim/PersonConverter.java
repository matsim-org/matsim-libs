package playground.johannes.synpop.data.io.spic2matsim;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.*;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.ActivityFacility;
import org.matsim.utils.objectattributes.ObjectAttributes;
import playground.johannes.synpop.data.CommonKeys;
import playground.johannes.synpop.data.Episode;
import playground.johannes.synpop.data.Segment;

import java.util.Collection;
import java.util.Set;

/**
 * Created by johannesillenberger on 06.04.17.
 */
public class PersonConverter {

    private final Population population;

    private final ActivityFacilities facilities;

    public PersonConverter(Population population, ActivityFacilities facilities) {
        this.population = population;
        this.facilities = facilities;
    }

    public Population convert(Set<? extends playground.johannes.synpop.data.Person> persons) {
        for(playground.johannes.synpop.data.Person person : persons) {
            convert(person);
        }
        return population;
    }

    public Person convert(playground.johannes.synpop.data.Person person) {
        PopulationFactory factory = population.getFactory();
        ObjectAttributes attributes = population.getPersonAttributes();
        /*
        Create person and add to population.
         */
        Person matsimPerson = factory.createPerson(Id.create(person.getId(), Person.class));
        population.addPerson(matsimPerson);
        /*
        Transfer attributes.
         */
        for(String key : person.keys()) {
            attributes.putAttribute(person.getId(), key, person.getAttribute(key));
        }
        /*
        Convert episodes.
         */
        Collection<? extends Episode> episodes = person.getEpisodes();
        for (Episode episode : episodes) {
            /*
            Create and add plan.
             */
            Plan matsimPlan = factory.createPlan();
            matsimPerson.addPlan(matsimPlan);
            /*
            Insert activities and legs.
             */
            for (int i = 0; i < episode.getActivities().size(); i++) {
                Segment actSegment = episode.getActivities().get(i);
                /*
                Create and add activity.
                 */
                String type = actSegment.getAttribute(CommonKeys.ACTIVITY_TYPE);
                ActivityFacility facility = facilities.getFacilities().get(
                        Id.create(actSegment.getAttribute(CommonKeys.ACTIVITY_FACILITY), ActivityFacility.class));
                Activity matsimAct = factory.createActivityFromCoord(type, facility.getCoord());
                matsimAct.setFacilityId(facility.getId());
                matsimPlan.addActivity(matsimAct);
                /*
                Transfer attributes.
                 */
                String startTime = actSegment.getAttribute(CommonKeys.ACTIVITY_START_TIME);
                matsimAct.setStartTime(Integer.parseInt(startTime));

                String endTime = actSegment.getAttribute(CommonKeys.ACTIVITY_END_TIME);
                matsimAct.setEndTime(Integer.parseInt(endTime));
                /*
                Create and add leg.
                 */
                if (i < episode.getLegs().size()) {
                    Segment legSegment = episode.getLegs().get(i);
                    String mode = legSegment.getAttribute(CommonKeys.LEG_MODE);
                    Leg matsimLeg = factory.createLeg(mode);
                    matsimPlan.addLeg(matsimLeg);
                    /*
                    Transfer attributes
                     */
                    matsimLeg.setDepartureTime(Double.parseDouble(legSegment.getAttribute(CommonKeys.LEG_START_TIME)));
                }
            }
        }

        return matsimPerson;
    }
}
