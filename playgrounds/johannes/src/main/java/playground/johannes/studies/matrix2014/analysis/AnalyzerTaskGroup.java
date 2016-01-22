/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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

package playground.johannes.studies.matrix2014.analysis;

import org.apache.log4j.Logger;
import playground.johannes.synpop.analysis.AnalyzerTask;
import playground.johannes.synpop.analysis.FileIOContext;
import playground.johannes.synpop.analysis.StatsContainer;

import java.util.List;

/**
 * @author johannes
 */
public class AnalyzerTaskGroup<T> implements AnalyzerTask<T> {

    private static final Logger logger = Logger.getLogger(AnalyzerTaskGroup.class);

    private final FileIOContext ioContext;

    private final String name;

    private final AnalyzerTask<T> task;

    public AnalyzerTaskGroup(AnalyzerTask<T> task, FileIOContext ioContext, String name) {
        this.task = task;
        this.ioContext = ioContext;
        this.name = name;
    }

    @Override
    public void analyze(T object, List<StatsContainer> containers) {
        String appendix = ioContext.getPath().substring(ioContext.getRoot().length());
        ioContext.append(String.format("%s/%s", appendix, name));
        logger.trace(String.format("Executing group %s...", name));
        task.analyze(object, containers);
        ioContext.append(appendix);
    }
}
