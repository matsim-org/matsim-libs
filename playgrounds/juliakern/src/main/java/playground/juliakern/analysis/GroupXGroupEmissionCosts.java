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

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;

import playground.benjamin.scenarios.munich.analysis.filter.UserGroup;
import playground.benjamin.scenarios.munich.analysis.spatialAvg.Cell;
import playground.benjamin.scenarios.munich.analysis.spatialAvg.LinkWeightUtil;
import playground.benjamin.scenarios.munich.analysis.spatialAvg.SpatialGrid;

public class GroupXGroupEmissionCosts {
	
	Double [][] groupXgroupMatrix;
	Map <UserGroup, Integer> userGroupToMatrixIndex;
	private UserGroup[] userGroups;

	public GroupXGroupEmissionCosts(){
		userGroups = UserGroup.values();
		groupXgroupMatrix = new Double[userGroups.length][userGroups.length];
		initializeMatrix(groupXgroupMatrix);
		userGroupToMatrixIndex = new HashMap<UserGroup, Integer>();
		int i=0;
		for(UserGroup ug: userGroups){
			userGroupToMatrixIndex.put(ug, i);
			i++;
		}
	}


	public void calculateGroupCosts(
			GroupLinkFlatEmissions causingGroupLinkFlatEmissions,
			Map<UserGroup, SpatialGrid> mapOfDurations, LinkWeightUtil linkweightUtil,
			Double averageDurationPerCell, Map<Id<Link>, ? extends Link> links) { //TODO
		
			if(averageDurationPerCell<=0.0)averageDurationPerCell=1.0;
			Double normalizationFactor = linkweightUtil.getNormalizationFactor();
			// for every causing group
			for(UserGroup causingGroup: userGroups){
			
				// for every caused flat emission by causing group:
				Map<Id<Link>, Double> causedFlatEmissionsByCurrentCausingGroup = causingGroupLinkFlatEmissions.getLinks2FlatEmissionsFromCausingUserGroup(causingGroup);
				
				for(Id<Link> linkId: causedFlatEmissionsByCurrentCausingGroup.keySet()){
				
				// for each receiving group
				for(UserGroup receivingGroup: userGroups){ 
					// go through all cells
					SpatialGrid durationsOfReceivingGroup = mapOfDurations.get(receivingGroup);
					for(Cell cell: durationsOfReceivingGroup.getCells()){
						// calc weight of link for that cell
						Double weightOfLinkForCell = linkweightUtil.getWeightFromLink(links.get(linkId), cell.getCentroid())*normalizationFactor;		
						// calc 'duration density'
						Double durationDensity = cell.getWeightedValue()/averageDurationPerCell;	
						// emission cost from causing group to receiving group 
						// = duration density x flat emission cost x link weight
						Double causedEmissionCost = weightOfLinkForCell*durationDensity*causedFlatEmissionsByCurrentCausingGroup.get(linkId);
						// add to matrix
						groupXgroupMatrix[userGroupToMatrixIndex.get(causingGroup)][userGroupToMatrixIndex.get(receivingGroup)]+= causedEmissionCost;
					}
				}
			}
			}
	}

	public void print() {
		System.out.println("     ");
			for(UserGroup causing: userGroups){
				System.out.print(causing.toString() + "   ");
			}
			System.out.println();
			for(UserGroup receiving: userGroups){
				System.out.println(receiving.toString());
				for(UserGroup causing: userGroups){
					System.out.print(groupXgroupMatrix[userGroupToMatrixIndex.get(causing)][userGroupToMatrixIndex.get(receiving)] + "   ");
				}
			}
		
	}
	
	
	private void initializeMatrix(Double[][] groupXgroupMatrix2) {
		for(int i=0; i<groupXgroupMatrix2.length; i++){
			for(int j=0; j<groupXgroupMatrix2[i].length; j++){
				groupXgroupMatrix2[i][j]=new Double(0.0);
			}
		}
		
	}

}
