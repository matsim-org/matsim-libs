package org.matsim.application.automatedCalibration.modeChoiceEstimation;

import ch.sbb.matsim.routing.pt.raptor.SwissRailRaptorModule;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.config.groups.PlansCalcRouteConfigGroup;
import org.matsim.core.config.groups.VspExperimentalConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.scenario.ScenarioUtils;

import java.util.List;

public class PtPopulationRunner {
    public static void main(String[] args) {
        Config config = ConfigUtils.createConfig();
//        config.plans().setInputFile("/Users/luchengqi/Documents/MATSimScenarios/Kelheim/auto-calibration/trips-plans/pt-trips.plans.xml.gz");
        config.plans().setInputFile("/Users/luchengqi/Documents/MATSimScenarios/Kelheim/auto-calibration/trips-plans/car-trips.plans.xml.gz");
        config.network().setInputFile("/Users/luchengqi/Documents/MATSimScenarios/Kelheim/kelheim-v1.4-network-with-pt.xml.gz");
        config.global().setCoordinateSystem("EPSG:25832");

        config.qsim().setEndTime(129600);
        config.qsim().setFlowCapFactor(10000);
        config.qsim().setStorageCapFactor(10000);

        config.vehicles().setVehiclesFile("https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/duesseldorf/duesseldorf-v1.0/input/duesseldorf-v1.0-vehicle-types.xml");

        config.transit().setVehiclesFile("/Users/luchengqi/Documents/MATSimScenarios/Kelheim/auto-calibration/kelheim-v1.4-transitVehicles.xml.gz");
        config.transit().setTransitScheduleFile("/Users/luchengqi/Documents/MATSimScenarios/Kelheim/auto-calibration/kelheim-v1.4-transitSchedule.xml.gz");
//        config.transit().setUseTransit(true);
        config.transit().setUseTransit(false);

        config.controler().setLastIteration(0);
//        config.controler().setOutputDirectory("/Users/luchengqi/Documents/MATSimScenarios/Kelheim/auto-calibration/trips-plans/pt");
        config.controler().setOutputDirectory("/Users/luchengqi/Documents/MATSimScenarios/Kelheim/auto-calibration/mode-choice-estimation/car");
        config.controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);

        // for testing
        config.planCalcScore().setPerforming_utils_hr(0);
        config.planCalcScore().getModes().get(TransportMode.pt).setConstant(-1.0);
        config.planCalcScore().getModes().get(TransportMode.pt).setMarginalUtilityOfTraveling(-1.0);
//        config.planCalcScore().getModes().get(TransportMode.walk).setMarginalUtilityOfTraveling(-0.0); //TODO if this line is enabled, then there won't be any PT trips!!! A potential bug?

        config.plansCalcRoute().setTeleportedModeSpeed(TransportMode.walk, 1.0555556);

        config.planCalcScore().setWriteExperiencedPlans(true);

        for (long ii = 600; ii <= 97200; ii += 600) {

            for (String act : List.of("home", "restaurant", "other", "visit", "errands", "accomp_other", "accomp_children",
                    "educ_higher", "educ_secondary", "educ_primary", "educ_tertiary", "educ_kiga", "educ_other")) {
                config.planCalcScore()
                        .addActivityParams(new PlanCalcScoreConfigGroup.ActivityParams(act + "_" + ii).setTypicalDuration(ii));
            }

            config.planCalcScore().addActivityParams(new PlanCalcScoreConfigGroup.ActivityParams("work_" + ii).setTypicalDuration(ii)
                    .setOpeningTime(6. * 3600.).setClosingTime(20. * 3600.));
            config.planCalcScore().addActivityParams(new PlanCalcScoreConfigGroup.ActivityParams("business_" + ii).setTypicalDuration(ii)
                    .setOpeningTime(6. * 3600.).setClosingTime(20. * 3600.));
            config.planCalcScore().addActivityParams(new PlanCalcScoreConfigGroup.ActivityParams("leisure_" + ii).setTypicalDuration(ii)
                    .setOpeningTime(9. * 3600.).setClosingTime(27. * 3600.));

            config.planCalcScore().addActivityParams(new PlanCalcScoreConfigGroup.ActivityParams("shop_daily_" + ii).setTypicalDuration(ii)
                    .setOpeningTime(8. * 3600.).setClosingTime(20. * 3600.));
            config.planCalcScore().addActivityParams(new PlanCalcScoreConfigGroup.ActivityParams("shop_other_" + ii).setTypicalDuration(ii)
                    .setOpeningTime(8. * 3600.).setClosingTime(20. * 3600.));
        }

        config.vspExperimental().setVspDefaultsCheckingLevel(VspExperimentalConfigGroup.VspDefaultsCheckingLevel.info);
        config.plansCalcRoute().setAccessEgressType(PlansCalcRouteConfigGroup.AccessEgressType.accessEgressModeToLink);

        Scenario scenario = ScenarioUtils.loadScenario(config);
        Controler controler = new Controler(scenario);

        controler.addOverridingModule(new AbstractModule() {
            @Override
            public void install() {
                install(new SwissRailRaptorModule());
            }
        });

        controler.run();

    }
}
