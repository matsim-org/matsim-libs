/**
 * 
 */
package playground.mzilske.freight;

import java.util.ArrayList;
import java.util.Collection;

/**
 * @author schroeder
 *
 */
public class TransportServiceProviders {
	
	private Collection<TransportServiceProviderImpl> transportServiceProviders = new ArrayList<TransportServiceProviderImpl>();

	public Collection<TransportServiceProviderImpl> getTransportServiceProviders() {
		return transportServiceProviders;
	}

}
