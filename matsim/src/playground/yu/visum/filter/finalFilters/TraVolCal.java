package playground.yu.visum.filter.finalFilters;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.matsim.events.BasicEvent;
import org.matsim.events.EventLinkEnter;
import org.matsim.network.NetworkLayer;
import org.matsim.plans.Plans;

import playground.yu.visum.writer.UserDefAtt;

/**
 * @author ychen
 * 
 */
public class TraVolCal extends FinalEventFilterA {
	// --------------------------MEMBER VARIABLES------------------------
	/**
	 * traVols - map to save traffic volumes Map<String linkId, Map<Integer
	 * timeBin (hour), Integer traffic Volume>>
	 */
	private final Map<String, Map<Long, Double>> traVols = new HashMap<String, Map<Long, Double>>();

	private long timeBinMin = 25;

	private long timeBinMax = -1;

	// --------------------------OVERRIDE METHODS------------------------
	@Override
	public void handleEvent(final BasicEvent enter) {
		if (enter instanceof EventLinkEnter) {
			count();
			long hour = (long) (enter.time / 3600);
			if (hour > this.timeBinMax)
				this.timeBinMax = hour;
			if (hour < this.timeBinMin)
				this.timeBinMin = hour;
			EventLinkEnter ele = rebuildEventLinkEnter((EventLinkEnter) enter);
			String linkId = null;
			try {
				linkId = ele.linkId;
			} catch (NullPointerException npe) {
				System.err.println(npe);
			}
			if (linkId != null) {
				Map<Long, Double> inside;
				int traVol = 0;
				if (this.traVols.containsKey(linkId)) {
					inside = this.traVols.get(linkId);
					if (inside.containsKey(hour))
						traVol = inside.get(hour).intValue();
				} else
					inside = new TreeMap<Long, Double>();
				inside.put(hour, Double.valueOf(++traVol));
				this.traVols.put(linkId, inside);
			}
		}
	}

	// ----------------------CONSTUCTOR---------------------------
	/**
	 * @param plans -
	 *            a Plans-object in the simulation
	 * @param network -
	 *            a NetworkLayer-object in the simulation
	 */
	public TraVolCal(final Plans plans, final NetworkLayer network) {
		super(plans, network);
	}

	/*-----------------OVERRIDE METHODS OF ABSTRACTED---------------- */
	@Override
	public List<UserDefAtt> UDAexport() {
		if (this.udas.size() > 0)
			return this.udas;
		for (long i = this.timeBinMin; i <= this.timeBinMax; i++) {
			UserDefAtt uda = new UserDefAtt("LINK", i + "TO"
					+ Long.toString(i + 1) + "TRAVOL", i + "to"
					+ Long.toString(i + 1) + "Travol", i + "-"
					+ Long.toString(i + 1) + "Travol", UserDefAtt.DatenTyp.Int,
					0, UserDefAtt.SparseDocu.duenn);
			this.udas.add(uda);
		}
		return this.udas;
	}

	/**
	 * a function like
	 * org.matsim.demandmodeling.filters.filter.finalFilters.FinalEventFilterA#UDAexport()for
	 * Noise evaluation
	 * 
	 * @return a list of UserDefAtt for Noise evaluation
	 */
	public List<UserDefAtt> LmUDAexport() {
		if (this.udas.size() > 0)
			return this.udas;
		for (long i = this.timeBinMin; i <= this.timeBinMax; i++) {
			UserDefAtt uda = new UserDefAtt("LINK", i + "TO"
					+ Long.toString(i + 1) + "LAERM", i + "to"
					+ Long.toString(i + 1) + "Laerm", i + "-"
					+ Long.toString(i + 1) + "Laerm", UserDefAtt.DatenTyp.Int,
					2, UserDefAtt.SparseDocu.duenn);
			this.udas.add(uda);
		}
		return this.udas;
	}

	@Override
	public Map<String, List<Double>> UDAWexport() {
		if (this.udaws.size() > 0)
			return this.udaws;
		for (String linkID : this.traVols.keySet()) {
			List<Double> udaw = new ArrayList<Double>();
			for (UserDefAtt uda : this.udas) {
				String[] s = uda.getATTID().split("TO");
				udaw.add(this.traVols.get(linkID).get(Long.parseLong(s[0])));
			}
			this.udaws.put(linkID, udaw);
		}
		return this.udaws;
	}

	/**
	 * a function like
	 * org.matsim.demandmodeling.filters.filter.finalFilters.FinalEventFilterA#UDAWexport()
	 * for noise evaluation
	 * 
	 * @return the TreeMap of values of attributs defined by VISUM9.3-user, but
	 *         only for noise evaluation
	 */
	public Map<String, List<Double>> LmUDAWexport() {
		if (this.udaws.size() > 0)
			return this.udaws;
		for (String linkID : this.traVols.keySet()) {
			List<Double> udaw = new ArrayList<Double>();
			for (UserDefAtt uda : this.udas) {
				String[] s = uda.getATTID().split("TO");
				double Lm = 37.3 + 10.0 * Math.log10(this.traVols.get(linkID)
						.get(Integer.parseInt(s[0]))
						* (1.0 + 0.082 * 0.1));
				udaw.add(Lm);
			}
			this.udaws.put(linkID, udaw);
		}
		return this.udaws;
	}
}
