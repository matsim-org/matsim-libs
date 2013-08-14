/* *********************************************************************** *
 * project: org.matsim.*
 * BiPedQTest.java
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

package org.matsim.core.mobsim.qsim.qnetsimengine;

import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.mobsim.framework.MobsimTimer;
import org.matsim.testcases.MatsimTestCase;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleTypeImpl;

public class BiPedQTest extends MatsimTestCase{


	@Test
	public void testBiPedQ() {

		MobsimTimer t = new MobsimTimer(1);
		t.setTime(0);

		double delay = 0.1;
		BiPedQ q0 = new BiPedQ(t,delay);
		BiPedQ q1 = new BiPedQ(t,delay);
		q0.setRevQ(q1);
		q1.setRevQ(q0);


		double travelTime = 10;

		for (int i = 0; i < 10; i++) {
			Vehicle bv0  = new DummyVehicle(new IdImpl("q1_"+i));
			QVehicle v0 = new QVehicle(bv0 );
			v0.setEarliestLinkExitTime(travelTime+t.getTimeOfDay());
			q1.offer(v0);
			t.incrementTime();
		}

		Vehicle bv0  = new DummyVehicle(new IdImpl("q0_1"));
		QVehicle v0 = new QVehicle(bv0 );
		double baseTT = travelTime+t.getTimeOfDay();
		v0.setEarliestLinkExitTime(baseTT);
		q0.offer(v0);
		
		
		//this should be baseTT + 10* delay
		assertEquals(q0.peek().getEarliestLinkExitTime(),baseTT+10*delay); 
		
		for (int i = 10; i < 20; i++) {
			Vehicle bv1  = new DummyVehicle(new IdImpl("q1_"+i));
			QVehicle v1 = new QVehicle(bv1 );
			v1.setEarliestLinkExitTime(travelTime+t.getTimeOfDay());
			q1.offer(v1);
			q1.poll();
			t.incrementTime();
			//this should be baseTT + 10* delay
			assertEquals(q0.peek().getEarliestLinkExitTime(),baseTT+10*delay);
		}
		t.incrementTime();
		//this should be baseTT + 20* delay
		assertEquals(q0.peek().getEarliestLinkExitTime(),baseTT+20*delay);
		Vehicle bv1  = new DummyVehicle(new IdImpl("q1_"+21));
		QVehicle v1 = new QVehicle(bv1 );
		v1.setEarliestLinkExitTime(travelTime+t.getTimeOfDay());
		q1.offer(v1);
		t.incrementTime();
		//one additional vehicle entered q1 --> earliest link exit time should be baseTT+21*delay
		assertEquals(q0.peek().getEarliestLinkExitTime(),baseTT+21*delay);

	}


	private static final class DummyVehicle implements Vehicle {


		private final VehicleType vt = new VehicleTypeImpl(new IdImpl("dummy"));
		private final IdImpl id;

		public DummyVehicle(IdImpl idImpl) {
			this.id = idImpl;
		}

		@Override
		public Id getId() {
			return this.id;
		}

		@Override
		public VehicleType getType() {
			return this.vt;
		}

	}
}
