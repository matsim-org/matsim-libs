/**
 * 
 */
package org.matsim.vis.snapshots.writers;

/**This is mostly a hook since right now the access from the mobsim to the otfvis is implemented as a "feature",
 * and in order to go to the original mobsim, you need to go "up".  kai, may'10
 * 
 * @author nagel
 *
 */
public interface VisMobsimFeature {
	VisMobsim getVisMobsim() ;
}
