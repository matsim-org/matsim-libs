package playground.clruch;

import org.jdom.Attribute;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Set;

/**
 * Created by Claudio on 1/26/2017.
 */
public class NodeBasedEventXML extends AbstractEventXML {
    @Override
    public void generate(Map<String, NavigableMap<Double, Integer>> waitStepFctn, File file) {
        // from the event file extract requests of AVs and arrivals of AVs at customers
        // calculate data in the form <node, time, numWaitCustomers> for all node, for all time
        // save as XML file
        try {
            Element SimulationResult = new Element("SimulationResult");
            Document doc = new Document(SimulationResult);
            doc.setRootElement(SimulationResult);


            Set<String> s = waitStepFctn.keySet();
            Iterator<String> e = s.iterator();

            // iterate through all stations with passenger movements and save waiting customers step function.
            while (e.hasNext()) {
                String statID = (String) e.next();
                Element node = new Element("node");
                node.setAttribute(new Attribute("id", statID));

                // iterate through step function for each node and save number of waiting customers
                NavigableMap<Double, Integer> StepFctn = waitStepFctn.get(statID);
                for (Double timeVal : StepFctn.keySet()) {
                    Element event = new Element("event");
                    event.setAttribute("time", timeVal.toString());
                    event.setAttribute("numCustWait", StepFctn.get(timeVal).toString());
                    node.addContent(event);
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
