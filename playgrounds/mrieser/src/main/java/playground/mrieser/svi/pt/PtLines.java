/* *********************************************************************** *
 * project: org.matsim.*
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

package playground.mrieser.svi.pt;

import java.util.ArrayList;
import java.util.List;

/**
 * @author mrieser
 */
public class PtLines {

	public final List<PtLine> lines;
	
	public PtLines() {
		this.lines = new ArrayList<PtLine>();
	}
	
	public void add(final PtLine line) {
		this.lines.add(line);
	}
	
	public Iterable<PtLine> getLines() {
		return this.lines;
	}
	
}
