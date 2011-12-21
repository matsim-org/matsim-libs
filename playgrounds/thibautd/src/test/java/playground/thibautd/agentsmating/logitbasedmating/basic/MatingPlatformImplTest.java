/* *********************************************************************** *
 * project: org.matsim.*
 * MatingPlatformImplTest.java
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
package playground.thibautd.agentsmating.logitbasedmating.basic;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.core.basic.v01.IdImpl;

import playground.thibautd.agentsmating.logitbasedmating.framework.Alternative;
import playground.thibautd.agentsmating.logitbasedmating.framework.ChoiceModel;
import playground.thibautd.agentsmating.logitbasedmating.framework.ChoiceSetFactory;
import playground.thibautd.agentsmating.logitbasedmating.framework.DecisionMaker;
import playground.thibautd.agentsmating.logitbasedmating.framework.DecisionMakerFactory;
import playground.thibautd.agentsmating.logitbasedmating.framework.MateProposer;
import playground.thibautd.agentsmating.logitbasedmating.framework.Mating;
import playground.thibautd.agentsmating.logitbasedmating.framework.TripRequest;
import playground.thibautd.agentsmating.logitbasedmating.framework.TripRequest.Type;
import playground.thibautd.agentsmating.logitbasedmating.framework.UnexistingAttributeException;

/**
 * @author thibautd
 */
public class MatingPlatformImplTest {

	@Test
	public void testAffectation() {
		MatingPlatformImpl platform =
			new MatingPlatformImpl( new FakeModel() , new FakeProposer() );

		GraphInfo.passRequests( platform );

		List<Mating> matings = platform.getMatings();

		Assert.assertEquals(
				"unexpected number of matings",
				GraphInfo.driver2passenger.size(),
				matings.size());

		for (Mating mating : matings) {
			Id driver = mating.getDriver().getDecisionMaker().getPersonId();
			Id passenger = mating.getPassengers().get(0).getDecisionMaker().getPersonId();

			Assert.assertEquals(
					"unexpected passenger for driver "+driver,
					GraphInfo.driver2passenger.get( driver ),
					passenger);
		}
	}

}

// the classes defined hereafter are fake model components, which create
// a simple and known graph.
// -----------------------------------------------------------------------------
class FakeModel implements ChoiceModel {

	@Override
	public Alternative performChoice(
			DecisionMaker decisionMaker,
			List<Alternative> alternatives) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Map<Alternative, Double> getChoiceProbabilities(
			DecisionMaker decisionMaker, List<Alternative> alternatives) {
		throw new UnsupportedOperationException();
	}

	@Override
	public DecisionMakerFactory getDecisionMakerFactory() {
		throw new UnsupportedOperationException();
	}

	@Override
	public ChoiceSetFactory getChoiceSetFactory() {
		throw new UnsupportedOperationException();
	}

	@Override
	public double getSystematicUtility(
			DecisionMaker decisionMaker,
			Alternative alternative) {
		if (alternative instanceof TripRequest) {
			Id goodId = GraphInfo.driver2passenger.get( decisionMaker.getPersonId() );

			if ( ((TripRequest) alternative).getDecisionMaker().getPersonId().equals( goodId ) ) {
				return 2;
			}
			return 1;
		}

		return 0;
	}

	@Override
	public TripRequest changePerspective(TripRequest tripToConsider,
			TripRequest perspective) {
		return tripToConsider;
	}
}

class FakeProposer implements MateProposer {

	@Override
	public <T extends TripRequest> List<T> proposeMateList(
			final TripRequest trip,
			final List<T> allPossibleMates) throws UnhandledMatingDirectionException {
		if (trip.getTripType().equals( Type.PASSENGER )) throw new UnhandledMatingDirectionException();

		Id driverId = trip.getDecisionMaker().getPersonId();

		List<T> out = new ArrayList<T>();
		List<Id> validIds = GraphInfo.driver2passengerLinks.get( driverId );

		for (T request : allPossibleMates) {
			if ( validIds.contains( request.getDecisionMaker().getPersonId() ) ) {
				out.add( request );
			}
		}

		return out;
	}
}

