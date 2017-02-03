package playground.clruch.export;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import org.jdom.Attribute;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;

/**
 * Created by Claudio on 1/26/2017.
 */
class TimeBasedChangeEventXML extends AbstractEventXML {
    @Override
    public void generate(Map<String, NavigableMap<Double, Integer>> waitStepFctn, File file) {
        // from the event file extract requests of AVs and arrivals of AVs at customers
        // calculate data in the form <time,ID,change>
        // save as XML file
        try {
            Element SimulationResult = new Element("SimulationResult");
            Document doc = new Document(SimulationResult);
            doc.setRootElement(SimulationResult);

            Set<String> s = waitStepFctn.keySet();
            Iterator<String> e = s.iterator();
            SortedMap<Double, List<IdNumCust>> visualizationEvents = new TreeMap<>();

            // iterate through all stations and record events of customer number changes
            while (e.hasNext()) {
                String statID = (String) e.next();
                // for the node, extract the step function of numWaitCusotmer changes
                NavigableMap<Double, Integer> StepFctn = waitStepFctn.get(statID);
                for (Double timeVal : StepFctn.keySet()) {
                    // check if the time is already recorded
                    if (!visualizationEvents.containsKey(timeVal)) {
                        visualizationEvents.put(timeVal, new ArrayList<>());
                    }
                    // extract the list and add the event
                    IdNumCust temp = new IdNumCust();
                    temp.id = statID;
                    temp.numberCust = StepFctn.get(timeVal);
                    visualizationEvents.get(timeVal).add(temp);
                }
            }

            // iterate through the new list and output in the XML file
            Set<Double> s2 = visualizationEvents.keySet();
            System.out.println("Times with events:");
            for (double timeVal : s2) {
                Element node = new Element("time");
                node.setAttribute(new Attribute("seconds", Double.toString(timeVal)));
                List<IdNumCust> tempList = visualizationEvents.get(timeVal);
                for (IdNumCust idNumCust : tempList) {
                    Element waitChange = new Element("waitChange");
                    waitChange.setAttribute("id", idNumCust.id);
                    waitChange.setAttribute("numCustWait", Integer.toString(idNumCust.numberCust));
                    node.addContent(waitChange);
                }
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
}
