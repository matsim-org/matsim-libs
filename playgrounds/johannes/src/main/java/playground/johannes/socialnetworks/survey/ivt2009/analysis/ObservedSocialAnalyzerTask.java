/* *********************************************************************** *
 * project: org.matsim.*
 * ObservedSocialAnalyzerTask.java
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
package playground.johannes.socialnetworks.survey.ivt2009.analysis;

import playground.johannes.socialnetworks.graph.social.analysis.AgeTask;
import playground.johannes.socialnetworks.graph.social.analysis.GenderTask;
import playground.johannes.socialnetworks.graph.social.analysis.SocialAnalyzerTask;
import playground.johannes.socialnetworks.snowball2.social.analysis.ObservedAge;

/**
 * @author illenberger
 *
 */
public class ObservedSocialAnalyzerTask extends SocialAnalyzerTask {

	public ObservedSocialAnalyzerTask() {
		addTask(new AgeTask(new ObservedAge()));
		addTask(new GenderTask(new ObservedGender()));
	}

}
