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

package playground.johannes.gsv.matrices.episodes2matrix;

import gnu.trove.TObjectIntHashMap;
import gnu.trove.TObjectIntIterator;
import playground.johannes.coopsim.mental.choice.ChoiceSet;
import playground.johannes.socialnetworks.utils.XORShiftRandom;
import playground.johannes.synpop.data.Person;
import playground.johannes.synpop.processing.PersonsTask;
import playground.johannes.synpop.source.mid2008.MiDKeys;

import java.util.Collection;

/**
 * @author johannes
 */
public class ImputeMonth implements PersonsTask {

    @Override
    public void apply(Collection<? extends Person> persons) {
        TObjectIntHashMap<String> monthCounts = new TObjectIntHashMap<>();

        for(Person person : persons) {
            String month = person.getAttribute(MiDKeys.PERSON_MONTH);
            if(month != null) {
                monthCounts.adjustOrPutValue(month, 1, 1);
            }
        }

        ChoiceSet<String> months = new ChoiceSet<>(new XORShiftRandom());
        TObjectIntIterator<String> it = monthCounts.iterator();
        for(int i = 0; i < monthCounts.size(); i++) {
            it.advance();
            months.addChoice(it.key(), it.value());
        }

        for(Person person : persons) {
            if(person.getAttribute(MiDKeys.PERSON_MONTH) == null) {
                person.setAttribute(MiDKeys.PERSON_MONTH, months.randomWeightedChoice());
            }
        }
    }
}
