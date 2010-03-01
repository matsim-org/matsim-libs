/* *********************************************************************** *
 * project: org.matsim.*
 * VDSSign.java
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

package org.matsim.withinday.trafficmanagement;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.population.routes.LinkNetworkRouteImpl;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.utils.misc.NetworkUtils;
import org.matsim.core.utils.misc.RouteUtils;
import org.matsim.ptproject.qsim.QSimTimer;
import org.matsim.withinday.trafficmanagement.feedbackcontroler.FeedbackControler;

/**
 *
 * The core class for automatic feedback guidance.
 *
 */
public class VDSSign {

	private static final Logger log = Logger.getLogger(VDSSign.class);

	private ControlInput controlInput;

	private FeedbackControler controler;

	private int messageHoldTime;

	private double nextUpdate;

	private int controlUpdateTime;

	private int controlEvents;

	private double nominalSplitting;

	private double deadZoneSystemInput;

	private double deadZoneSystemOutput;

	private boolean benefitControl;

	private Link signLink;

	private Link directionLink;

	private NetworkRoute currentRoute;

	private List<NetworkRoute> currentRouteSet;

	private NetworkRoute mainRoute;

	private NetworkRoute alternativeRoute;

	private double complianceRate;

	private VDSSignOutput signOutput;

	private Network network = null;

	public VDSSign(final Network network) {
		this.network = network;
	}

	public void setupIteration(IterationStartsEvent event) {
		this.controlUpdateTime = this.messageHoldTime * this.controlEvents;
		// completes the routes, i.e. calculates out and inlinks
		this.mainRoute = completeRoute(this.controlInput.getMainRoute());
		this.alternativeRoute = completeRoute(this.controlInput.getAlternativeRoute());
		this.currentRouteSet = new ArrayList<NetworkRoute>(this.controlEvents);
		if ((this.signOutput != null) && (event != null)) {
			try {
				this.signOutput.init(event);
			} catch (IOException e) {
				log.error("Cannot create output files for VDSSign, no output will be written");
				e.printStackTrace();
				this.signOutput = null;
			}
		}
	}

	public void setupIteration(){
		this.setupIteration(null);
	}

	/**
	 * Sets the time for the first guidance message generation.
	 *
	 */
	public void simulationPrepared() {
		this.nextUpdate = QSimTimer.getTime();
	}

	private double calculateDisbenefitValue(final double nashTime) {
		log.info("Using benefit control!");
		// Generating d(t)
		double disValue = 0.0;
		if (nashTime > 0) {
			disValue = this.controlInput.getNumberOfVehiclesOnRoute(this.mainRoute) * nashTime;
		}
		else if (nashTime < 0) {
			disValue = this.controlInput.getNumberOfVehiclesOnRoute(this.alternativeRoute) * nashTime;
		}
		else {
			disValue = 0;
		}

		if ((disValue < -1 * this.deadZoneSystemOutput) || (disValue > this.deadZoneSystemOutput)) {
			return disValue;
		}
		return 0;
	}

	public void calculateOutput(final double time) {
		double nashTime = this.controlInput.getNashTime();
		if (log.isTraceEnabled()) {
			log.trace("");
			log.trace("System time: " + time);
			log.trace("NashTime: " + nashTime);
		}
		if (this.signOutput != null) {
			this.signOutput.addMeasurement(time, this.controlInput.getMeasuredRouteTravelTime(this.controlInput.getMainRoute()), this.controlInput.getMeasuredRouteTravelTime(this.controlInput.getAlternativeRoute()), nashTime);
		}
		if (time >= this.nextUpdate) {
			// Choosing the output for the controler, y(t) or d(t),
			double finalOutput;

			// as benefitControl is false by carl-default jump to else!
			if (this.benefitControl) {
				finalOutput = calculateDisbenefitValue(nashTime);
			}
			else {
				//check deadZone for system output
				if ((nashTime < -1 * this.deadZoneSystemOutput) || (nashTime > this.deadZoneSystemOutput)) {
					finalOutput = nashTime;
				}
				else {
					log.trace("System output in deadZone, setting output to 0");
					finalOutput = 0;
				}
			}
			if (log.isTraceEnabled()) {
				log.trace("Output: " + finalOutput);
			}
			this.calculateInput(finalOutput);
		}
	}

