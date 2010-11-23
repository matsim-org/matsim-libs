/**
 * 
 */
package playground.jbischoff.BAsignals;

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
	private SignalsControllerListenerFactory delegate;
	private JBSignalControllerListener jbscl;
	
	public JBSignalControllerListenerFactory(SignalsControllerListenerFactory signalsControllerListenerFactory){
		this.delegate = signalsControllerListenerFactory;
		
	}
	
	@Override
	public SignalsControllerListener createSignalsControllerListener() {
		log.info("Using JB SignalControllerListener...");
		jbscl = new JBSignalControllerListener(this.delegate.createSignalsControllerListener());
		return jbscl;
	}



	
}


