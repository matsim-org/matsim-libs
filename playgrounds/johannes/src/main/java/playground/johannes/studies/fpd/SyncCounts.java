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

package playground.johannes.studies.fpd;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.network.Link;
import org.matsim.counts.Count;
import org.matsim.counts.Counts;
import org.matsim.counts.CountsWriter;
import org.matsim.counts.MatsimCountsReader;

import java.util.HashSet;
import java.util.Set;

/**
 * @author johannes
 */
public class SyncCounts {

    private static final Logger logger = Logger.getLogger(SyncCounts.class);

    public static void main(String args[]) {
        Counts<Link> refCounts = new Counts<>();
        Counts<Link> newCounts = new Counts<>();

        MatsimCountsReader reader = new MatsimCountsReader(refCounts);
        reader.readFile("/Volumes/johannes/sge/prj/matrix2014/data/counts/counts.2014.net20140909.5.24h.xml");

        reader = new MatsimCountsReader(newCounts);
        reader.readFile("/Users/johannes/gsv/fpd/telefonica/032016/analysis/20160623/counts.2015.aug-sep.xml");

        Set<String> refLinks = new HashSet<>();
        for(Count<Link> count : refCounts.getCounts().values()) {
            refLinks.add(count.getId().toString());
        }

        Set<Count> remove = new HashSet<>();
        for(Count<Link> count : newCounts.getCounts().values()) {
            String id = count.getId().toString();
            if(!refLinks.contains(id)) {
                remove.add(count);
            }
        }

        logger.info(String.format("Found %s counts for removal.", remove.size()));

        int before = newCounts.getCounts().size();
        for(Count<Link> count : remove) {
            newCounts.getCounts().remove(count.getId());
        }
        int after = newCounts.getCounts().size();
        logger.info(String.format("Removed %s counts.", before-after));

        CountsWriter writer = new CountsWriter(newCounts);
        writer.write("/Users/johannes/gsv/fpd/telefonica/032016/analysis/20160623/counts.2015.aug-sep.2.xml");
    }
}
