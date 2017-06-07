// code by clruch
package playground.clruch.prep;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import org.jdom2.Attribute;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.XMLOutputter;

/**
 * Created by Claudio on 3/18/2017.
 */
public class InterCityScheduleConverter {
    public static void main(String args[]) throws JDOMException, IOException {
        final File dir = new File(args[0]);
        if (!dir.isDirectory()) {
            new RuntimeException("not a directory: " + dir).printStackTrace();
            System.exit(-1);
        }

        // load schedules file
        // define input/ouptput file names
        final File fileImport = new File(dir, "2015_ch_schedule_Corrected.xml");
        final File fileExportGz = new File(dir, "2015_ch_schedule_CorrectedIntercityOnly.xml.gz");
        final File fileExport = new File(dir, "2015_ch_schedule_CorrectedIntercityOnly.xml");

        // load existing XML file
        System.out.println("starting conversion");
        SAXBuilder builder = new SAXBuilder();
        Document document = builder.build(fileImport);

        // perform conversion, remove non-rail lines and lines shorter than minimal duration
        ConvertRailOnly(document);
        ConvertLongerThanOnly(document, 60);
        RemoveEmptyTransitLines(document);


        // save file
        FileWriter writer = new FileWriter("2015_ch_schedule_Corrected_IntercityOnly.xml");
        XMLOutputter outputter = new XMLOutputter();
        //outputter.setFormat(Format.getPrettyFormat());
        outputter.output(document, writer);
        writer.close(); // close writer
    }

    private static void ConvertRailOnly(Document document) {
        Element rootNode = document.getRootElement();
        List<Element> childrenNodes = rootNode.getChildren();
        int removedelements = 0;
        for (Element element : childrenNodes) {
            if (element.getName().equals("transitLine")) {
                List<Element> cchildrenNodes = element.getChildren();
                Iterator<Element> eelementIterator = cchildrenNodes.iterator();
                while(eelementIterator.hasNext()){
                    List<Element> ccchildrenNodes = eelementIterator.next().getChildren();
                    for (Element eeelement : ccchildrenNodes) {
                        if (eeelement.getName().equals("transportMode")) {
                            String transportMean = eeelement.getContent().get(0).getValue();
                            if (!transportMean.equals("rail")) {
                                eelementIterator.remove();
                                removedelements++;
                                break;
                            }
                        }
                    }
                }

                /*
                for (Element eelement : cchildrenNodes) {
                    List<Element> ccchildrenNodes = eelement.getChildren();
                    for (Element eeelement : ccchildrenNodes) {
                        if (eeelement.getName().equals("transportMode")) {
                            String transportMean = eeelement.getContent().get(0).getValue();
                            if (!transportMean.equals("rail")) {
                                eelement.removeContent();
                                removedelements++;
                            }
                        }
                    }
                }

                */
            }
        }
        System.out.println("removed " + removedelements + " which don't have transportMode rail.");
    }

    private static void ConvertLongerThanOnly(Document document, int minMinutes) {
        Element rootNode = document.getRootElement();
        List<Element> childrenNodes = rootNode.getChildren();
        int removedelements = 0;
        for (Element element : childrenNodes) {
            if (element.getName().equals("transitLine")) {
                List<Element> cchildrenNodes = element.getChildren();
                Iterator<Element> eelementIterator = cchildrenNodes.iterator();
                while(eelementIterator.hasNext()){
                    List<Element> ccchildrenNodes = eelementIterator.next().getChildren();
                    for (Element eeelement : ccchildrenNodes) {
                        // TODO add code here to remove lines with short duration
                        System.out.println(eeelement);
                        if (eeelement.getName().equals("routeProfile")) {
                            List<Element> cccchildrenNodes = eeelement.getChildren();
                            Element lastElem = cccchildrenNodes.get(cccchildrenNodes.size() - 1);
                            for (Attribute attribute : lastElem.getAttributes()) {
                                if (attribute.getName().equals("arrivalOffset")) {
                                    String timeVal = attribute.getValue();
                                    String[] parts = timeVal.split(":");
                                    int hours = Integer.parseInt(parts[0]);
                                    int minutes = Integer.parseInt(parts[1]);
                                    int seconds = Integer.parseInt(parts[2]);
                                    if ((hours * 60 + minutes + Math.round((double) seconds / 60.0)) < minMinutes) {
                                        eelementIterator.remove();
                                        removedelements++;
                                        break;
                                    }
                                }
                            }
                        }
                    }


                }

                /*
                for (Element eelement : cchildrenNodes) {
                    List<Element> ccchildrenNodes = eelement.getChildren();
                    for (Element eeelement : ccchildrenNodes) {
                        // TODO add code here to remove lines with short duration
                        System.out.println(eeelement);
                        if (eeelement.getName().equals("routeProfile")) {
                            List<Element> cccchildrenNodes = eeelement.getChildren();
                            Element lastElem = cccchildrenNodes.get(cccchildrenNodes.size() - 1);
                            for (Attribute attribute : lastElem.getAttributes()) {
                                if (attribute.getName().equals("arrivalOffset")) {
                                    String timeVal = attribute.getValue();
                                    String[] parts = timeVal.split(":");
                                    int hours = Integer.parseInt(parts[0]);
                                    int minutes = Integer.parseInt(parts[1]);
                                    int seconds = Integer.parseInt(parts[2]);
                                    if ((hours * 60 + minutes + Math.round((double) seconds / 60.0)) < minMinutes) {
                                        eelement.removeContent();
                                        removedelements++;
                                    }
                                }
                            }
                        }
                    }
                }
                */
            }
        }
        System.out.println("removed " + removedelements + " which are of duration shorter than " + minMinutes + " minutes.");
    }

    private static void RemoveEmptyTransitLines(Document document){
        Element rootNode = document.getRootElement();
        List<Element> childrenNodes = rootNode.getChildren();
        int removedelements = 0;
        Iterator<Element> eelementIterator = childrenNodes.iterator();
        while(eelementIterator.hasNext()){
            Element eelement = eelementIterator.next();
            if(eelement.getName().equals("transitLine")){
                if(eelement.getChildren().size() == 0){
                    eelementIterator.remove();
                }
            }
        }
    }
}
