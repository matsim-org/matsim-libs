/* *********************************************************************** *
 * project: org.matsim.*
 * AnalyseEquilibriumOptimalPlans.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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
package playground.thibautd.analysis.aposteriorianalysis;

import org.matsim.core.controler.Controler;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.core.utils.charts.ChartUtil;

import playground.thibautd.jointtrips.population.PopulationWithCliques;
import playground.thibautd.jointtrips.utils.JointControlerUtils;
import playground.thibautd.utils.MoreIOUtils;

/**
 * Executable which uses an {@link EquilibriumOptimalPlansAnalyser} to produce
 * analysis from equilibrium-optimal plans files.
 *
 * @author thibautd
 */
public class AnalyseEquilibriumOptimalPlans {
	private static final int WIDTH = 800;
	private static final int HEIGHT = 600;

	/**
	 * @param args the command line arguments, in the form
	 * <tt>
	 * configUntoggledPopulation configToggledPopulation configIndividualPopulation outputDirectory
	 * </tt>
	 */
	public static void main(final String[] args) {
		String untoggledConfigFile;
		String toggledConfigFile;
		String individualConfigFile;
		String outputDir;

		try {
			untoggledConfigFile = args[ 0 ];
			toggledConfigFile = args[ 1 ];
			individualConfigFile = args[ 2 ];
			outputDir = args[ 3 ];
		} catch (ArrayIndexOutOfBoundsException e) {
			System.out.println( "usage: AnalyseEquilibriumOptimalPlans configUntoggledPopulation configToggledPopulation configIndividualPopulation outputDirectory" );
			return;
		}

		MoreIOUtils.initOut( outputDir );

		PopulationWithCliques popUntoggled;
		PopulationWithCliques popToggled;
		PopulationWithCliques popIndividual;
		ScoringFunctionFactory scoringFunctionFactory;

		{
			Controler controler = JointControlerUtils.createControler( untoggledConfigFile );
			popUntoggled = (PopulationWithCliques) controler.getScenario().getPopulation();
			scoringFunctionFactory = controler.getScoringFunctionFactory();
		}

		popToggled = (PopulationWithCliques)
			JointControlerUtils.createScenario( toggledConfigFile ).getPopulation();
		popIndividual = (PopulationWithCliques)
			JointControlerUtils.createScenario( individualConfigFile ).getPopulation();

		EquilibriumOptimalPlansAnalyser analyser =
			new EquilibriumOptimalPlansAnalyser(
					popUntoggled,
					popToggled,
					popIndividual,
					scoringFunctionFactory);

		ChartUtil chart = analyser.getTravelTimeRelativeImprovementsChart();
		chart.saveAsPng( outputDir+"/travelTimeImprovements.png", WIDTH, HEIGHT );

		chart = analyser.getScoreAbsoluteImprovementsChart();
		chart.saveAsPng( outputDir+"/scoreImprovements.png", WIDTH, HEIGHT );

		chart = analyser.getScoreAbsoluteImprovementsToggleChart();
		chart.saveAsPng( outputDir+"/toggleScoreImprovements.png", WIDTH, HEIGHT );

		chart = analyser.getScoreDistributionsChart();
		chart.saveAsPng( outputDir+"/scoreDistributions.png", WIDTH, HEIGHT );
	}
}

