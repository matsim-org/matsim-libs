package playground.sebhoerl.analysis.aggregate_events;

import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.api.core.v01.population.Person;

import playground.sebhoerl.analysis.XmlElement;

public class WaitingTimeHandler implements PersonDepartureEventHandler, PersonEntersVehicleEventHandler {  
    final private HashMap<Id<Person>, Double> pending = new HashMap<>();
    final private HashMap<Id<Person>, Double> pendingPt = new HashMap<>();
    
    final private Writer writer;
    
    public WaitingTimeHandler(Writer writer) {
        this.writer = writer;
    }    
    
    @Override
    public void reset(int iteration) {}

    @Override
    public void handleEvent(PersonEntersVehicleEvent event) {
        Double start = pending.remove(event.getPersonId());
        
        if (start != null) {
            XmlElement element = new XmlElement("av_waiting");
            element.addAttribute("start", start);
            element.addAttribute("end", event.getTime());
            element.addAttribute("person", event.getPersonId());
            element.addAttribute("av", event.getVehicleId());
            
            try {
                writer.write(element.toString() + "\n");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        
        start = pendingPt.remove(event.getPersonId());
        
        if (start != null) {
            XmlElement element = new XmlElement("pt_waiting");
            element.addAttribute("start", start);
            element.addAttribute("end", event.getTime());
            element.addAttribute("person", event.getPersonId());
            
            try {
                writer.write(element.toString() + "\n");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void handleEvent(PersonDepartureEvent event) {
        if (event.getLegMode() == "av") {
            pending.put(event.getPersonId(), event.getTime());
        } else if (event.getLegMode() == "pt") {
            pendingPt.put(event.getPersonId(), event.getTime());
        }
    }
}
