/* *********************************************************************** *
 * project: org.matsim.*
 * Income.java
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
package playground.johannes.socialnetworks.survey.ivt2009.analysis;

import playground.johannes.socialnetworks.graph.social.SocialVertex;
import playground.johannes.socialnetworks.graph.social.analysis.SocioMatrix;

/**
 * @author illenberger
 *
 */
public class Income extends SocioMatrix {

	@Override
	protected String getAttributeValue(SocialVertex vertex) {
		return String.valueOf(vertex.getPerson().getIncome());
	}

}
