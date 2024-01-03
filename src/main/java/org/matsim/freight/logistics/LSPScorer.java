/*
 *  *********************************************************************** *
 *  * project: org.matsim.*
 *  * *********************************************************************** *
 *  *                                                                         *
 *  * copyright       : (C) 2022 by the members listed in the COPYING,        *
 *  *                   LICENSE and WARRANTY file.                            *
 *  * email           : info at matsim dot org                                *
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  *   This program is free software; you can redistribute it and/or modify  *
 *  *   it under the terms of the GNU General Public License as published by  *
 *  *   the Free Software Foundation; either version 2 of the License, or     *
 *  *   (at your option) any later version.                                   *
 *  *   See also COPYING, LICENSE and WARRANTY file                           *
 *  *                                                                         *
 *  * ***********************************************************************
 */

package org.matsim.freight.logistics;

import org.matsim.api.core.v01.population.Activity;
import org.matsim.core.controler.listener.ControlerListener;
import org.matsim.core.events.handler.EventHandler;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.freight.carriers.Carrier;

/**
 * This is somewhat similar to the MATSim core {@link ScoringFunction}, which is also used for
 * {@link Carrier}s. A difference, however, is that it does not implement the separate methods
 * {@link ScoringFunction#handleActivity(Activity)} etc., but is just an {@link EventHandler} and a
 * {@link ControlerListener}. (This is, in some sense, the old design for {@link ScoringFunction},
 * and one, where I am still not sure if the new design is truly better.) In any case, here there is
 * not a question: LSP scoring is not so much about activities and legs, since those are handled
 * through the carrier scoring, and need to be pulled in by the lsp scoring if the company is
 * vertically integrated (i.e. if the LSP owns its carriers).
 *
 * <p>also @see {@link LSPScorerFactory}
 */
public interface LSPScorer extends LSPSimulationTracker<LSP> {
  double getScoreForCurrentPlan();
}
