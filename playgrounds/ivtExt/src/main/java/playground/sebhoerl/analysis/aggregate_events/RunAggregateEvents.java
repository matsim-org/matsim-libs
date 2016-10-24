package playground.sebhoerl.analysis.aggregate_events;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.zip.GZIPOutputStream;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.scoring.EventsToActivities;
import org.matsim.core.scoring.EventsToLegs;
import org.matsim.pt.transitSchedule.TransitScheduleReaderV1;
import org.matsim.vehicles.VehicleReaderV1;

public class RunAggregateEvents {    
    public static void main(String[] args) throws IOException {
        String configPath = args[0];
        String networkPath = args[1];
        String transitSchedulePath = args[2];
        String transitVehiclesPath = args[3];
        String eventsPath = args[4];
        String scoreStatsPath = args[5];
        String outputPath = args[6];
        
        OutputStreamWriter writer = new OutputStreamWriter(
                new GZIPOutputStream(new FileOutputStream(outputPath)));

        Config config = ConfigUtils.loadConfig(configPath);
        Scenario scenario = ScenarioUtils.createScenario(config);
        
        writer.write("<?xml version=\"1.0\" encoding=\"utf-8\" ?>\n");
        writer.write("<aggregate>\n");
        
        (new org.matsim.core.network.io.MatsimNetworkReader(scenario.getNetwork())).readFile(networkPath);
        (new TransitScheduleReaderV1(scenario)).readFile(transitSchedulePath);
        (new VehicleReaderV1(scenario.getTransitVehicles())).readFile(transitVehiclesPath);
        
        EventsManagerImpl events = new EventsManagerImpl();
        MatsimEventsReader reader = new MatsimEventsReader(events);
        
        EventsToLegs events2legs = new EventsToLegs(scenario);
        EventsToActivities events2activities = new EventsToActivities();
        
        events.addHandler(events2legs);
        events.addHandler(events2activities);

        TripHandler tripHandler = new TripHandler(writer);        
        events2legs.addLegHandler(tripHandler);
        events2activities.addActivityHandler(tripHandler);
        
        StuckHandler stuckHandler = new StuckHandler(writer);
        events.addHandler(stuckHandler);
        
        AVHandler avHandler = new AVHandler(writer);
        events.addHandler(avHandler);
        
        WaitingTimeHandler waitingTimehandler = new WaitingTimeHandler(writer);
        events.addHandler(waitingTimehandler);
        
        reader.readFile(eventsPath);
        
        tripHandler.finishRemainingTrips();
        stuckHandler.finish();
        avHandler.finishStates();
        
        FinalScoreReader scoreReader = new FinalScoreReader(writer);
        scoreReader.read(scoreStatsPath);
        
        writer.write("</aggregate>\n");
        writer.close();
    }
}

