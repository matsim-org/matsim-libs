package playground.wrashid.parkingChoice.util;

import junit.framework.TestCase;

import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.controler.Controler;
import org.matsim.core.utils.misc.ConfigUtils;

import playground.wrashid.lib.EventHandlerAtStartupAdder;

public class ActivityDurationEstimatorTest extends TestCase {

	public void testConfig1(){
		Config config=ConfigUtils.loadConfig("test/input/playground/wrashid/parkingChoice/utils/chessConfig1.xml");
		ActivityDurationEstimator activityDurationEstimator = getActivityDurationEstimations(config);
	
		assertEquals(26254, activityDurationEstimator.getActivityDurationEstimations().get(0),1);
		assertEquals(55322, activityDurationEstimator.getActivityDurationEstimations().get(1),1);
	}
	
	public void testConfig2(){
		Config config=ConfigUtils.loadConfig("test/input/playground/wrashid/parkingChoice/utils/chessConfig2.xml");
	
		
		ActivityDurationEstimator activityDurationEstimator = getActivityDurationEstimations(config);
	
		assertEquals(28800, activityDurationEstimator.getActivityDurationEstimations().get(0),1);
		assertEquals(52776, activityDurationEstimator.getActivityDurationEstimations().get(1),1);
	}
	
	public void testConfig3(){
		Config config=ConfigUtils.loadConfig("test/input/playground/wrashid/parkingChoice/utils/config3.xml");
		
		ActivityDurationEstimator activityDurationEstimator = getActivityDurationEstimations(config);
	
		assertEquals(600, activityDurationEstimator.getActivityDurationEstimations().get(0),1);
		assertEquals(12600, activityDurationEstimator.getActivityDurationEstimations().get(1),1);
		assertEquals(69960, activityDurationEstimator.getActivityDurationEstimations().get(2),1);
	}
	
	public void testConfig4(){
		Config config=ConfigUtils.loadConfig("test/input/playground/wrashid/parkingChoice/utils/chessConfig4.xml");
		
		ActivityDurationEstimator activityDurationEstimator = getActivityDurationEstimations(config);
	
		assertEquals(51600, activityDurationEstimator.getActivityDurationEstimations().get(0),1);
		assertEquals(29976, activityDurationEstimator.getActivityDurationEstimations().get(1),1);
	}
	
	//TODO: go through the numbers of this test...
	public void testConfig5(){
		Config config=ConfigUtils.loadConfig("test/input/playground/wrashid/parkingChoice/utils/chessConfig5.xml");
		
		ActivityDurationEstimator activityDurationEstimator = getActivityDurationEstimations(config);
	
		assertEquals(118933, activityDurationEstimator.getActivityDurationEstimations().get(0),1);
		assertEquals(3600, activityDurationEstimator.getActivityDurationEstimations().get(1),1);
		assertEquals(3600, activityDurationEstimator.getActivityDurationEstimations().get(2),1);
		assertEquals(3803, activityDurationEstimator.getActivityDurationEstimations().get(3),1);
	}

	private ActivityDurationEstimator getActivityDurationEstimations(Config config) {
		Controler controler=new Controler(config);
		
		EventHandlerAtStartupAdder eventHandlerAtStartupAdder = new EventHandlerAtStartupAdder();
		controler.addControlerListener(eventHandlerAtStartupAdder);
		
		ActivityDurationEstimator activityDurationEstimator = new ActivityDurationEstimator(controler, new IdImpl(1));
		eventHandlerAtStartupAdder.addEventHandler(activityDurationEstimator);
		
		controler.setOverwriteFiles(true);
		controler.run();
		return activityDurationEstimator;
	}
	
}
