package opdytsintegration;

import static java.lang.Math.min;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.core.events.handler.EventHandler;

import floetteroed.utilities.DynamicData;
import opdytsintegration.utils.RecursiveCountAverage;

/**
 * Keeps track of a (part of a) MATSim state vector that is composed of counts
 * (for instance, vehicles on a road or passengers waiting at a stop).
 * 
 * @author Gunnar Flötteröd
 *
 */
public class MATSimCountingStateAnalyzer<L extends Object> implements EventHandler {

	// -------------------- MEMBERS --------------------

	// The object gets locked once a value is read out of it.
	private boolean locked = false;

	private final DynamicData<Id<L>> counts;

	private final Map<Id<L>, RecursiveCountAverage> location2avgCnt = new LinkedHashMap<>();

	private int lastCompletedBin = -1;

	// -------------------- CONSTRUCTION --------------------

	public MATSimCountingStateAnalyzer(final int startTime_s, final int binSize_s, final int binCnt) {
		this.counts = new DynamicData<>(startTime_s, binSize_s, binCnt);
		this.reset(-1);
	}

	// -------------------- IMPLEMENTATION OF EventHandler --------------------

	@Override
	public void reset(final int iteration) {
		this.locked = false;
		this.counts.clear();
		this.location2avgCnt.clear();
		this.lastCompletedBin = -1;
	}

	// -------------------- INTERNALS --------------------

	private void checkLocked() {
		if (this.locked) {
			throw new RuntimeException(this.getClass().getSimpleName()
					+ " is locked, i.e. read has taken place, meaning that no more writes are allowed.");
		}
	}

	private int lastCompletedBinEndTime() {
		return this.counts.getStartTime_s() + (this.lastCompletedBin + 1) * this.counts.getBinSize_s();
	}

	private void completeBins(final int lastBinToComplete) {
		while (this.lastCompletedBin < lastBinToComplete) {
			this.lastCompletedBin++; // is now zero or larger
			final int lastCompletedBinEndTime = this.lastCompletedBinEndTime();
			for (Map.Entry<Id<L>, RecursiveCountAverage> link2avgEntry : this.location2avgCnt.entrySet()) {
				link2avgEntry.getValue().advanceTo(lastCompletedBinEndTime);
				this.counts.put(link2avgEntry.getKey(), this.lastCompletedBin, link2avgEntry.getValue().getAverage());
				link2avgEntry.getValue().resetTime(lastCompletedBinEndTime);
			}
		}
	}

	private void advanceToTime(final int time_s) {
		final int lastBinToComplete = this.counts.bin(time_s) - 1;
		this.completeBins(min(lastBinToComplete, this.counts.getBinCnt() - 1));
	}

	private RecursiveCountAverage avg(final Id<L> link) {
		RecursiveCountAverage avg = this.location2avgCnt.get(link);
		if (avg == null) {
			avg = new RecursiveCountAverage(this.lastCompletedBinEndTime());
			this.location2avgCnt.put(link, avg);
		}
		return avg;
	}

	// -------------------- FOR SUBCLASSING --------------------

	protected void registerIncrease(final Id<L> location, final int time_s) {
		this.checkLocked();
		this.advanceToTime(time_s);
		this.avg(location).inc(time_s);
	}

	protected void registerDecrease(final Id<L> location, final int time_s) {
		this.checkLocked();
		this.advanceToTime(time_s);
		this.avg(location).dec(time_s);
	}

	// Use this, otherwise the last bin gets lost!
	public void advanceToEnd() {
		this.completeBins(this.counts.getBinCnt() - 1);
	}

	// TODO only used for testing
	public Set<Id<L>> observedLinkSetView() {
		return Collections.unmodifiableSet(this.counts.keySet());
	}

	// -------------------- CONTENT ACCESS --------------------

	public double getCount(final Id<L> link, final int bin) {
		this.advanceToEnd();
		this.locked = true;
		return this.counts.getBinValue(link, bin);
	}
}
