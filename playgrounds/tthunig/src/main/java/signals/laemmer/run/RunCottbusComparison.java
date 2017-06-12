package signals.laemmer.run;

import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.signals.SignalSystemsConfigGroup;
import org.matsim.contrib.signals.data.SignalsData;
import org.matsim.contrib.signals.data.SignalsDataLoader;
import org.matsim.contrib.signals.otfvis.OTFVisWithSignals;
import org.matsim.contrib.signals.otfvis.OTFVisWithSignalsLiveModule;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.ConfigWriter;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.vis.otfvis.OTFVisConfigGroup;
import playground.dgrether.koehlerstrehlersignal.analysis.TtTotalDelay;
import signals.CombinedSignalsModule;
import signals.laemmer.model.LaemmerConfig;

/**
 * Created by Nico on 30.05.2017.
 */
public class RunCottbusComparison {

    private static boolean vis = true;

//    private static final String OUTPUT_DIR = "C:/Users/Nico/Dropbox/MA-Arbeit/Ergebnisse/Cottbus/OutputFixedLongLanes";
//    private static final String OUTPUT_DIR = "C:/Users/Nico/Dropbox/MA-Arbeit/Ergebnisse/Cottbus/OutputFixed";
//    private static final String OUTPUT_DIR = "C:/Users/Nico/Dropbox/MA-Arbeit/Ergebnisse/Cottbus/OutputLongLanes";
private static final String OUTPUT_DIR = "C:/Users/Nico/Dropbox/MA-Arbeit/Ergebnisse/Cottbus/OutputLongLanesT90M5";
//private static final String OUTPUT_DIR = "C:/Users/Nico/Dropbox/MA-Arbeit/Ergebnisse/Cottbus/OutputLongLanesNoSignals";


    private static final String NETWORK_PATH = "C:/Users/Nico/Dropbox/MA-Arbeit/Ergebnisse/Cottbus/Input/network.xml.gz";
    private static final String LANES_PATH = "C:/Users/Nico/Dropbox/MA-Arbeit/Ergebnisse/Cottbus/Input/lanes_long.xml";
    private static final String PLANS_PATH = "C:/Users/Nico/Dropbox/MA-Arbeit/Ergebnisse/Cottbus/Input/plans.xml.gz";
//        private static final String SIGNAL_GROUPS_PATH = "C:/Users/Nico/Dropbox/MA-Arbeit/Ergebnisse/Cottbus/Input/signal_groups.xml";
    private static final String SIGNAL_GROUPS_PATH = "C:/Users/Nico/Dropbox/MA-Arbeit/Ergebnisse/Cottbus/Input/signal_groups_laemmer.xml";
    private static final String SIGNAL_SYSTEMS_PATH = "C:/Users/Nico/Dropbox/MA-Arbeit/Ergebnisse/Cottbus/Input/signal_systems.xml";
//    private static final String SIGNAL_CONTROL_PATH = "C:/Users/Nico/Dropbox/MA-Arbeit/Ergebnisse/Cottbus/Input/signal_control.xml";
    private static final String SIGNAL_CONTROL_PATH = "C:/Users/Nico/Dropbox/MA-Arbeit/Ergebnisse/Cottbus/Input/signal_control_laemmer.xml";


    public static void main(String[] args) {

        Config config = ConfigUtils.createConfig();


        config.controler().setLastIteration(0);
        config.controler().setLinkToLinkRoutingEnabled(true);
        config.travelTimeCalculator().setCalculateLinkToLinkTravelTimes(true);
        config.controler().setOutputDirectory(OUTPUT_DIR);
        config.controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);
        config.controler().setWriteEventsInterval(config.controler().getLastIteration());
        config.controler().setWritePlansInterval(config.controler().getLastIteration());
        config.controler().setCreateGraphs(true);
        config.controler().setWriteSnapshotsInterval(1);
        config.controler().setWriteEventsInterval(1);

        config.network().setInputFile(NETWORK_PATH);
        config.network().setLaneDefinitionsFile(LANES_PATH);

