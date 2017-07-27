/**
 * 
 */
package org.matsim.core.mobsim.qsim.qnetsimengine;

import org.matsim.core.mobsim.qsim.interfaces.NetsimNode;

/**
 * @author kainagel
 *
 */
abstract class QNodeI implements NetsimNode {

	abstract boolean doSimStep(double now) ;

	abstract void init() ;

}
