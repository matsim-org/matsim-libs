/* *********************************************************************** *
 * project: org.matsim.*
 * ObservedAnalyzerTask.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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
package playground.johannes.studies.sbsurvey.analysis;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.socnetgen.sna.gis.ZoneLayer;
import org.matsim.contrib.socnetgen.sna.graph.analysis.AnalyzerTaskArray;
import org.matsim.contrib.socnetgen.sna.graph.analysis.AnalyzerTaskComposite;

import java.util.Set;

/**
 * @author illenberger
 *
 */
public class ObservedAnalyzerTask extends AnalyzerTaskComposite {
	
	public ObservedAnalyzerTask(ZoneLayer zones, Set<Point> choiceSet, Network network, Geometry boundary) {
		AnalyzerTaskArray array = new AnalyzerTaskArray();
		array.addAnalyzerTask(new ObsTopologyAnalyzerTask(), "topo");
		array.addAnalyzerTask(new SnowballAnalyzerTask(), "snowball");
		array.addAnalyzerTask(new ObsSpatialAnalyzerTask(choiceSet, boundary), "spatial");
		array.addAnalyzerTask(new ObservedSocialAnalyzerTask(), "social");
		addTask(array);
	}

}
