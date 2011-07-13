/* *********************************************************************** *
 * project: org.matsim.*
 * AnalyseUtilityGains.java
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.utils.charts.ChartUtil;

import playground.thibautd.jointtripsoptimizer.population.Clique;
import playground.thibautd.jointtripsoptimizer.population.JointActingTypes;
import playground.thibautd.jointtripsoptimizer.population.ScenarioWithCliques;
import playground.thibautd.jointtripsoptimizer.utils.JointControlerUtils;
import playground.thibautd.utils.BoxAndWhiskersChart;
import playground.thibautd.utils.XYChartUtils;

/**
 * Output graphs related to the utility gains due to joint trips at the
 * clique and individual level.
 *
 * @author thibautd
 */
public class AnalyseUtilityGains {
	private static final Logger log =
		Logger.getLogger(AnalyseUtilityGains.class);

	private static final String TITLE_CLIQUE = "utility gains at the clique level";
	private static final String X_CLIQUE = "average number of joint trips per clique member";
	private static final String Y_CLIQUE = "average utility gain per clique member";
	private static final double BIN_WIDTH_CLIQUE = 0.15;

	private static final String TITLE_INDIVIDUAL = "utility gains at the individual level";
	private static final String X_INDIVIDUAL = "number of joint trips";
	private static final String Y_INDIVIDUAL = "utility gain";
	private static final double BIN_WIDTH_INDIVIDUAL = 1;

	private static final String Y_REL = "percentage of gain";

	private static final int[] CLIQUE_SIZES = {3,5,10,30,Integer.MAX_VALUE};

	private static final int WIDTH = 800;
	private static final int HEIGHT = 600;

	/**
	 * usage: AnalyseUtilityGains config1 config2 outputPath
	 *
	 * config1: individual
	 * config2: joint
	 */
	public static void main(final String[] args) {
		String configFile1 = args[0];
		String configFile2 = args[1];
		String outputPath = args[2];

		ScenarioWithCliques scenarioIndividual = JointControlerUtils.createScenario(configFile1);
		ScenarioWithCliques scenarioJoint = JointControlerUtils.createScenario(configFile2);

		for (int maxCliqueSize : CLIQUE_SIZES) {
			ChartUtil cliqueLevelChart = createCliqueLevelChart(scenarioIndividual, scenarioJoint, maxCliqueSize);
			cliqueLevelChart.saveAsPng(outputPath+"/cliqueLevelGains-lessThan"+maxCliqueSize+".png", WIDTH, HEIGHT);

			cliqueLevelChart = createCliqueLevelRelativeChart(scenarioIndividual, scenarioJoint, maxCliqueSize);
			cliqueLevelChart.saveAsPng(outputPath+"/cliqueLevelRelativeGains-lessThan"+maxCliqueSize+".png", WIDTH, HEIGHT);

			ChartUtil perCliqueSizeChart = createPerCliqueSizeChart(scenarioIndividual, scenarioJoint, maxCliqueSize);
			perCliqueSizeChart.saveAsPng(outputPath+"/perCliqueSizeGains-lessThan"+maxCliqueSize+".png", WIDTH, HEIGHT);

			perCliqueSizeChart = createPerCliqueSizeRelativeChart(scenarioIndividual, scenarioJoint, maxCliqueSize);
			perCliqueSizeChart.saveAsPng(outputPath+"/perCliqueSizeRelativeGains-lessThan"+maxCliqueSize+".png", WIDTH, HEIGHT);
		}

		ChartUtil individualLevelChart = createIndividualLevelChart(scenarioIndividual, scenarioJoint);
		individualLevelChart.saveAsPng(outputPath+"/individualLevelGains.png", WIDTH, HEIGHT);

		ChartUtil individualLevelPassengerChart = createIndividualLevelPassengerOnlyChart(scenarioIndividual, scenarioJoint);
		individualLevelPassengerChart.saveAsPng(outputPath+"/individualLevelPassengerGains.png", WIDTH, HEIGHT);

		ChartUtil individualLevelDriverChart = createIndividualLevelDriverOnlyChart(scenarioIndividual, scenarioJoint);
		individualLevelDriverChart.saveAsPng(outputPath+"/individualLevelDriversGains.png", WIDTH, HEIGHT);

		individualLevelChart = createIndividualLevelRelativeChart(scenarioIndividual, scenarioJoint);
		individualLevelChart.saveAsPng(outputPath+"/individualLevelRelativeGains.png", WIDTH, HEIGHT);

		individualLevelPassengerChart = createIndividualLevelPassengerOnlyRelativeChart(scenarioIndividual, scenarioJoint);
		individualLevelPassengerChart.saveAsPng(outputPath+"/individualLevelRelativePassengerGains.png", WIDTH, HEIGHT);

		individualLevelDriverChart = createIndividualLevelDriverOnlyRelativeChart(scenarioIndividual, scenarioJoint);
		individualLevelDriverChart.saveAsPng(outputPath+"/individualLevelRelativeDriversGains.png", WIDTH, HEIGHT);
	}

