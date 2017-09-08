/**
 * 
 */
package playground.clruch.netdata;

import org.matsim.api.core.v01.network.Link;

/**
 * @author Claudio Ruch
 *
 */
public enum VirtualNetworkUtils {
    ;
    
    /*package*/ static String linkToID(Link link){
        return link.getId().toString();
    }


}
