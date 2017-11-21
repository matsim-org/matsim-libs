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

/**
 * 
 */
package wobscenario.network;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.utils.io.tabularFileParser.TabularFileHandler;
import org.matsim.core.utils.io.tabularFileParser.TabularFileParser;
import org.matsim.core.utils.io.tabularFileParser.TabularFileParserConfig;
import org.matsim.counts.Count;
import org.matsim.counts.Counts;
import org.matsim.counts.CountsWriter;
import org.matsim.counts.MatsimCountsReader;


/**
 * @author  jbischoff
 *
 */
/**
 *
 */
public class BSCountCreator {
/**
 * 
 */
public static void main(String[] args) {
	Counts<Link> counts = new Counts<>(); 
	new MatsimCountsReader(counts).readFile("C:/Users/Joschka/Documents/shared-svn/projects/vw_rufbus/projekt2/input/network/counts_added.xml");
	
	TabularFileParserConfig config = new TabularFileParserConfig();
	config.setDelimiterRegex(";");
	config.setFileName("C:/Users/Joschka/Documents/shared-svn/projects/vw_rufbus/projekt2/input/network/LoopsCounts_1hr_LinkID.csv");
	TabularFileParser parser = new  TabularFileParser();
	TabularFileHandler handler = new TabularFileHandler() {
	
		@Override
		public void startRow(String[] row) {
			Id<Link> linkId = Id.createLinkId(row[5]);
			Count<Link> count = counts.getCount(linkId);
			if (count== null){
				counts.createAndAddCount(linkId, row[0]);
				count = counts.getCount(linkId);
			}
			int time = Integer.parseInt(row[2]) + 1;
			double countvalue = Double.parseDouble(row[3]);
			count.createVolume(time, countvalue);
		}
		
		
	};
	parser.parse(config, handler);
	
	new CountsWriter(counts).write("C:/Users/Joschka/Documents/shared-svn/projects/vw_rufbus/projekt2/input/network/counts_added_bs.xml");
	
}
}
