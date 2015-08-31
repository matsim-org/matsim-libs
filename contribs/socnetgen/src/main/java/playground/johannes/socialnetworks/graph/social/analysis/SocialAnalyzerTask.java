/* *********************************************************************** *
 * project: org.matsim.*
 * SocialAnalyzerTask.java
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

import playground.johannes.socialnetworks.graph.analysis.AnalyzerTaskComposite;

/**
 * @author illenberger
 *
 */
public class SocialAnalyzerTask extends AnalyzerTaskComposite {

	public SocialAnalyzerTask() {
		addTask(new AgeTask());
		addTask(new GenderTask());
	}
}
