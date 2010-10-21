/**
 * 
 */
package org.matsim.ptproject.qsim.interfaces;

import org.matsim.vis.snapshots.writers.VisMobsimFeature;


/**
 * @author nagel
 * @deprecated use controler listeners and mobsim listeners instead.  kai, oct'10
 */
@Deprecated // if you think you need to use this, ask kai.  aug'10
public interface AcceptsVisMobsimFeatures {
	@Deprecated // if you think you need to use this, ask kai.  aug'10
	public void addFeature(final VisMobsimFeature queueSimulationFeature) ;
}
