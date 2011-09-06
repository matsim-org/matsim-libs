/*******************************************************************************
 * Copyright (C) 2011 Stefan Schršder.
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
/**
 * 
 */
package vrp.basics;

import java.util.ArrayList;
import java.util.Collection;

import vrp.api.Constraints;
import vrp.api.Costs;
import vrp.api.Customer;
import vrp.api.Node;
import vrp.api.VRP;


/**
 * @author stefan schroeder
 *
 */
public class VRPDistanceMatrixBuilder {
	
	private double[][] distanceMatrix;
	
	private Nodes nodes = new Nodes();
	
	private String depotId = null;
	
	private Constraints constraints;
	
	private NodeFactory nodeFactory;
	
	private boolean depotNodeSet = false;
	
	private boolean distanceMatrixSet = false;
	
	private Collection<Customer> customers = new ArrayList<Customer>();

	private Customer depot = null;
	
	public VRPDistanceMatrixBuilder(NodeFactory nodeFactory) {
		super();
		this.nodeFactory = nodeFactory;
	}

	public void setDistanceMatrix(double[][] distanceMatrix){
		this.distanceMatrix = distanceMatrix;
		distanceMatrixSet = true;
	}
	
	public Node createAndAddNode(String id, int matrixId){
		Node node = nodeFactory.createNode(id, matrixId);
		nodes.getMatrixIdNodeMap().put(matrixId, node);
		nodes.getNodes().put(id, node);
		return node;
	}
	
	public Customer createAndAddCustomer(String id, Node node, int demand){
		Customer customer = createCustomer(id, node, demand, 0.0, 24*3600, 0.0);
		customers.add(customer);
		return customer;
	}
	
	public Customer createCustomer(String id, Node node, int demand, double start, double end, double serviceTime){
		Customer customer = new CustomerImpl(id, node);
		customer.setDemand(demand);
		customer.setServiceTime(serviceTime);
		customer.setTheoreticalTimeWindow(start, end);
		return customer;
	}
	
	public Customer createAndAddCustomer(String id, Node node, int demand, double start, double end, double serviceTime){
		Customer customer = createCustomer(id, node, demand, start, end, serviceTime);
		customers.add(customer);
		return customer;
	}
	
//	public Relationship createAndAddRelationship(Node fromNode, Node toNode){
//		Relationship rel = new Relationship(toNode);
//		toNode.setRelationship(rel);
//		return rel;
//	}
	
	public void setDepot(String depotId){
		this.depotId = depotId;
		depotNodeSet = true;
	}
	
	
	public void setConstraints(Constraints contraints){
		this.constraints = contraints;
	}
	
	public VRP build(){
		if(depotNodeSet && distanceMatrixSet){
			Costs costs = new LowerTriangularDistanceMatrix(distanceMatrix);
			VRP vrp = new VrpImpl(depotId, customers, costs, constraints);
			return vrp;
		}
		throw new IllegalStateException("some error occured"); 
	}

	

}
