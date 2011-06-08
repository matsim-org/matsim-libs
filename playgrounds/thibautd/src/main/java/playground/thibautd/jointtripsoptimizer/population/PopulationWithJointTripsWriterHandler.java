/* *********************************************************************** *
 * project: org.matsim.*
 * PopulationWithJointTripsWriterHandler.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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
package playground.thibautd.jointtripsoptimizer.population;

import java.io.BufferedWriter;
import java.io.IOException;

import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.facilities.ActivityOptionImpl;
import org.matsim.core.population.AbstractPopulationWriterHandler;
import org.matsim.core.population.PopulationWriterHandlerImplV4;
import org.matsim.knowledges.KnowledgeImpl;
import org.matsim.knowledges.Knowledges;
import org.matsim.population.Desires;

/**
 * Exports joint plans with a "linked" naming of pick-up activities.
 *
 * This allows to import again the exported plans.
 *
 * HOWEVER:
 * <ul>
 * <li>synchronisation at import modifies the plans slightly</li>
 * <li>all non-selected plans are lost at import: if joint trip was
 * unselected, it is irremediably lost</li>
 * </ul>
 * @author thibautd
 */
public class PopulationWithJointTripsWriterHandler 
	extends AbstractPopulationWriterHandler {

	private final AbstractPopulationWriterHandler delegate;

	public PopulationWithJointTripsWriterHandler(Network network) {
		super();
		this.delegate = new PopulationWriterHandlerImplV4(network);
	}

	public PopulationWithJointTripsWriterHandler(Network network, Knowledges knowledges) {
		super(knowledges);
		this.delegate = new PopulationWriterHandlerImplV4(network, knowledges);
	}

	/*
	 * =========================================================================
	 * activity-related starters
	 * =========================================================================
	 */
	@Override
	public void startActDur(String act_type, double dur, BufferedWriter out)
			throws IOException {
		checkAct(act_type);
		this.delegate.startActDur(act_type, dur, out);
	}

	@Override
	public void startActivity(String act_type, BufferedWriter out)
			throws IOException {
		checkAct(act_type);
		this.delegate.startActivity(act_type, out);
	}

	private void checkAct(String type) {
		if (type.equals(JointActingTypes.PICK_UP)) {
			throw new IllegalArgumentException("cannot handle PU acts by type");
		}
	}

	@Override
	public void startAct(Activity act, BufferedWriter out) throws IOException {
		if (act instanceof JointActivity) {
			JointActivity jointAct = new JointActivity((JointActivity) act);
			jointAct.setType(((JointActivity) act).getInitialType());
			this.delegate.startAct(jointAct, out);
		}
		else {
			this.delegate.startAct(act, out);
		}
	}



	/*
	 * =========================================================================
	 * delegate methods
	 * =========================================================================
	 */
	@Override
	public void writeHeaderAndStartElement(BufferedWriter out)
			throws IOException {
		this.delegate.writeHeaderAndStartElement(out);	
	}

	@Override
	public void startPlans(Population plans, BufferedWriter out)
			throws IOException {
		this.delegate.startPlans(plans, out);
	}

	@Override
	public void endPlans(BufferedWriter out) throws IOException {
		this.delegate.endPlans(out);
	}

	@Override
	public void writeSeparator(BufferedWriter out) throws IOException {
		this.delegate.writeSeparator(out);
	}

	@Override
	public void startPerson(Person person, BufferedWriter out)
			throws IOException {
		this.delegate.startPerson(person, out);
		
	}

	@Override
	public void endPerson(BufferedWriter out) throws IOException {
		this.delegate.endPerson(out);
	}

	@Override
	public void startTravelCard(String travelcard, BufferedWriter out)
			throws IOException {
		this.delegate.startTravelCard(travelcard, out);
	}

	@Override
	public void endTravelCard(BufferedWriter out) throws IOException {
		this.delegate.endTravelCard(out);
	}

	@Override
	public void startDesires(Desires desires, BufferedWriter out)
			throws IOException {
		this.delegate.startDesires(desires, out);
	}

	@Override
	public void endDesires(BufferedWriter out) throws IOException {
		this.delegate.endDesires(out);
	}



	@Override
	public void endActDur(BufferedWriter out) throws IOException {
		this.delegate.endActDur(out);
	}

	@Override
	public void startKnowledge(KnowledgeImpl knowledge, BufferedWriter out)
			throws IOException {
		this.delegate.startKnowledge(knowledge, out);
	}

	@Override
	public void endKnowledge(BufferedWriter out) throws IOException {
		this.delegate.endKnowledge(out);
	}



	@Override
	public void endActivity(BufferedWriter out) throws IOException {
		this.delegate.endActivity(out);
	}

	@Override
	public void startPrimaryLocation(ActivityOptionImpl activity,
			BufferedWriter out) throws IOException {
		this.delegate.startPrimaryLocation(activity, out);
	}

	@Override
	public void endPrimaryLocation(BufferedWriter out) throws IOException {
		this.delegate.endPrimaryLocation(out);
	}

	@Override
	public void startSecondaryLocation(ActivityOptionImpl activity,
			BufferedWriter out) throws IOException {
		this.delegate.startSecondaryLocation(activity, out);
	}

	@Override
	public void endSecondaryLocation(BufferedWriter out) throws IOException {
		this.delegate.endSecondaryLocation(out);
	}

	@Override
	public void startPlan(Plan plan, BufferedWriter out) throws IOException {
		this.delegate.startPlan(plan, out);
	}

	@Override
	public void endPlan(BufferedWriter out) throws IOException {
		this.delegate.endPlan(out);
	}

	@Override
	public void endAct(BufferedWriter out) throws IOException {
		this.delegate.endAct(out);
	}

	@Override
	public void startLeg(Leg leg, BufferedWriter out) throws IOException {
		this.delegate.startLeg(leg, out);
	}

	@Override
	public void endLeg(BufferedWriter out) throws IOException {
		this.delegate.endLeg(out);
	}

	@Override
	public void startRoute(Route route, BufferedWriter out) throws IOException {
		this.delegate.startRoute(route, out);
	}

	@Override
	public void endRoute(BufferedWriter out) throws IOException {
		this.delegate.endRoute(out);
	}
}

