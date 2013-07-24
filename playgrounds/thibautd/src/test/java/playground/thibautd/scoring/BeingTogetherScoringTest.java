/* *********************************************************************** *
 * project: org.matsim.*
 * BeingTogetherScoringTest.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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
package playground.thibautd.scoring;

import java.util.Collections;

import org.junit.Assert;
import org.junit.Test;

import org.matsim.api.core.v01.Id;
import org.matsim.core.api.experimental.events.EventsFactory;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.events.EventsUtils;
import org.matsim.testcases.MatsimTestUtils;

import playground.thibautd.scoring.BeingTogetherScoring.AcceptAllFilter;
import playground.thibautd.scoring.BeingTogetherScoring.RejectAllFilter;

/**
 * @author thibautd
 */
public class BeingTogetherScoringTest {

	@Test
	public void testOvelapsOfActivities() throws Exception {
		final Id ego = new IdImpl( "ego" );
		final Id alter = new IdImpl( "alter" );
		
		final Id linkId = new IdImpl( 1 );
		final String type = "type";
		
		final EventsFactory fact = EventsUtils.createEventsManager().getFactory();
		for ( OverlapSpec os : new OverlapSpec[]{
				new OverlapSpec( 0 , 10 , 20 , 30 ),
				new OverlapSpec( 0 , 10 , 10 , 30 ),
				new OverlapSpec( 0 , 20 , 10 , 30 ),
				new OverlapSpec( 10 , 20 , 10 , 30 ),
				new OverlapSpec( 20 , 30 , 10 , 30 ),
				new OverlapSpec( 30 , 50 , 10 , 30 ) }) {
			final BeingTogetherScoring testee =
				new BeingTogetherScoring(
						1,
						ego,
						Collections.singleton( alter ) );
			testee.handleEvent(
					fact.createActivityStartEvent(
						os.startEgo,
						ego,
						linkId,
						null,
						type) );
			testee.handleEvent(
					fact.createActivityStartEvent(
						os.startAlter,
						alter,
						linkId,
						null,
						type) );
			testee.handleEvent(
					fact.createActivityEndEvent(
						os.endEgo,
						ego,
						linkId,
						null,
						type) );
			testee.handleEvent(
					fact.createActivityEndEvent(
						os.endAlter,
						alter,
						linkId,
						null,
						type) );

			Assert.assertEquals(
					"unexpected overlap for "+os,
					Math.max( Math.min( os.endAlter , os.endEgo ) - Math.max( os.startAlter , os.startEgo ) , 0 ),
					testee.getScore(),
					MatsimTestUtils.EPSILON);
		}
	}

	@Test
	public void testWrapAround() throws Exception {
		final Id ego = new IdImpl( "ego" );
		final Id alter = new IdImpl( "alter" );
		
		final Id linkId = new IdImpl( 1 );
		final String type = "type";

		final EventsFactory fact = EventsUtils.createEventsManager().getFactory();

		final BeingTogetherScoring testee =
			new BeingTogetherScoring(
					1,
					ego,
					Collections.singleton( alter ) );

		testee.handleEvent(
				fact.createActivityEndEvent(
					10,
					ego,
					linkId,
					null,
					type) );
		testee.handleEvent(
				fact.createActivityStartEvent(
					10,
					ego,
					linkId,
					null,
					type) );

		testee.handleEvent(
				fact.createActivityEndEvent(
					100,
					alter,
					linkId,
					null,
					type) );
		testee.handleEvent(
				fact.createActivityStartEvent(
					100,
					alter,
					linkId,
					null,
					type) );

		Assert.assertEquals(
				"unexpected overlap",
				24 * 3600,
				testee.getScore(),
				MatsimTestUtils.EPSILON);
	}

