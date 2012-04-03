/* *********************************************************************** *
 * project: org.matsim.*
 * PatternSearchListener.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package playground.yu.parameterSearch;

import org.matsim.core.config.Config;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.StartupListener;

public class PatternSearchListener implements PatternSearchListenerI,
		StartupListener {

	@Override
	public void notifyStartup(StartupEvent event) {
		Config cfg = event.getControler().getConfig();

		nameParametersMap.clear();

		// travelTime
		nameParametersMap.put(TRAVELING,
				ParametersGetter.getValueOfParameter(cfg, TRAVELING));
		nameParametersMap.put(TRAVELING_PT,
				ParametersGetter.getValueOfParameter(cfg, TRAVELING_PT));
		nameParametersMap.put(TRAVELING_WALK,
				ParametersGetter.getValueOfParameter(cfg, TRAVELING_WALK));

		// actPerforming
		nameParametersMap.put(PERFORMING,
				ParametersGetter.getValueOfParameter(cfg, PERFORMING));

		// attrNameList.addScaleFactor("lateArrival");//in the future

		// stuck
		nameParametersMap.put(STUCK,
				ParametersGetter.getValueOfParameter(cfg, STUCK));

		// distanceAttr
		nameParametersMap.put(MONETARY_DISTANCE_COST_RATE_PT, ParametersGetter
				.getValueOfParameter(cfg, MONETARY_DISTANCE_COST_RATE_PT));
		nameParametersMap.put(MONETARY_DISTANCE_COST_RATE_CAR, ParametersGetter
				.getValueOfParameter(cfg, MONETARY_DISTANCE_COST_RATE_CAR));
		nameParametersMap.put(MARGINAL_UTL_OF_DISTANCE_WALK, ParametersGetter
				.getValueOfParameter(cfg, MARGINAL_UTL_OF_DISTANCE_WALK));

		// LegOffsetAttr
		nameParametersMap.put(CONSTANT_CAR,
				ParametersGetter.getValueOfParameter(cfg, CONSTANT_CAR));
		nameParametersMap.put(CONSTANT_PT,
				ParametersGetter.getValueOfParameter(cfg, CONSTANT_PT));
		nameParametersMap.put(CONSTANT_WALK,
				ParametersGetter.getValueOfParameter(cfg, CONSTANT_WALK));

		// leftTurn (additive)
		nameParametersMap.put(CONSTANT_LEFT_TURN,
				ParametersGetter.getValueOfParameter(cfg, CONSTANT_LEFT_TURN));
	}

}
