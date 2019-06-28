
/* *********************************************************************** *
 * project: org.matsim.*
 * package-info.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2019 by the members listed in the COPYING,        *
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

 /**
 * 
 * Contains the scoring functions, i.e. the implementations of ScoringFunctionFactory, used in the
 * project.
 * 
 * The classes ending with Scoring (and not with ScoringFunction) are "sum terms" which you
 * can use independently when you write your own ScoringFunction, see {@link org.matsim.core.scoring.SumScoringFunction}.
 * On the other hand, it may be more straight-forward to just implement ScoringFunction yourself and copy/paste some of the
 * math to your own class.
 *
 * @see org.matsim.core.scoring
 */
package org.matsim.core.scoring.functions;