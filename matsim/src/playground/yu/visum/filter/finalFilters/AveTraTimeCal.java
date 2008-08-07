package playground.yu.visum.filter.finalFilters;

import java.util.List;

import org.matsim.events.EventLinkEnter;
import org.matsim.network.Link;
import org.matsim.network.NetworkLayer;
import org.matsim.population.Plans;
import org.matsim.utils.misc.Time;

import playground.yu.visum.writer.UserDefAtt;

/**
 * A AveTraTimeCal can calculate the average traveltime, which can be exported
 * as the attribut defined by user of VISUM 9.3, on a link in a timeBin(15 min)
 * and also output corresponding result to print.
 * 
 * @author ychen
 * 
 */
public class AveTraTimeCal extends LinkAveCalA {
	/*------------------------GETTER---Impl-------------------*/
	/**
	 * Is called by double atxCal(int linkID, String s0)
	 * 
	 * @param linkId -
	 *            linkId the id of the LINK in VISUM 9.3.
	 * @attention LinkId isn't the linkId in MATSIM!
	 * @param time -
	 *            time_s any definite point of time with unit second. e.g.
	 *            06:00:00 is here 21600.
	 * @return the value of the traveltime direct measured on a link during a
	 *         timeBin
	 */
	public double getLinkTraTime(String linkId, int time) {
		return getLinkCalResult(linkId, time, "time", "s");
	}

	/* ---------------------CONSTRUCTOR-------------------- */
	/**
	 * @attention the complete Information of some events can not be read
	 *            regularly without the constructor
	 * @param plans -
	 *            contains useful information from plans-file
	 * @param network -
	 *            contains useful information from network-file
	 */
	public AveTraTimeCal(Plans plans, NetworkLayer network) {
		super(plans, network);
	}

	/*-----------------------OVERRIDE MOTHODS--------------------------*/
	/**
	 * Accumulates the traveltime; Is called in void
	 * org.matsim.playground.filters.filter.finalFilters.LinkAveCalA.handleEvent(BasicEvent
	 * event)
	 */
	public void compute(EventLinkEnter enter, double leaveTime_s) {
		double tt = leaveTime_s - enter.time;
		try {
			computeInside(tt, rebuildEventLinkEnter(enter).linkId,
					(long) (enter.time / 900));
		} catch (NullPointerException e) {
			System.err.println("link exits not in the network");
		}
	}

	/**
	 * exports a set of attribut, which is defined by user of VISUM 9.3. The
	 * corresponding ATTID is called timeBin-No.+"AVETT", and the corresponding
	 * CODE and NAME is called e.g. "aveTraTime06:00-06:14",
	 * "aveTraTime08:30-08:44" etc.; Is called in void
	 * org.matsim.playground.filters.writer.PrintStreamUDANET.output(FinalEventFilterI
	 * fef).
	 * 
	 * @return a set of attribut, which is defined by user of VISUM 9.3. The
	 *         corresponding ATTID is called timeBin-No.+"AVETS", and the
	 *         corresponding CODE and NAME is called e.g.
	 *         "aveTraSpeed06:00-06:14", "aveTraSpeed08:30-08:44" etc.;
	 */
	@Override
	public List<UserDefAtt> UDAexport() {
		return UDAexport("AveTraTime", "AVETT");
	}

	/**
	 * Determines the average traveltime on a link during a timeBin; If att(the
	 * average travelspeed)=0, then att=freespeed on the link; If att>0, the
	 * ats=ats. Is called by Set<Integer>
	 * org.matsim.playground.filters.filter.finalFilters.LinkAveCalA.getLinkAveCalKeySet()
	 * 
	 * @return the value of average travelspeed on a link during a timeBin.
	 */
	@Override
	protected double atxCal(String linkID, String s0) {
		double att = getLinkTraTime(linkID, Integer.parseInt(s0) * 900);
		Link l = (Link) network.getLocation(linkID);
		if (l == null)
			return 0.0;
		att = (att != 0) ? att : l.getLength() / l.getFreespeed(Time.UNDEFINED_TIME);
		return att;
	}
}
