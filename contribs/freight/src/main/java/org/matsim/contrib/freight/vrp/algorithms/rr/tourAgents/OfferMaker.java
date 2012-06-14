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
import org.matsim.contrib.freight.vrp.basics.Tour;
import org.matsim.contrib.freight.vrp.basics.Vehicle;

/**
 * offerMaker are basically marginal cost calculators. they determine the locations within
 * a tour resulting in least costs. 
 * calculating mc can be quite expensive, thus the calculator is adaptapted to the problem type.
 * 
 * @author schroeder
 *
 */
public interface OfferMaker {

	class Offer {
		double price;

		public Offer(double price) {
			super();
			this.price = price;
		}
		
	}
	
	class MetaData {
		int pickupInsertionIndex;
		int deliveryInsertionIndex;
		public MetaData(int pickupInsertionIndex, int deliveryInsertionIndex) {
			super();
			this.pickupInsertionIndex = pickupInsertionIndex;
			this.deliveryInsertionIndex = deliveryInsertionIndex;
		}
		
	}
	
	class OfferData {
		
		public Offer offer;
		
		public MetaData metaData;

		public OfferData(Offer offer, MetaData metaData) {
			super();
			this.offer = offer;
			this.metaData = metaData;
		}
	}

	
	OfferData makeOffer(Vehicle vehicle, Tour tour, Job job, double bestKnownPrice);

}
