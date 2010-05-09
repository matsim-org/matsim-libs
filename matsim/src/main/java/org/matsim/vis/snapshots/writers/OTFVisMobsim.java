/**
 * 
 */
package org.matsim.vis.snapshots.writers;

import org.matsim.vis.otfvis.server.OnTheFlyServer;

/**Interface that goes beyond VisMobsim in that it adds OTFVis infrastructure
 * 
 * @author nagel
 *
 */
public interface OTFVisMobsim extends VisMobsim {
	void setServer( OnTheFlyServer server ) ;

	VisMobsimFeature getQueueSimulationFeature() ;
	// yyyy named to retrofit Dominik's name; should be changed.  kai, may'10
	
	void setVisualizeTeleportedAgents(boolean active) ;
	// yyyy I find it odd to have this here.  Maybe config file/object??  kai, may'10
}
