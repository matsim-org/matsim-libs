package playground.joel.data;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Set;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;

class BinnedRatiosXML {
    String xmlTitle = "SimulationResult";
    String L1ElName;
    //String L1AttrName = "attribute";
    String L2ElName = "data";
    String L2Attr1Name = "start-end";
    String L2Attr2Name = "ratio";

    public BinnedRatiosXML(String name) {
        L1ElName = name;
    }

    public BinnedRatiosXML(String name, String attrName) {
        L1ElName = name;
        L2Attr2Name = attrName;
    }

    // Take the Map timeStepData which for some IDs contains series of changes for times Double
    // in the data format Type. Save them in an XML with the structure:
    // <xmlTitle>
    // <L1ElName L1AttrName="String1">
    // <L2ElName L2Attr1Name="Double1ofString1" L2Attr2Name="Type1ofString1" />
    // <L2ElName L2Attr1Name="Double2ofString1" L2Attr2Name="Type2ofString1" />
    // <L2ElName L2Attr1Name="Double3ofString1" L2Attr2Name="Type3ofString1" />
    // ...
    // </L1ElName>
    // <L1ElName L1AttrName="String1">
    // <L2ElName L2Attr1Name="Double1ofString2" L2Attr2Name="Type1ofString2" />
    // <L2ElName L2Attr1Name="Double2ofString2" L2Attr2Name="Type2ofString2" />
    // <L2ElName L2Attr1Name="Double3ofString2" L2Attr2Name="Type3ofString2" />
    // ...
    // </L1ElName>
    // ...
    public final void generate(Map<String, NavigableMap<String, Double>> timeStepData, File file) {
        try {
            Element SimulationResult = new Element(xmlTitle);
            Document doc = new Document(SimulationResult);
            doc.setRootElement(SimulationResult);
            Set<String> s = timeStepData.keySet();
            Iterator<String> e = s.iterator();
            // Iterate through all L1ElName entries
            while (e.hasNext()) {
                String statID = (String) e.next();
                Element node = new Element(L1ElName);
                //node.setAttribute(new Attribute(L1AttrName, statID));
                // iterate through step function to save L2ElName entries
                NavigableMap<String, Double> Ratios = timeStepData.get(statID);
                for (String ratio : Ratios.keySet()) {
                    Element event = new Element(L2ElName);
                    event.setAttribute(L2Attr1Name, ratio.toString());
                    event.setAttribute(L2Attr2Name, Ratios.get(ratio).toString());
                    node.addContent(event);
                }
                doc.getRootElement().addContent(node);
            }
            // output the XML
            XMLOutputter xmlOutput = new XMLOutputter();
            // display nice nice
            xmlOutput.setFormat(Format.getPrettyFormat());
            xmlOutput.output(doc, new FileWriter(file));

            System.out.println("exported " + file.getName());
        } catch (IOException io) {
            System.out.println(io.getMessage());
        }
    }
}
