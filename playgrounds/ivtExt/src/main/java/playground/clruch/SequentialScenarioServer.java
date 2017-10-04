/**
 * 
 */
package playground.clruch;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.io.FileUtils;
import org.jdom.Attribute;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;

import ch.ethz.idsc.queuey.util.GlobalAssert;

/** @author Claudio Ruch */
public class SequentialScenarioServer {

    public static void main(String[] args) throws MalformedURLException, Exception {

        final int iterations = 5;
        String rawFolderName = "2017_10_04_SiouxFareDataRaw";
        File rawFolder = new File(rawFolderName);
        double[] fareRatios = { 0.1, 0.5, 1.0, 2.0, 10.0 };

        // copy the raw folder name including changed settings
        for (int i = 0; i < iterations; ++i) {
            File simFolder = new File(rawFolderName + "_Iteration_" + Integer.toString(i + 1));

            // copy the raw folder
            System.out.println("creating new simulation folder "+ simFolder.getAbsolutePath());
            FileUtils.copyDirectory(rawFolder, simFolder);

            // change the respective setting

            changeFareRatioTo(fareRatios[i], simFolder);

            // simulate the folder
            String userDir = System.getProperty("user.dir");
            System.setProperty( "user.dir", simFolder.getAbsolutePath());
            System.out.println("now in the working directory: " + (new File("").toString()));
            Thread.sleep(5000);
            ScenarioServer.simulate();
            System.setProperty("user.dir", userDir);
        }

    }

    private static void changeFareRatioTo(double fareRatio, File simFolder) //
            throws ParserConfigurationException, JDOMException, IOException {
        System.out.println("changing fare ratio to " + fareRatio);

        File xmlFile = new File(simFolder, "av.xml");

        GlobalAssert.that(xmlFile.exists());

        SAXBuilder builder = new SAXBuilder();
        builder.setValidation(false);
        builder.setFeature("http://xml.org/sax/features/validation", false);
        builder.setFeature("http://apache.org/xml/features/nonvalidating/load-dtd-grammar", false);
        builder.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        
        Document doc = (Document) builder.build(xmlFile);
        Element rootNode = doc.getRootElement();
        Element operator = rootNode.getChild("operator");
        Element dispatcher = operator.getChild("dispatcher");
        List<Element> children = dispatcher.getChildren();

        for (Element element : children) {
            List<Attribute> theAttributes = element.getAttributes();

            if (theAttributes.get(0).getValue().equals("fareRatioMultiply")) {
                theAttributes.get(1).setValue(Double.toString(fareRatio));

            }

        }

        XMLOutputter xmlOutput = new XMLOutputter();
        xmlOutput.setFormat(Format.getPrettyFormat());
        xmlOutput.output(doc, new FileWriter(xmlFile));

    }

}