	@Test
	public void testWrapAroundAfter24h() throws Exception {
		final Id ego = new IdImpl( "ego" );
		final Id alter = new IdImpl( "alter" );
		
		final Id linkId = new IdImpl( 1 );
		final String type = "type";

		final EventsFactory fact = EventsUtils.createEventsManager().getFactory();

		final BeingTogetherScoring testee =
			new BeingTogetherScoring(
					1,
					ego,
					Collections.singleton( alter ) );

		testee.handleEvent(
				fact.createActivityEndEvent(
					10,
					ego,
					linkId,
					null,
					type) );
		testee.handleEvent(
				fact.createActivityStartEvent(
					10,
					ego,
					linkId,
					null,
					type) );

		testee.handleEvent(
				fact.createActivityEndEvent(
					26 * 3600,
					alter,
					linkId,
					null,
					type) );
		testee.handleEvent(
				fact.createActivityStartEvent(
					26 * 3600,
					alter,
					linkId,
					null,
					type) );

		Assert.assertEquals(
				"unexpected overlap",
				24 * 3600,
				testee.getScore(),
				MatsimTestUtils.EPSILON);
	}

	@Test
	public void testNoOverlapIfDifferentActTypes() throws Exception {
		final Id ego = new IdImpl( "ego" );
		final Id alter = new IdImpl( "alter" );
		
		final Id linkId = new IdImpl( 1 );
		final String type = "type";
		final String type2 = "type2";

		final EventsFactory fact = EventsUtils.createEventsManager().getFactory();

		final BeingTogetherScoring testee =
			new BeingTogetherScoring(
					1,
					ego,
					Collections.singleton( alter ) );

		testee.handleEvent(
				fact.createActivityStartEvent(
					0,
					ego,
					linkId,
					null,
					type) );
		testee.handleEvent(
				fact.createActivityStartEvent(
					0,
					alter,
					linkId,
					null,
					type2) );
		testee.handleEvent(
				fact.createActivityEndEvent(
					100,
					ego,
					linkId,
					null,
					type) );
		testee.handleEvent(
				fact.createActivityEndEvent(
					100,
					alter,
					linkId,
					null,
					type2) );

		Assert.assertEquals(
				"unexpected overlap",
				0,
				testee.getScore(),
				MatsimTestUtils.EPSILON);

	}

	@Test
	public void testNoOverlapIfDifferentLocations() throws Exception {
		final Id ego = new IdImpl( "ego" );
		final Id alter = new IdImpl( "alter" );
		
		final Id linkId = new IdImpl( 1 );
		final Id linkId2 = new IdImpl( 2 );
		final String type = "type";

		final EventsFactory fact = EventsUtils.createEventsManager().getFactory();

		final BeingTogetherScoring testee =
			new BeingTogetherScoring(
					1,
					ego,
					Collections.singleton( alter ) );

		testee.handleEvent(
				fact.createActivityStartEvent(
					0,
					ego,
					linkId,
					null,
					type) );
		testee.handleEvent(
				fact.createActivityStartEvent(
					0,
					alter,
					linkId2,
					null,
					type) );
		testee.handleEvent(
				fact.createActivityEndEvent(
					100,
					ego,
					linkId,
					null,
					type) );
		testee.handleEvent(
				fact.createActivityEndEvent(
					100,
					alter,
					linkId2,
					null,
					type) );

		Assert.assertEquals(
				"unexpected overlap",
				0,
				testee.getScore(),
				MatsimTestUtils.EPSILON);

	}

	@Test
	public void testNoOverlapIfRejectedActivity() throws Exception {
		final Id ego = new IdImpl( "ego" );
		final Id alter = new IdImpl( "alter" );
		
		final Id linkId = new IdImpl( 1 );
		final String type = "type";

		final EventsFactory fact = EventsUtils.createEventsManager().getFactory();

		final BeingTogetherScoring testee =
			new BeingTogetherScoring(
					new RejectAllFilter(),
					new AcceptAllFilter(),
					1,
					ego,
					Collections.singleton( alter ) );

		testee.handleEvent(
				fact.createActivityStartEvent(
					0,
					ego,
					linkId,
					null,
					type) );
		testee.handleEvent(
				fact.createActivityStartEvent(
					0,
					alter,
					linkId,
					null,
					type) );
		testee.handleEvent(
				fact.createActivityEndEvent(
					100,
					ego,
					linkId,
					null,
					type) );
		testee.handleEvent(
				fact.createActivityEndEvent(
					100,
					alter,
					linkId,
					null,
					type) );

		Assert.assertEquals(
				"unexpected overlap",
				0,
				testee.getScore(),
				MatsimTestUtils.EPSILON);

	}

