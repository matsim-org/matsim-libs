/**
 * 
 */
package playground.jbischoff.BAsignals;

import org.apache.log4j.Logger;
import org.matsim.core.controler.listener.ControlerListener;
import org.matsim.signalsystems.initialization.SignalsControllerListenerFactory;

/**
 * @author jbischoff
 *
 */
public class JBSignalControllerListenerFactory implements
		SignalsControllerListenerFactory {
	
	private static final Logger log = Logger.getLogger(JBSignalControllerListenerFactory.class);
	
	@Override
	public ControlerListener createSignalsControllerListener() {
		log.info("Using JB SignalControllerListener...");
		return new JBSignalControllerListener();
	}

}