        SignalSystemsConfigGroup signalConfigGroup = ConfigUtils.addOrGetModule(config, SignalSystemsConfigGroup.GROUPNAME, SignalSystemsConfigGroup.class);
        signalConfigGroup.setUseSignalSystems(true);
        signalConfigGroup.setSignalSystemFile(SIGNAL_SYSTEMS_PATH);
        signalConfigGroup.setSignalControlFile(SIGNAL_CONTROL_PATH);
        signalConfigGroup.setSignalGroupsFile(SIGNAL_GROUPS_PATH);
        config.global().setCoordinateSystem(TransformationFactory.WGS84_UTM33N);
        if(vis) {
            config.qsim().setSnapshotStyle(QSimConfigGroup.SnapshotStyle.withHoles);
//        config.qsim().setNodeOffset(5.);
            OTFVisConfigGroup otfvisConfig =
                    ConfigUtils.addOrGetModule(config, OTFVisConfigGroup.GROUP_NAME, OTFVisConfigGroup.class);
            otfvisConfig.setScaleQuadTreeRect(true);
            otfvisConfig.setMapOverlayMode(true);
        }


        config.vspExperimental().setWritingOutputEvents(true);

        config.qsim().setUsingFastCapacityUpdate(false);
        config.qsim().setUseLanes(true);
//        config.qsim().setStuckTime(100);
        config.qsim().setEndTime(24 * 3600);
//        Factor does not work for Laemmer
//        config.qsim().setFlowCapFactor(0.7);
//        config.qsim().setStorageCapFactor(0.7);

        config.planCalcScore().setLateArrival_utils_hr(-18);
        config.planCalcScore().setEarlyDeparture_utils_hr(0);
        config.planCalcScore().setPerforming_utils_hr(6);
        config.planCalcScore().setWriteExperiencedPlans(true);
        PlanCalcScoreConfigGroup.ModeParams carParams = new PlanCalcScoreConfigGroup.ModeParams("car");
        carParams.setMarginalUtilityOfTraveling(-6);
        config.planCalcScore().addModeParams(carParams);
        config.planCalcScore().setMarginalUtlOfWaiting_utils_hr(0);
        PlanCalcScoreConfigGroup.ActivityParams work = new PlanCalcScoreConfigGroup.ActivityParams("work");
        work.setTypicalDuration(8 * 3600);
        config.planCalcScore().addActivityParams(work);
        PlanCalcScoreConfigGroup.ActivityParams home = new PlanCalcScoreConfigGroup.ActivityParams("home");
        home.setTypicalDuration(16 * 3600);
        config.planCalcScore().addActivityParams(home);
        PlanCalcScoreConfigGroup.ActivityParams fb = new PlanCalcScoreConfigGroup.ActivityParams("fb");
        work.setTypicalDuration(2 * 3600);
        config.planCalcScore().addActivityParams(work);

        config.plans().setInputFile(PLANS_PATH);

        Scenario scenario = ScenarioUtils.loadScenario(config);
        scenario.addScenarioElement(SignalsData.ELEMENT_NAME, new SignalsDataLoader(config).loadSignalsData());
//        OTFVisWithSignals.playScenario(scenario);



        Controler controler = new Controler(scenario);

        CombinedSignalsModule signalsModule = new CombinedSignalsModule();
        LaemmerConfig laemmerConfig = new LaemmerConfig();
        laemmerConfig.setDESIRED_PERIOD(90);
        laemmerConfig.setMAX_PERIOD(135);
        laemmerConfig.setMinG(5);
        signalsModule.setLaemmerConfig(laemmerConfig);
        controler.addOverridingModule(signalsModule);
        if(vis) {
            controler.addOverridingModule(new OTFVisWithSignalsLiveModule());
        }
        TtTotalDelay ttTotalDelay = new TtTotalDelay(scenario.getNetwork(), controler.getEvents());
//        ConfigWriter configWriter = new ConfigWriter(config);
//        configWriter.write(OUTPUT_DIR + "/config.xml");
        controler.run();
        System.out.println(ttTotalDelay.getTotalDelay());
    }
}
