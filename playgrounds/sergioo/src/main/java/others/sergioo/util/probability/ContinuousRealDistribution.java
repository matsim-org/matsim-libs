package others.sergioo.util.probability;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.commons.math3.distribution.AbstractRealDistribution;
import org.apache.commons.math3.random.Well19937c;

public class ContinuousRealDistribution extends AbstractRealDistribution {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public static double NUM_DIVS = 20;
	
	private Collection<Double> values;
	
	
	public ContinuousRealDistribution() {
		super(new Well19937c());
		this.values = new ArrayList<>();
	}
	public ContinuousRealDistribution(Collection<Double> values) {
		super(new Well19937c());
		this.values = values;
	}

	public void addValue(double value) {
		if(!Double.isNaN(value) && !Double.isInfinite(value)) 
			values.add(value);
	}
	@Override
	public double probability(double x) {
		return density(x);
	}
	@Override
	public double density(double x) {
		SortedMap<Double, Integer> sortedValues = new TreeMap<>();
		for(double value:values) {
			Integer num = sortedValues.get(value);
			if(num==null)
				num = 0;
			sortedValues.put(value, ++num);
		}
		double interval = (sortedValues.lastKey()-sortedValues.firstKey())/NUM_DIVS;
		boolean start = false;
		boolean end = false;
		double area = 0;
		Double prev = null;
		for(Entry<Double, Integer> value:sortedValues.entrySet()) {
			Double postVal = value.getKey();
			Double prevVal = prev;
			if(!start && !end && x-interval<value.getKey()) {
				start = true;
				prevVal = x-interval;
				if(prev==null)
					prev = prevVal;
			}
			if(start) {
				if(x+interval<=value.getKey()) {
					end = true;
					start = false;
					postVal = x+interval;
				}
				area += value.getValue()*(postVal-prevVal)/((values.size())*(value.getKey()-prev));
			}
			prev = value.getKey();
		}
		return area;
	}

	@Override
	public double cumulativeProbability(double x) {
		SortedMap<Double, Integer> sortedValues = new TreeMap<>();
		for(double value:values) {
			Integer num = sortedValues.get(value);
			if(num==null)
				num = 0;
			sortedValues.put(value, ++num);
		}
		double sum = 0;
		for(Entry<Double, Integer> value:sortedValues.entrySet()) {
			if(x<value.getKey())
				return sum/values.size();
			sum += value.getValue();
		}
		return 1;
	}
	@Override
	public double getNumericalMean() {
		double sum = 0;
		for(Double value:values)
			sum+=value;
		return sum/values.size();
	}

	@Override
	public double getNumericalVariance() {
		double sum = 0, mean = getNumericalMean();
		for(Double value:values)
			sum+=Math.pow(value-mean, 2);
		return sum/values.size();
	}

	@Override
	public double getSupportLowerBound() {
		double min = Double.POSITIVE_INFINITY;
		double max = -Double.POSITIVE_INFINITY;
		for(Double value:values) {
			if(value<min)
				min = value;
			if(value>max)
				max = value;
		}
		return min-(max-min)/(10*NUM_DIVS);
	}

	@Override
	public double getSupportUpperBound() {
		double max = -Double.POSITIVE_INFINITY;
		for(Double value:values)
			if(value>max)
				max = value;
		return max;
	}

	@Override
	public boolean isSupportLowerBoundInclusive() {
		return true;
	}

	@Override
	public boolean isSupportUpperBoundInclusive() {
		return true;
	}

	@Override
	public boolean isSupportConnected() {
		return true;
	}

}
