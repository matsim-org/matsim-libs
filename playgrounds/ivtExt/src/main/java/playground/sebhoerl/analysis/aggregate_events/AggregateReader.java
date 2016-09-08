package playground.sebhoerl.analysis.aggregate_events;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.zip.GZIPInputStream;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;

public class AggregateReader extends DefaultHandler implements AggregateHandler {
    private long iterations;
    private double score;
    private long stuck;
    
    public long getIterations() { return iterations; }
    public double getScore() { return score; }
    public long getStuckCount() { return stuck; }
    
    final private LinkedList<AggregateHandler> handlers = new LinkedList<>();
    
    public void addHandler(AggregateHandler handler) {
        handlers.add(handler);
    }
    
    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) {
        switch (qName) {
        case "simulation":
            iterations = Long.parseLong(attributes.getValue("iterations"));
            score = Double.parseDouble(attributes.getValue("score"));
            break;
        case "stuck":
            stuck = Long.parseLong(attributes.getValue("count"));
            break;
        case "trip":
            Trip trip = new Trip();
            trip.setMode(attributes.getValue("mode"));
            trip.setDistance(Double.parseDouble(attributes.getValue("distance")));
            trip.setStartTime(Double.parseDouble(attributes.getValue("start_time")));
            trip.setEndTime(Double.parseDouble(attributes.getValue("end_time")));
            trip.setStartLink(Id.createLinkId(attributes.getValue("start_link")));
            trip.setEndLink(Id.createLinkId(attributes.getValue("end_link")));
            trip.setPerson(Id.createPersonId(attributes.getValue("person")));
            
            if (attributes.getValue("walk_time") != null) {
                trip.setWalkTime(Double.parseDouble(attributes.getValue("walk_time")));
            }
            
            if (attributes.getValue("walk_distance") != null) {
                trip.setWalkDistance(Double.parseDouble(attributes.getValue("walk_distance")));
            }
            
            handleTrip(trip);
            break;
        case "av_waiting":
            handleWaiting(
                Id.createPersonId(attributes.getValue("person")),
                Id.createPersonId(attributes.getValue("av")),
                Double.parseDouble(attributes.getValue("start")),
                Double.parseDouble(attributes.getValue("end"))
                );
            break;
        case "av_state":
            handleAVState(
                Id.createPersonId(attributes.getValue("agent")),
                Double.parseDouble(attributes.getValue("start_time")),
                Double.parseDouble(attributes.getValue("end_time")),
                attributes.getValue("state")
                );
            break;
        case "av_dispatcher_mode":
            handleAVDispatcherMode(
                attributes.getValue("mode"),
                Double.parseDouble(attributes.getValue("start_time")),
                Double.parseDouble(attributes.getValue("end_time"))
                );
            break;
        }
    }

    @Override
    public void handleTrip(Trip trip) {
        for (AggregateHandler handler : handlers) handler.handleTrip(trip);
    }

    @Override
    public void handleWaiting(Id<Person> person, Id<Person> av, double start, double end) {
        for (AggregateHandler handler : handlers) handler.handleWaiting(person, av, start, end);
    } 
    
    public void read(String path) {
        try { 
            GZIPInputStream inputStream = new GZIPInputStream(new FileInputStream(path));
            SAXParserFactory factory = SAXParserFactory.newInstance();
            SAXParser saxParser = factory.newSAXParser();
            saxParser.parse(inputStream, this);     
         } catch (Exception e) {
            e.printStackTrace();
         }
    }
    
    @Override
    public void handleAVState(Id<Person> av, double start, double end, String state) {
        for (AggregateHandler handler : handlers) handler.handleAVState(av, start, end, state);
    }
    
    @Override
    public void handleAVDispatcherMode(String mode, double start, double end) {
        for (AggregateHandler handler : handlers) handler.handleAVDispatcherMode(mode, start, end);
    }
}
