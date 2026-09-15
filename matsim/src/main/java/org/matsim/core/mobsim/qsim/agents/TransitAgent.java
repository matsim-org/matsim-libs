/* *********************************************************************** *
 * project: org.matsim.*
 * TransitAgent.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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

package org.matsim.core.mobsim.qsim.agents;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Message;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.mobsim.dsim.DistributedMobsimAgent;
import org.matsim.core.mobsim.framework.HasPerson;
import org.matsim.core.mobsim.framework.PlanAgent;
import org.matsim.core.mobsim.qsim.interfaces.MobsimVehicle;
import org.matsim.core.mobsim.qsim.interfaces.Netsim;
import org.matsim.core.mobsim.qsim.pt.MobsimDriverPassengerAgent;
import org.matsim.core.mobsim.qsim.pt.TransitVehicle;
import org.matsim.core.utils.misc.OptionalTime;
import org.matsim.core.utils.timing.TimeInterpretation;
import org.matsim.facilities.Facility;
import org.matsim.pt.transitSchedule.api.*;
import org.matsim.vehicles.Vehicle;

import java.util.List;

/**
 * @author mrieser
 */
public final class TransitAgent implements DistributedMobsimAgent, MobsimDriverPassengerAgent, PlanAgent, HasPerson, HasModifiablePlan {
	@SuppressWarnings("unused")
	private static final Logger log = LogManager.getLogger(TransitAgent.class);

	private final BasicPlanAgentImpl basicAgentDelegate;
	private final PlanBasedDriverAgentImpl driverAgentDelegate;
	private final TransitAgentImpl transitAgentDelegate;

	public static TransitAgent createTransitAgent(Person p, Netsim simulation, TimeInterpretation timeInterpretation) {
		return new TransitAgent(p, simulation, timeInterpretation);
	}

	public static TransitAgent createTransitAgent(BasicPlanAgentImpl basicAgentDelegate, Scenario scenario) {
		return new TransitAgent(basicAgentDelegate, scenario);
	}

	private TransitAgent(final Person p, final Netsim simulation, TimeInterpretation timeInterpretation) {
		basicAgentDelegate = new BasicPlanAgentImpl(p.getSelectedPlan(), simulation.getScenario(), simulation.getEventsManager(),
			simulation.getSimTimer(), timeInterpretation);
		driverAgentDelegate = new PlanBasedDriverAgentImpl(basicAgentDelegate);
		transitAgentDelegate = new TransitAgentImpl(basicAgentDelegate, simulation.getScenario().getConfig().transit().getBoardingAcceptance());
	}

	private TransitAgent(BasicPlanAgentImpl basicAgentDelegate, Scenario scenario) {
		this.basicAgentDelegate = basicAgentDelegate;
		driverAgentDelegate = new PlanBasedDriverAgentImpl(basicAgentDelegate);
		transitAgentDelegate = new TransitAgentImpl(basicAgentDelegate, scenario.getConfig().transit().getBoardingAcceptance());
	}

	@Override
	public void endLegAndComputeNextState(double now) {
		basicAgentDelegate.endLegAndComputeNextState(now);
	}

	@Override
	public void setStateToAbort(double now) {
		basicAgentDelegate.setStateToAbort(now);
	}

	@Override
	public void notifyArrivalOnLinkByNonNetworkMode(Id<Link> linkId) {
		basicAgentDelegate.notifyArrivalOnLinkByNonNetworkMode(linkId);
	}

	@Override
	public void endActivityAndComputeNextState(double now) {
		basicAgentDelegate.endActivityAndComputeNextState(now);
	}

	@Override
	public Id<Vehicle> getPlannedVehicleId() {
		return basicAgentDelegate.getPlannedVehicleId();
	}

	@Override
	public String getMode() {
		return basicAgentDelegate.getMode();
	}

	@Override
	public String toString() {
		return basicAgentDelegate.toString();
	}

