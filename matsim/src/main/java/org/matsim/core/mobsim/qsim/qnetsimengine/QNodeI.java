/**
 * 
 */
package org.matsim.core.mobsim.qsim.qnetsimengine;

import org.matsim.core.mobsim.qsim.interfaces.NetsimNode;

/**
 * @author kainagel
 *
 */
interface QNodeI extends NetsimNode {

	boolean doSimStep(double now) ;

	void init(QNetwork network) ;

}
