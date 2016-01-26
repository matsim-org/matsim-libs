/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,       *
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
package playground.johannes.synpop.analysis;

import org.matsim.contrib.common.stats.LinearDiscretizer;
import playground.johannes.synpop.data.CommonKeys;
import playground.johannes.synpop.data.CommonValues;
import playground.johannes.synpop.data.Episode;
import playground.johannes.synpop.data.Person;

/**
 * @author jillenberger
 */
public class TripsPerPersonTask {

    public NumericAnalyzer build(FileIOContext ioContext) {
        ValueProvider<Double, Episode> provider = new TripsCounter(new ModePredicate(CommonValues.LEG_MODE_CAR));
        EpisodeCollector<Double> collector = new EpisodeCollector<>(provider);

        DiscretizerBuilder builder = new PassThroughDiscretizerBuilder(new LinearDiscretizer(1.0), "linear");
        HistogramWriter writer = new HistogramWriter(ioContext, builder);

        ValueProvider<Double, Person> weightsProvider = new NumericAttributeProvider<>(CommonKeys.PERSON_WEIGHT);
        EpisodePersonCollector<Double> weightsCollector = new EpisodePersonCollector<>(weightsProvider);

        NumericAnalyzer analyzer = new NumericAnalyzer(collector, weightsCollector, "nTrips", writer);

        return analyzer;
    }

}
