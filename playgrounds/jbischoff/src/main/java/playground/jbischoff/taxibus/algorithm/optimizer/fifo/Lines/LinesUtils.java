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

package playground.jbischoff.taxibus.algorithm.optimizer.fifo.Lines;

import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.dvrp.data.VrpData;
import org.matsim.contrib.zone.*;
import org.matsim.core.utils.io.tabularFileParser.*;

import playground.jbischoff.taxibus.run.configuration.TaxibusConfigGroup;

/**
 * @author  jbischoff
 *
 */
public class LinesUtils {

	public static LineDispatcher createLineDispatcher(String linesFile, String zonesXml, String zonesShp, VrpData vrpData, final TaxibusConfigGroup tbcg){
		final LineDispatcher dispatcher = new LineDispatcher(vrpData,tbcg);
		final Map<Id<Zone>,Zone> zones = Zones.readZones(zonesXml, zonesShp);
		
		TabularFileParserConfig config = new TabularFileParserConfig();
        config.setDelimiterTags(new String[] {"\t"});
        config.setFileName(linesFile);
        config.setCommentTags(new String[] { "#" });
        new TabularFileParser().parse(config, new TabularFileHandler() {
			
			@Override
			public void startRow(String[] row) {
				String from = row[0];
				String to = row[1];
				Id<TaxibusLine> id = Id.create(from+"_"+to,TaxibusLine.class);
				Id<Link> holdingPosition = Id.createLinkId(row[2]);
				double twMax = Double.parseDouble(row[3]);
				TaxibusLine line = new TaxibusLineImpl(id,holdingPosition, zones.get(Id.create(from,Zones.class)).getMultiPolygon(), zones.get(Id.create(to,Zones.class)).getMultiPolygon(),twMax,tbcg.getNumberOfVehiclesDispatchedAtSameTime(),tbcg.getVehCap());
				Id<TaxibusLine> rid = Id.create(to+"_"+from,TaxibusLine.class);
				
				line.setReturnRouteId(rid);
				System.out.println("line "+line.getId()+" added ");
				dispatcher.addLine(line);
			}
		});
        
        
		
		
		return dispatcher;
	}
}
