package saleem.stockholmscenario.teleportation.ptoptimisation.utils;

import java.util.ArrayList;

import junit.framework.TestCase;

import org.junit.Assert;
import org.junit.Test;

//Each time represents a departure time
public class PtOptimisationTest extends TestCase {
	 OptimisationUtils outils;
	 protected void setUp(){
		 outils = new OptimisationUtils();
	   }
//	 @Test
//		public void testNoChange() {//When no departure is added or removed
//			ArrayList<Double> origtimes = new ArrayList<Double>();
//			ArrayList<Double> changedtimes = new ArrayList<Double>();
//			origtimes.add(35200.0);
//			origtimes.add(37000.0);
//			changedtimes.add(35200.0);
//			changedtimes.add(37000.0);
//			ArrayList<Double> updatedtimes = outils.rearrangeTimes(origtimes,changedtimes);
//			Double[] expected = {35200.0, 37000.0};
//			Assert.assertArrayEquals(updatedtimes.toArray(), expected);
//		}
//	@Test
//	public void testRemovalOne() {//One departure, which is removed
//		
//		ArrayList<Double> origtimes = new ArrayList<Double>();
//		ArrayList<Double> changedtimes = new ArrayList<Double>();
//		origtimes.add(35200.0);//Departure to be removed
//		Double[] expected = {};//Expected return value is an empty list
//		ArrayList<Double> updatedtimes = outils.rearrangeTimes(origtimes,changedtimes);
//		Assert.assertArrayEquals(updatedtimes.toArray(), expected);
//	}
//	@Test
//	public void testRemovedTwo() {//When multiple departures are there, and last one is removed
//		ArrayList<Double> origtimes = new ArrayList<Double>();
//		ArrayList<Double> changedtimes = new ArrayList<Double>();
//		origtimes.add(14400.0);
//		origtimes.add(3600.0);
//		origtimes.add(7200.0);
//		origtimes.add(10800.0);
//		origtimes.add(18000.0);//Departure to be removed
//		changedtimes.add(14400.0);
//		changedtimes.add(3600.0);
//		changedtimes.add(7200.0);
//		changedtimes.add(10800.0);
//		ArrayList<Double> updatedtimes = outils.rearrangeTimes(origtimes,changedtimes);
//		Double[] expected = {3600.0, 7200.0,10800.0, 14400.0};
//		Assert.assertArrayEquals(updatedtimes.toArray(), expected);
//	}
//	@Test
//	public void testRemovedThree() {//When multiple departures are there, and first one is removed
//		ArrayList<Double> origtimes = new ArrayList<Double>();
//		ArrayList<Double> changedtimes = new ArrayList<Double>();
//		origtimes.add(14400.0);
//		origtimes.add(3600.0);//Departure to be removed
//		origtimes.add(7200.0);
//		origtimes.add(10800.0);
//		origtimes.add(18000.0);
//		changedtimes.add(14400.0);
//		changedtimes.add(18000.0);
//		changedtimes.add(7200.0);
//		changedtimes.add(10800.0);
//		ArrayList<Double> updatedtimes = outils.rearrangeTimes(origtimes,changedtimes);
//		Double[] expected = {7200.0,10800.0, 14400.0, 18000.0};
//		Assert.assertArrayEquals(updatedtimes.toArray(), expected);
//	}
//	@Test
//	public void testRemovedFour() {//When multiple departures are there, and one from middle is removed
//		ArrayList<Double> origtimes = new ArrayList<Double>();
//		ArrayList<Double> changedtimes = new ArrayList<Double>();
//		origtimes.add(3600.0);
//		origtimes.add(6600.0);
//		origtimes.add(9600.0);//Departure to be removed
//		origtimes.add(12600.0);
//		origtimes.add(15600.0);
//		origtimes.add(18600.0);
//		origtimes.add(21600.0);
//		changedtimes.add(3600.0);
//		changedtimes.add(6600.0);
//		changedtimes.add(12600.0);
//		changedtimes.add(15600.0);
//		changedtimes.add(18600.0);
//		changedtimes.add(21600.0);
//		
//		ArrayList<Double> updatedtimes = outils.rearrangeTimes(origtimes,changedtimes);
//		Double[] expected = {3600.0, 7200.0,10800.0, 14400.0, 18000.0, 21600.0};
//		Assert.assertArrayEquals(updatedtimes.toArray(), expected);
//	}
//	@Test
//	public void testRemovedFive() {//When multiple departures are there, and two are removed at times between existing time
//		ArrayList<Double> origtimes = new ArrayList<Double>();
//		ArrayList<Double> changedtimes = new ArrayList<Double>();
//		
//		origtimes.add(3600.0);
//		origtimes.add(6600.0);
//		origtimes.add(9600.0);
//		origtimes.add(12600.0);
//		origtimes.add(15600.0);
//		origtimes.add(18600.0);
//		origtimes.add(21600.0);
//		origtimes.add(24600.0);
//		origtimes.add(27600.0);
//		origtimes.add(30600.0);
//		origtimes.add(33600.0);
//		origtimes.add(36600.0);
//		origtimes.add(39600.0);
//		
//		changedtimes.add(3600.0);
//		changedtimes.add(6600.0);
//		changedtimes.add(9600.0);
//		changedtimes.add(12600.0);
//		changedtimes.add(18600.0);
//		changedtimes.add(21600.0);
//		changedtimes.add(24600.0);
//		changedtimes.add(27600.0);
//		changedtimes.add(30600.0);
//		changedtimes.add(36600.0);
//		changedtimes.add(39600.0);
//		
//		ArrayList<Double> updatedtimes = outils.rearrangeTimes(origtimes,changedtimes);
//		Double[] expected = {3600.0, 7200.0, 10800.0, 14400.0, 18000.0, 21600.0, 25200.0, 28800.0, 32400.0, 36000.0, 39600.0};
//		Assert.assertArrayEquals(updatedtimes.toArray(), expected);
//	}
//	@Test
//	public void testAddedOne() {//When no departure is there, and one is added
//		ArrayList<Double> origtimes = new ArrayList<Double>();
//		ArrayList<Double> changedtimes = new ArrayList<Double>();
//		changedtimes.add(35200.0);//Departure added
//		ArrayList<Double> updatedtimes = outils.rearrangeTimes(origtimes,changedtimes);
//		Double[] expected = {35200.0};
//		Assert.assertArrayEquals(updatedtimes.toArray(), expected);
//	}
//	@Test
//	public void testAddedTwo() {//When one departure is there, and one is added
//		ArrayList<Double> origtimes = new ArrayList<Double>();
//		ArrayList<Double> changedtimes = new ArrayList<Double>();
//		origtimes.add(35200.0);
//		changedtimes.add(35200.0);
//		changedtimes.add(37000.0);//Departure added
//		ArrayList<Double> updatedtimes = outils.rearrangeTimes(origtimes,changedtimes);
//		Double[] expected = {35200.0, 37000.0};
//		Assert.assertArrayEquals(updatedtimes.toArray(), expected);
//	}
//	@Test
//	public void testAddedThree() {//When multiple departures are there, and one is added at the end
//		ArrayList<Double> origtimes = new ArrayList<Double>();
//		ArrayList<Double> changedtimes = new ArrayList<Double>();
//		origtimes.add(14400.0);
//		origtimes.add(3600.0);
//		origtimes.add(7200.0);
//		origtimes.add(10800.0);
//		origtimes.add(18000.0);
//		changedtimes.add(21600.0);//Departure added
//		changedtimes.add(14400.0);
//		changedtimes.add(3600.0);
//		changedtimes.add(7200.0);
//		changedtimes.add(10800.0);
//		changedtimes.add(18000.0);
//		ArrayList<Double> updatedtimes = outils.rearrangeTimes(origtimes,changedtimes);
//		Double[] expected = {3600.0, 7200.0,10800.0, 14400.0,18000.0, 21600.0};
//		Assert.assertArrayEquals(updatedtimes.toArray(), expected);
//	}
//	@Test
//	public void testAddedFour() {//When multiple departures are there, and one is added at the start
//		ArrayList<Double> origtimes = new ArrayList<Double>();
//		ArrayList<Double> changedtimes = new ArrayList<Double>();
//		origtimes.add(14400.0);
//		origtimes.add(3600.0);
//		origtimes.add(7200.0);
//		origtimes.add(10800.0);
//		origtimes.add(18000.0);
//		changedtimes.add(0.0);//Departure added
//		changedtimes.add(14400.0);
//		changedtimes.add(3600.0);
//		changedtimes.add(7200.0);
//		changedtimes.add(10800.0);
//		changedtimes.add(18000.0);
//		ArrayList<Double> updatedtimes = outils.rearrangeTimes(origtimes,changedtimes);
//		Double[] expected = {0.0, 3600.0, 7200.0,10800.0, 14400.0,18000.0};
//		Assert.assertArrayEquals(updatedtimes.toArray(), expected);
//	}
//	@Test
//	public void testAddedFive() {//When multiple departures are there, and one is added at a time between existing time
//		ArrayList<Double> origtimes = new ArrayList<Double>();
//		ArrayList<Double> changedtimes = new ArrayList<Double>();
//		origtimes.add(14400.0);
//		origtimes.add(3600.0);
//		origtimes.add(7200.0);
//		origtimes.add(10800.0);
//		origtimes.add(18000.0);
//		origtimes.add(21600.0);
//		
//		changedtimes.add(8000.0);//Departure added
//		changedtimes.add(14400.0);
//		changedtimes.add(3600.0);
//		changedtimes.add(7200.0);
//		changedtimes.add(10800.0);
//		changedtimes.add(18000.0);
//		changedtimes.add(21600.0);
//		ArrayList<Double> updatedtimes = outils.rearrangeTimes(origtimes,changedtimes);
//		Double[] expected = {3600.0, 6600.0,9600.0, 12600.0,15600.0,18600.0, 21600.0};
//		Assert.assertArrayEquals(updatedtimes.toArray(), expected);
//	}
//	@Test
//	public void testAddedSix() {//When multiple departures are there, and two are added at times between existing time
//		ArrayList<Double> origtimes = new ArrayList<Double>();
//		ArrayList<Double> changedtimes = new ArrayList<Double>();
//		origtimes.add(14400.0);
//		origtimes.add(3600.0);
//		origtimes.add(7200.0);
//		origtimes.add(10800.0);
//		origtimes.add(18000.0);
//		origtimes.add(21600.0);
//		origtimes.add(28800.0);
//		origtimes.add(25200.0);
//		origtimes.add(32400.0);
//		origtimes.add(36000.0);
//		origtimes.add(39600.0);
//		
//		changedtimes.add(14400.0);
//		changedtimes.add(3600.0);
//		changedtimes.add(7200.0);
//		changedtimes.add(10800.0);
//		changedtimes.add(18000.0);
//		changedtimes.add(19000.0);
//		changedtimes.add(27000.0);
//		changedtimes.add(21600.0);
//		changedtimes.add(28800.0);
//		changedtimes.add(25200.0);
//		changedtimes.add(32400.0);
//		changedtimes.add(36000.0);
//		changedtimes.add(39600.0);
//		ArrayList<Double> updatedtimes = outils.rearrangeTimes(origtimes,changedtimes);
//		Double[] expected = {3600.0, 6600.0, 9600.0, 12600.0, 15600.0, 18600.0, 21600.0, 24600.0, 27600.0, 30600.0, 33600.0, 36600.0, 39600.0};
//		Assert.assertArrayEquals(updatedtimes.toArray(), expected);
//	}
//	@Test
//	public void testAddedSeven() {//When multiple departures are there, and one is added at the start
//		ArrayList<Double> origtimes = new ArrayList<Double>();
//		ArrayList<Double> changedtimes = new ArrayList<Double>();
//		origtimes.add(7200.0);
//		origtimes.add(14400.0);
//		origtimes.add(18000.0);
//		changedtimes.add(0.0);//Departure added
//		changedtimes.add(14400.0);
//		changedtimes.add(7200.0);
//		changedtimes.add(18000.0);
//		ArrayList<Double> updatedtimes = outils.rearrangeTimes(origtimes,changedtimes);
//		Double[] expected = {0.0, 7200.0, 14400.0, 18000.0};
//		Assert.assertArrayEquals(updatedtimes.toArray(), expected);
//	}
}
