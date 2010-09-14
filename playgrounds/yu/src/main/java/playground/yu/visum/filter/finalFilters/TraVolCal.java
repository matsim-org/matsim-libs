package playground.yu.visum.filter.finalFilters;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.matsim.api.core.v01.population.Population;
import org.matsim.core.api.experimental.events.Event;
import org.matsim.core.api.experimental.events.LinkEnterEvent;
import org.matsim.core.events.LinkEnterEventImpl;
import org.matsim.core.network.NetworkImpl;

import playground.yu.visum.writer.UserDefAtt;

/**
 * @author ychen
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
	public void handleEvent(final Event enter) {
		if (enter instanceof LinkEnterEventImpl) {
			count();
			long hour = (long) (enter.getTime() / 3600);
			if (hour > timeBinMax)
				timeBinMax = hour;
			if (hour < timeBinMin)
				timeBinMin = hour;
//			LinkEnterEvent ele = rebuildEventLinkEnter((LinkEnterEvent) enter);
			String linkId = null;
			try {
				linkId = ((LinkEnterEvent) enter).getLinkId().toString();
			} catch (NullPointerException npe) {
				System.err.println(npe);
			}
			if (linkId != null) {
				Map<Long, Double> inside;
				int traVol = 0;
				if (traVols.containsKey(linkId)) {
					inside = traVols.get(linkId);
					if (inside.containsKey(hour))
						traVol = inside.get(hour).intValue();
				} else
					inside = new TreeMap<Long, Double>();
				inside.put(hour, Double.valueOf(++traVol));
				traVols.put(linkId, inside);
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
	public TraVolCal(final Population plans, final NetworkImpl network) {
		super(plans, network);
	}

	/*-----------------OVERRIDE METHODS OF ABSTRACTED---------------- */
	@Override
	public List<UserDefAtt> UDAexport() {
		if (udas.size() > 0)
			return udas;
		for (long i = timeBinMin; i <= timeBinMax; i++) {
			UserDefAtt uda = new UserDefAtt("LINK", i + "TO"
					+ Long.toString(i + 1) + "TRAVOL", i + "to"
					+ Long.toString(i + 1) + "Travol", i + "-"
					+ Long.toString(i + 1) + "Travol", UserDefAtt.DatenTyp.Int,
					0, UserDefAtt.SparseDocu.duenn);
			udas.add(uda);
		}
		return udas;
	}

	/**
	 * a function like
	 * org.matsim.demandmodeling.filters.filter.finalFilters.FinalEventFilterA#UDAexport()for
	 * Noise evaluation
	 *
	 * @return a list of UserDefAtt for Noise evaluation
	 */
	public List<UserDefAtt> lmUDAexport() {
		if (udas.size() > 0)
			return udas;
		for (long i = timeBinMin; i <= timeBinMax; i++) {
			UserDefAtt uda = new UserDefAtt("LINK", i + "TO"
					+ Long.toString(i + 1) + "LAERM", i + "to"
					+ Long.toString(i + 1) + "Laerm", i + "-"
					+ Long.toString(i + 1) + "Laerm", UserDefAtt.DatenTyp.Int,
					2, UserDefAtt.SparseDocu.duenn);
			udas.add(uda);
		}
		return udas;
	}

	@Override
	public Map<String, List<Double>> UDAWexport() {
		if (udaws.size() > 0)
			return udaws;
		for (String linkID : traVols.keySet()) {
			List<Double> udaw = new ArrayList<Double>();
			for (UserDefAtt uda : udas) {
				String[] s = uda.getATTID().split("TO");
				udaw.add(traVols.get(linkID).get(Long.parseLong(s[0])));
			}
			udaws.put(linkID, udaw);
		}
		return udaws;
	}

	/**
	 * a function like
	 * org.matsim.demandmodeling.filters.filter.finalFilters.FinalEventFilterA#UDAWexport()
	 * for noise evaluation
	 *
	 * @return the TreeMap of values of attributs defined by VISUM9.3-user, but
	 *         only for noise evaluation
	 */
	public Map<String, List<Double>> lmUDAWexport() {
		if (udaws.size() > 0)
			return udaws;
		for (String linkID : traVols.keySet()) {
			List<Double> udaw = new ArrayList<Double>();
			for (UserDefAtt uda : udas) {
				String[] s = uda.getATTID().split("TO");
				double Lm = 37.3 + 10.0 * Math.log10(traVols.get(linkID).get(
						Integer.parseInt(s[0]))
						* (1.0 + 0.082 * 0.1));
				udaw.add(Lm);
			}
			udaws.put(linkID, udaw);
		}
		return udaws;
	}
}
