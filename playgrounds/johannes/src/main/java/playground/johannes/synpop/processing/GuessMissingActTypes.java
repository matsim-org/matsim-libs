/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

package playground.johannes.synpop.processing;

import gnu.trove.iterator.TObjectIntIterator;
import gnu.trove.map.hash.TObjectIntHashMap;
import org.apache.log4j.Logger;
import org.matsim.contrib.common.collections.ChoiceSet;
import playground.johannes.synpop.data.*;

import java.util.Collection;
import java.util.Random;

/**
 * @author johannes
 */
public class GuessMissingActTypes implements PersonsTask {

    private final static Logger logger = Logger.getLogger(GuessMissingActTypes.class);

    private final Random random;

    public GuessMissingActTypes(Random random) {
        this.random = random;
    }

    @Override
    public void apply(Collection<? extends Person> persons) {
        TObjectIntHashMap<String> counts = new TObjectIntHashMap<>();

        for (Person p : persons) {
            for (Episode e : p.getEpisodes()) {
                for (Segment s : e.getActivities()) {
                    String type = s.getAttribute(CommonKeys.ACTIVITY_TYPE);
                    if (type != null) counts.adjustOrPutValue(type, 1, 1);
                }
            }
        }

        ChoiceSet<String> set = new ChoiceSet<>(random);

        TObjectIntIterator<String> it = counts.iterator();
        for (int i = 0; i < counts.size(); i++) {
            it.advance();
            set.addOption(it.key(), it.value());
        }
        set.removeOption(ActivityTypes.HOME);

        int countHome = 0;
        int countNonHome = 0;

        for (Person p : persons) {
            for (Episode e : p.getEpisodes()) {
                for (int i = 0; i < e.getActivities().size(); i++) {
                    Segment act = e.getActivities().get(i);
                    String type = act.getAttribute(CommonKeys.ACTIVITY_TYPE);
                    if (type == null) {
                        if(i == 0 || i == e.getActivities().size() - 1) {
                            type = ActivityTypes.HOME;
                            countHome++;
                        } else {
                            type = set.randomWeightedChoice();
                            countNonHome++;
                        }
                        act.setAttribute(CommonKeys.ACTIVITY_TYPE, type);
                    }
                }
            }
        }

        logger.info(String.format("Inserted %s home activity types.", countHome));
        logger.info(String.format("Inserted %s non-home activity types.", countNonHome));
    }
}