class FakeTripRequest implements TripRequest {
	private final Id id;
	private final FakeDecisionMaker decisionMaker;
	private static final List<Alternative> alts =
		Arrays.asList( (Alternative) new AlternativeImpl( null , new HashMap<String , Object>() ) );

	public FakeTripRequest(final Id id) {
		this.id = id;
		this.decisionMaker = new FakeDecisionMaker( id );
	}

	@Override
	public Type getTripType() {
		return GraphInfo.drivers.contains( id ) ? Type.DRIVER : Type.PASSENGER;
	}

	@Override
	public DecisionMaker getDecisionMaker() {
		return decisionMaker;
	}


	// //////////////////////////////////////////////////////////////////////////
	// interface (unimplemented
	// //////////////////////////////////////////////////////////////////////////
	@Override
	public String getMode() {
		throw new UnsupportedOperationException();
	}

	@Override
	public double getAttribute(String attribute)
			throws UnexistingAttributeException {
		throw new UnexistingAttributeException();
	}

	@Override
	public Map<String, Object> getAttributes() {
		throw new UnsupportedOperationException();
	}

	@Override
	public List<Alternative> getAlternatives() {
		return alts;
	}

	@Override
	public int getIndexInPlan() {
		throw new UnsupportedOperationException();
	}

	@Override
	public double getDepartureTime() {
		throw new UnsupportedOperationException();
	}

	@Override
	public double getPlanArrivalTime() {
		throw new UnsupportedOperationException();
	}

	@Override
	public Activity getOrigin() {
		throw new UnsupportedOperationException();
	}

	@Override
	public Activity getDestination() {
		throw new UnsupportedOperationException();
	}
}

class FakeDecisionMaker implements DecisionMaker {
	private final Id id;

	public FakeDecisionMaker(final Id id) {
		this.id = id;
	}

	@Override
	public double getAttribute(String attribute)
			throws UnexistingAttributeException {
		throw new UnexistingAttributeException();
	}

	@Override
	public Map<String, Object> getAttributes() {
		throw new UnsupportedOperationException();
	}

	@Override
	public Id getPersonId() {
		return id;
	}
}

class GraphInfo {
	static final List<Id> drivers;
	static final List<Id> passengers;

	static final Map<Id, List<Id>> driver2passengerLinks;
	static final Map<Id, Id> driver2passenger;

	static {
		Id id1 = new IdImpl( 1 );
		Id id2 = new IdImpl( 2 );
		Id id3 = new IdImpl( 3 );
		Id id4 = new IdImpl( 4 );
		Id id5 = new IdImpl( 5 );
		Id id6 = new IdImpl( 6 );
		Id id7 = new IdImpl( 7 );

		drivers = Arrays.asList( id1 , id2 , id3 );
		passengers = Arrays.asList( id4 , id5 , id6 , id7 );

		Map<Id, List<Id>> links = new HashMap<Id, List<Id>>();
		links.put( id1 , Arrays.asList( id4 , id5 ) );
		links.put( id2 , Arrays.asList( id4 , id5 , id6 ) );
		links.put( id3 , Arrays.asList( id5 , id6 , id7 ) );

		driver2passengerLinks = Collections.unmodifiableMap( links );

		Map<Id, Id> matings = new HashMap<Id, Id>();
		matings.put( id1 , id4 );
		matings.put( id2 , id5 );
		matings.put( id3 , id6 );

		driver2passenger = Collections.unmodifiableMap( matings );
	}

	public static void passRequests(final MatingPlatformImpl platform) {
		for (Id id : drivers) {
			platform.handleRequest( new FakeTripRequest( id ) );
		}
		for (Id id : passengers) {
			platform.handleRequest( new FakeTripRequest( id ) );
		}
	}
}
