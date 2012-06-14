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
package org.matsim.contrib.freight.vrp.algorithms.rr.tourAgents;

import org.matsim.contrib.freight.vrp.basics.Job;

public interface JobInsertionFinder {
	
	class InsertionData {
		double mc;
		Integer pickupInsertionIndex;
		Integer deliveryInsertionIndex;
		public InsertionData(double mc, Integer pickupInsertionIndex, Integer deliveryInsertionIndex) {
			super();
			this.mc = mc;
			this.pickupInsertionIndex = pickupInsertionIndex;
			this.deliveryInsertionIndex = deliveryInsertionIndex;
		}
		public boolean isNull(){
			return pickupInsertionIndex == null || deliveryInsertionIndex == null;
		}
	}
	
	public InsertionData find(Job job, double bestKnownPrice);

}
