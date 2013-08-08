/* *********************************************************************** *
 * project: org.matsim.*
 * MarathonVelocityCalculator.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package playground.christoph.icem2012;

import java.util.concurrent.atomic.AtomicInteger;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;

import playground.christoph.evacuation.api.core.v01.Coord3d;
import playground.christoph.evacuation.utils.DeterministicRNG;
import playground.gregor.sim2d_v4.tmp.VelocityCalculator;

public class MarathonVelocityCalculator implements VelocityCalculator {

	private static final Logger log = Logger.getLogger(MarathonVelocityCalculator.class);
	
	// from 80% to -40%
	private final double[] slopeFactors = {
			0.0014, 0.0069, 0.0131, 0.0198, 0.0269, 0.0345, 0.0425, 0.0509, 0.0595, 0.0685,
			0.0776, 0.0870, 0.0966, 0.1064, 0.1163, 0.1264, 0.1365, 0.1468, 0.1572, 0.1677,
			0.1782, 0.1888, 0.1996, 0.2104, 0.2212, 0.2322, 0.2432, 0.2544, 0.2656, 0.2770,
			0.2884, 0.3000, 0.3117, 0.3236, 0.3356, 0.3477, 0.3600, 0.3725, 0.3852, 0.3981,
			0.4112, 0.4245, 0.4380, 0.4518, 0.4658, 0.4800, 0.4944, 0.5091, 0.5241, 0.5392,
			0.5546, 0.5703, 0.5861, 0.6022, 0.6185, 0.6349, 0.6516, 0.6683, 0.6852, 0.7023,
			0.7194, 0.7365, 0.7537, 0.7708, 0.7879, 0.8048, 0.8216, 0.8382, 0.8546, 0.8706,
			0.8862, 0.9013, 0.9160, 0.9300, 0.9433, 0.9558, 0.9675, 0.9782, 0.9878, 0.9963,
			1.0000, 1.0055, 1.0108, 1.0163, 1.0219, 1.0273, 1.0325, 1.0372, 1.0413, 1.0448,
			1.0474, 1.0491, 1.0497, 1.0494, 1.0478, 1.0451, 1.0412, 1.0361, 1.0297, 1.0221,
			1.0133, 1.0033, 0.9922, 0.9801, 0.9670, 0.9530, 0.9382, 0.9227, 0.9067, 0.8903,
			0.8737, 0.8570, 0.8405, 0.8242, 0.8085, 0.7935, 0.7795, 0.7667, 0.7555, 0.7460,
			0.7386};
	private AtomicInteger slopeWarnCount = new AtomicInteger(0);
	private AtomicInteger linkLengthWarnCount = new AtomicInteger(0);
	
	private final VelocityCalculator delegate;
	private final DeterministicRNG rng;
	
	public MarathonVelocityCalculator(VelocityCalculator delegate) {
		this.delegate = delegate;
		this.rng = new DeterministicRNG(1234590);
	}
	
	@Override
	public double getVelocity(Person person, Link link) {
		
		// Get person's walk velocity.
		double delegateVelocity = delegate.getVelocity(person, link);
		
		// We assume a person runs 2.0 times faster than the person would walk.
		double runFactor = 2.0;
		
		/*
		 * Add link steepness factor which is not included so far in the
		 * default velocity calculator.
		 */
		double slopeFactor = getSlopeFactor(calcSlope(link));
		
		// Add some random noise (+/- 10%) depending on the person and the link
		int seed = person.getId().hashCode() + link.getId().hashCode();
		double noiseFactor = 1 + (rng.hashCodeToRandomDouble(seed) - 0.5) / 5;
		
//		if (slopeFactor != 1.0) {
//			log.info("different slope factor: " + slopeFactor);
//		}
		
		return delegateVelocity * runFactor * slopeFactor * noiseFactor;
	}
	
	/*
	 * Returns the slope of a link in %.
	 */
	/*package*/ final double calcSlope(Link link) {
		double slope = 0.0;
		double length = link.getLength();
		if (length > 0.0) {
			Coord fromCoord = link.getFromNode().getCoord();
			Coord toCoord = link.getToNode().getCoord();
			
			/*
			 * If 3d coordinates are available, calculate the link's slope.
			 */
			if (fromCoord instanceof Coord3d && toCoord instanceof Coord3d) {
				double fromHeight = ((Coord3d) fromCoord).getZ();
				double toHeight = ((Coord3d) toCoord).getZ();
				double dHeight = toHeight - fromHeight;
				slope = dHeight / length;
			}		
		} else {
			double warnCount = linkLengthWarnCount.incrementAndGet();
			String text = "Link length is <= 0.0. Link's slope cannot be calculated. Assuming slope of 0%.";
			if (warnCount == 10) {
				log.warn(text + " No further warnings from this type will be given!");
			} else if (warnCount < 10) {
				log.warn(text);
			}
		}
		
		// convert slope to % and return it
		return 100 * slope;
	}

	/*package*/ double getSlopeFactor(double slope) {
		double slopeFactor = 1.0;
		
		double warnCount = slopeWarnCount.incrementAndGet();
		
		if (slope > 80.0) {
			String text = "Slope is out of expected range (-40% .. -80%). Found slope of " + slope + ". Use 80.0 instead.";
			if (warnCount == 10) {
					log.warn(text + " No further warnings from this type will be given!");
			} else if (warnCount < 10) {
				log.warn(text);
			}
			slope = 80.0;
		} else if (slope < -40.0) {
			String text = "Slope is out of expected range (-40% .. -80%). Found slope of " + slope + ". Use 80.0 instead.";
			if (warnCount == 10) {
				log.warn(text + " No further warnings from this type will be given!");
			} else if (warnCount < 10) {
				log.warn(text);
			}
			slope = -40.0;
		}
		slopeFactor = slopeFactors[-(int)Math.round(slope) + 80];
		return slopeFactor;
	}
}