	// ////////////////////////////////////////////////////////////////////////
	// chart creation methods
	// ////////////////////////////////////////////////////////////////////////
	private static ChartUtil createCliqueLevelChart(
			final ScenarioWithCliques scenarioIndividual,
			final ScenarioWithCliques scenarioJoint,
			final int maxCliqueSize) {
		BoxAndWhiskersChart chart = new BoxAndWhiskersChart(
				(maxCliqueSize == Integer.MAX_VALUE ?
					 TITLE_CLIQUE :
					 TITLE_CLIQUE+", cliques of less than "+maxCliqueSize+" members"),
				X_CLIQUE,
				Y_CLIQUE,
				BIN_WIDTH_CLIQUE);

		double gain;
		double avgNJointTrips;
		int nMembers;
		Clique cliqueIndiv;
		Clique cliqueJoint;

		Map<Id, Clique> cliquesIndiv = new HashMap<Id, Clique>(scenarioIndividual.getCliques().getCliques());
		Map<Id, Clique> cliquesJoint = new HashMap<Id, Clique>(scenarioJoint.getCliques().getCliques());
		List<Id> cliqueIds = new ArrayList<Id>(cliquesIndiv.keySet());

		for (Id id : cliqueIds) {
			cliqueIndiv = cliquesIndiv.remove(id);
			cliqueJoint = cliquesJoint.remove(id);

			if (!cliqueIndiv.getMembers().keySet().equals(cliqueJoint.getMembers().keySet())) {
				throw new RuntimeException("inconsistent clique composition");
			}

			nMembers = cliqueIndiv.getMembers().size();

			if (nMembers <= maxCliqueSize) {
				avgNJointTrips = getNumberOfJointTrips(cliqueJoint) / ((double) nMembers);

				gain = ( cliqueJoint.getSelectedPlan().getScore() -
					cliqueIndiv.getSelectedPlan().getScore() )
					/ ((double) cliqueIndiv.getMembers().size());

				chart.add(avgNJointTrips, gain);
			}
		}

		formatXYGraph(chart);
		return chart;
	}

	private static ChartUtil createCliqueLevelRelativeChart(
			final ScenarioWithCliques scenarioIndividual,
			final ScenarioWithCliques scenarioJoint,
			final int maxCliqueSize) {
		BoxAndWhiskersChart chart = new BoxAndWhiskersChart(
				(maxCliqueSize == Integer.MAX_VALUE ?
					 TITLE_CLIQUE :
					 TITLE_CLIQUE+", cliques of less than "+maxCliqueSize+" members"),
				X_CLIQUE,
				Y_REL,
				BIN_WIDTH_CLIQUE);

		double gain;
		double avgNJointTrips;
		int nMembers;
		Clique cliqueIndiv;
		Clique cliqueJoint;

		Map<Id, Clique> cliquesIndiv = new HashMap<Id, Clique>(scenarioIndividual.getCliques().getCliques());
		Map<Id, Clique> cliquesJoint = new HashMap<Id, Clique>(scenarioJoint.getCliques().getCliques());
		List<Id> cliqueIds = new ArrayList<Id>(cliquesIndiv.keySet());

		for (Id id : cliqueIds) {
			cliqueIndiv = cliquesIndiv.remove(id);
			cliqueJoint = cliquesJoint.remove(id);

			if (!cliqueIndiv.getMembers().keySet().equals(cliqueJoint.getMembers().keySet())) {
				throw new RuntimeException("inconsistent clique composition");
			}

			nMembers = cliqueIndiv.getMembers().size();

			if (nMembers <= maxCliqueSize) {
				avgNJointTrips = getNumberOfJointTrips(cliqueJoint) / ((double) nMembers);

				gain = ( cliqueJoint.getSelectedPlan().getScore() -
					cliqueIndiv.getSelectedPlan().getScore() ) * 100d
					/ cliqueIndiv.getSelectedPlan().getScore();

				chart.add(avgNJointTrips, gain);
			}
		}

		formatXYGraph(chart);
		return chart;
	}

