/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2023 by the members listed in the COPYING,        *
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

package ch.sbb.matsim.contrib.railsim.qsimengine;

import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.pt.TransitQSimEngine;
import org.matsim.core.mobsim.qsim.pt.TransitStopHandlerFactory;
import org.matsim.pt.UmlaufBuilder;

public class RailsimQSimEngine extends TransitQSimEngine {

	public RailsimQSimEngine(QSim queueSimulation, TransitStopHandlerFactory stopHandlerFactory, UmlaufBuilder umlaufBuilder) {
		super(queueSimulation, stopHandlerFactory, umlaufBuilder);
	}

	// Get inspiration from SBBTransitQSimEngine on what methods to overwrite

	/* some implementation notes and ideas:
	   - data structure to store a track-state per link (or multiple track-state if a link depicts multiple parallel tracks)
	     - track-state can be `free`, `blocked` (only a single train can be in a blocked track), or `reserved` (multiple trains can reserve the same track)
	   - data structure to store position and other data of trains
	     - head position, given as meters from fromNode on a link
	     - current route (ordered list of links)
	     - length of the train
	     - current speed of the train
	     - current acceleration/deceleration of the train
	     - additional attributes as required, e.g. required stopping distance ("bremsweg") given the current speed
	   - in each simStep (may be optimized later to a lower interval), the position of each train is updated
	   - each train tries to block as many links in front of the train to cover the stopping distance
	     - if not enough links can be blocked, the train must decelerate accordingly
	   - a train can only accelerate, if the complete train is on links with the higher allowed speed
	     (e.g. train cannot accelerate if only the engine is on a faster link, but the rest of the train are still on links with lower freespeed)

	   For visualization purposes, the following output should be produced:
	   - CSV containing time-dependent link-attributes depicting the track-state (needs discussion what we output when a link contains multiple tracks)
	     - optionally include information which trains (vehicleId) blocked or reserved a track?
	   - CSV containing time-dependent vehicle-attributes: e.g. current acceleration
	   - CSV containing XYT data to show head and tail of train, maybe even multiple points (e.g. every 25m) to show length of train?
	     - how often? every 1min, every 5min? --> config?
	     - additional attributes? e.g. current speed and acceleration?
	   - linkEnter/linkLeave-Events for the front of the train
	     instead of XYT (see above), we could think about if we could create linkEnter/linkLeave-Events for each wagon (estimated every 25m of the train),
	     but this might not look good as Via still interpolates the position independently.

	   We will have to think about deadlock prevention at some stage.
	   Maybe design some test networks for that, e.g. a single track with a loop on one end; if too many trains try to get into the loop, they can't get out.
	   This is where the `reserved` track-state might come into play.


	   TODO Implementation steps:
	   0. RailsimConfigGroup
	   1. RailsimQSimEngine extracts all "rail"-links (depending on config) and builds a data structure to store track-states
	   2. RailsimQSimEngine handles all "rail"-vehicles (also depending on config) and moves the trains each second
	      First implementation can be very basic, e.g. constant speed per link according to freespeed, no checks if track is free
	   3. Each train blocks the links it currently occupies, plus 1 link in front of it if possible
	   4. A train can only move to the next link it that link is blocked by that train
	   5. Each train tries to block as many link in front of it along the route as it needs for the stopping distance
	   6. Trains accelerate smoothly when entering links with a higher freespeed
	   7. Trains decelerate smoothly before entering links with a lower freespeed
	   8. Trains decelerate smoothly if they cannot block enough links in front of them
	   9. Deadlock Prevention

	 */
}
