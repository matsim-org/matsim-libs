package playground.michalm.taxi.run;

import java.io.PrintWriter;
import java.util.Map;

import org.matsim.analysis.LegHistogram;
import org.matsim.contrib.dvrp.run.VrpLauncherUtils;
import org.matsim.contrib.dvrp.util.Schedules2GIS;
import org.matsim.contrib.dynagent.run.DynAgentLauncherUtils;
import org.matsim.contrib.util.chart.ChartWindowUtils;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.algorithms.*;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.vis.otfvis.OTFVisConfigGroup.ColoringScheme;

import playground.michalm.taxi.util.chart.TaxiScheduleChartUtils;
import playground.michalm.taxi.util.stats.TaxiStatsCalculator;
import playground.michalm.taxi.util.stats.TaxiStatsCalculator.TaxiStats;
import playground.michalm.util.*;


class SingleRunTaxiLauncher
    extends TaxiLauncher
{
    private LegHistogram legHistogram;
    private EventWriter eventWriter;
    private MovingAgentsRegister movingAgents;


    SingleRunTaxiLauncher(TaxiLauncherParams params)
    {
        super(params);
    }


    @Override
    public void beforeQSim(QSim qSim)
    {
        EventsManager events = qSim.getEventsManager();

        if (params.eventsOutFile != null) {
            eventWriter = new EventWriterXML(params.eventsOutFile);
            events.addHandler(eventWriter);
        }

        movingAgents = new MovingAgentsRegister();
        events.addHandler(movingAgents);

        if (params.otfVis) { // OFTVis visualization
            DynAgentLauncherUtils.runOTFVis(qSim, false, ColoringScheme.taxicab);
        }

        if (params.histogramOutDir != null) {
            events.addHandler(legHistogram = new LegHistogram(300));
        }
    }


    @Override
    public void afterQSim(QSim qSim)
    {
        if (params.eventsOutFile != null) {
            eventWriter.closeFile();
        }
    }


    void run()
    {
        initTravelTimeAndDisutility();
        simulateIteration();
        generateOutput();
    }


    void generateOutput()
    {
        PrintWriter pw = new PrintWriter(System.out);
        pw.println(params.algorithmConfig.name());
        pw.println("m\t" + context.getVrpData().getVehicles().size());
        pw.println("n\t" + context.getVrpData().getRequests().size());
        pw.println(TaxiStats.HEADER);
        TaxiStats stats = new TaxiStatsCalculator(context.getVrpData().getVehicles().values())
                .getStats();
        pw.println(stats);
        pw.flush();

        if (params.vrpOutDir != null) {
            new Schedules2GIS(context.getVrpData().getVehicles().values(),
                    TransformationFactory.WGS84_UTM33N).write(params.vrpOutDir);
        }

        // ChartUtils.showFrame(RouteChartUtils.chartRoutesByStatus(data.getVrpData()));
        ChartWindowUtils.showFrame(
                TaxiScheduleChartUtils.chartSchedule(context.getVrpData().getVehicles().values()));

        if (params.histogramOutDir != null) {
            VrpLauncherUtils.writeHistograms(legHistogram, params.histogramOutDir);
        }
    }


    static void run(TaxiLauncherParams params)
    {
        SingleRunTaxiLauncher launcher = new SingleRunTaxiLauncher(params);
        launcher.run();
    }


    public static void main(String... args)
    {
        Map<String, String> params = ParameterFileReader.readParametersToMap(args[0]);
        SingleRunTaxiLauncher.run(new TaxiLauncherParams(params));
    }
}
