package playground.clruch;

/**
 * Created by Claudio on 1/6/2017.
 */

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.events.handler.BasicEventHandler;
import org.matsim.core.events.handler.EventHandler;
import org.matsim.core.population.io.PopulationWriter;
import org.matsim.core.scenario.ScenarioUtils;
import playground.clruch.utils.PopulationTools;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

import java.io.FileWriter;
import java.io.IOException;
import org.jdom.Attribute;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;


class DoubleInterval {
    double start;
    double end;

    public boolean isInside(double value) {
        return start<=value && value <end;
    }

    @Override
    public String toString() {
        return start+" "+end;
    }
}

public class EventFileToProcessingXML {
    public static boolean isPerson(String id) {
        return !id.startsWith("av_");
    }

    public static NavigableMap<Double,Integer> integrate(NavigableMap<Double,Integer> map) {
        NavigableMap<Double,Integer> result= new TreeMap<>();
        int value = 0;
        result.put(0.0,value);
        for (Map.Entry<Double,Integer> entry : map.entrySet()) {
            value+=entry.getValue();
            result.put(entry.getKey(),value);

        }
        return result;
    }


    public static void main(String[] args) {

        // read an event output file given String[] args
        final File dir = new File(args[0]);
        File fileImport = new File(dir, "/output/output_events.xml");
        File fileExport = new File(dir, "/output/output_processing.xml");
        System.out.println("Is directory?  " + dir.isDirectory());

        // From the existing output_events file, load the
        List<Event> relevantEvents  = new ArrayList<>();
        Map<String, PersonDepartureEvent> deptEvent = new HashMap<>();
        Map<String, List<DoubleInterval>> linkOccupation = new HashMap<>();
        Map<String, NavigableMap<Double,Integer>> waitDelta = new TreeMap<>();
        EventsManager events = EventsUtils.createEventsManager();
        events.addHandler(i->System.out.println(""+i));

        events.addHandler(new PersonDepartureEventHandler() {

            @Override
            public void reset(int iteration) {
                // empty content
            }

            @Override
            public void handleEvent(PersonDepartureEvent event) {
                relevantEvents.add(event);
               final String id =event.getPersonId().toString();
               if (isPerson(id)) {
//                   System.out.println("dept " + id);
                   String linkId = event.getLinkId().toString();
                   deptEvent.put(id, event);
                   if (!waitDelta.containsKey(linkId))
                       waitDelta.put(linkId,new TreeMap<>());
                   waitDelta.get(linkId).put(event.getTime(),//
                           waitDelta.get(linkId).containsKey(event.getTime())? //
                                   waitDelta.get(linkId).get(event.getTime())+1 : 1);
               }
            }
        });
        events.addHandler(new PersonArrivalEventHandler() {

            @Override
            public void reset(int iteration) {
                // empty content
            }

            @Override
            public void handleEvent(PersonArrivalEvent event) {
                relevantEvents.add(event);
//                System.out.println("ARRIVE "+event.getTime()+" "+event.getPersonId());
//                System.out.println("arrival "+event.getPersonId());
            }
        });
        events.addHandler(new PersonEntersVehicleEventHandler(){


            @Override
            public void reset(int iteration) {

            }

            @Override
            public void handleEvent(PersonEntersVehicleEvent event) {
                relevantEvents.add(event);
                final String id =event.getPersonId().toString();
                if (isPerson(id)) {
                double wait =event.getTime()-deptEvent.get(id).getTime();
                    String linkId = deptEvent.get(id).getLinkId().toString();

                    System.out.println("enter "+id +"  " + deptEvent.get(id).getTime()+" - "+event.getTime()+" ="+wait +" "+linkId);
                    if (!linkOccupation.containsKey(linkId))
                        linkOccupation.put(linkId,new ArrayList<>());
                    DoubleInterval doubleInterval = new DoubleInterval();
                    doubleInterval.start=deptEvent.get(id).getTime();
                    doubleInterval.end=event.getTime();
                    linkOccupation.get(linkId).add(doubleInterval);

                    waitDelta.get(linkId).put(event.getTime(),//
                            waitDelta.get(linkId).containsKey(event.getTime())? //
                                    waitDelta.get(linkId).get(event.getTime())-1 : -1);



                }
            }
        });
        new MatsimEventsReader(events).readFile(fileImport.toString());

        // process events list to fill XML
        // linkOccupation.entrySet().stream().forEach(System.out::println);
        System.out.println("========== "+fileImport.getAbsoluteFile());

        // waitDelta.entrySet().stream().forEach(System.out::println);

        // integrate customer number changes to get to waiting customers step function
        {
            Map<String, NavigableMap<Double,Integer>> waitStepFctn = //
                    waitDelta.entrySet().stream().parallel().collect(Collectors.toMap(Map.Entry::getKey , v-> integrate(v.getValue())));
            System.out.println("==========");
            waitStepFctn.entrySet().stream().forEach(System.out::println);
        }

        // from the event file extract requests of AVs and arrivals of AVs at customers
        // calculate data in the form <node, time, numWaitCustomers> for all node, for all time
        // save as XML file

        try {
            Element SimulationResult = new Element("SimulationResult");
            Document doc = new Document(SimulationResult);
            doc.setRootElement(SimulationResult);

            Element node = new Element("node");
            node.setAttribute(new Attribute("id", "1"));
            node.addContent(new Element("firstname").setText("yong"));
            node.addContent(new Element("lastname").setText("mook kim"));
            node.addContent(new Element("nickname").setText("mkyong"));
            node.addContent(new Element("salary").setText("199999"));

            doc.getRootElement().addContent(node);

            Element node2 = new Element("node");
            node2.setAttribute(new Attribute("id", "2"));
            node2.addContent(new Element("firstname").setText("low"));
            node2.addContent(new Element("lastname").setText("yin fong"));
            node2.addContent(new Element("nickname").setText("fong fong"));
            node2.addContent(new Element("salary").setText("188888"));

            doc.getRootElement().addContent(node2);

            // new XMLOutputter().output(doc, System.out);
            XMLOutputter xmlOutput = new XMLOutputter();

            // display nice nice
            xmlOutput.setFormat(Format.getPrettyFormat());
            xmlOutput.output(doc, new FileWriter(fileExport));

            System.out.println("File Saved!");
        } catch (IOException io) {
            System.out.println(io.getMessage());
        }


        System.out.println("routine finished successfully");
    }
}