	private static ChartUtil createIndividualLevelChart(
			final Scenario scenarioIndividual,
			final Scenario scenarioJoint) {
		BoxAndWhiskersChart chart = new BoxAndWhiskersChart(
				TITLE_INDIVIDUAL,
				X_INDIVIDUAL,
				Y_INDIVIDUAL,
				BIN_WIDTH_INDIVIDUAL);

		double gain;
		Person personIndiv;
		Person personJoint;

		Map<Id, Person> personsIndiv = new HashMap<Id, Person>(scenarioIndividual.getPopulation().getPersons());
		Map<Id, Person> personsJoint = new HashMap<Id, Person>(scenarioJoint.getPopulation().getPersons());
		List<Id> personsIds = new ArrayList<Id>(personsIndiv.keySet());

		for (Id id : personsIds) {
			personIndiv = personsIndiv.remove(id);
			personJoint = personsJoint.remove(id);

			//if (nMembers <= MAX_CLIQUE_SIZE) {

				gain = ( personJoint.getSelectedPlan().getScore() -
					personIndiv.getSelectedPlan().getScore() );

				chart.add(getNumberOfJointTrips(personJoint) - 0.5, gain);
			//}
		}

		formatXYGraph(chart);
		XYChartUtils.integerXAxis(chart.getChart());
		return chart;
	}

	private static ChartUtil createIndividualLevelRelativeChart(
			final Scenario scenarioIndividual,
			final Scenario scenarioJoint) {
		BoxAndWhiskersChart chart = new BoxAndWhiskersChart(
				TITLE_INDIVIDUAL,
				X_INDIVIDUAL,
				Y_REL,
				BIN_WIDTH_INDIVIDUAL);

		double gain;
		Person personIndiv;
		Person personJoint;

		Map<Id, Person> personsIndiv = new HashMap<Id, Person>(scenarioIndividual.getPopulation().getPersons());
		Map<Id, Person> personsJoint = new HashMap<Id, Person>(scenarioJoint.getPopulation().getPersons());
		List<Id> personsIds = new ArrayList<Id>(personsIndiv.keySet());

		for (Id id : personsIds) {
			personIndiv = personsIndiv.remove(id);
			personJoint = personsJoint.remove(id);

			//if (nMembers <= MAX_CLIQUE_SIZE) {

				gain = ( personJoint.getSelectedPlan().getScore() -
					personIndiv.getSelectedPlan().getScore() ) * 100d
					/ personIndiv.getSelectedPlan().getScore();

				chart.add(getNumberOfJointTrips(personJoint) - 0.5, gain);
			//}
		}

		formatXYGraph(chart);
		XYChartUtils.integerXAxis(chart.getChart());
		return chart;
	}

	private static ChartUtil createIndividualLevelDriverOnlyChart(
			final Scenario scenarioIndividual,
			final Scenario scenarioJoint) {
		BoxAndWhiskersChart chart = new BoxAndWhiskersChart(
				TITLE_INDIVIDUAL+", driver agents",
				X_INDIVIDUAL,
				Y_INDIVIDUAL,
				BIN_WIDTH_INDIVIDUAL);

		double gain;
		Person personIndiv;
		Person personJoint;

		Map<Id, Person> personsIndiv = new HashMap<Id, Person>(scenarioIndividual.getPopulation().getPersons());
		Map<Id, Person> personsJoint = new HashMap<Id, Person>(scenarioJoint.getPopulation().getPersons());
		List<Id> personsIds = new ArrayList<Id>(personsIndiv.keySet());

		for (Id id : personsIds) {
			personIndiv = personsIndiv.remove(id);
			personJoint = personsJoint.remove(id);

			if (isDriverOnly(personJoint)) {
				gain = ( personJoint.getSelectedPlan().getScore() -
					personIndiv.getSelectedPlan().getScore() );

				chart.add(getNumberOfJointTrips(personJoint) - 0.5, gain);
			}
		}

		formatXYGraph(chart);
		XYChartUtils.integerXAxis(chart.getChart());
		return chart;
	}

