/**
 * 
 */
package stats;

import java.util.Iterator;

import org.apache.commons.math.stat.Frequency;
import org.matsim.core.gbl.MatsimRandom;

/**
 * @author stefan
 *
 */
public class EmpiricalWalker {
	
	private Frequency frequency;
	
	public EmpiricalWalker(){
		frequency = new Frequency();
	}
	
	public EmpiricalWalker(Frequency frequency) {
		super();
		this.frequency = frequency;
	}

	public Frequency getFrequency() {
		return frequency;
	}

	public Comparable<?> nextValue(){
		double randomValue = MatsimRandom.getRandom().nextDouble();
		Iterator<Comparable<?>> iter = frequency.valuesIterator();
		while(iter.hasNext()){
			Comparable<?> val = iter.next();
			if(randomValue < frequency.getCumPct(val)){
				return val;
			}
		}
		return null;
	}
}
