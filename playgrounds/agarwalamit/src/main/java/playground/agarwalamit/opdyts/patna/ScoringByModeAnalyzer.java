/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2017 by the members listed in the COPYING,        *
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

package playground.agarwalamit.opdyts.patna;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import playground.agarwalamit.utils.FileUtils;
import playground.agarwalamit.utils.LoadMyScenarios;

/**
 * Created by amit on 06.06.17.
 */

public class ScoringByModeAnalyzer {
    private static final String outputPlans = FileUtils.RUNS_SVN+"/opdyts/patna/output_networkModes/initialPlans2RelaxedPlans/ITERS/it.1/1.plans.xml.gz"; // probably use experienced plans
    private static final String outputConfig = FileUtils.RUNS_SVN+"/opdyts/patna/output_networkModes/initialPlans2RelaxedPlans/output_config.xml.gz";

    public static void main(String[] args) {
        Map<String, Double> mode2scores = new HashMap<>();
        Map<String, Integer> mode2Counter = new HashMap<>();

        Scenario scenario = LoadMyScenarios.loadScenarioFromPlansAndConfig(outputPlans, outputConfig);
        PlanCalcScoreConfigGroup planCalcScoreConfigGroup = scenario.getConfig().planCalcScore();

        for (Person p : scenario.getPopulation().getPersons().values()) {
            Plan selectedPlan = p.getSelectedPlan();
            List<PlanElement> pes = selectedPlan.getPlanElements();
            double score = selectedPlan.getScore();
            String mode  = null;
            for (PlanElement pe : pes){
                if (pe instanceof  Leg) {
                    if (mode == null) mode = ((Leg)pe).getMode();
                    else if (! mode.equals(((Leg)pe).getMode())) throw new RuntimeException("Modes in all legs are not same.");
                    score += planCalcScoreConfigGroup.getModes().get(mode).getConstant();

                    if (mode2Counter.containsKey(mode)) mode2Counter.put(mode, mode2Counter.get(mode)+1);
                    else mode2Counter.put(mode, 1);
                }
            }

            if ( mode2scores.containsKey(mode) )  mode2scores.put(mode, mode2scores.get(mode)+score);
            else mode2scores.put(mode, score);
        }

        // take avg scores and then simply get the c_mode_0 for each mode
        Map<String, Double> mode2avgScore = mode2Counter.entrySet().parallelStream().collect(Collectors.toMap(
           e -> e.getKey(), e -> mode2scores.get(e.getKey())/e.getValue()
        ));

        mode2avgScore.entrySet().parallelStream().forEach(e -> System.out.println("mode :"+e.getKey() + "; avgScore: "+ e.getValue()));
        mode2Counter.entrySet().parallelStream().forEach(e -> System.out.println("mode :"+e.getKey() + "; legs: "+ e.getValue()));
        mode2avgScore.entrySet().parallelStream().forEach(e -> System.out.println("mode :"+e.getKey() + "; ASC: "+planCalcScoreConfigGroup.getModes().get(e.getKey()).getConstant()));
    }
}
