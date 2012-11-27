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

public interface TourActivity {

	public abstract String getLocationId();
	
	public double getTheoreticalEarliestOperationStartTime();
	
	public double getTheoreticalLatestOperationStartTime();

	public abstract void setEarliestOperationStartTime(double early);

	public abstract double getEarliestOperationStartTime();

	public abstract double getLatestOperationStartTime();

	public abstract void setLatestOperationStartTime(double late);

	public double getOperationTime();

	public TourActivity duplicate();

	@Deprecated
	public abstract int getCurrentLoad();

	@Deprecated
	public abstract void setCurrentLoad(int load);

	@Deprecated
	public abstract double getCurrentCost();

	@Deprecated
	public abstract void setCurrentCost(double cost);

}