	private static ChartUtil createIndividualLevelDriverOnlyRelativeChart(
			final Scenario scenarioIndividual,
			final Scenario scenarioJoint) {
		BoxAndWhiskersChart chart = new BoxAndWhiskersChart(
				TITLE_INDIVIDUAL+", driver agents",
				X_INDIVIDUAL,
				Y_REL,
				BIN_WIDTH_INDIVIDUAL);

		double gain;
		Person personIndiv;
		Person personJoint;

		Map<Id, Person> personsIndiv = new HashMap<Id, Person>(scenarioIndividual.getPopulation().getPersons());
		Map<Id, Person> personsJoint = new HashMap<Id, Person>(scenarioJoint.getPopulation().getPersons());
		List<Id> personsIds = new ArrayList<Id>(personsIndiv.keySet());

		for (Id id : personsIds) {
			personIndiv = personsIndiv.remove(id);
			personJoint = personsJoint.remove(id);

			if (isDriverOnly(personJoint)) {
				gain = ( personJoint.getSelectedPlan().getScore() -
					personIndiv.getSelectedPlan().getScore() ) * 100d
					/ personIndiv.getSelectedPlan().getScore();

				chart.add(getNumberOfJointTrips(personJoint) - 0.5, gain);
			}
		}

		formatXYGraph(chart);
		XYChartUtils.integerXAxis(chart.getChart());
		return chart;
	}


	private static ChartUtil createIndividualLevelPassengerOnlyChart(
			final Scenario scenarioIndividual,
			final Scenario scenarioJoint) {
		BoxAndWhiskersChart chart = new BoxAndWhiskersChart(
				TITLE_INDIVIDUAL+", passenger agents",
				X_INDIVIDUAL,
				Y_INDIVIDUAL,
				BIN_WIDTH_INDIVIDUAL);

		double gain;
		Person personIndiv;
		Person personJoint;

		Map<Id, Person> personsIndiv = new HashMap<Id, Person>(scenarioIndividual.getPopulation().getPersons());
		Map<Id, Person> personsJoint = new HashMap<Id, Person>(scenarioJoint.getPopulation().getPersons());
		List<Id> personsIds = new ArrayList<Id>(personsIndiv.keySet());

		for (Id id : personsIds) {
			personIndiv = personsIndiv.remove(id);
			personJoint = personsJoint.remove(id);

			if (isPassengerOnly(personJoint)) {
				gain = ( personJoint.getSelectedPlan().getScore() -
					personIndiv.getSelectedPlan().getScore() );

				chart.add(getNumberOfJointTrips(personJoint) - 0.5, gain);
			}
		}

		formatXYGraph(chart);
		XYChartUtils.integerXAxis(chart.getChart());
		return chart;
	}

	private static ChartUtil createIndividualLevelPassengerOnlyRelativeChart(
			final Scenario scenarioIndividual,
			final Scenario scenarioJoint) {
		BoxAndWhiskersChart chart = new BoxAndWhiskersChart(
				TITLE_INDIVIDUAL+", passenger agents",
				X_INDIVIDUAL,
				Y_REL,
				BIN_WIDTH_INDIVIDUAL);

		double gain;
		Person personIndiv;
		Person personJoint;

		Map<Id, Person> personsIndiv = new HashMap<Id, Person>(scenarioIndividual.getPopulation().getPersons());
		Map<Id, Person> personsJoint = new HashMap<Id, Person>(scenarioJoint.getPopulation().getPersons());
		List<Id> personsIds = new ArrayList<Id>(personsIndiv.keySet());

		for (Id id : personsIds) {
			personIndiv = personsIndiv.remove(id);
			personJoint = personsJoint.remove(id);

			if (isPassengerOnly(personJoint)) {
				gain = ( personJoint.getSelectedPlan().getScore() -
					personIndiv.getSelectedPlan().getScore() ) * 100d
					/ personIndiv.getSelectedPlan().getScore();

				chart.add(getNumberOfJointTrips(personJoint) - 0.5, gain);
			}
		}

		formatXYGraph(chart);
		XYChartUtils.integerXAxis(chart.getChart());
		return chart;
	}


	private static ChartUtil createPerCliqueSizeChart(
			final ScenarioWithCliques scenarioIndividual,
			final ScenarioWithCliques scenarioJoint,
			final int maxCliqueSize) {
		BoxAndWhiskersChart chart = new BoxAndWhiskersChart(
				"utility gain per clique size",
				"clique size",
				"gain",
				1);

		double gain;
		double scoreIndiv;
		double scoreJoint;
		int nMembers;
		Clique cliqueIndiv;
		Clique cliqueJoint;

		Map<Id, Clique> cliquesIndiv = new HashMap<Id, Clique>(scenarioIndividual.getCliques().getCliques());
		Map<Id, Clique> cliquesJoint = new HashMap<Id, Clique>(scenarioJoint.getCliques().getCliques());
		List<Id> cliqueIds = new ArrayList<Id>(cliquesIndiv.keySet());

		for (Id id : cliqueIds) {
			cliqueIndiv = cliquesIndiv.remove(id);
			cliqueJoint = cliquesJoint.remove(id);

			if (!cliqueIndiv.getMembers().keySet().equals(cliqueJoint.getMembers().keySet())) {
				throw new RuntimeException("inconsistent clique composition");
			}

			nMembers = cliqueIndiv.getMembers().size();

			if (nMembers <= maxCliqueSize) {
				for (Id idIndiv : cliqueIndiv.getMembers().keySet()) {
					scoreIndiv = cliqueIndiv.getMembers().get(idIndiv).getSelectedPlan().getScore();
					scoreJoint = cliqueJoint.getMembers().get(idIndiv).getSelectedPlan().getScore();
					gain = ( scoreJoint - scoreIndiv );

					chart.add(nMembers - 0.5, gain);
				}
			}
		}

		formatXYGraph(chart);
		return chart;
	}

