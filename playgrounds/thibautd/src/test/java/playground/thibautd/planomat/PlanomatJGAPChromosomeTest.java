/* *********************************************************************** *
 * project: org.matsim.*
 * PlanomatJGAPChromosomeTest.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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

package playground.thibautd.planomat;

import org.jgap.Configuration;
import org.jgap.Gene;
import org.jgap.IChromosome;
import org.jgap.InvalidConfigurationException;
import org.jgap.impl.IntegerGene;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

public class PlanomatJGAPChromosomeTest {

	@Ignore @Test
	public void testHashCode() throws InvalidConfigurationException {
		
		Configuration jgapConfiguration = new Configuration();

		int numGenes = 4;
		
		Gene[] testGenes = new Gene[4];

		Integer startPlan = Integer.valueOf(64);
		Integer workDur = Integer.valueOf(66);
		Integer homeDur = Integer.valueOf(67);
		Integer modeIndex = Integer.valueOf(69);
		
		for (int ii=0; ii < numGenes; ii++) {
			switch(ii) {
			case 0:
				testGenes[ii] = new IntegerGene(jgapConfiguration);
				testGenes[ii].setAllele(startPlan);
				break;
			case 1:
				testGenes[ii] = new IntegerGene(jgapConfiguration);
				testGenes[ii].setAllele(workDur);
				break;
			case 2:
				testGenes[ii] = new IntegerGene(jgapConfiguration);
				testGenes[ii].setAllele(homeDur);
				break;
			case 3:
				testGenes[ii] = new IntegerGene(jgapConfiguration);
				testGenes[ii].setAllele(modeIndex);
				break;
			}

		}

		IChromosome testee = new PlanomatJGAPChromosome(jgapConfiguration, testGenes);
		System.out.println(Integer.toString(testee.hashCode(), 16));
		
		Assert.fail("Not yet implemented");
	}

}
