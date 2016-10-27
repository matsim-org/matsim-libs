package playground.sebhoerl.av_paper;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

public class BinCalculator {
	final double start;
	final double end;
	final double interval;
	final int bins;
	
	public class BinEntry {
		final private int index;
		final private double weight;
		
		public BinEntry(int index, double weight) {
			this.index = index;
			this.weight = weight;
		}
		
		public int getIndex() {
			return index;
		}
		
		public double getWeight() {
			return weight;
		}
	}
	
	private BinCalculator(double start, double end, double interval, int bins) {
		this.start = start;
		this.end = end;
		this.interval = interval;
		this.bins = bins;
	}
	
	public static BinCalculator createByBins(double start, double end, int bins) {
		double interval = (end - start) / (double) bins;
		return new BinCalculator(start, end, interval, bins);
	}
	
	public static BinCalculator createByInterval(double start, double end, double interval) {
		int bins = (int)Math.ceil((end - start) / interval);
		return new BinCalculator(start, start + bins * interval, interval, bins);
	}
	
	public boolean isCoveredValue(double value) {
		return value >= start && value <= end;
	}
	
	public boolean isIntersecting(double start, double end) {
		if (start < this.start && end < this.start) return false; 
		if (start > this.end && end > this.end) return false;
		return true;
	}
	
	public double getStart() {
		return start;
	}
	
	public double getEnd() {
		return end;
	}
	
	public int getBins() {
		return bins;
	}
	
	public double getInterval() {
		return interval;
	}
	
	public boolean isValidIndex(int index) {
		return index < bins;
	}
	
	public double getStart(int index) {
		if (isValidIndex(index)) {
			return index * interval + start;
		} else {
			throw new IllegalArgumentException();
		}
	}
	
	public double getEnd(int index) {
		if (isValidIndex(index)) {
			return (index + 1) * interval + start;
		} else {
			throw new IllegalArgumentException();
		}
	}
	
	public double normalize(double value) {
		return Math.min(end, Math.max(start, value));
	}
	
	public double normalize(double value, int index) {
		return Math.min(getEnd(index), Math.max(getStart(index), value));
	}
	
	public int getIndex(double value) {
		if (isCoveredValue(value)) {
			return (int) Math.min(Math.floor((value - start) / interval), bins - 1);
		} else {
			throw new IllegalArgumentException();
		}
	}
	
	public int getIndexNormalized(double value) {
		return getIndex(normalize(value));
	}
	
	public int[] getRangeIndices(double start, double end) {
		int result[] = { getIndex(start),  getIndex(end) };
		return result;		
	}
	
	public int[] getRangeIndicesNormalized(double start, double end) {
		return getRangeIndices(normalize(start), normalize(end));
	}
	
	private double[] computeRangeWeights(double start, double end, int startIndex, int endIndex) {
		if (startIndex == endIndex) {
			double weights[] = { (end - start) / interval };
			return weights;
		}
		
		double weights[] = {
			(getEnd(startIndex) - start) / interval,
			(end - getStart(endIndex)) / interval
		};
		
		return weights;
	}
	
	public double[] getRangeWeights(double start, double end) {
		int indices[] = getRangeIndices(start, end);
		return computeRangeWeights(start, end, indices[0], indices[1]);		
	}
	
	public double[] getRangeWeightsNormalized(double start, double end) {
		return getRangeWeights(normalize(start), normalize(end));
	}
	
	public int[] getIndices(double start, double end) {
		int range[] = getRangeIndices(start, end);
		int indices[] = new int[range[1] - range[0] + 1];
		
		for (int i = range[0]; i <= range[1]; i++) {
			indices[i - range[0]] = i;
		}
		
		return indices;
	}
	
	public int[] getIndicesNormalized(double start, double end) {
		return getIndices(normalize(start), normalize(end));
	}
	
	public double[] getWeights(double start, double end) {
		int range[] = getRangeIndices(start, end);
		double rangeWeights[] = computeRangeWeights(start, end, range[0], range[1]);
		double weights[] = new double[range[1] - range[0] + 1];
		
		for (int i = range[0] + 1; i < range[1]; i++) {
			weights[i - range[0]] = 1.0;
		}
		
		if (range[0] == range[1]) {
			weights[0] = rangeWeights[0];
		} else {
			weights[0] = rangeWeights[0];
			weights[range[1] - range[0]] = rangeWeights[1];
		}

		return weights;
	}
	
	public double[] getWeightsNormalized(double start, double end) {
		return getWeights(normalize(start), normalize(end));
	}
	
	public Collection<BinEntry> getBinEntries(double start, double end) {
		int range[] = getRangeIndices(start, end);
		
		double weights[] = computeRangeWeights(start, end, range[0], range[1]);
		ArrayList<BinEntry> entries = new ArrayList<BinEntry>(Collections.nCopies(range[1] - range[0] + 1, (BinEntry)null));
		
		for (int i = range[0] + 1; i < range[1]; i++) {
			entries.set(i - range[0], new BinEntry(i, 1.0));
		}
		
		if (range[0] == range[1]) {
			entries.set(0, new BinEntry(range[0], weights[0]));
		} else {
			entries.set(0, new BinEntry(range[0], weights[0]));
			entries.set(range[1] - range[0], new BinEntry(range[1], weights[1]));
		}
		
		return entries;
	}
	
	public Collection<BinEntry> getBinEntriesNormalized(double start, double end) {
		if (isIntersecting(start, end)) {
			return getBinEntries(normalize(start), normalize(end));
		} else {
			return Collections.emptyList();
		}
	}
}
