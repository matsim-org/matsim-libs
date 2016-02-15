package playground.michalm.taxi.run;

import java.io.PrintWriter;

import org.apache.commons.configuration.Configuration;
import org.matsim.contrib.dynagent.run.DynAgentLauncherUtils;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.algorithms.*;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.vis.otfvis.OTFVisConfigGroup.ColoringScheme;

import playground.michalm.taxi.optimizer.AbstractTaxiOptimizerParams;
import playground.michalm.taxi.util.stats.*;
import playground.michalm.util.MovingAgentsRegister;


class SingleRunTaxiLauncher
    extends TaxiLauncher
{
    private EventWriter eventWriter;
    private MovingAgentsRegister movingAgents;


    SingleRunTaxiLauncher(Configuration config)
    {
        super(config);
        optimParams = AbstractTaxiOptimizerParams
                .createParams(TaxiConfigUtils.getOptimizerConfig(config));
    }


    @Override
    public void beforeQSim(QSim qSim)
    {
        EventsManager events = qSim.getEventsManager();

        if (launcherParams.eventsOutFile != null) {
            eventWriter = new EventWriterXML(launcherParams.eventsOutFile);
            events.addHandler(eventWriter);
        }

        if (launcherParams.debugMode) {
            movingAgents = new MovingAgentsRegister();
            events.addHandler(movingAgents);
        }

        if (launcherParams.otfVis) { // OFTVis visualization
            DynAgentLauncherUtils.runOTFVis(qSim, false, ColoringScheme.taxicab);
        }
    }


    @Override
    public void afterQSim(QSim qSim)
    {
        if (launcherParams.eventsOutFile != null) {
            eventWriter.closeFile();
        }
    }


    void run()
    {
        initTravelTimeAndDisutility();
        simulateIteration("");
        generateOutput();
    }


    void generateOutput()
    {
        PrintWriter pw = new PrintWriter(System.out);
        //        pw.println(params.algorithmConfig.name());
        pw.println("m\t" + context.getVrpData().getVehicles().size());
        pw.println("n\t" + context.getVrpData().getRequests().size());
        pw.println(TaxiStats.HEADER);
        TaxiStats stats = new TaxiStatsCalculator(context.getVrpData().getVehicles().values())
                .getStats();
        pw.println(stats);
        pw.flush();
    }


    static void run(Configuration config)
    {
        SingleRunTaxiLauncher launcher = new SingleRunTaxiLauncher(config);
        launcher.run();
    }


    public static void main(String... args)
    {
        SingleRunTaxiLauncher.run(TaxiConfigUtils.loadConfig(args[0]));
    }
}
