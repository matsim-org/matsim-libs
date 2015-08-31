/* *********************************************************************** *
 * project: org.matsim.*
 * AbstractLinguisticAttribute.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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
package playground.johannes.socialnetworks.graph.social.analysis;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;

import playground.johannes.socialnetworks.graph.social.SocialVertex;

/**
 * @author illenberger
 *
 */
public abstract class AbstractLinguisticAttribute {

	private static final Logger logger = Logger.getLogger(AbstractLinguisticAttribute.class);
	
	protected abstract String attribute(SocialVertex v);
	
	public Map<SocialVertex, String> values(Set<? extends SocialVertex> vertices) {
		Map<SocialVertex, String> map = new HashMap<SocialVertex, String>(vertices.size());
		int nullAtts = 0;
		for(SocialVertex v : vertices) {
			String att = attribute(v);
			if(att == null)
				nullAtts++;
			else
				map.put(v, att);
		}
	
		if(nullAtts > 0)
			logger.debug(String.format("%1$s vertices with null attribute.", nullAtts));
		
		return map;
	}
	
	public SocioMatrix<String> countsMatrix(Set<? extends SocialVertex> vertices) {
		return SocioMatrixBuilder.countsMatrix(this.values(vertices));
	}
	
	public SocioMatrix<String> probaMatrix(Set<? extends SocialVertex> vertices) {
		Map<SocialVertex, String> values = this.values(vertices);
		return SocioMatrixBuilder.probaMatrix(values, values);
	}
}
