/**
 * 
 */
package playground.lsieber.networkshapecutter;

import org.matsim.api.core.v01.network.Network;

/** @author Claudio Ruch */
public abstract class ANetworkCutter {

    protected Network networkOrig;
    protected Network networkModif;
    protected String cutInfo;
    
    public ANetworkCutter(Network network) {
        networkOrig = network;
    };
    protected abstract void filter();
    public Network getNetwork(){
        return networkModif;
    };
    
    
    
    

}
