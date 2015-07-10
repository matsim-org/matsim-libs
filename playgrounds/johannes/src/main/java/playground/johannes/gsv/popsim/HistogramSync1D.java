/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

package playground.johannes.gsv.popsim;

import gnu.trove.TDoubleArrayList;
import gnu.trove.TDoubleDoubleHashMap;

import java.util.Collection;

import playground.johannes.gsv.synPop.ProxyPerson;
import playground.johannes.gsv.synPop.sim3.Hamiltonian;
import playground.johannes.gsv.synPop.sim3.SamplerListener;
import playground.johannes.sna.math.Discretizer;
import playground.johannes.sna.math.FixedSampleSizeDiscretizer;
import playground.johannes.sna.math.Histogram;
import playground.johannes.sna.math.InterpolatingDiscretizer;
import playground.johannes.sna.math.LinearDiscretizer;

/**
 * @author johannes
 *
 */
public class HistogramSync1D implements Hamiltonian, SamplerListener {

	private TDoubleDoubleHashMap refHist;
	
	private TDoubleDoubleHashMap simHist;
	
	private String attName;
	
	private Discretizer discretizer;
	
//	private double[] keys;
	
//	private double partialDelta;
	
	private double currentDelta;
	
	private double simFactor;
	
	public HistogramSync1D(Collection<ProxyPerson> refPop, Collection<ProxyPerson> simPop, String attName, Discretizer discretizer) {
		this.attName = attName;
		this.discretizer = discretizer;
		
		TDoubleArrayList refValues = new TDoubleArrayList(refPop.size());
		for(ProxyPerson person : refPop) {
			String value = person.getAttribute(attName);
			if(value != null) {
				refValues.add(Double.parseDouble(value));
			}
		}
		this.discretizer = new InterpolatingDiscretizer(refValues.toNativeArray());
//		this.discretizer = new LinearDiscretizer(10);
		
		refHist = Histogram.createHistogram(refValues.toNativeArray(), this.discretizer, false);
//		keys = refHist.keys();
		Histogram.normalize(refHist);
		
		TDoubleArrayList simValues = new TDoubleArrayList(simPop.size());
		for(ProxyPerson person : simPop) {
			String value = person.getAttribute(attName);
			if(value != null) {
				simValues.add(Double.parseDouble(value));
			}
		}
		simHist = Histogram.createHistogram(simValues.toNativeArray(), this.discretizer, false);
		simFactor = 1/(double)simValues.size();
//		Histogram.normalize(simHist);
		
		currentDelta = fullDiff();
	}
	
	public void notifyChange(String attName, double prevValue, double newValue) {
		if(attName.equals(this.attName)) {
			double bin = discretizer.discretize(prevValue);
			double prevDiff = Math.abs((simHist.get(bin) * simFactor) - refHist.get(bin));
			simHist.adjustValue(bin, -1);
			double newDiff = Math.abs((simHist.get(bin) * simFactor) - refHist.get(bin));
			double partialDelta = newDiff - prevDiff;
			
			
			
			bin = discretizer.discretize(newValue);
			prevDiff = Math.abs((simHist.get(bin) * simFactor) - refHist.get(bin));
			simHist.adjustValue(bin, 1);
			newDiff = Math.abs((simHist.get(bin) * simFactor) - refHist.get(bin));
			partialDelta += newDiff - prevDiff;
			
			currentDelta += partialDelta;
		}
	}
	
	public double fullDiff() {
		double delta = 0;
		double[] keys = refHist.keys();
		for(double key : keys) {
			if(refHist.containsKey(key)) {
			double ref = refHist.get(key);
			double sim = simHist.get(key) * simFactor;
			
			delta += Math.abs(sim - ref);
			}
		}
		
		return delta;///(double)keys.length;
	}

	/* (non-Javadoc)
	 * @see playground.johannes.gsv.synPop.sim3.Hamiltonian#evaluate(playground.johannes.gsv.synPop.ProxyPerson)
	 */
	@Override
	public double evaluate(ProxyPerson person) {
//		return currentDelta;
		return fullDiff();
	}

	/* (non-Javadoc)
	 * @see playground.johannes.gsv.synPop.sim3.SamplerListener#afterStep(java.util.Collection, java.util.Collection, boolean)
	 */
	@Override
	public void afterStep(Collection<ProxyPerson> population, Collection<ProxyPerson> mutations, boolean accepted) {
		// TODO Auto-generated method stub
		
	}
}
			
			
			
		