package playground.andreas.fixedHeadway;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.mobsim.qsim.InternalInterface;
import org.matsim.core.mobsim.qsim.pt.TransitStopAgentTracker;
import org.matsim.core.mobsim.qsim.pt.TransitDriverAgentImpl;
import org.matsim.core.utils.misc.Time;
import org.matsim.pt.Umlauf;


/**
 * 
 * @author aneumann
 *
 */
public class FixedHeadwayCycleUmlaufDriver extends TransitDriverAgentImpl {
	
	private final static Logger log = Logger.getLogger(FixedHeadwayCycleUmlaufDriver.class);
	
	private double additionalDelayAtNextStop = 0.0;
	private static int useageCnt = 0 ;

	public FixedHeadwayCycleUmlaufDriver(Umlauf umlauf, TransitStopAgentTracker thisAgentTracker,
			InternalInterface internalInterface) {
		super(umlauf, TransportMode.car, thisAgentTracker, internalInterface);
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
	public void endActivityAndComputeNextState(final double now) {
		super.endActivityAndComputeNextState(now);
		this.additionalDelayAtNextStop = 0.0;
	}
}
