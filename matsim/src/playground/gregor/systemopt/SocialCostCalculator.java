/* *********************************************************************** *
 * project: org.matsim.*
 * SocialCostCalculator.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package playground.gregor.systemopt;

import org.matsim.controler.listener.ControlerListener;
import org.matsim.events.handler.EventHandler;
import org.matsim.interfaces.core.v01.Link;


public interface SocialCostCalculator extends EventHandler, ControlerListener {

		
	public double getSocialCost(final Link link, final double time);

}