	private void calculateInput(final double finalOutput) {
		double rawInput, input;
		rawInput = this.controler.control(finalOutput);
		if (log.isTraceEnabled()) {
			log.trace("Controled raw input: " + rawInput);
		}
		if (rawInput == 0) {
			input = this.nominalSplitting;
		}
		else if (rawInput > 0) {
			input = this.nominalSplitting + ((1 - this.nominalSplitting) * rawInput);
		}
		else {
			input = this.nominalSplitting	+ (this.nominalSplitting * rawInput);
		}
		calculateRouteSet(input);
	}

	/**
	 * Transforms the continious control signal to an apropraite sequence of
	 * binary guidance messages.
	 *
	 * @param input
	 */
	private void calculateRouteSet(final double input) {
		if  (log.isTraceEnabled()) {
			log.trace("Calculating route set for input: " + input);
		}
		boolean allEqual = true;
		this.currentRouteSet.clear();
		if ((this.nominalSplitting - this.deadZoneSystemInput <= input)	&& (input <= this.nominalSplitting + this.deadZoneSystemInput)) {
			log.trace("System input equal to nominalSplitting or in DeadZone of system input, switching sign off!");
			allEqual = true;
			for (int i = 0; i < this.controlEvents; i++) {
				this.currentRouteSet.add(null);
			}
		}
		else {
			int numberOfOnes = (int) ((input * (this.controlEvents + 1)) - 0.1);
			int numberOfZeros = this.controlEvents - numberOfOnes;
			//as the nextUpdate is shortened if only one of the routes are shown all the time, only the first route has to be set
			if (numberOfOnes == this.controlEvents) {
				allEqual = true;
				this.currentRouteSet.add(this.mainRoute);
				log.trace("Sign shows the main route");
			}
			else if (numberOfZeros == this.controlEvents) {
				allEqual = true;
				this.currentRouteSet.add(this.alternativeRoute);
				log.trace("Sign shows the alternative route");
			}
			//we split the traffic according to a certain percentage thus we need all entries of the currentRouteSet
			else {
				log.trace("Sign shows sequence: ");
				allEqual = false;
				int ones = 0;
				int zeros = 0;
				//computing randomized sequence of binary guidance message
				while ((ones + zeros) < this.controlEvents) {
					if ((input > MatsimRandom.getRandom().nextDouble()) && (ones < numberOfOnes)) {
						this.currentRouteSet.add((ones + zeros), this.mainRoute);
						ones++;
						log.trace("1, i.e. main route");
					}
					else if ((input < MatsimRandom.getRandom().nextDouble()) && (zeros < numberOfZeros)) {
						this.currentRouteSet.add((ones + zeros), this.alternativeRoute);
						zeros++;
						log.trace("0, i.e. alternative route");
					}
				}
			}
		}
		if (allEqual) {
			this.nextUpdate = this.nextUpdate + this.messageHoldTime;
		}
		else {
			this.nextUpdate = this.nextUpdate + this.controlUpdateTime ;
		}
		if (log.isTraceEnabled()) {
			log.trace("next update of sign will be at timestep: " + this.nextUpdate);
		}
	}

	/**
	 * Returns the guidance message as a <code>Route</code>
	 * @return current route
	 */
	public NetworkRoute requestRoute() {
		double time = QSimTimer.getTime();
		double trust = MatsimRandom.getRandom().nextDouble();
		if (time > this.nextUpdate) {
			throw new RuntimeException(
					"Current system time: "
							+ time
							+ " is greater than the next update time of the sign: "
							+ this.nextUpdate
							+ ", thus the sign will never be updated and is not initialized correctly!");
		} else if (this.complianceRate < trust) {
			if (log.isTraceEnabled()) {
				log.trace("trust in sign is less than compliance rate, i.e. "
						+ this.complianceRate + " < " + trust);
			}
			return null;
		} else {
			double deltaT = this.nextUpdate - time - 1;
			int index;
			index = (int) deltaT / this.messageHoldTime;
			if (log.isTraceEnabled()) {
				log.trace("nextUpdate: " + this.nextUpdate);
				log.trace("time: " + time);
				log.trace("index: " + index);
			}
			this.currentRoute = this.currentRouteSet.get(index);
			return this.currentRoute;
		}
	}

