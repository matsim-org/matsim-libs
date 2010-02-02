/* *********************************************************************** *
 * project: org.matsim.*
 * KMLPersonDescriptor.java
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
package playground.johannes.socialnetworks.survey.ivt2009;

import playground.johannes.socialnetworks.graph.spatial.io.KMLObjectDescriptor;
import playground.johannes.socialnetworks.survey.ivt2009.graph.SampledSocialVertex;


/**
 * @author illenberger
 *
 */
public class KMLSnowballDescriptor implements KMLObjectDescriptor<SampledSocialVertex> {

	public String getDescription(SampledSocialVertex object) {
		StringBuilder builder = new StringBuilder();
		
		builder.append("Name: ");
		builder.append(object.getPerson().getId().toString());
		builder.append("<br>");
		builder.append("Sampled: ");
		builder.append(String.valueOf(object.getIterationSampled()));
		builder.append("<br>");
		builder.append("Detected: ");
		builder.append(String.valueOf(object.getIterationDetected()));
		builder.append("<br>");
		builder.append("Recruited by: ");
		SampledSocialVertex name = object.getSources().get(0);
		if(name == null)
			builder.append("seed");
		else
			builder.append(name.getPerson().getId().toString());
		
		return builder.toString();
	}

	public String getName(SampledSocialVertex object) {
		return object.getPerson().getId().toString();
	}

}
