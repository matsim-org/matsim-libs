/* *********************************************************************** *
 * project: org.matsim.*
 * SampledSpatialEdge.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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
package playground.johannes.socialnetworks.survey.ivt2009.spatial;

import playground.johannes.socialnetworks.graph.spatial.SpatialEdge;
import playground.johannes.socialnetworks.survey.ivt2009.SampledEdge;

/**
 * @author illenberger
 *
 */
public class SampledSpatialEdge extends SpatialEdge implements SampledEdge {

	public SampledSpatialEdge(SampledSpatialVertex v1, SampledSpatialVertex v2) {
		super(v1, v2);
	}

}
