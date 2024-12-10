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

/**
 *
 */
package org.matsim.freight.receiver.collaboration;

import org.matsim.freight.carriers.TimeWindow;
import org.matsim.freight.receiver.ReceiverPlan;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.replanning.ReplanningContext;
import org.matsim.core.replanning.modules.GenericPlanStrategyModule;
import org.matsim.core.utils.misc.Time;

/**
 *
 * @author jwjoubert
 */
public class TimeWindowMutator implements GenericPlanStrategyModule<ReceiverPlan> {
	final private double stepSize;
	final private double MINIMUM_TIME_WINDOW = Time.parseTime("02:00:00");
	final private double MAXIMUM_TIME_WINDOW = Time.parseTime("12:00:00");


	public TimeWindowMutator(double stepSize) {
//		this.stepSize = stepSize*MatsimRandom.getLocalInstance().nextDouble();
		this.stepSize = stepSize;
	}

	@Override
	public void prepareReplanning(ReplanningContext replanningContext) {
	}

	@Override
	public void handlePlan(ReceiverPlan plan) {
		//TimeWindow oldWindow = pickRandomTimeWindow(plan);
		TimeWindow oldWindow = plan.getTimeWindows().get(0);
		TimeWindow newWindow = wiggleTimeWindow(oldWindow);
		plan.getTimeWindows().remove(oldWindow);
		plan.getTimeWindows().add(newWindow);
	}

	public TimeWindow pickRandomTimeWindow(ReceiverPlan plan) {
		int item = MatsimRandom.getLocalInstance().nextInt(plan.getTimeWindows().size());
		return plan.getTimeWindows().get(item);
	}

	/**
	 * Randomly performs a perturbation to the given {@link TimeWindow}. The
	 * perturbations include increasing or decreasing either the {@link TimeWindow}'s
	 * start or end time.
	 *
	 * @param tw
	 * @return
	 */
	public TimeWindow wiggleTimeWindow(TimeWindow tw) {
		int move = MatsimRandom.getLocalInstance().nextInt(4);
		switch (move) {
		case 0:
			return extendTimeWindowDownwards(tw);
		case 1:
			return extendTimeWindowUpwards(tw);
		case 2:
			return contractTimeWindowBottom(tw);
		case 3:
			return contractTimeWindowTop(tw);
		default:
			throw new IllegalArgumentException("Cannot wiggle TimeWindow with move type '" + move + "'.");
		}
	}

//	public ReceiverOrder pickRandomReceiverOrder(ReceiverPlan plan) {
//		/* Randomly identify a (single) receiver order to adapt. */
//		Collection<ReceiverOrder> ro = plan.getReceiverOrders();
//		int item = MatsimRandom.getLocalInstance().nextInt(ro.size());
//		ReceiverOrder selectedRo = null;
//		Object o = ro.toArray()[item];
//		if(o instanceof ReceiverOrder) {
//			selectedRo = (ReceiverOrder) o;
//		} else {
//			throw new RuntimeException("Randomly selected 'ReceiverOrder' is of the wrong type: " + o.getClass().toString() );
//		}
//		return selectedRo;
//	}

	/**
	 * Decreases the {@link TimeWindow} start time by some random step size that
	 * is no more than the threshold specified at this {@link TimeWindowMutator}'s
	 * instantiation.
	 *
	 * @param tw
	 * @return
	 */
	public TimeWindow extendTimeWindowDownwards(final TimeWindow tw) {
		//double newLow = tw.getStart() - MatsimRandom.getLocalInstance().nextDouble()*this.stepSize;
		double newLow;

		if (tw.getEnd() - (tw.getStart() - this.stepSize) <= MAXIMUM_TIME_WINDOW){
			newLow = tw.getStart() - this.stepSize;
		} else newLow = tw.getEnd() - MAXIMUM_TIME_WINDOW ;

		if (newLow > Time.parseTime("06:00:00")){
			return TimeWindow.newInstance(newLow, tw.getEnd());
		}
		else {
			return TimeWindow.newInstance(Time.parseTime("06:00:00"), tw.getEnd());
		}

	}


	/**
	 * Increases the {@link TimeWindow} end time by some random step size that
	 * is no more than the threshold specified at this {@link TimeWindowMutator}'s
	 * instantiation.
	 *
	 * @param tw
	 * @return
	 */
	public TimeWindow extendTimeWindowUpwards(final TimeWindow tw) {
//		double newHigh = tw.getEnd() + MatsimRandom.getLocalInstance().nextDouble()*this.stepSize;
		double newHigh;

		if ((tw.getEnd() + this.stepSize) - tw.getStart() <= MAXIMUM_TIME_WINDOW){
			newHigh = tw.getEnd() + this.stepSize;
		} else newHigh = tw.getStart() + MAXIMUM_TIME_WINDOW ;

		if (newHigh < Time.parseTime("18:00:00")){
			return TimeWindow.newInstance(tw.getStart(), newHigh);
		}
		else {
			return TimeWindow.newInstance(tw.getStart(), Time.parseTime("18:00:00"));
		}

	}


	/**
	 * Increases the {@link TimeWindow} start time by some random step size that
	 * is no more than the threshold specified at this {@link TimeWindowMutator}'s
	 * instantiation, provided the minimum {@link TimeWindow} width is maintained.
	 *
	 * @param tw
	 * @return
	 */
	public TimeWindow contractTimeWindowBottom(final TimeWindow tw) {
		double gap = Math.max(0, (tw.getEnd() - tw.getStart()) - MINIMUM_TIME_WINDOW);
		double step = Math.min(gap, stepSize);
		//double newLow = tw.getStart() + MatsimRandom.getLocalInstance().nextDouble()*step;
		double newLow = tw.getStart() + step;

		if (newLow > Time.parseTime("06:00:00")){
			return TimeWindow.newInstance(newLow, tw.getEnd());
		}
		else {
			return TimeWindow.newInstance(Time.parseTime("06:00:00"), tw.getEnd());
		}
	}


	/**
	 * Decreases the {@link TimeWindow} end time by some random step size that
	 * is no more than the threshold specified at this {@link TimeWindowMutator}'s
	 * instantiation, provided the minimum {@link TimeWindow} width is maintained.
	 *
	 * @param tw
	 * @return
	 */
	public TimeWindow contractTimeWindowTop(final TimeWindow tw) {
		double gap = Math.max(0, (tw.getEnd() - tw.getStart()) - MINIMUM_TIME_WINDOW);
		double step = Math.min(gap, stepSize);
		//double newHigh = tw.getEnd() - MatsimRandom.getLocalInstance().nextDouble()*step;
		double newHigh = tw.getEnd() - step;

		if (newHigh < Time.parseTime("18:00:00")){
			return TimeWindow.newInstance(tw.getStart(), newHigh);
		}
		else {
			return TimeWindow.newInstance(tw.getStart(), Time.parseTime("18:00:00"));
		}

	}


	@Override
	public void finishReplanning() {

	}

}
