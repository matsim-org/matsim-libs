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
import playground.johannes.gsv.synPop.sim3.SamplerListener;
import playground.johannes.sna.math.Discretizer;
import playground.johannes.sna.math.Histogram;
import playground.johannes.sna.math.InterpolatingDiscretizer;
import playground.johannes.synpop.data.Person;
import playground.johannes.synpop.data.PlainPerson;
import playground.johannes.synpop.sim.Hamiltonian;

import java.util.Collection;

/**
 * @author johannes
 *
 */
public class HistogramSync1D implements Hamiltonian, SamplerListener, HistogramSync {

	private TDoubleDoubleHashMap refHist;

	private TDoubleDoubleHashMap simHist;

	private String attName;

	private Object attKey;

	private Discretizer discretizer;

//	private double[] keys;

//	private double partialDelta;

	private double currentDelta;

	private double simFactor;

	public HistogramSync1D(Collection<PlainPerson> refPop, Collection<PlainPerson> simPop, String attName, Object attKey, Discretizer discretizer) {
		this.attName = attName;
		this.attKey = attKey;
		this.discretizer = discretizer;

		TDoubleArrayList refValues = new TDoubleArrayList(refPop.size());
		for(PlainPerson person : refPop) {
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
		for(PlainPerson person : simPop) {
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

	public void notifyChange(Object key, double prevValue, double newValue, PlainPerson person) {
		if(key.equals(this.attKey)) {
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
	 * @see playground.johannes.synpop.sim.Hamiltonian#evaluate(playground.johannes.synpop.data.PlainPerson)
	 */
	@Override
	public double evaluate(Person person) {
//		return currentDelta;
		return fullDiff();
	}

	/* (non-Javadoc)
	 * @see playground.johannes.gsv.synPop.sim3.SamplerListener#afterStep(java.util.Collection, java.util.Collection, boolean)
	 */
	@Override
	public void afterStep(Collection<? extends Person> population, Collection<? extends Person> mutations, boolean accepted) {
		// TODO Auto-generated method stub

	}
}
			
			
			
		