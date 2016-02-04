package org.matsim.core.controler;

import org.junit.Assert;
import org.junit.Test;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.core.controler.listener.ShutdownListener;
import org.matsim.core.controler.listener.StartupListener;

/**
 * @author mrieser / senozon
 */
public class ControlerListenerManagerImplTest {

	@Test
	public void testAddControlerListener_ClassHierarchy() {
		ControlerListenerManagerImpl m = new ControlerListenerManagerImpl();
		CountingControlerListener ccl = new CountingControlerListener();
		ExtendedControlerListener ecl = new ExtendedControlerListener();
		m.addControlerListener(ccl);
		m.addControlerListener(ecl);
		
		m.fireControlerStartupEvent();
		Assert.assertEquals(1, ccl.nOfStartups);
		Assert.assertEquals(0, ccl.nOfIterStarts);
		Assert.assertEquals(1, ecl.nOfStartups);
		Assert.assertEquals(0, ecl.nOfIterStarts);
		Assert.assertEquals(0, ecl.nOfShutdowns);
		
		m.fireControlerIterationStartsEvent(0);
		Assert.assertEquals(1, ccl.nOfStartups);
		Assert.assertEquals(1, ccl.nOfIterStarts);
		Assert.assertEquals(1, ecl.nOfStartups);
		Assert.assertEquals(1, ecl.nOfIterStarts);
		Assert.assertEquals(0, ecl.nOfShutdowns);
		
		m.fireControlerIterationStartsEvent(1);
		Assert.assertEquals(1, ccl.nOfStartups);
		Assert.assertEquals(2, ccl.nOfIterStarts);
		Assert.assertEquals(1, ecl.nOfStartups);
		Assert.assertEquals(2, ecl.nOfIterStarts);
		Assert.assertEquals(0, ecl.nOfShutdowns);
		
		m.fireControlerShutdownEvent(false);
		Assert.assertEquals(1, ccl.nOfStartups);
		Assert.assertEquals(2, ccl.nOfIterStarts);
		Assert.assertEquals(1, ecl.nOfStartups);
		Assert.assertEquals(2, ecl.nOfIterStarts);
		Assert.assertEquals(1, ecl.nOfShutdowns);
	}

	@Test
	public void testAddCoreControlerListener_ClassHierarchy() {
		ControlerListenerManagerImpl m = new ControlerListenerManagerImpl();
		CountingControlerListener ccl = new CountingControlerListener();
		ExtendedControlerListener ecl = new ExtendedControlerListener();
		m.addCoreControlerListener(ccl);
		m.addCoreControlerListener(ecl);
		
		m.fireControlerStartupEvent();
		Assert.assertEquals(1, ccl.nOfStartups);
		Assert.assertEquals(0, ccl.nOfIterStarts);
		Assert.assertEquals(1, ecl.nOfStartups);
		Assert.assertEquals(0, ecl.nOfIterStarts);
		Assert.assertEquals(0, ecl.nOfShutdowns);

		m.fireControlerIterationStartsEvent(0);
		Assert.assertEquals(1, ccl.nOfStartups);
		Assert.assertEquals(1, ccl.nOfIterStarts);
		Assert.assertEquals(1, ecl.nOfStartups);
		Assert.assertEquals(1, ecl.nOfIterStarts);
		Assert.assertEquals(0, ecl.nOfShutdowns);

		m.fireControlerIterationStartsEvent(1);
		Assert.assertEquals(1, ccl.nOfStartups);
		Assert.assertEquals(2, ccl.nOfIterStarts);
		Assert.assertEquals(1, ecl.nOfStartups);
		Assert.assertEquals(2, ecl.nOfIterStarts);
		Assert.assertEquals(0, ecl.nOfShutdowns);
		
		m.fireControlerShutdownEvent(false);
		Assert.assertEquals(1, ccl.nOfStartups);
		Assert.assertEquals(2, ccl.nOfIterStarts);
		Assert.assertEquals(1, ecl.nOfStartups);
		Assert.assertEquals(2, ecl.nOfIterStarts);
		Assert.assertEquals(1, ecl.nOfShutdowns);
	}
	
	private static class CountingControlerListener implements StartupListener, IterationStartsListener {

		/*package*/ int nOfStartups = 0;
		/*package*/ int nOfIterStarts = 0;
		
		@Override
		public void notifyIterationStarts(IterationStartsEvent event) {
			this.nOfIterStarts++;
		}

		@Override
		public void notifyStartup(StartupEvent event) {
			this.nOfStartups++;
		}
		
	}
	
	private static class ExtendedControlerListener extends CountingControlerListener implements ShutdownListener, StartupListener {
		// StartupListener is explicitly implemented, even if not necessary, to test that it will not be called twice 

		/*package*/ int nOfShutdowns = 0;
		
		@Override
		public void notifyShutdown(ShutdownEvent event) {
			this.nOfShutdowns++;
		}
		
	}
	
}
