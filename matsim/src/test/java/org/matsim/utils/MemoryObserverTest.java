package org.matsim.utils;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Assert;
import org.junit.Test;
import org.matsim.testcases.utils.LogCounter;

/**
 * @author mrieser / Simunto GmbH
 */
public class MemoryObserverTest {

	private final static Logger LOG = LogManager.getLogger(MemoryObserverTest.class);

	@Test
	public void testStartStop() throws InterruptedException {
		LogCounter logger = new LogCounter(Level.INFO);
		logger.activate();

		int count1 = logger.getInfoCount();

		MemoryObserver.start(1);
		Thread.sleep(3_000);
		int count2 = logger.getInfoCount();

		MemoryObserver.stop();
		Thread.sleep(3_000);
		int count3 = logger.getInfoCount();

		MemoryObserver.startMillis(300);
		Thread.sleep(900);
		int count4 = logger.getInfoCount();

		MemoryObserver.stop();
		Thread.sleep(900);
		int count5 = logger.getInfoCount();

		logger.deactivate();

		int activeLogs1 = count2 - count1;
		int inactiveLogs2 = count3 - count2;
		int activeLogs3 = count4 - count3;
		int inactiveLogs4 = count5 - count4;

		// there should be 3 log messages, than none, than 3 again, than none.
		// but due to it being threads and influences of the OS scheduler, the numbers might be off by 1 or so, let's consider this in the tests.

		LOG.info("received the following numbers of log statements: " + activeLogs1 + ", " + inactiveLogs2 + ", " + activeLogs3 + ", " + inactiveLogs4);
		Assert.assertTrue("There should be between 2 and 4 log messages", activeLogs1 >= 2 && activeLogs1 <= 4);
		Assert.assertTrue("There should be at most 1 log message when being stopped", inactiveLogs2 <= 1);
		Assert.assertTrue("There should be between 2 and 4 log messages", activeLogs3 >= 2 && activeLogs3 <= 4);
		Assert.assertTrue("There should be at most 1 log message when being stopped", inactiveLogs4 <= 1);
	}

	@Test
	public void testDoubleStart() throws InterruptedException {
		LogCounter logger = new LogCounter(Level.INFO);
		logger.activate();

		int count1 = logger.getInfoCount();

		MemoryObserver.start(1);
		Thread.sleep(3_000);
		int count2 = logger.getInfoCount();

		// start it again without stopping, the logging should not double
		MemoryObserver.start(1);
		Thread.sleep(3_000);
		int count3 = logger.getInfoCount();

		MemoryObserver.stop();
		Thread.sleep(900);
		int count4 = logger.getInfoCount();

		logger.deactivate();

		int activeLogs1 = count2 - count1;
		int activeLogs2 = count3 - count2;
		int inactiveLogs3 = count4 - count3;

		// there should be 3 log messages, again 3, than none.
		// but due to it being threads and influences of the OS scheduler, the numbers might be off by 1 or so, let's consider this in the tests.

		LOG.info("received the following numbers of log statements: " + activeLogs1 + ", " + activeLogs2 + ", " + inactiveLogs3);
		Assert.assertTrue("There should be between 2 and 4 log messages", activeLogs1 >= 2 && activeLogs1 <= 4);
		Assert.assertTrue("There should be between 2 and 4 log messages", activeLogs2 >= 2 && activeLogs2 <= 4);
		Assert.assertTrue("There should be at most 1 log message when being stopped", inactiveLogs3 <= 1);

	}

}