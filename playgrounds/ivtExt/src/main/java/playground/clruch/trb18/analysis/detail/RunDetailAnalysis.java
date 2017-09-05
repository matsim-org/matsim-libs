package playground.clruch.trb18.analysis.detail;

import java.io.File;
import java.io.IOException;

import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.router.StageActivityTypesImpl;
import org.matsim.pt.PtConstants;

import playground.clruch.trb18.analysis.detail.events.ActivityCollector;
import playground.clruch.trb18.analysis.detail.events.LegCollector;
import playground.clruch.trb18.analysis.detail.events.PairCollector;
import playground.clruch.trb18.analysis.detail.events.TripCollector;

public class RunDetailAnalysis {
    static public void main(String[] args) throws IOException, InterruptedException {
        String networkInputPath = args[0];
        String referenceEventsPath = args[1];
        String scenarioEventsPath = args[2];
        String outputPath = args[3];

        Network network = NetworkUtils.createNetwork();
        new MatsimNetworkReader(network).readFile(networkInputPath);

        EventsManager referenceEventsManager = EventsUtils.createEventsManager();
        EventsManager scenarioEventsManager = EventsUtils.createEventsManager();




        LegCollector referenceLegCollector = new LegCollector(network);
        referenceEventsManager.addHandler(referenceLegCollector);

        ActivityCollector referenceActivityCollector = new ActivityCollector();
        referenceEventsManager.addHandler(referenceActivityCollector);

        TripCollector referenceTripCollector = new TripCollector(new StageActivityTypesImpl(PtConstants.TRANSIT_ACTIVITY_TYPE));
        referenceLegCollector.addHandler(referenceTripCollector);
        referenceActivityCollector.addHandler(referenceTripCollector);



        LegCollector scenarioLegCollector = new LegCollector(network);
        scenarioEventsManager.addHandler(scenarioLegCollector);

        ActivityCollector scenarioActivityCollector = new ActivityCollector();
        scenarioEventsManager.addHandler(scenarioActivityCollector);

        TripCollector scenarioTripCollector = new TripCollector(new StageActivityTypesImpl(PtConstants.TRANSIT_ACTIVITY_TYPE));
        scenarioLegCollector.addHandler(scenarioTripCollector);
        scenarioActivityCollector.addHandler(scenarioTripCollector);






        PairCollector pairCollector = new PairCollector();
        referenceTripCollector.addHandler(pairCollector.referenceHandler);
        scenarioTripCollector.addHandler(pairCollector.scenarioHandler);

        AnalysisWriter writer = new AnalysisWriter(new File(outputPath), network);
        pairCollector.addHandler(writer);

        Thread referenceThread = new Thread(new Runnable() {
            @Override
            public void run() {
                new MatsimEventsReader(referenceEventsManager).readFile(referenceEventsPath);
                referenceActivityCollector.finish();
            }
        });

        Thread scenarioThread = new Thread(new Runnable() {
            @Override
            public void run() {
                new MatsimEventsReader(scenarioEventsManager).readFile(scenarioEventsPath);
                scenarioActivityCollector.finish();
            }
        });

        referenceThread.start();
        scenarioThread.start();

        referenceThread.join();
        scenarioThread.join();
    }

    static TripCollector createTripCollector(EventsManager eventsManager, Network network) {
        LegCollector legCollector = new LegCollector(network);
        eventsManager.addHandler(legCollector);

        ActivityCollector activityCollector = new ActivityCollector();
        eventsManager.addHandler(activityCollector);

        TripCollector tripCollector = new TripCollector(new StageActivityTypesImpl(PtConstants.TRANSIT_ACTIVITY_TYPE));
        legCollector.addHandler(tripCollector);
        activityCollector.addHandler(tripCollector);

        return tripCollector;
    }
}
