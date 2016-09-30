package cba;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
class TourSequenceAnalyzer {

	private Map<List<Tour.Act>, Integer> tourActSeq2cnt = new LinkedHashMap<>();

	private Map<Tour.Act, Map<Link, Integer>> act2link2cnt = new LinkedHashMap<>();

	private Map<Tour.Mode, Integer> mode2cnt = new LinkedHashMap<>();

	// private BasicStatistics utilityStatistics = new BasicStatistics();

	TourSequenceAnalyzer(final Network network) {

		this.tourActSeq2cnt.put(new ArrayList<Tour.Act>(), 0);
		this.tourActSeq2cnt.put(Arrays.asList(Tour.Act.work), 0);
		this.tourActSeq2cnt.put(Arrays.asList(Tour.Act.other), 0);
		this.tourActSeq2cnt.put(Arrays.asList(Tour.Act.work, Tour.Act.other), 0);

		this.mode2cnt.put(Tour.Mode.car, 0);
		this.mode2cnt.put(Tour.Mode.pt, 0);

		this.act2link2cnt.put(Tour.Act.home, new LinkedHashMap<Link, Integer>());
		this.act2link2cnt.put(Tour.Act.work, new LinkedHashMap<Link, Integer>());
		this.act2link2cnt.put(Tour.Act.other, new LinkedHashMap<Link, Integer>());
		for (Link link : network.getLinks().values()) {
			this.act2link2cnt.get(Tour.Act.home).put(link, 0);
			this.act2link2cnt.get(Tour.Act.work).put(link, 0);
			this.act2link2cnt.get(Tour.Act.other).put(link, 0);
		}
	}

	void add(final Link homeLoc, final TourSequence tourSequence) {
		// this.utilityStatistics.add(utility);
		this.tourActSeq2cnt.put(tourSequence.getTourPurposes(),
				this.tourActSeq2cnt.get(tourSequence.getTourPurposes()) + 1);
		this.act2link2cnt.get(Tour.Act.home).put(homeLoc, this.act2link2cnt.get(Tour.Act.home).get(homeLoc) + 1);
		for (Tour tour : tourSequence.tours) {
			this.mode2cnt.put(tour.mode, this.mode2cnt.get(tour.mode) + 1);
			this.act2link2cnt.get(tour.act).put(tour.destination,
					this.act2link2cnt.get(tour.act).get(tour.destination) + 1);
		}
	}

	private int totalCnt(final Tour.Act tourAct) {
		int result = 0;
		for (Map.Entry<List<Tour.Act>, Integer> entry : tourActSeq2cnt.entrySet()) {
			if (entry.getKey().contains(tourAct)) {
				result += entry.getValue();
			}
		}
		return result;
	}

	private class SizeEntryComp implements Comparator<Map.Entry<Link, Integer>> {
		@Override
		public int compare(Entry<Link, Integer> o1, Entry<Link, Integer> o2) {
			int result = -o1.getValue().compareTo(o2.getValue());
			if (result == 0) {
				result = o1.getKey().getId().compareTo(o2.getKey().getId());
			}
			return result;
		}
	}

	void printReport() {

		// System.out.println();
		// System.out.println("UTILITY STATISTICS");
		// System.out.println("mean\t" + this.utilityStatistics.getAvg());
		// System.out.println("stddev\t" + this.utilityStatistics.getStddev());
		// System.out.println("min\t" + this.utilityStatistics.getMin());
		// System.out.println("max\t" + this.utilityStatistics.getMax());

		System.out.println();
		int total = 0;
		System.out.println("TOTAL TOUR PURPOSE SEQUENCE FREQUENCIES");
		for (Map.Entry<List<Tour.Act>, Integer> entry : this.tourActSeq2cnt.entrySet()) {
			System.out.println(entry.getKey() + "\t" + entry.getValue());
			total += entry.getValue();
		}
		System.out.println("TOTAL\t" + total);

		System.out.println();
		System.out.println("TOTAL TOUR PURPOSE FREQUENCIES");
		System.out.println(Tour.Act.work + "\t" + this.totalCnt(Tour.Act.work));
		System.out.println(Tour.Act.other + "\t" + this.totalCnt(Tour.Act.other));
		System.out.println("TOTAL\t" + this.totalCnt(Tour.Act.work) + this.totalCnt(Tour.Act.other));

		System.out.println();
		total = 0;
		System.out.println("TOTAL MODE FREQUENCIES");
		for (Map.Entry<Tour.Mode, Integer> entry : this.mode2cnt.entrySet()) {
			System.out.println(entry.getKey() + "\t" + entry.getValue());
			total += entry.getValue();
		}
		System.out.println("TOTAL\t" + total);

		for (Tour.Act act : Tour.Act.values()) {
			System.out.println();
			total = 0;
			System.out.println("TOTAL " + act + " LOCATION CHOICE FREQUENCIES");
			final List<Map.Entry<Link, Integer>> entries = new ArrayList<>(this.act2link2cnt.get(act).entrySet());
			Collections.sort(entries, new SizeEntryComp());
			for (Map.Entry<Link, Integer> entry : entries) {
				System.out.println(entry.getKey().getId() + "\t" + entry.getValue());
				total += entry.getValue();
			}
			System.out.println("TOTAL\t" + total);
		}

		System.out.println();
	}
}
