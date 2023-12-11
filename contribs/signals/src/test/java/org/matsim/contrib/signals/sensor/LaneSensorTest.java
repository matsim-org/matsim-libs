package org.matsim.contrib.signals.sensor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.matsim.core.api.experimental.events.LaneEnterEvent;

public class LaneSensorTest {

	@Test
	void testGetAvgVehiclesPerSecondAfterBucketCollection() {
		//test if average is working for constant flow
		LaneSensor sensor = new LaneSensor(null, null);
		sensor.registerAverageVehiclesPerSecondToMonitor(60, 15);
		for (int time = 0; time <= 3600; time++) {
			//generate a vehicle every third second
			if (time % 3 == 0)
				sensor.handleEvent(new LaneEnterEvent(time, null, null, null));
			//after enough buckets are collected the average should be 0,333 per second.
			if (time > 60)
				assertEquals(1.0/3.0, sensor.getAvgVehiclesPerSecond(time), 0.04);
		}
	}

	@Test
	void testGetAvgVehiclesPerSecondDuringBucketCollection() {
		//test if average is working for constant flow
		LaneSensor sensor = new LaneSensor(null, null);
		sensor.registerAverageVehiclesPerSecondToMonitor(60, 15);
		for (int time = 0; time <= 3600; time++) {
			//generate a vehicle every third second
			if (time % 3 == 0)
				sensor.handleEvent(new LaneEnterEvent(time, null, null, null));
			//after enough buckets are collected the average should be 0,333 per second.
			if (time > 15)
				assertEquals(1.0/3.0, sensor.getAvgVehiclesPerSecond(time), 0.04);
		}
	}

	@Test
	void testGetAvgVehiclesPerSecondWithNoTrafficForTwoBucket() {
		//test if average is working for constant flow
		LaneSensor sensor = new LaneSensor(null, null);
		sensor.registerAverageVehiclesPerSecondToMonitor(60, 15);
		for (int time = 0; time <= 3600; time++) {
			//generate a vehicle every third second, but generate two empty buckets (150-165 and 165-180)
			if (time % 3 == 0 && !(time >= 120 && time < 150))
				sensor.handleEvent(new LaneEnterEvent(time, null, null, null));
			//after generating two empty bucket the avg should be 0,1666 for two buket collection times
			if (time > 150 && time < 195)
				assertEquals((1.0/3.0)/(4.0/2.0), sensor.getAvgVehiclesPerSecond(time), 0.02);
			//after a bucket with vehicles is colleted now the avg should be 0,25 since there is still an empty bucket
			if (time > 195 && time < 210)
				assertEquals((1.0/3.0)/(4.0/3.0), sensor.getAvgVehiclesPerSecond(time), 0.02);
			//after four buckets are collected again (150+60)the average should be 0,333 per second.
			if (time > 210)
				assertEquals((1.0/3.0), sensor.getAvgVehiclesPerSecond(time), 0.02);
		}
	}

	@Test
	void testGetAvgVehiclesPerSecondWithNoTrafficForTwoBucketWhileHavingNotEnoughBuckets() {
		//test if average is working for constant flow
		LaneSensor sensor = new LaneSensor(null, null);
		sensor.registerAverageVehiclesPerSecondToMonitor(60, 15);
		for (int time = 0; time <= 3600; time++) {
			//generate a vehicle every third second, but generate two empty buckets (150-165 and 165-180)
			if (time % 3 == 0 && !(time >= 15 && time < 45)) {
				sensor.handleEvent(new LaneEnterEvent(time, null, null, null));
			}
			//after generating two empty bucket the avg should be 0,1111 for two bucket collection times since there's only one full bucket (second 0 to 15) of total three
			if (time > 45 && time < 60)
				assertEquals((1.0/3.0)/(3.0/1.0), sensor.getAvgVehiclesPerSecond(time), 0.02);
			//after collection another full bucket it should be 0,1666 in avg.
			if (time > 60 && time < 90)
				assertEquals((1.0/3.0)/(4.0/2.0), sensor.getAvgVehiclesPerSecond(time), 0.02);
			//after the first empty bucket is removed and avg should be 0,25 since there is still one empty bucket
			if (time > 90 && time < 105)
				assertEquals((1.0/3.0)/(4.0/3.0), sensor.getAvgVehiclesPerSecond(time), 0.02);
			//after four buckets are collected again (45+60)the average should be 0,333 per second.
			if (time > 105)
				assertEquals((1.0/3.0), sensor.getAvgVehiclesPerSecond(time), 0.02);
		}
	}

	@Test
	void testClassicBehaviour() {
		LaneSensor sensor = new LaneSensor(null, null);
		sensor.registerAverageVehiclesPerSecondToMonitor();
		for (int time = 0; time <= 3600; time++) {
			if (time > 30 && time != 45 && !(time > 100 && time < 129)) {
				sensor.handleEvent(new LaneEnterEvent(time, null, null, null));
			}
			if (time <= 30)
				assertEquals(0.0, sensor.getAvgVehiclesPerSecond(time), 0.02);
			if (time == 32)
				assertEquals(1.0, sensor.getAvgVehiclesPerSecond(time), 0.02);
			if (time > 50)
				assertTrue(sensor.getAvgVehiclesPerSecond(time) < 1.0);
			if (time == 1000)
				assertTrue(sensor.getAvgVehiclesPerSecond(time)> 0.96);
		}
	}
	
}
