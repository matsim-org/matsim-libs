package playground.joel.data;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.jdom.Attribute;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;

/**
 * Created by Joel on 01.03.2017.
 */
public class TotalData <Type> {
    String xmlTitle = "SimulationResult";
    String L1ElName = "TotalTimeRatio";
    String L1AttrName = "TimeRatio";
    String L2ElName = "TotalDistanceRatio";
    String L2AttrName = "DistanceRatio";
    String L3ElName = "TotalWaitingTime";
    String L3Attr1Name = "WaitingTimeMean";
    String L3Attr2Name = "WaitingTime50Quantile";
    String L3Attr3Name = "WaitingTime95Quantile";

    public TotalData() {}

    // Take the Map timeStepData which for some IDs contains series of changes for times Double
    // in the data format Type. Save them in an XML with the structure:
    // <xmlTitle>
    // <L1ElName L1AttrName="String1">
    // </L1ElName>
    // <L2ElName L2AttrName="String2">
    // </L1ElName>
    // ...
    public final void generate(String value1, String value2, String value3, String value4, String value5, File file) {
        try {
            Element results = new Element(xmlTitle);
            Document doc = new Document(results);
            doc.setRootElement(results);

            // time ratio
            Element element1 = new Element(L1ElName);
            element1.setAttribute(new Attribute(L1AttrName, value1));
            doc.getRootElement().addContent(element1);

            // distance ratio
            Element element2 = new Element(L2ElName);
            element2.setAttribute(new Attribute(L2AttrName, value2));
            doc.getRootElement().addContent(element2);


            // waiting times
            Element element3 = new Element(L3ElName);
            element3.setAttribute(new Attribute(L3Attr1Name, value3));
            element3.setAttribute(new Attribute(L3Attr2Name, value4));
            element3.setAttribute(new Attribute(L3Attr3Name, value5));
            doc.getRootElement().addContent(element3);

            // output the XML
            XMLOutputter xmlOutput = new XMLOutputter();
            // display nice nice
            xmlOutput.setFormat(Format.getPrettyFormat());
            xmlOutput.output(doc, new FileWriter(file));

            System.out.println("Exported " + file.getName());
        } catch (IOException io) {
            System.out.println(io.getMessage());
        }
    }
}
