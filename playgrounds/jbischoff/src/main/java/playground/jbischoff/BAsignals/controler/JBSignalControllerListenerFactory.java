/**
 * 
 */
package playground.jbischoff.BAsignals.controler;

import org.apache.log4j.Logger;
import org.matsim.signalsystems.controler.SignalsControllerListener;
import org.matsim.signalsystems.controler.SignalsControllerListenerFactory;

/**
 * @author jbischoff
 *
 */
public class JBSignalControllerListenerFactory implements
		SignalsControllerListenerFactory {
	
	private static final Logger log = Logger.getLogger(JBSignalControllerListenerFactory.class);
	
	public JBSignalControllerListenerFactory(){}
	
	@Override
	public SignalsControllerListener createSignalsControllerListener() {
		log.info("Using JB SignalControllerListener...");
		return new JBSignalControllerListener();
	}



	
}


