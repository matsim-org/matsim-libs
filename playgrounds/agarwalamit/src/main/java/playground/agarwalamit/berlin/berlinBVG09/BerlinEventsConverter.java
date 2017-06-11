/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2017 by the members listed in the COPYING,        *
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

package playground.agarwalamit.berlin.berlinBVG09;

import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsConverterXML;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.algorithms.EventWriterXML;
import playground.agarwalamit.utils.FileUtils;

/**
 * Created by amit on 09.06.17.
 */


public class BerlinEventsConverter {

    private static final String eventsFile = FileUtils.RUNS_SVN+"/berlin-bvg09/bvg.run189.10pct/ITERS/it.100/bvg.run189.10pct.100.events.filtered.xml.gz";
    private static final String outEventsFile = FileUtils.RUNS_SVN+"/berlin-bvg09/bvg.run189.10pct/emissionsRelatedFiles/bvg.run189.10pct.100.eventsWithNetworkModeInEvents.xml.gz";

    public static void main(String[] args) {

        EventsManager em = EventsUtils.createEventsManager();
        EventWriterXML eventWriter = new EventWriterXML(outEventsFile);
        em.addHandler(eventWriter);

        EventsConverterXML converter = new EventsConverterXML(em);
        converter.readFile(eventsFile);

        eventWriter.closeFile();
    }
}