	@Test
	public void testOvelapsOfLegs() throws Exception {
		final Id ego = new IdImpl( "ego" );
		final Id alter = new IdImpl( "alter" );
		
		final Id vehId = new IdImpl( 1 );
		
		final EventsFactory fact = EventsUtils.createEventsManager().getFactory();
		for ( OverlapSpec os : new OverlapSpec[]{
				new OverlapSpec( 0 , 10 , 20 , 30 ),
				new OverlapSpec( 0 , 10 , 10 , 30 ),
				new OverlapSpec( 0 , 20 , 10 , 30 ),
				new OverlapSpec( 10 , 20 , 10 , 30 ),
				new OverlapSpec( 20 , 30 , 10 , 30 ),
				new OverlapSpec( 30 , 50 , 10 , 30 ) }) {
			final BeingTogetherScoring testee =
				new BeingTogetherScoring(
						1,
						ego,
						Collections.singleton( alter ) );
			testee.handleEvent(
					fact.createPersonEntersVehicleEvent(
						os.startEgo,
						ego,
						vehId ) );
			testee.handleEvent(
					fact.createPersonEntersVehicleEvent(
						os.startAlter,
						alter,
						vehId ) );
			testee.handleEvent(
					fact.createPersonLeavesVehicleEvent(
						os.endEgo,
						ego,
						vehId ) );
			testee.handleEvent(
					fact.createPersonLeavesVehicleEvent(
						os.endAlter,
						alter,
						vehId) );

			Assert.assertEquals(
					"unexpected overlap for "+os,
					Math.max( Math.min( os.endAlter , os.endEgo ) - Math.max( os.startAlter , os.startEgo ) , 0 ),
					testee.getScore(),
					MatsimTestUtils.EPSILON);
		}
	}

	@Test
	public void testNoOvelapIfDifferentVehicles() throws Exception {
		final Id ego = new IdImpl( "ego" );
		final Id alter = new IdImpl( "alter" );
		
		final Id vehId = new IdImpl( 1 );
		final Id vehId2 = new IdImpl( 2 );
		
		final EventsFactory fact = EventsUtils.createEventsManager().getFactory();
		for ( OverlapSpec os : new OverlapSpec[]{
				new OverlapSpec( 0 , 10 , 20 , 30 ),
				new OverlapSpec( 0 , 10 , 10 , 30 ),
				new OverlapSpec( 0 , 20 , 10 , 30 ),
				new OverlapSpec( 10 , 20 , 10 , 30 ),
				new OverlapSpec( 20 , 30 , 10 , 30 ),
				new OverlapSpec( 30 , 50 , 10 , 30 ) }) {
			final BeingTogetherScoring testee =
				new BeingTogetherScoring(
						1,
						ego,
						Collections.singleton( alter ) );
			testee.handleEvent(
					fact.createPersonEntersVehicleEvent(
						os.startEgo,
						ego,
						vehId ) );
			testee.handleEvent(
					fact.createPersonEntersVehicleEvent(
						os.startAlter,
						alter,
						vehId2 ) );
			testee.handleEvent(
					fact.createPersonLeavesVehicleEvent(
						os.endEgo,
						ego,
						vehId ) );
			testee.handleEvent(
					fact.createPersonLeavesVehicleEvent(
						os.endAlter,
						alter,
						vehId2) );

			Assert.assertEquals(
					"unexpected overlap for "+os,
					0,
					testee.getScore(),
					MatsimTestUtils.EPSILON);
		}
	}

