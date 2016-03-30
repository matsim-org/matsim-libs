package others.sergioo.util.probability;

import java.io.Serializable;
import java.util.Collection;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.commons.math3.distribution.AbstractRealDistribution;
import org.apache.commons.math3.random.Well19937c;

public class ContinuousRealDistribution extends AbstractRealDistribution implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private double numDivs = 100;
	
	private SortedMap<Double, Integer> values = new TreeMap<>();

	private int numValues=0;
	
	
	public ContinuousRealDistribution() {
		super(new Well19937c());
	}
	public ContinuousRealDistribution(Collection<Double> values) {
		super(new Well19937c());
		for(Double value:values)
			addValue(value);
	}
	public ContinuousRealDistribution(int numDivs) {
		super(new Well19937c());
		this.numDivs = numDivs;
	}
	public ContinuousRealDistribution(Collection<Double> values, int numDivs) {
		super(new Well19937c());
		for(Double value:values)
			addValue(value);
		this.numDivs = numDivs;
	}

	public void addValue(double value) {
		if(!Double.isNaN(value) && !Double.isInfinite(value)) {
			Integer num = values.get(value);
			if(num==null)
				num = 0;
			values.put(value, ++num);
		}
		numValues++;
	}
	@Override
	public double probability(double x) {
		return density(x);
	}
	@Override
	public double density(double x) {
		double interval = (values.lastKey()-values.firstKey())/numDivs;
		boolean start = false;
		boolean end = false;
		double area = 0;
		Double prev = null;
		for(Entry<Double, Integer> value:values.entrySet()) {
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
				area += value.getValue()*(postVal-prevVal)/((numValues)*(value.getKey()-prev));
			}
			prev = value.getKey();
		}
		return area;
	}

	@Override
	public double cumulativeProbability(double x) {
		double sum = 0;
		for(Entry<Double, Integer> value:values.entrySet()) {
			if(x<value.getKey())
				return sum/numValues;
			sum += value.getValue();
		}
		return 1;
	}
	@Override
	public double getNumericalMean() {
		double sum = 0;
		for(Entry<Double, Integer> value:values.entrySet())
			sum+=value.getKey()*value.getValue();
		return sum/numValues;
	}

	@Override
	public double getNumericalVariance() {
		double sum = 0, mean = getNumericalMean();
		for(Entry<Double, Integer> value:values.entrySet())
			for(int i=0; i<value.getValue(); i++)
				sum+=Math.pow(value.getKey()-mean, 2);
		return sum/numValues;
	}

	@Override
	public double getSupportLowerBound() {
		return values.firstKey()-(values.lastKey()-values.firstKey())/(10*numDivs);
	}

	@Override
	public double getSupportUpperBound() {
		return values.lastKey();
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
	
	public SortedMap<Double, Integer> getValues() {
		return values;
	}

}
