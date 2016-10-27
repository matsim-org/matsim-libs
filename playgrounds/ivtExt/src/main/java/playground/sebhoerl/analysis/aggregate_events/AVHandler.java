package playground.sebhoerl.analysis.aggregate_events;

import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.events.handler.ActivityStartEventHandler;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.events.handler.BasicEventHandler;

import playground.sebhoerl.analysis.XmlElement;

public class AVHandler implements BasicEventHandler, ActivityStartEventHandler {
    final private Writer writer;
    
    class AVActiveState {
        public Id<Person> agent;
        public double startTime;
        public double endTime;
        public String state;
    }
    
    final Map<Id<Person>, AVActiveState> states = new HashMap<>();
    
    String currentDispatcherMode = "OVERSUPPLY";
    double currentDispatcherModeStart = 0.0;
    
    @Override
    public void reset(int iteration) {}
    
    public AVHandler(Writer writer) {
        this.writer = writer;
    }

    @Override
    public void handleEvent(Event event) {
        if (event.getEventType().equals("AVDispatchModeChange")) {            
            writeDispatcherChange(event.getTime());
            
            currentDispatcherMode = event.getAttributes().get("mode");
            currentDispatcherModeStart = event.getTime();
        }
    }
    
    protected void writeDispatcherChange(double end) {
        XmlElement element = new XmlElement("av_dispatcher_mode");
        element.addAttribute("start_time", currentDispatcherModeStart);
        element.addAttribute("end_time", end);
        element.addAttribute("mode", currentDispatcherMode);
        
        try {
            writer.write(element.toString() + "\n");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    protected void writeState(AVActiveState state) {
        XmlElement element = new XmlElement("av_state");
        element.addAttribute("agent", state.agent);
        element.addAttribute("start_time", state.startTime);
        element.addAttribute("end_time", state.endTime);
        element.addAttribute("state", state.state);
        
        try {
            writer.write(element.toString() + "\n");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void handleEvent(ActivityStartEvent event) {
        if (event.getActType().startsWith("AV")) {
            AVActiveState previous = states.get(event.getPersonId());
            
            if (previous != null) {
                previous.endTime = event.getTime();
                writeState(previous);
            }
            
            AVActiveState current = new AVActiveState();
            current.agent = event.getPersonId();
            current.startTime = event.getTime();
            current.state = event.getActType();
            
            states.put(event.getPersonId(), current);
        }
    }
    
    public void finishStates() {
        for (AVActiveState state : states.values()) {
            state.endTime = Double.POSITIVE_INFINITY;
            writeState(state);
        }
        
        writeDispatcherChange(Double.POSITIVE_INFINITY);
    }
}
