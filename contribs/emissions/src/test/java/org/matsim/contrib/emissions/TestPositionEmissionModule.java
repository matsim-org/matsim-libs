package org.matsim.contrib.emissions;

import org.junit.Ignore;
import org.junit.Test;
import org.matsim.contrib.emissions.utils.EmissionsConfigGroup;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.ControlerConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.scenario.ScenarioUtils;

import java.util.Set;

public class TestPositionEmissionModule {

    private static final String configFile = "./scenarios/sampleScenario/testv2_Vehv1/config_detailed.xml";

    @Test
    @Ignore
    public void simpleTest() {

        var emissionConfig = new EmissionsConfigGroup();
        emissionConfig.setHbefaVehicleDescriptionSource(EmissionsConfigGroup.HbefaVehicleDescriptionSource.fromVehicleTypeDescription);
        emissionConfig.setDetailedVsAverageLookupBehavior(
                EmissionsConfigGroup.DetailedVsAverageLookupBehavior.tryDetailedThenTechnologyAverageThenAverageTable); //This is the previous behaviour
        var config = ConfigUtils.loadConfig(configFile, emissionConfig);
        config.controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);
        config.qsim().setSnapshotPeriod(1);
        config.controler().setWriteSnapshotsInterval(1);
        config.controler().setSnapshotFormat(Set.of(ControlerConfigGroup.SnapshotFormat.positionevents));

        var scenario = ScenarioUtils.loadScenario(config);

        var controler = new Controler(scenario);

        controler.addOverridingModule(new PositionEmissionsModule());
        controler.run();
    }
}
