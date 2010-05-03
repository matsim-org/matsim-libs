/* *********************************************************************** *
 * project: org.matsim.*
 * LaneDefinitonsV11ToV20Converter
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */
package org.matsim.lanes;

import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.SortedMap;

import javax.xml.bind.JAXBException;
import javax.xml.parsers.ParserConfigurationException;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.utils.io.MatsimFileTypeGuesser;
import org.matsim.signalsystems.CalculateAngle;
import org.xml.sax.SAXException;


/**
 * @author dgrether
 *
 */
public class LaneDefinitonsV11ToV20Converter {

	public LaneDefinitonsV11ToV20Converter(){
	}
	
	private void checkFileTypes(String laneDefs11Filename, String laneDefs20Filename){
		MatsimFileTypeGuesser fileTypeGuesser;
		try {
			fileTypeGuesser = new MatsimFileTypeGuesser(laneDefs11Filename);
			String sid11 = fileTypeGuesser.getSystemId();

//			fileTypeGuesser = new MatsimFileTypeGuesser(laneDefs20Filename);
//			String sid20 = fileTypeGuesser.getSystemId();
			
			if (!(sid11.compareTo(MatsimLaneDefinitionsReader.SCHEMALOCATIONV11) == 0)){
				throw new IllegalArgumentException("File " + laneDefs11Filename + " is no laneDefinitions_v1.1.xsd format");
			}
//			if (!(sid20.compareTo(MatsimLaneDefinitionsReader.SCHEMALOCATIONV20) == 0)){
//				throw new IllegalArgumentException("File " + laneDefs20Filename + " is no laneDefinitions_v2.0.xsd format");
//			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void convert(String laneDefs11Filename, String laneDefs20Filename, String networkFilename){
		this.checkFileTypes(laneDefs11Filename, laneDefs20Filename);
		
		Scenario sc = new ScenarioImpl();
		MatsimNetworkReader netReader = new MatsimNetworkReader(sc);
		netReader.readFile(networkFilename);
		Network net = sc.getNetwork();
		LaneDefinitions lanedefs11 = new LaneDefinitionsImpl();
		LaneDefinitionsReader11 reader11 = new LaneDefinitionsReader11(lanedefs11, MatsimLaneDefinitionsReader.SCHEMALOCATIONV11);
		try {
			reader11.readFile(laneDefs11Filename);
			LaneDefinitions lanedefs20 = convertTo20(lanedefs11, net);
			LaneDefinitionsWriter20 writer20 = new LaneDefinitionsWriter20(lanedefs20);
			writer20.write(laneDefs20Filename);
		} catch (JAXBException e) {
			e.printStackTrace();
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	
	public LaneDefinitions convertTo20(LaneDefinitions lanedefs11, Network network) {
		LaneDefinitions lanedefs20 = new LaneDefinitionsImpl();
		LaneDefinitionsFactory lanedefs20fac = lanedefs20.getFactory();
		LanesToLinkAssignment l2lnew;
		Lane lanenew;
		Link link;
		for (LanesToLinkAssignment l2l : lanedefs11.getLanesToLinkAssignments().values()){
			//create the lane2linkassignment
			l2lnew = lanedefs20fac.createLanesToLinkAssignment(l2l.getLinkId());
			link = network.getLinks().get(l2l.getLinkId());
			lanedefs20.addLanesToLinkAssignment(l2lnew);
			//create the already in 1.1 defined lanes and add them to the 2.0 format objects
			for (Lane lane : l2l.getLanes().values()){
				lanenew = lanedefs20fac.createLane(lane.getId());
				l2lnew.addLane(lanenew);
				//copy values
				lanenew.setNumberOfRepresentedLanes(lane.getNumberOfRepresentedLanes());
				lanenew.setStartsAtMeterFromLinkEnd(lane.getStartsAtMeterFromLinkEnd());
				for (Id toLinkId : lane.getToLinkIds()){
					lanenew.addToLinkId(toLinkId);
				}
			}
			//further processing of not defined lanes in 1.1 format
			//add original lane
			List<Lane> sortedLanes =  new LinkedList<Lane>(l2lnew.getLanes().values());
			Collections.sort(sortedLanes, new LaneMeterFromLinkEndComparator());
			Lane longestLane = sortedLanes.get(sortedLanes.size()-1);
//			double originalLaneLength = link.getLength() - longestLane.getStartsAtMeterFromLinkEnd();
			String originalLaneIdString = link.getId().toString() + ".ol";
			Lane originalLane = lanedefs20fac.createLane(new IdImpl(originalLaneIdString));
			originalLane.setNumberOfRepresentedLanes(link.getNumberOfLanes());
			originalLane.setStartsAtMeterFromLinkEnd(link.getLength());
			originalLane.addToLaneId(longestLane.getId());
			l2lnew.addLane(originalLane);
			
			//add other lanes
			Lane lastLane = originalLane;
			Lane secondLongestLane;
			Lane intermediateLane;
			Id intermediateLaneId;
			int intermediateLanesCounter = 1;
			for (int i = sortedLanes.size() - 2; i >= 0; i--){
				secondLongestLane = sortedLanes.get(i);
				if (longestLane.getStartsAtMeterFromLinkEnd() > secondLongestLane.getStartsAtMeterFromLinkEnd()){
					//create intermediate lane
					intermediateLaneId = new IdImpl(intermediateLanesCounter + ".cl");
					intermediateLanesCounter++;
					intermediateLane = lanedefs20fac.createLane(intermediateLaneId);
					intermediateLane.setStartsAtMeterFromLinkEnd(longestLane.getStartsAtMeterFromLinkEnd());
					intermediateLane.setNumberOfRepresentedLanes(link.getNumberOfLanes());
					l2lnew.addLane(intermediateLane);
					//intermdiateLane needs values as startsAt and 
					lastLane.addToLaneId(intermediateLaneId);
					lastLane = intermediateLane;
					longestLane = secondLongestLane;
				}
				else if (longestLane.getStartsAtMeterFromLinkEnd() == secondLongestLane.getStartsAtMeterFromLinkEnd()){
					//this case is rather easy, just add the toLaneId and proceed
					lastLane.addToLaneId(secondLongestLane.getId());
				}
				else {
					throw new RuntimeException("Illegal sort order");
				}
			}
			
			
			//calculate the alignment
			int mostRight = l2l.getLanes().size() / 2;
			SortedMap<Double, Link> result = CalculateAngle.getOutLinksSortedByAngle(link);
			Lane newLane;
			Set<Lane> assignedLanes = new HashSet<Lane>();
			for (Link tmpLink : result.values()){
				for (Lane l : l2l.getLanes().values()){
					if (assignedLanes.contains(l)){
						break;
					}
					newLane = l2lnew.getLanes().get(l.getId());
					if (newLane.getToLinkIds().contains(tmpLink.getId())){
						newLane.setAlignment(mostRight);
						assignedLanes.add(l);
						//decrement mostRigth skip 0 if number of lanes is even
						mostRight--;
						if ((mostRight == 0) && (l2l.getLanes().size() % 2  == 0)){
							mostRight--;
						}
						break;
					}
				}
			}
			
			
		}//end outer for
		
		return lanedefs20;
	}

	public static class LaneMeterFromLinkEndComparator implements Comparator<Lane>{

		@Override
		public int compare(Lane o1, Lane o2) {
      if (o1.getStartsAtMeterFromLinkEnd() < o2.getStartsAtMeterFromLinkEnd()) {
        return -1;
      } else if (o1.getStartsAtMeterFromLinkEnd() > o2.getStartsAtMeterFromLinkEnd()) {
        return 1;
      } else {
        return 0;
      }
		}
  };
	

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String inputDir = "./test/input/org/matsim/signalsystems/TravelTimeFourWaysTest/";
		
		String net = inputDir + "network.xml.gz";
		String lanes = inputDir + "testLaneDefinitions_v1.1.xml";
		String lanes20 = inputDir + "testLaneDefinitions_v2.0.xml";
		
		new LaneDefinitonsV11ToV20Converter().convert(lanes, lanes20, net);
		
	}

}