	@Test
	public void testNoOvelapIfRejectedMode() throws Exception {
		final Id ego = new IdImpl( "ego" );
		final Id alter = new IdImpl( "alter" );
		
		final Id vehId = new IdImpl( 1 );
		
		final EventsFactory fact = EventsUtils.createEventsManager().getFactory();
		for ( OverlapSpec os : new OverlapSpec[]{
				new OverlapSpec( 0 , 10 , 20 , 30 ),
				new OverlapSpec( 0 , 10 , 10 , 30 ),
				new OverlapSpec( 0 , 20 , 10 , 30 ),
				new OverlapSpec( 10 , 20 , 10 , 30 ),
				new OverlapSpec( 20 , 30 , 10 , 30 ),
				new OverlapSpec( 30 , 50 , 10 , 30 ) }) {
			final BeingTogetherScoring testee =
				new BeingTogetherScoring(
						new AcceptAllFilter(),
						new RejectAllFilter(),
						1,
						ego,
						Collections.singleton( alter ) );
			testee.handleEvent(
					fact.createAgentDepartureEvent(
						0,
						new IdImpl( 1 ),
						ego,
						"mode" ) );
			testee.handleEvent(
					fact.createPersonEntersVehicleEvent(
						os.startEgo,
						ego,
						vehId ) );
			testee.handleEvent(
					fact.createAgentDepartureEvent(
						0,
						new IdImpl( 1 ),
						alter,
						"mode" ) );
			testee.handleEvent(
					fact.createPersonEntersVehicleEvent(
						os.startAlter,
						alter,
						vehId ) );
			testee.handleEvent(
					fact.createPersonLeavesVehicleEvent(
						os.endEgo,
						ego,
						vehId ) );
			testee.handleEvent(
					fact.createPersonLeavesVehicleEvent(
						os.endAlter,
						alter,
						vehId) );

			Assert.assertEquals(
					"unexpected overlap for "+os,
					0,
					testee.getScore(),
					MatsimTestUtils.EPSILON);
		}
	}

	@Test
	public void testOvelapsOfActivitiesInActiveTimeWindow() throws Exception {
		final Id ego = new IdImpl( "ego" );
		final Id alter = new IdImpl( "alter" );
		
		final Id linkId = new IdImpl( 1 );
		final String type = "type";

		final double startWindow = 10;
		final double endWindow = 30;
		
		final EventsFactory fact = EventsUtils.createEventsManager().getFactory();
		for ( OverlapSpec os : new OverlapSpec[]{
				new OverlapSpec( 0 , 10 , 20 , 30 ),
				new OverlapSpec( 0 , 10 , 10 , 30 ),
				new OverlapSpec( 0 , 20 , 10 , 30 ),
				new OverlapSpec( 10 , 20 , 10 , 30 ),
				new OverlapSpec( 20 , 30 , 10 , 30 ),
				new OverlapSpec( 30 , 50 , 10 , 30 ) }) {
			final BeingTogetherScoring testee =
				new BeingTogetherScoring(
						startWindow,
						endWindow,
						1,
						ego,
						Collections.singleton( alter ) );
			testee.handleEvent(
					fact.createActivityStartEvent(
						os.startEgo,
						ego,
						linkId,
						null,
						type) );
			testee.handleEvent(
					fact.createActivityStartEvent(
						os.startAlter,
						alter,
						linkId,
						null,
						type) );
			testee.handleEvent(
					fact.createActivityEndEvent(
						os.endEgo,
						ego,
						linkId,
						null,
						type) );
			testee.handleEvent(
					fact.createActivityEndEvent(
						os.endAlter,
						alter,
						linkId,
						null,
						type) );

			Assert.assertEquals(
					"unexpected overlap for "+os,
					Math.max(
						Math.min(
							endWindow,
							Math.min(
								os.endAlter,
								os.endEgo ) ) -
						Math.max(
							startWindow,
							Math.max(
								os.startAlter,
								os.startEgo ) ) ,
						0 ),
					testee.getScore(),
					MatsimTestUtils.EPSILON);
		}
	}

	private static class OverlapSpec {
		public double startEgo, startAlter, endEgo, endAlter;

		public OverlapSpec(
				final double startEgo,
				final double endEgo,
				final double startAlter,
				final double endAlter) {
			this.startEgo = startEgo;
			this.startAlter = startAlter;
			this.endEgo = endEgo;
			this.endAlter = endAlter;
		}

		@Override
		public String toString() {
			return "[OvelapSpec: startEgo="+startEgo+
				", endEgo="+endEgo+
				", startAlter="+startAlter+
				", endAlter="+endAlter+"]";
		}
	}
}

