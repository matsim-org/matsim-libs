package org.matsim.ptproject.qsim.qnetsimengine;

/**
 * @author nagel
 */
abstract class QItem {
	
	abstract double getEarliestLinkExitTime();

	abstract void setEarliestLinkExitTime(double earliestLinkEndTime);

}