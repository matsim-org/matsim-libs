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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.BasicPlan;
import org.matsim.api.core.v01.population.Person;
import org.matsim.freight.carriers.Carrier;
import org.matsim.freight.carriers.TimeWindow;
import org.matsim.freight.receiver.collaboration.CollaborationUtils;
import org.matsim.utils.objectattributes.attributable.Attributable;
import org.matsim.utils.objectattributes.attributable.Attributes;
import org.matsim.utils.objectattributes.attributable.AttributesImpl;

import java.util.*;

/**
 * Like a natural {@link Person} a plan contains the intention of a {@link Receiver}
 * agent.  In consequence, all information is <i>expected</i>. This container
 * describes a {@link Receiver}'s behaviour in terms of how orders are placed
 * with different {@link Carrier}s.
 * <p></p>
 * The only thing which is not "expected" in the same sense is the score.
 *
 * @author jwjoubert
 */
public final class ReceiverPlan implements BasicPlan, Attributable {
	private final Logger log = LogManager.getLogger(ReceiverPlan.class);
	private final Attributes attributes;
	private Receiver receiver = null;
	private Double score;
	private Map<Id<Carrier>, ReceiverOrder> orderMap;
	private List<TimeWindow> timeWindows;
	private boolean selected = false;

	private ReceiverPlan() {
		this.attributes = new AttributesImpl();
		this.timeWindows = new ArrayList<>();
		this.orderMap = new TreeMap<>();
	}


//	public void addReceiverOrder(final ReceiverOrder ro) {
//		if(orderMap.containsKey(ro.getCarrierId())) {
//			throw new IllegalArgumentException("Receiver '" + this.receiver.getId().toString()
//					+ "' already has an order with carrier '" + ro.getCarrierId().toString() + "'");
//		}
//		orderMap.put(ro.getCarrierId(), ro);
//	}

	@Override
	public void setScore(final Double score) {
		this.score = score;
	}

	@Override
	public Double getScore() {
		return this.score;
	}

	public final Receiver getReceiver() {
		return this.receiver;
	}

	public boolean isSelected() {
		return this.selected;
	}

	public void setSelected(boolean selected) {
		this.selected = selected;
	}

//	public void setCollaborationStatus(boolean status){
//		this.attributes.putAttribute(ReceiverUtils.ATTR_COLLABORATION_STATUS, status);
//	}
//
//	public boolean getCollaborationStatus(){
//		return (boolean) this.attributes.getAttribute(ReceiverUtils.ATTR_COLLABORATION_STATUS);
//	}

	/**
	 * Returns the {@link ReceiverOrder} for a given {@link Carrier}.
	 */
	public final ReceiverOrder getReceiverOrder(Id<Carrier> carriedId) {
		if(!orderMap.containsKey(carriedId)) {
			log.warn("Receiver '" + this.receiver.getId().toString() +
					"' does not have an order with carrier '" +
					carriedId.toString() + "'. Returning null");
			return null;
		}
		return this.orderMap.get(carriedId);
	}


	public String toString() {
		StringBuilder strb = new StringBuilder(  ) ;

		String receiverString = "undefined";
		if(this.receiver != null) {
			receiverString = this.receiver.getId().toString();
		}
		strb.append( "[receiverId=" ).append( receiverString );

		String scoreString = "undefined";
		if(this.score != null) {
			scoreString = this.score.toString();
		}
		strb.append( "; score=" ).append( scoreString );

		if ( this.selected ){
			strb.append( "; SELECTED" );
		} else {
			strb.append( "; unselected" );
		}

		strb.append("; collabStatus=").append( this.attributes.getAttribute( CollaborationUtils.ATTR_COLLABORATION_STATUS ) ) ;

		strb.append("; time windows=[") ;
		for( TimeWindow timeWindow : timeWindows ){
			strb.append( timeWindow.toString() ) ;
		}
		strb.append("]") ;

		strb.append( "; number of orders with carriers=" ).append( orderMap.size() );

		strb.append( "; orders=[" );
		for( Map.Entry<Id<Carrier>, ReceiverOrder> entry : orderMap.entrySet() ){
			strb.append( "[carrierId=" ).append( entry.getKey() ).append( "; order=" ).append( entry.getValue() ).append( "]" ) ;
		}

		return strb.toString() + "]" ;
	}

	public final Collection<ReceiverOrder> getReceiverOrders(){
		return this.orderMap.values();
	}

	@Override
	public Attributes getAttributes() {
		return this.attributes;
	}

	public List<TimeWindow> getTimeWindows(){
		return this.timeWindows;
	}

	/**
	 * Checks if a given time is within the allowable time window(s).
	 *
	 * @return true if the time is within at least one of the set time
	 * window(s), or <i>if no time windows are set</i>.
	 */
//	public boolean isInTimeWindow(double time) {
//		if(this.timeWindows.isEmpty()) {
//			log.warn("No time windows are set! Assuming any time is suitable.");
//			return true;
//		}
//
//		boolean inTimeWindow = false;
//		Iterator<TimeWindow> iterator = this.timeWindows.iterator();
//
//		while(!inTimeWindow & iterator.hasNext()) {
//			TimeWindow tw = iterator.next();
//			if(time >= tw.getStart() && time <= tw.getEnd()) {
//				inTimeWindow = true;
//			}
//		}
//		return false;
//	}


	public ReceiverPlan createCopy() {
		Builder builder = Builder.newInstance(receiver, (boolean) attributes.getAttribute(CollaborationUtils.ATTR_COLLABORATION_STATUS));
		for(ReceiverOrder ro : this.orderMap.values()) {
			builder = builder.addReceiverOrder(ro.createCopy());
		}
		for(TimeWindow tw : this.timeWindows) {
			builder.addTimeWindow( TimeWindow.newInstance(tw.getStart(), tw.getEnd()) );
			// use copy!
		}
		return builder.build();
	}


	/**
	 * The constructor mechanism for creating a {@link ReceiverPlan}. Once
	 * built the only thing one will be able to change is the score.
	 *
	 * @author jwjoubert
	 */
	public static class Builder{
		private Receiver receiver = null;
		private final Map<Id<Carrier>, ReceiverOrder> map = new HashMap<>();
		private boolean selected = false;
		private Double score = null;
		private final List<TimeWindow> timeWindows = new ArrayList<>();
		private final boolean status;

		private Builder(Receiver receiver, boolean status) {
			this.receiver = receiver;
			this.status  = status;
		}

		public static Builder newInstance(Receiver receiver, boolean status) {
			return new Builder(receiver, status);
		};


		public Builder addReceiverOrder(ReceiverOrder ro) {
			this.map.put(ro.getCarrierId(), ro);
			return this;
		}

		public Builder addTimeWindow(TimeWindow tw) {
			this.timeWindows.add(tw);
			return this;
		}

		public Builder setSelected(boolean selected) {
			this.selected = selected;
			return this;
		}

		public Builder setScore(double score) {
			this.score = score;
			return this;
		}

		public ReceiverPlan build() {
			ReceiverPlan plan = new ReceiverPlan();
			plan.receiver = this.receiver;
			plan.selected = this.selected;
			plan.attributes.putAttribute(CollaborationUtils.ATTR_COLLABORATION_STATUS, this.status );
			if(this.map.size() > 0) {
				plan.orderMap.putAll(this.map);
			} else {
				plan.orderMap = this.map;
			}
			plan.score = this.score;
			plan.timeWindows = this.timeWindows;
			return plan;
		}
	}

}
