package playground.sebhoerl.analysis.aggregate_events;

import java.io.IOException;
import java.io.Writer;

import org.matsim.api.core.v01.events.PersonStuckEvent;
import org.matsim.api.core.v01.events.handler.PersonStuckEventHandler;

import playground.sebhoerl.analysis.XmlElement;

public class StuckHandler implements PersonStuckEventHandler {
    private long stuckCount = 0;
    final private Writer writer;
    
    public StuckHandler(Writer writer) {
        this.writer = writer;
    }
    
    @Override
    public void reset(int iteration) {}

    @Override
    public void handleEvent(PersonStuckEvent event) {
        stuckCount++;
    }
    
    public long getStuckCount() {
        return stuckCount;
    }
    
    public void finish() {
        XmlElement element = new XmlElement("stuck");
        element.addAttribute("count", stuckCount);
        
        try {
            writer.write(element.toString() + "\n");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
