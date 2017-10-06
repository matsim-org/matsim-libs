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

        int iterations = 20;
        if (iterations % 2 == 0) {
            iterations = iterations - 1;
        }
        File rawFolder = new File("").getCanonicalFile();
        // double[] fareRatios = { 0.1, 0.5, 1.0, 2.0, 10.0 };

        double factorPlus = 1.5;
        double[] fareRatios = fareRatioCreator(iterations, factorPlus);

        // copy the raw folder name including changed settings
        for (int i = 0; i < iterations; ++i) {
            System.out.println("working in the directory " + rawFolder.getAbsolutePath());

            // modify the av.xml file
            changeFareRatioTo(fareRatios[i], rawFolder);

            // set the output-directory correctly
            changeOutputDirectoryTo("output" + i, rawFolder);

            // simulate
            ScenarioServer.simulate();

        }

    }

    private static double[] fareRatioCreator(int iterations, double factorPlus) throws InterruptedException {
        GlobalAssert.that(iterations % 2 != 0);

        double[] fareRatios = new double[iterations];

        fareRatios[iterations / 2] = 1.0;

        for (int i = iterations / 2 + 1; i < iterations; ++i) {
            fareRatios[i] = fareRatios[i - 1] * factorPlus;
        }

        for (int i = iterations / 2 - 1; i >= 0; --i) {
            fareRatios[i] = fareRatios[i + 1] / factorPlus;
        }

        System.out.println("fare Ratios:");
        for (int i = 0; i < iterations; ++i) {
            System.out.print(" , " + fareRatios[i]);
        }
        System.out.println(" ");
        Thread.sleep(5000);

        return fareRatios;

    }

    private static void changeFareRatioTo(double fareRatio, File simFolder) //
            throws ParserConfigurationException, JDOMException, IOException {
        System.out.println("changing fare ratio to " + fareRatio);

        File xmlFile = new File(simFolder, "av.xml");

        System.out.println("looking for av.xml file at " + xmlFile.getAbsolutePath());

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

    private static void changeOutputDirectoryTo(String name, File simFolder) //
            throws ParserConfigurationException, JDOMException, IOException {

        System.out.println("changing output directory to " + name);

        File xmlFile = new File(simFolder, "av_config.xml");
        GlobalAssert.that(xmlFile.exists());

        SAXBuilder builder = new SAXBuilder();
        builder.setValidation(false);
        builder.setFeature("http://xml.org/sax/features/validation", false);
        builder.setFeature("http://apache.org/xml/features/nonvalidating/load-dtd-grammar", false);
        builder.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);

        Document doc = (Document) builder.build(xmlFile);
        Element rootNode = doc.getRootElement();

        List<Element> children = rootNode.getChildren();

        for (Element child : children) {
            if (child.getAttributeValue("name").equals("controler")) {
                List<Element> cchildren = child.getChildren();
                for (Element cchild : cchildren) {
                    if (cchild.getAttributeValue("name").equals("outputDirectory")) {
                        List<Attribute> theAttributes = cchild.getAttributes();
                        if (theAttributes.get(0).getValue().equals("outputDirectory")) {
                            theAttributes.get(1).setValue(name);
                        }
                    }
                }
            }
        }
        XMLOutputter xmlOutput = new XMLOutputter();
        xmlOutput.setFormat(Format.getPrettyFormat());
        xmlOutput.output(doc, new FileWriter(xmlFile));

    }

}