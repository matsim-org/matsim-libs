/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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

package playground.benjamin.scenarios.munich.analysis.exposure;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;

import playground.benjamin.scenarios.munich.analysis.filter.UserGroup;
import playground.benjamin.utils.spatialAvg.Cell;
import playground.benjamin.utils.spatialAvg.LinkWeightUtil;
import playground.benjamin.utils.spatialAvg.SpatialGrid;

/**
 * 
 * @author julia
 *
 */
public class GroupXGroupExposureCosts {
	
	Double [][] groupXgroupMatrix;
	private UserGroup[] userGroups;
	private Double scalingFactor;
	private int noGroups;
	private static final Logger logger = Logger.getLogger(GroupXGroupExposureCosts.class);
	private final String tab = "\t";

	public GroupXGroupExposureCosts(Double scalingFactor){
		userGroups = UserGroup.values();
		this.noGroups = userGroups.length;
		groupXgroupMatrix = new Double[noGroups][noGroups];
		initializeMatrix(groupXgroupMatrix);
		this.scalingFactor= scalingFactor;
	}

	public void calculateGroupCosts(
			GroupLinkFlatEmissions causingGroupLinkFlatEmissions,
			Map<UserGroup, SpatialGrid> mapOfDurations, LinkWeightUtil linkweightUtil,
			Double averageDurationPerCell, Map<Id<Link>, ? extends Link> links) { 
		
			if(averageDurationPerCell<=0.0)averageDurationPerCell=1.0;
			Double normalizationFactor = linkweightUtil.getNormalizationFactor();
			// for every causing group
			for(int causing=0; causing<noGroups; causing++){
				logger.info("Causing group:" + userGroups[causing].toString());
				// for every caused flat emission by causing group:
				Map<Id<Link>, Double> causedFlatEmissionsByCurrentCausingGroup = causingGroupLinkFlatEmissions.getLinks2FlatEmissionsFromCausingUserGroup(userGroups[causing]);
				
				for(Id<Link> linkId: causedFlatEmissionsByCurrentCausingGroup.keySet()){
				
				// for each receiving group
					for(int receiving=0; receiving<noGroups; receiving++){
						
					// go through all cells
					SpatialGrid durationsOfReceivingGroup = mapOfDurations.get(userGroups[receiving]);
					for(Cell cell: durationsOfReceivingGroup.getCells()){
						// calc weight of link for that cell
						Double weightOfLinkForCell = linkweightUtil.getWeightFromLink(links.get(linkId), cell.getCentroid())*normalizationFactor;		
						// calc 'duration density'
						Double durationDensity = cell.getWeightedValue()/averageDurationPerCell;	
						// emission cost from causing group to receiving group 
						// = duration density x flat emission cost x link weight
						Double causedEmissionCost = scalingFactor*weightOfLinkForCell*durationDensity*causedFlatEmissionsByCurrentCausingGroup.get(linkId);
						// add to matrix
						groupXgroupMatrix[causing][receiving]+= causedEmissionCost;
					}
				}
			}
			}
	}

	public void print() {
		System.out.println("     ");
		System.out.println(" Causing: ");
		for(UserGroup causing: userGroups){
			System.out.print(causing.toString() + "   ");
		}
		System.out.println("total");
		System.out.println();
		for(int receiving=0; receiving<noGroups; receiving++){
			System.out.print("Receiving:" + userGroups[receiving].toString() + "  ");
			for(int causing=0; causing<noGroups; causing++){
				System.out.print(groupXgroupMatrix[causing][receiving] + "   ");
			}
			
			System.out.println(getTotalReceivedValue(receiving));
			System.out.println();
		}
		System.out.print("Total receiving:   ");
		for(int causing=0; causing<noGroups; causing++){
			System.out.print(getTotalCausedValue(causing) + "   ");
		}
		System.out.println();
		System.out.println("Total costs: " + getTotalCosts());
		
		System.out.println();
		System.out.println("Received/Causing:");
		for(int group=0; group<noGroups; group++){
			System.out.println(userGroups[group].toString() + " : "+ (getTotalReceivedValue(group)/getTotalCausedValue(group)));
		}
	}
	
	public void writeOutputFile(String outputPath){
			try {
				BufferedWriter buffW = new BufferedWriter(new FileWriter(outputPath+"ExposureCostsByGroup.txt"));
				String valueString = new String(); //TODO rename
				
				// head
				valueString = "Receiving/Causing: " + tab;
				
				for(UserGroup causing: userGroups){
					valueString +=(causing.toString() + tab);
				}
				valueString += "total";
				buffW.write(valueString);
				buffW.newLine();
				
				// all user groups receiving
				for(int receiving=0; receiving<noGroups; receiving++){
					valueString = new String();
					valueString += userGroups[receiving].toString() + tab;
					for(int causing=0; causing<noGroups; causing++){
						valueString += groupXgroupMatrix[causing][receiving] + tab;
					}
					valueString += getTotalReceivedValue(receiving);
					buffW.write(valueString);
					buffW.newLine();
				}
				
				// total receiving
				valueString = new String();
				valueString = "total" + tab;
				for(int causing=0; causing<noGroups; causing++){
					valueString+= getTotalCausedValue(causing) + tab;
				}
				buffW.write(valueString);
				buffW.newLine();buffW.newLine();
				
				buffW.write("Total costs: " + getTotalCosts());
				buffW.newLine();buffW.newLine();
				buffW.write("Causing/Receiving:");
				buffW.newLine();
				for(int group=0; group<noGroups; group++){
					buffW.write((userGroups[group].toString() + " : "+ (getTotalCausedValue(group)/getTotalReceivedValue(group))));
					buffW.newLine();
				}

				buffW.close();	
			} catch (IOException e) {
				throw new RuntimeException("Failed writing output. Reason: " + e);
			}	
			logger.info("Finished writing exposure cost output to " + outputPath);
		
	}
	
	private Double getTotalCosts() {
		Double totalCosts =0.0;
		for(int causing =0; causing<noGroups; causing++){
			totalCosts += getTotalCausedValue(causing);
		}
		return totalCosts;
	}


	private Double getTotalReceivedValue(int receiving) {
		Double totalReceived= new Double(0.0);
		for(int causing = 0; causing<noGroups; causing++){
			totalReceived += groupXgroupMatrix[causing][receiving];
		}
		return totalReceived;
	}

	private Double getTotalCausedValue(int causing) {
		Double totalCaused= new Double(0.0);
		for(int receiving = 0; receiving<noGroups; receiving++){
			totalCaused += groupXgroupMatrix[causing][receiving];
		}
		return totalCaused;
	}
	
	private void initializeMatrix(Double[][] matrix) {
		for(int i=0; i<matrix.length; i++){
			for(int j=0; j<matrix[i].length; j++){
				matrix[i][j]=new Double(0.0);
			}
		}
	}
}
