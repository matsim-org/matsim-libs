package org.matsim.core.mobsim.qsim.qnetsimengine;

/**
 * @author nagel
 */
abstract class QItem {
	
	abstract double getEarliestLinkExitTime();

	abstract void setEarliestLinkExitTime(double earliestLinkEndTime);
	
	abstract double getSizeInEquivalents();


}