	@Override
	public OptionalTime getExpectedTravelTime() {
		return basicAgentDelegate.getExpectedTravelTime();
	}

	@Override
	public Double getExpectedTravelDistance() {
		return basicAgentDelegate.getExpectedTravelDistance();
	}
	@Override
	public PlanElement getCurrentPlanElement() {
		return basicAgentDelegate.getCurrentPlanElement();
	}

	@Override
	public PlanElement getNextPlanElement() {
		return basicAgentDelegate.getNextPlanElement();
	}

	@Override
	public Plan getCurrentPlan() {
		return basicAgentDelegate.getCurrentPlan();
	}

	@Override
	public Id<Person> getId() {
		return basicAgentDelegate.getId();
	}

	@Override
	public Person getPerson() {
		return basicAgentDelegate.getPerson();
	}

	@Override
	public MobsimVehicle getVehicle() {
		return basicAgentDelegate.getVehicle();
	}

	@Override
	public void setVehicle(MobsimVehicle vehicle) {
		basicAgentDelegate.setVehicle(vehicle);
	}

	@Override
	public Id<Link> getCurrentLinkId() {
		return basicAgentDelegate.getCurrentLinkId();
	}

	@Override
	public Id<Link> getDestinationLinkId() {
		return basicAgentDelegate.getDestinationLinkId();
	}

	@Override
	public double getActivityEndTime() {
		return basicAgentDelegate.getActivityEndTime();
	}

	@Override
	public State getState() {
		return basicAgentDelegate.getState();
	}

	@Override
	public void notifyMoveOverNode(Id<Link> newLinkId) {
		driverAgentDelegate.notifyMoveOverNode(newLinkId);
	}

	@Override
	public Id<Link> chooseNextLinkId() {
		return driverAgentDelegate.chooseNextLinkId();
	}

	@Override
	public boolean isWantingToArriveOnCurrentLink() {
		return driverAgentDelegate.isWantingToArriveOnCurrentLink();
	}

	@Override
	public boolean getExitAtStop(TransitStopFacility stop) {
		return transitAgentDelegate.getExitAtStop(stop);
	}

	@Override
	public boolean getRelocationAtStop(TransitStopFacility stop) {
		return transitAgentDelegate.getRelocationAtStop(stop);
	}

	@Override
	public boolean getEnterTransitRoute(TransitLine line, TransitRoute transitRoute, List<TransitRouteStop> stopsToCome,
										TransitVehicle transitVehicle) {
		return transitAgentDelegate.getEnterTransitRoute(line, transitRoute, stopsToCome, transitVehicle);
	}

	@Override
	public double getWeight() {
		return transitAgentDelegate.getWeight();
	}

	@Override
	public Id<TransitStopFacility> getDesiredAccessStopId() {
		return transitAgentDelegate.getDesiredAccessStopId();
	}

	@Override
	public Id<TransitStopFacility> getDesiredDestinationStopId() {
		return transitAgentDelegate.getDesiredDestinationStopId();
	}

	@Override
	public PlanElement getPreviousPlanElement() {
		return this.basicAgentDelegate.getPreviousPlanElement();
	}

	@Override
	public Facility getCurrentFacility() {
		return this.basicAgentDelegate.getCurrentFacility();
	}

	@Override
	public Facility getDestinationFacility() {
		return this.basicAgentDelegate.getDestinationFacility();
	}

	@Override
	public Plan getModifiablePlan() {
		return this.basicAgentDelegate.getModifiablePlan();
	}

	@Override
	public void resetCaches() {
		this.basicAgentDelegate.resetCaches();
		this.driverAgentDelegate.resetCaches();
	}

	@Override
	public int getCurrentLinkIndex() {
		return this.basicAgentDelegate.getCurrentLinkIndex();
	}

	@Override
	public Message toMessage() {
		return basicAgentDelegate.toMessage();
	}
}
