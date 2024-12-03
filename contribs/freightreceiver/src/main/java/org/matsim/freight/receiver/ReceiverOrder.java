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

package org.matsim.freight.receiver;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.BasicPlan;
import org.matsim.freight.carriers.Carrier;

import java.util.ArrayList;
import java.util.Collection;

/**
 * A collection of all the orders of a receiver delivered by a single carrier.
 *
 * @author wlbean, jwjoubert
 */

public class ReceiverOrder implements BasicPlan{

	private final Logger log = LogManager.getLogger(ReceiverOrder.class);
	private final Id<Receiver> receiverId;
	private final Collection<Order> orders;
	private Double cost = null;
	private final Id<Carrier> carrierId;
	private Carrier carrier = null;

	@Override
	public String toString() {
		StringBuilder strb = new StringBuilder(  ) ;
		strb.append("[") ;

		for( Order order : orders ){
			strb.append( order.toString() ) ;
		}

		strb.append("]") ;
		return strb.toString() ;
	}

	public ReceiverOrder(final Id<Receiver> receiverId, final Collection<Order> orders, final Id<Carrier> carrierId){
		this.orders = orders;
		this.receiverId = receiverId;
		this.carrierId = carrierId;
	}

	public final ReceiverOrder createCopy() {
		Collection<Order> ordersCopy = new ArrayList<>() ;
		for( Order order : orders ){
			ordersCopy.add( order.createCopy() ) ;
		}
		ReceiverOrder receiverOrderCopy = new ReceiverOrder( this.receiverId, ordersCopy, this.carrierId );
		receiverOrderCopy.cost = this.cost;
		receiverOrderCopy.carrier = this.carrier ;
		return receiverOrderCopy ;
	}

	/**
	 * Get the back pointer to this {@link ReceiverOrder}'s {@link Receiver}.
	 */
	public Id<Receiver> getReceiverId(){
		return receiverId;
	}

	@Override
	public Double getScore() {
		if(cost == null) {
			log.warn("The cost/score has not been set yet. Returning null.");
		}
		return cost;
	}

	@Override
	public void setScore(Double cost) {
		this.cost  = cost;
	}

	public Collection<Order> getReceiverProductOrders(){
		return this.orders;
	}


	/**
	 * Get the actual {@link Carrier} of this {@link ReceiverOrder}. This will
	 * only be set once FIXME ... has been called to link the receivers and
	 * carriers.
	 */
	public Carrier getCarrier() {
		if(this.carrier == null) {
			log.error("The carriers have not been linked to the receivers yet. Returning null.");
		}
		return this.carrier;
	}

	/**
	 * Get the pointer {@link Id} of this {@link ReceiverOrder}'s {@link Carrier}.
	 */
	public Id<Carrier> getCarrierId(){
		return this.carrierId;
	}


	public void setCarrier(final Carrier carrier) {
		this.carrier = carrier;
	}


}

