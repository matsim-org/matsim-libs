package playground.clruch.export;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

import org.jdom.Attribute;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.Event;
import org.matsim.core.api.internal.HasPersonId;

/**
 * Created by Claudio on 2/2/2017.
 */



class VehicleLocationEventXML extends AbstractEventXML<IdAVStatus> {

    // TODO: implement without this workaround and ovverride from abstract class in a way that different Map types can be used
    // TODO: all of type Map<String,NavigableMap<Double,ANYDATATYPE>>
    Map<String, NavigableMap<Double, IdAVStatus>> vehicleStatus;
    NavigableMap<Double, Event> relevantEvents;

    public VehicleStatusEventXML(Map<String, NavigableMap<Double, IdAVStatus>> vehicleLocations) {
        vehicleStatus = vehicleStatusIn;
        relevantEvents = relevantEventsIn;
    }

    @Override
    public void generate(Map<String, NavigableMap<Double, IdAVStatus>> waitStepFctn, File file) {

    }



    public void generate3(File file) {
        // from the event file extract requests of AVs and arrivals of AVs at customers
        // calculate data in the form <node, time, numWaitCustomers> for all node, for all time
        // save as XML file
        try {
            Element SimulationResult = new Element("SimulationResult");
            Document doc = new Document(SimulationResult);
            doc.setRootElement(SimulationResult);

            // iterate through all relevant events and write them into an XML file
            for (Map.Entry<Double, Event> entry : relevantEvents.entrySet()) {
                Double time = entry.getKey();
                Event event = entry.getValue();
                Element node = new Element("event");
                node.setAttribute(new Attribute("time", time.toString()));
                node.setAttribute(new Attribute("eventType", event.getEventType().toString()));
                //activitystart
                if (event.getEventType().toString().equals("actstart")) {
                    ActivityStartEvent tempEvent = (ActivityStartEvent) event;
                    node.setAttribute(new Attribute("person", tempEvent.getPersonId().toString()));
                    node.setAttribute(new Attribute("link", tempEvent.getLinkId().toString()));
                    node.setAttribute(new Attribute("actType", tempEvent.getActType().toString()));
                }

                //activityend
                if(event.getEventType().toString().equals("actend")){
                    ActivityEndEvent tempEvent = (ActivityEndEvent) event;
                    node.setAttribute(new Attribute("person", tempEvent.getPersonId().toString()));
                    node.setAttribute(new Attribute("link", tempEvent.getLinkId().toString()));
                    node.setAttribute(new Attribute("actType", tempEvent.getPersonId().toString()));
                }

                //departure event
                if(event.getEventType().toString().equals("departure")){

                }

                // node.setAttribute(new Attribute("eventtoString", event.getPerson toString()));
                // node.setAttribute(new Attribute("timewubaba", time.toString()));
                // ...
                doc.getRootElement().addContent(node);

            }

            // new XMLOutputter().output(doc, System.out);
            XMLOutputter xmlOutput = new XMLOutputter();

            // display nice nice
            xmlOutput.setFormat(Format.getPrettyFormat());
            xmlOutput.output(doc, new FileWriter(file));

            System.out.println("File Saved!");
        } catch (IOException io) {
            System.out.println(io.getMessage());
        }
    }



};


