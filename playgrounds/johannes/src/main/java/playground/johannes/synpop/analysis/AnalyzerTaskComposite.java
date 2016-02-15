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

import org.apache.log4j.Logger;
import org.matsim.contrib.common.collections.Composite;

import java.util.List;

/**
 * @author jillenberger
 */
public class AnalyzerTaskComposite<T> extends Composite<AnalyzerTask<T>> implements AnalyzerTask<T> {

    private static final Logger logger = Logger.getLogger(AnalyzerTaskComposite.class);

    @Override
    public void analyze(T object, List<StatsContainer> containers) {
        for(AnalyzerTask<T> task : components) {
            logger.trace(String.format("Executing task %s...", task.getClass().getSimpleName()));
            task.analyze(object, containers);
        }
        logger.trace("All tasks executed.");
    }
}
