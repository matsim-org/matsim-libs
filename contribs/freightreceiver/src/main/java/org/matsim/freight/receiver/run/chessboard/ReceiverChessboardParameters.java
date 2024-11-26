/* *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2018 by the members listed in the COPYING,        *
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

package org.matsim.freight.receiver.run.chessboard;

import org.matsim.freight.receiver.ReceiverReplanningType;

/**
 * Class to help with setting experimental parameters. These parameters are
 * called from multiple places, so they need to be in ONE location to ensure
 * consistency.
 *
 * @author jwjoubert, wlbean
 */
class ReceiverChessboardParameters {



	static String OUTPUT_FOLDER = "./output/receivers/chessboard";
	static ReceiverReplanningType RECEIVER_REPLANNING = ReceiverReplanningType.serviceTime;

	static int NUM_ITERATIONS = 200;

	static int STAT_INTERVAL = 1;

	static int REPLAN_INTERVAL = 10;

	static long SEED_BASE = 12345L;
	static int NUMBER_OF_RECEIVERS = 5;

	static int TIME_WINDOW_DURATION_IN_HOURS = 12;

	static String SERVICE_TIME = "02:00:00";

	static int NUM_DELIVERIES = 5;

	static String DAY_START = "06:00:00";

	static String DAY_END = "18:00:00";

	static double TIME_WINDOW_HOURLY_COST = 0.0;
}
