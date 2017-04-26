package playground.sergioo.mixedTraffic2017.qsimmixed;

import org.matsim.vis.snapshotwriters.VisVehicle;

/**
 * @author nagel
 */
abstract class QItem implements VisVehicle {
	
	abstract double getEarliestLinkExitTime();

	abstract void setEarliestLinkExitTime(double earliestLinkEndTime);
	
	@Override public abstract double getSizeInEquivalents();


}