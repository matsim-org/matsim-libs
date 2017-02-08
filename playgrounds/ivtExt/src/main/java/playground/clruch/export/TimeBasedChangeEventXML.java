package playground.clruch.export;

import java.util.*;

/**
 * Created by Claudio on 1/26/2017.
 */

// OBSOLETE: Saving the changes in fill levels at station per time is not needed anymore. Can be deleted in future commit.


class TimeBasedChangeEventXML extends AbstractEventXML<IdNumCust> {
    public TimeBasedChangeEventXML(String xmlTitleIn, String L1ElNameIn, String L1AttrNameIn, String L2ElNameIn, String L2Attr1NameIn, String L2Attr2NameIn) {
        super(xmlTitleIn, L1ElNameIn, L1AttrNameIn, L2ElNameIn, L2Attr1NameIn, L2Attr2NameIn);
    }



    // rearranges stated sorted values to a time sorted list
    public void rearrangeInTime(Map<String, NavigableMap<Double, Integer>> waitStepFctn, NavigableMap<Double, List<IdNumCust>> timeSortedEvents) {

        SortedMap<Double, List<IdNumCust>> tempMap = new TreeMap<>();

        // iterate through all stations and record events of customer number changes
        for (String statID : waitStepFctn.keySet()) {

            // for the node, extract the step function of numWaitCusotmer changes
            NavigableMap<Double, Integer> StepFctn = waitStepFctn.get(statID);
            for (Double timeVal : StepFctn.keySet()) {
                // check if the time is already recorded
                if (!tempMap.containsKey(timeVal)) {
                    tempMap.put(timeVal, new ArrayList<>());
                }
                // extract the list and add the event
                IdNumCust temp = new IdNumCust();
                temp.id = statID;
                temp.numberCust = StepFctn.get(timeVal);
                tempMap.get(timeVal).add(temp);
            }
        }

        // cast to format required by subsequent method
        timeSortedEvents = (NavigableMap<Double, List<IdNumCust>>) tempMap;
    }

    // FIXME
    // The inherited generate function cannot be called at the moment because the required Map type is
    // Map<String, NavigableMap<Double, IdNumCust>> but the current format is
    // List<IdNumCust>> timeSortedEvents
    // to be changed if the outputfile should be used again.


}








// -----------------------------------------------------------------------------------
// OLD IMPLEMENTATION


// unfinished for timebasedChangeVentXML.java
/*


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

*/