package playground.johannes.synpop.data.io.spic2matsim;

import playground.johannes.synpop.data.CommonKeys;
import playground.johannes.synpop.data.Episode;
import playground.johannes.synpop.data.Segment;
import playground.johannes.synpop.processing.EpisodeTask;

/**
 * Created by johannesillenberger on 06.04.17.
 */
public class ActTimeValidator implements EpisodeTask {

    private static final int DEFAULT_OFFSET = 28800; // 08:00

    private static final int DEFAULT_STEP = 3600; // 1h

    @Override
    public void apply(Episode episode) {
        double offset = DEFAULT_OFFSET;
        for(Segment act : episode.getActivities()) {
            String start = act.getAttribute(CommonKeys.ACTIVITY_START_TIME);
            if(start == null) {
                offset += DEFAULT_STEP;
                act.setAttribute(CommonKeys.ACTIVITY_START_TIME, String.valueOf(offset));
            } else {
                offset = Double.parseDouble(start);
            }

            String end = act.getAttribute(CommonKeys.ACTIVITY_END_TIME);
            if(end == null) {
                offset += DEFAULT_STEP;
                act.setAttribute(CommonKeys.ACTIVITY_END_TIME, String.valueOf(offset));
            } else {
                offset = Double.parseDouble(end);
            }
        }
    }
}
