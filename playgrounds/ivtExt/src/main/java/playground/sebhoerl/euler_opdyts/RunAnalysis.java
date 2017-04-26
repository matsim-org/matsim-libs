package playground.sebhoerl.euler_opdyts;

import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.events.EventsReaderXMLv1;

public class RunAnalysis {
    static public void main(String[] args) {
        String path = args[0];

        MyObjectiveHandler handler = new MyObjectiveHandler();

        EventsManager events = new EventsManagerImpl();
        events.addHandler(handler);

        EventsReaderXMLv1 reader = new EventsReaderXMLv1(events);
        reader.readFile(path);

        System.out.println(handler.getState());
    }
}
