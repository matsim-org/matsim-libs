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

package playground.juliakern.analysis;

import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;

import playground.benjamin.scenarios.munich.analysis.filter.UserGroup;
import playground.benjamin.scenarios.munich.analysis.spatialAvg.Cell;
import playground.benjamin.scenarios.munich.analysis.spatialAvg.LinkWeightUtil;
import playground.benjamin.scenarios.munich.analysis.spatialAvg.SpatialGrid;

public class GroupXGroupEmissionCosts {
	
	Double [][] groupXgroupMatrix;
	private UserGroup[] userGroups;
	private Double scalingFactor;
	private static final Logger logger = Logger.getLogger(GroupXGroupEmissionCosts.class);

	public GroupXGroupEmissionCosts(Double scalingFactor){
		userGroups = UserGroup.values();
		groupXgroupMatrix = new Double[userGroups.length][userGroups.length];
		initializeMatrix(groupXgroupMatrix);
		this.scalingFactor= scalingFactor;
	}


	public void calculateGroupCosts(
			GroupLinkFlatEmissions causingGroupLinkFlatEmissions,
			Map<UserGroup, SpatialGrid> mapOfDurations, LinkWeightUtil linkweightUtil,
			Double averageDurationPerCell, Map<Id<Link>, ? extends Link> links) { //TODO
		
			if(averageDurationPerCell<=0.0)averageDurationPerCell=1.0;
			Double normalizationFactor = linkweightUtil.getNormalizationFactor()*scalingFactor;
			// for every causing group
			for(int causing=0; causing<userGroups.length; causing++){
				logger.info("Causing group:" + userGroups[causing].toString());
				// for every caused flat emission by causing group:
				Map<Id<Link>, Double> causedFlatEmissionsByCurrentCausingGroup = causingGroupLinkFlatEmissions.getLinks2FlatEmissionsFromCausingUserGroup(userGroups[causing]);
				
				for(Id<Link> linkId: causedFlatEmissionsByCurrentCausingGroup.keySet()){
				
				// for each receiving group
					for(int receiving=0; receiving<userGroups.length; receiving++){
						
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
		for(int receiving=0; receiving<userGroups.length; receiving++){
			System.out.print("Receiving:" + userGroups[receiving].toString() + "  ");
			//Double totalReceivedValue =0.0;
			for(int causing=0; causing<userGroups.length; causing++){
				System.out.print(groupXgroupMatrix[causing][receiving] + "   ");
				//totalReceivedValue+= groupXgroupMatrix[causing][receiving];
			}
			
			System.out.println(getTotalReceivedValue(receiving));
			System.out.println();
		}
		System.out.print("Total receiving:   ");
		for(int causing=0; causing<userGroups.length; causing++){
//			Double totalCausing=0.0;
//			for(int receiving=0; receiving<userGroups.length; receiving++){
//				totalCausing+= groupXgroupMatrix[causing][receiving];
//			}
			System.out.print(getTotalCausedValue(causing) + "   ");
		}
		System.out.println();
		System.out.println("Total costs: " + getTotalCosts());
		//TODO factors received/caused
		System.out.println();
		System.out.println("Received/Causing:");
		for(int group=0; group<userGroups.length; group++){
			System.out.println(userGroups[group].toString() + " : "+ (getTotalReceivedValue(group)/getTotalCausedValue(group)));
		}
	}
	
	
	private Double getTotalCosts() {
		Double totalCosts =0.0;
		for(int causing =0; causing<userGroups.length; causing++){
			totalCosts += getTotalCausedValue(causing);
		}
		return totalCosts;
	}


	private Double getTotalReceivedValue(int receiving) {
		Double totalReceived= new Double(0.0);
		for(int causing = 0; causing<userGroups.length; causing++){
			totalReceived += groupXgroupMatrix[causing][receiving];
		}
		return totalReceived;
	}

	private Double getTotalCausedValue(int causing) {
		Double totalCaused= new Double(0.0);
		for(int receiving = 0; receiving<userGroups.length; receiving++){
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
