/*******************************************************************************
 * Copyright (C) 2011 Stefan Schroeder.
 * eMail: stefan.schroeder@kit.edu
 * 
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 * 
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 * 
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package vrp.basics;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;

import vrp.api.Costs;
import vrp.api.Node;


/**
 * 
 * @author stefan schroeder
 *
 */

public class CostsImpl implements Costs{

	private static class CostMatrixKey {
		private String fromNodeId;
		private String toNodeId;
		
		public CostMatrixKey(String fromNodeId, String toNodeId) {
			super();
			this.fromNodeId = fromNodeId;
			this.toNodeId = toNodeId;
		}
		
		@Override
		public boolean equals(Object obj) {
			CostMatrixKey other = (CostMatrixKey) obj;
			return fromNodeId.equals(other.fromNodeId) && toNodeId.equals(other.toNodeId);
		}

		@Override
		public int hashCode() {
			return fromNodeId.hashCode() + toNodeId.hashCode();
		}
		
	}
	
	private static class CostValues {
		private double time;
		private double distance;
		private double cost;
		
		public CostValues(double time, double distance, double cost) {
			super();
			this.time = time;
			this.distance = distance;
			this.cost = cost;
		}
		
		public double getTime() {
			return time;
		}
		
		public double getDistance() {
			return distance;
		}
		
		public double getCost() {
			return cost;
		}
		
	}
	
	private static Logger logger = Logger.getLogger(CostsImpl.class);
	
	private Map<CostMatrixKey,CostValues> costMatrix = new HashMap<CostMatrixKey, CostValues>();
	
	@Override
	public Double getCost(Node from, Node to) {
		CostMatrixKey costMatrixKey = new CostMatrixKey(from.getId(), to.getId());
		if(costMatrix.containsKey(costMatrixKey)){
			return costMatrix.get(costMatrixKey).getCost();
		}
		else{
			if(from.getId().equals(to.getId())){
				return 0.0;
			}
			else{
				throw new IllegalStateException("no cost data availabe for " + costMatrixKey);
			}
			
		}
	}

	@Override
	public Double getDistance(Node from, Node to) {
		CostMatrixKey costMatrixKey = new CostMatrixKey(from.getId(), to.getId());
		if(costMatrix.containsKey(costMatrixKey)){
			return costMatrix.get(costMatrixKey).getDistance();
		}
		else{
			if(from.getId().equals(to.getId())){
				return 0.0;
			}
			else{
				throw new IllegalStateException("no cost data availabe for " + costMatrixKey);
			}
		}
	}

	@Override
	public Double getTime(Node from, Node to) {
		CostMatrixKey costMatrixKey = new CostMatrixKey(from.getId(), to.getId());
		if(costMatrix.containsKey(costMatrixKey)){
			return costMatrix.get(costMatrixKey).getTime();
		}
		else{
			if(from.getId().equals(to.getId())){
				return 0.0;
			}
			else{
				throw new IllegalStateException("no cost data availabe for " + costMatrixKey);
			}
		}
	}
	
	public void addCostValues(String from, String to, Double time, Double distance, Double cost){
		CostMatrixKey costMatrixKey = new CostMatrixKey(from,to);
		CostValues costValues = new CostValues(time,distance,cost);
		if(!costMatrix.containsKey(costMatrixKey)){
			costMatrix.put(costMatrixKey, costValues);
		}
		else{
			logger.warn("key already exist " + costMatrixKey + " and has been overwrited");
			costMatrix.put(costMatrixKey, costValues);
		}
	}
}
