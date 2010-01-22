/* *********************************************************************** *
 * project: org.matsim.*
 * SampledEgo.java
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

import java.util.List;

import org.matsim.contrib.sna.snowball.SampledVertex;
import org.matsim.contrib.sna.snowball.SnowballAttributes;

import playground.johannes.socialnetworks.graph.social.Ego;
import playground.johannes.socialnetworks.graph.social.SocialPerson;

/**
 * @author illenberger
 *
 */
public class SampledEgo extends Ego implements SampledVertex {

	private SnowballAttributes attributes;
	
	private SampledEgo recruitedBy;
	
	protected SampledEgo(SocialPerson person) {
		super(person);
		attributes = new SnowballAttributes();
	}

	@Override
	@SuppressWarnings("unchecked")
	public List<? extends SampledSocialTie> getEdges() {
		return (List<? extends SampledSocialTie>) super.getEdges();
	}

	@Override
	@SuppressWarnings("unchecked")
	public List<? extends SampledEgo> getNeighbours() {
		return (List<? extends SampledEgo>) super.getNeighbours();
	}

	public void detect(int iteration) {
		attributes.detect(iteration);
	}

	public int getIterationDetected() {
		return attributes.getIterationDeteted();
	}

	public int getIterationSampled() {
		return attributes.getIterationSampled();
	}

	public boolean isDetected() {
		return attributes.isDetected();
	}

	public boolean isSampled() {
		return attributes.isSampled();
	}

	public void sample(int iteration) {
		attributes.sample(iteration);
	}

	public void setRecruitedBy(SampledEgo ego) {
		recruitedBy = ego;
	}
	
	public SampledEgo getRecruitedBy() {
		return recruitedBy;
	}
}
