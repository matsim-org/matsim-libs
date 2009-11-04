/* *********************************************************************** *
 * project: org.matsim.*
 * SampledSocialTie.java
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

import playground.johannes.socialnetworks.graph.social.SocialTie;
import playground.johannes.socialnetworks.snowball2.SampledEdge;

/**
 * @author illenberger
 *
 */
public class SampledSocialTie extends SocialTie implements SampledEdge {

	protected SampledSocialTie(int created) {
		super(created);
	}

}
