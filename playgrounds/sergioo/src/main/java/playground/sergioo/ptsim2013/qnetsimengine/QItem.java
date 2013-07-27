package playground.sergioo.ptsim2013.qnetsimengine;

/**
 * @author nagel
 */
abstract class QItem {
	
	abstract double getEarliestLinkExitTime();

	abstract void setEarliestLinkExitTime(double earliestLinkEndTime);

}