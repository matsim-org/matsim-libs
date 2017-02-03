package playground.clruch.export;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NavigableMap;

import org.jdom.Attribute;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;

/**
 * Created by Claudio on 2/2/2017.
 */

class VehicleStatusEventXML extends AbstractEventXML<AVStatus> {

    // TODO: implement without this workaround and override from abstract class in a way that different Map types can be used
    // TODO: all of type Map<String,NavigableMap<Double,ANYDATATYPE>>

    @Override
    public void generate(Map<String, NavigableMap<Double, AVStatus>> waitStepFctn, File file) {
        try {
            Element SimulationResult = new Element("SimulationResult");
            Document doc = new Document(SimulationResult);
            doc.setRootElement(SimulationResult);

            // iterate through all stations with passenger movements and save waiting customers step function.
            for (Entry<String, NavigableMap<Double, AVStatus>> entry : waitStepFctn.entrySet()) {
                String statID = entry.getKey();
                Element node = new Element("av");
                node.setAttribute(new Attribute("id", statID));

                // iterate through step function for each node and save number of waiting customers
                NavigableMap<Double, AVStatus> map = entry.getValue();
                for (Entry<Double, AVStatus> timeStatus : map.entrySet()) {
                    Element event = new Element("event");
                    event.setAttribute("time", "" + timeStatus.getKey());
                    event.setAttribute("status", timeStatus.getValue().xmlTag);
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