	private NetworkRoute completeRoute(final NetworkRoute r) {
		NetworkRoute ret = new LinkNetworkRouteImpl(null, null);
		Link startLink = this.network.getLinks().get(r.getStartLinkId());
		Node startNode = startLink.getFromNode();
		ArrayList<Node> rNodes = new ArrayList<Node>();
		rNodes.addAll(RouteUtils.getNodes(r, this.network));
//		if (this.signLink != startLink) {
		if (!(this.signLink.getToNode().equals(startNode) || (this.signLink == startLink))) {
			startLink = calculateInLink(this.signLink, startNode);
			rNodes.add(0, startNode);
		}
		Link endLink  = this.network.getLinks().get(r.getEndLinkId());
		Node endNode = endLink.getToNode();
		if (!(this.directionLink.getFromNode().equals(endNode) || (this.directionLink == endLink))) {
			endLink = calculateOutLink(endNode);
			rNodes.add(endNode);
		}
		ret.setLinkIds(startLink.getId(), NetworkUtils.getLinkIds(RouteUtils.getLinksFromNodes(rNodes)), endLink.getId());
		return ret;
	}

	private Link calculateInLink(final Link signLink, final Node firstRouteNode) {
		for (Link l : firstRouteNode.getInLinks().values()) {
			if (l.getFromNode().equals(signLink.getToNode())) {
				return l;
			}
		}
		throw new UnsupportedOperationException(
				"Calculation of inLinks failed, please extend implementation!");
	}

	private Link calculateOutLink(final Node lastRouteNode) {
		for (Link l : this.directionLink.getFromNode().getInLinks().values()) {
			if (l.getFromNode().equals(lastRouteNode)) {
				return l;
			}
		}
		throw new UnsupportedOperationException(
				"Calculation of OutLinks failed, please extend implementation!");
	}

	// #############################################################################
	// normal getter and setter
	/**
	 * Returns the <code>signLinks</code>
	 *
	 * @return signLinks
	 */
	public Link getSignLink() {
		return this.signLink;
	}

	/**
	 * Returns the <code>directionLink</code>
	 *
	 * @return directionLink
	 */
	public Link getDirectionLink() {
		return this.directionLink;
	}

	/**
	 * @param controlInput
	 *          the controlInput to set
	 */
	public void setControlInput(final ControlInput controlInput) {
		this.controlInput = controlInput;
	}

	public ControlInput getControlInput() {
		return this.controlInput;
	}

	/**
	 * @param controler
	 *          the controler to set
	 */
	public void setControler(final FeedbackControler controler) {
		this.controler = controler;
	}

	/**
	 * @param controlEvents
	 *          the controlEvents to set
	 */
	public void setControlEvents(final int controlEvents) {
		this.controlEvents = controlEvents;
	}

	/**
	 * @param nominalSplitting
	 *          the nominalSplitting to set
	 */
	public void setNominalSplitting(final double nominalSplitting) {
		this.nominalSplitting = nominalSplitting;
	}

	/**
	 * @param deadZone
	 *          the deadZone to set
	 */
	public void setDeadZoneSystemInput(final double deadZone) {
		this.deadZoneSystemInput = deadZone;
	}

	/**
	 * @param deadZone
	 *          the deadZone to set
	 */
	public void setDeadZoneSystemOutput(final double deadZone) {
		this.deadZoneSystemOutput = deadZone;
	}

	/**
	 * @param benefitControl
	 *          the benefitControl to set
	 */
	public void setBenefitControl(final boolean benefitControl) {
		this.benefitControl = benefitControl;
	}

	/**
	 * Sets the link where the sign is placed in the network
	 *
	 * @param link
	 *          the link
	 */
	public void setSignLink(final Link link) {
		this.signLink = link;
	}

	public void setDirectionLink(final Link link) {
		this.directionLink = link;
	}

	public void setMessageHoldTime(final int time) {
		this.messageHoldTime = time;
	}

	public void setCompliance(final double complianceRate) {
		this.complianceRate = complianceRate;
	}

	public void setOutput(VDSSignOutput vdsSignOutput) {
		this.signOutput = vdsSignOutput;
	}

	public void finishIteration() {
		if (this.controlInput != null) {
			this.controlInput.finishIteration();
		}
		if (this.signOutput != null) {
			this.signOutput.close();
		}
	}

}