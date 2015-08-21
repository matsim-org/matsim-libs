/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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

package playground.johannes.gsv.synPop;

import playground.johannes.sna.util.Composite;
import playground.johannes.synpop.data.Episode;
import playground.johannes.synpop.processing.EpisodeTask;

/**
 * @author johannes
 *
 */
public class EpisodeTaskComposite extends Composite<EpisodeTask> implements EpisodeTask {

	/* (non-Javadoc)
	 * @see playground.johannes.synpop.processing.EpisodeTask#apply(playground.johannes.synpop.data.PlainEpisode)
	 */
	@Override
	public void apply(Episode plan) {
		for(EpisodeTask task : components) {
			task.apply(plan);
		}
	}

}
