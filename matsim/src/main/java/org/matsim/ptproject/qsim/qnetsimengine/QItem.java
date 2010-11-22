package org.matsim.ptproject.qsim.qnetsimengine;

/**
 * yyyyyy For the time being, this is public, since (minimally) the visualization uses it.  I would prefer to convert it into
 * an internal interface.  kai, nov'10
 * <p/>
 * @author nagel
 */
public interface QItem {

	double getEarliestLinkExitTime();

	void setEarliestLinkExitTime(double earliestLinkEndTime);

}