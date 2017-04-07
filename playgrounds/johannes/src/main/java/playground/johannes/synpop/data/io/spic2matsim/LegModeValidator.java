package playground.johannes.synpop.data.io.spic2matsim;

import playground.johannes.synpop.data.CommonKeys;
import playground.johannes.synpop.data.Episode;
import playground.johannes.synpop.data.Person;
import playground.johannes.synpop.data.Segment;
import playground.johannes.synpop.processing.EpisodeTask;
import playground.johannes.synpop.processing.PersonTask;
import playground.johannes.synpop.processing.PersonsTask;
import playground.johannes.synpop.processing.SegmentTask;

import java.util.Collection;

/**
 * Created by johannesillenberger on 06.04.17.
 */
public class LegModeValidator implements SegmentTask, EpisodeTask, PersonTask, PersonsTask {

    private static final String DEFAULT_LEG_MODE = "undefined";

    @Override
    public void apply(Segment segment) {
        String mode = segment.getAttribute(CommonKeys.LEG_MODE);
        if(mode == null) segment.setAttribute(CommonKeys.LEG_MODE, DEFAULT_LEG_MODE);
    }

    @Override
    public void apply(Episode episode) {
        for(Segment leg : episode.getLegs()) {
            apply(leg);
        }
    }

    @Override
    public void apply(Person person) {
        for(Episode episode : person.getEpisodes()) {
            apply(episode);
        }
    }

    @Override
    public void apply(Collection<? extends Person> persons) {
        for(Person person : persons) apply(person);
    }
}
