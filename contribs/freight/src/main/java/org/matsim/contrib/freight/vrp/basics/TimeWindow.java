/*******************************************************************************
 * Copyright (c) 2011 Stefan Schroeder.
 * eMail: stefan.schroeder@kit.edu
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 * 
 * Contributors:
 *     Stefan Schroeder - initial API and implementation
 ******************************************************************************/
package org.matsim.contrib.freight.vrp.basics;

/**
 * 
 * @author stefan schroeder
 * 
 */

public class TimeWindow {
	private double start;
	private double end;

	public TimeWindow(double start, double end) {
		super();
		this.start = start;
		this.end = end;
	}

	public double getStart() {
		return start;
	}

	public void setStart(double start) {
		this.start = start;
	}

	public void setEnd(double end) {
		this.end = end;
	}

	public double getEnd() {
		return end;
	}

	public boolean conflict() {
		if (start > end) {
			return true;
		}
		return false;
	}

	@Override
	public String toString() {
		return "[start=" + start + "][end=" + end + "]";
	}

}