	private static ChartUtil createPerCliqueSizeRelativeChart(
			final ScenarioWithCliques scenarioIndividual,
			final ScenarioWithCliques scenarioJoint,
			final int maxCliqueSize) {
		BoxAndWhiskersChart chart = new BoxAndWhiskersChart(
				"utility gain per clique size",
				"clique size",
				Y_REL,
				1);

		double gain;
		double scoreIndiv;
		double scoreJoint;
		int nMembers;
		Clique cliqueIndiv;
		Clique cliqueJoint;

		Map<Id, Clique> cliquesIndiv = new HashMap<Id, Clique>(scenarioIndividual.getCliques().getCliques());
		Map<Id, Clique> cliquesJoint = new HashMap<Id, Clique>(scenarioJoint.getCliques().getCliques());
		List<Id> cliqueIds = new ArrayList<Id>(cliquesIndiv.keySet());

		for (Id id : cliqueIds) {
			cliqueIndiv = cliquesIndiv.remove(id);
			cliqueJoint = cliquesJoint.remove(id);

			if (!cliqueIndiv.getMembers().keySet().equals(cliqueJoint.getMembers().keySet())) {
				throw new RuntimeException("inconsistent clique composition");
			}

			nMembers = cliqueIndiv.getMembers().size();

			if (nMembers <= maxCliqueSize) {
				for (Id idIndiv : cliqueIndiv.getMembers().keySet()) {
					scoreIndiv = cliqueIndiv.getMembers().get(idIndiv).getSelectedPlan().getScore();
					scoreJoint = cliqueJoint.getMembers().get(idIndiv).getSelectedPlan().getScore();
					gain = ( scoreJoint - scoreIndiv ) * 100 / scoreIndiv;

					chart.add(nMembers - 0.5, gain);
				}
			}
		}

		formatXYGraph(chart);
		return chart;
	}

	// ////////////////////////////////////////////////////////////////////////
	// helpers
	// ////////////////////////////////////////////////////////////////////////
	private static double getNumberOfJointTrips(final Person person) {
		double count = 0;

		for (PlanElement pe : person.getSelectedPlan().getPlanElements()) {
			if ((pe instanceof Activity) &&
					(JointActingTypes.DROP_OFF.equals(((Activity) pe).getType()))) {
				count += 1;
			}
		}

		return count;
	}

	private static void formatXYGraph(final ChartUtil chart) {
		//chart.getChart().getXYPlot().setRangeZeroBaselinePaint(Color.black);
		chart.getChart().getXYPlot().setRangeZeroBaselineVisible(true);
		chart.getChart().getXYPlot().getRangeAxis().setAutoRange(true);
	}

	/**
	 * @return true iif the person has driver trips and no passenger trips in its
	 * plan
	 */
	private static boolean isDriverOnly(final Person person) {
		boolean out = false;
		List<PlanElement> pes = person.getSelectedPlan().getPlanElements();
		Activity act;

		for (int i=2; i < pes.size(); i+=2) {
			act = (Activity) pes.get(i);
			if (JointActingTypes.DROP_OFF.equals(act.getType())) {
				if (((Leg) pes.get(i-1)).getMode().equals(JointActingTypes.PASSENGER)) {
					return false;
				}
				out = true;
			}
		}

		return out;
	}

	/**
	 * @return true iif the person has passenger trips and no driver trips in its
	 * plan
	 */
	private static boolean isPassengerOnly(final Person person) {
		boolean out = false;
		List<PlanElement> pes = person.getSelectedPlan().getPlanElements();
		Activity act;

		for (int i=2; i < pes.size(); i+=2) {
			act = (Activity) pes.get(i);
			if (JointActingTypes.DROP_OFF.equals(act.getType())) {
				if (!((Leg) pes.get(i-1)).getMode().equals(JointActingTypes.PASSENGER)) {
					return false;
				}
				out = true;
			}
		}

		return out;
	}
}

