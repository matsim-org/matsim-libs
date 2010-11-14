package playground.andreas.fixedHeadway;

import org.apache.log4j.Logger;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.utils.misc.Time;
import org.matsim.pt.Umlauf;
import org.matsim.pt.qsim.TransitStopAgentTracker;
import org.matsim.pt.qsim.UmlaufDriver;
import org.matsim.ptproject.qsim.interfaces.Mobsim;


/**
 * 
 * @author aneumann
 *
 */
public class FixedHeadwayCycleUmlaufDriver extends UmlaufDriver {
	
	private final static Logger log = Logger.getLogger(FixedHeadwayCycleUmlaufDriver.class);
	
	private double additionalDelayAtNextStop = 0.0;
	private static int useageCnt = 0 ;

	public FixedHeadwayCycleUmlaufDriver(Umlauf umlauf, TransitStopAgentTracker thisAgentTracker,
			Mobsim transitQueueSimulation) {
		super(umlauf, thisAgentTracker, transitQueueSimulation);
		if ( useageCnt <1 ) {
			useageCnt++ ;
			log.info(" will be used. " + Gbl.ONLYONCE );
		}
	}
	
	
	@Override
	protected double longerStopTimeIfWeAreAheadOfSchedule(final double now,	final double stopTime) {
		if ((this.nextStop.isAwaitDepartureTime()) && (this.nextStop.getDepartureOffset() != Time.UNDEFINED_TIME)) {
			double earliestDepTime = getActivityEndTime() + this.nextStop.getDepartureOffset() + this.additionalDelayAtNextStop;
			this.additionalDelayAtNextStop = 0.0;
			if (now + stopTime < earliestDepTime) {
				return earliestDepTime - now;
			}
		}
		return stopTime;
	}
	
	public void setAdditionalDelayAtNextStop(double additionalDelayAtNextStop) {
		this.additionalDelayAtNextStop = additionalDelayAtNextStop;		
	}
	
	@Override
	public void endActivityAndAssumeControl(final double now) {
		super.endActivityAndAssumeControl(now);
		this.additionalDelayAtNextStop = 0.0;
	}
}
