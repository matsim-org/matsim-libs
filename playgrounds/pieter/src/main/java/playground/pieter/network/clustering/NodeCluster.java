package playground.pieter.network.clustering;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.network.LinkImpl;

/**
 * @author fouriep
 *
 */
/**
 * @author fouriep
 * 
 */
public class NodeCluster{
	private NodeClusteringAlgorithm algo;
	private int clusterStep;
	private int id;
	private double internalFlow = Double.NaN;
	private Map<Id, ClusterLink> outLinks;
	private Map<Id, ClusterLink> inLinks;
	private Map<Id, ClusterLink> interLinks;
	private Map<Id, ClusterNode> nodes;
	private ClusterNode leafNode;
	private NodeCluster child1;
	private NodeCluster child2;
	private NodeCluster parent = null;
	private boolean isLeaf = false;
	private double deltaFlow;
	private double inFlowSum;
	private double outFlowSum;
	private HashSet<NodeCluster> outBoundClusters;
	private LinkedHashMap<Id, ClusterLink> newInterlinks;
	private static long invocations =0;

	//
	//
	/**
	 * at its most basic, we start at step 0, with only a node as a child. at
	 * this point, all the node's links need to be converted to clusterlinks, so
	 * they can be associated with this cluster.
	 * 
	 * @param n
	 *            the node being converted
	 * @param algorithm
	 * @param linkMethod 
	 * @param args 
	 */
	public NodeCluster(ClusterNode n, NodeClusteringAlgorithm algorithm, 
			int clusterStep, int id, Method linkMethod, Object[] args) {
		this.id = id;
		this.clusterStep = clusterStep;
		isLeaf = true;
		this.algo = algorithm;
		leafNode = n;
		inLinks = new LinkedHashMap<Id, ClusterLink>(n.getInLinks().size());
		outLinks = new LinkedHashMap<Id, ClusterLink>(n.getOutLinks().size());
		this.nodes = new LinkedHashMap<Id, ClusterNode>();
		this.nodes.put(n.getId(), n);
		for (Link l : n.getInLinks().values()) {
			inLinks.put(l.getId(), algo.getLinks().get(l.getId()));
			// the links don't know yet who's their daddy
//			 inLinks.get(l.getId()).setNewRoot(this, false);
			inLinks.get(l.getId()).setToCluster(this);
		}
		for (Link l : n.getOutLinks().values()) {
			outLinks.put(l.getId(), algo.getLinks().get(l.getId()));
			// outLinks.get(l.getId()).setNewRoot(this, false);
			outLinks.get(l.getId()).setFromCluster(this);
		}
		// by definition, there is no flow internal to a point
		internalFlow = 0;
//		setInFlowSum(linkMethod, args);
//		setOutFlowSum(linkMethod, args);
		setInvocations(getInvocations() + 1);
	}

	public NodeCluster(NodeCluster nc1, NodeCluster nc2, Method linkMethod,
			Object[] args, int clusterStep, int id) {
		
		isLeaf = false;
		this.clusterStep = clusterStep;
		this.id = id;
		this.child1 = nc1;
		this.child2 = nc2;
		inLinks = new LinkedHashMap<Id, ClusterLink>();
		outLinks = new LinkedHashMap<Id, ClusterLink>();
		interLinks = new LinkedHashMap<Id, ClusterLink>();
		nodes = new LinkedHashMap<Id, ClusterNode>(nc1.getNodes().size()
				+ nc2.getNodes().size(), 1f);
		// first add all in and out links, then subtract them using the set of
		// interlinks
		inLinks.putAll(nc1.getInLinks());
		inLinks.putAll(nc2.getInLinks());
		outLinks.putAll(nc1.getOutLinks());
		outLinks.putAll(nc2.getOutLinks());
		nodes.putAll(nc1.getNodes());
		nodes.putAll(nc2.getNodes());
		// first add all the existing interLinks for the two NodeClusters, but
		// only if they are not leaves
		if (!nc1.isLeaf)
			interLinks.putAll(nc1.getInterLinks());
		if (!nc2.isLeaf)
			interLinks.putAll(nc2.getInterLinks());
		newInterlinks = findInterLInks(nc1, nc2);
		interLinks.putAll(newInterlinks);
//		removeInterLinksFromOtherLinkMaps();
		setInternalFlow(nc1, nc2, linkMethod, args,newInterlinks);
		setDeltaFlow();
//		setInFlowSum(linkMethod, args);
//		setOutFlowSum(linkMethod, args);
		setInvocations(getInvocations() + 1);
	}

	/**
	 * Sets the change in flow produced by creation of this cluster, due to the
	 * flows of the interlinks between immediate child clusters
	 */
	private void setDeltaFlow() {
		deltaFlow = this.getInternalFlow() - this.getChild1().getInternalFlow()
				- this.getChild2().getInternalFlow();
	}

	/**
	 * This method also adds the set of links connecting the two clusters to the
	 * interlinks map
	 * 
	 * @param nc1
	 *            the first NodeCluster
	 * @param nc2
	 *            the second NodeCluster
	 */
	private LinkedHashMap<Id, ClusterLink> findInterLInks(NodeCluster nc1, NodeCluster nc2) {
		LinkedHashMap<Id, ClusterLink> newInterlinks = new LinkedHashMap<Id, ClusterLink>();
		for (ClusterLink l : nc1.getInLinks().values()) {
			if (nc2.getOutLinks().get(l.getId()) != null) {
//				interLinks.put(l.getId(), l);
				newInterlinks.put(l.getId(), l);
			}
		}
		for (ClusterLink l : nc2.getInLinks().values()) {
			if (nc1.getOutLinks().get(l.getId()) != null) {
//				interLinks.put(l.getId(), l);
				newInterlinks.put(l.getId(), l);
			}
		}
		return newInterlinks;
	}

	/**
	 * Checks if any newly formed interlinks are in the outerlink or innerlink
	 * collections
	 * @param newInterlinks 
	 */
	public void removeInterLinksFromOtherLinkMaps() {
		for (ClusterLink l : newInterlinks.values()) {
//			if (newInterlinks.get(l.getId()) != null) {
				inLinks.remove(l.getId());
//			}
//			if (newInterlinks.get(l.getId()) != null) {
				outLinks.remove(l.getId());
//			}
		}
	}

	/**
	 * Method called that finds any internal sum for the linkCluster, given by
	 * the link method supplied. The sum should be castable to double.
	 * <p>
	 * This method is called after the cluster has been initialized, and is more
	 * expensive than the {@link #getinternalFlow()} method, as it is recursive
	 * on all the clusters in clusters.
	 * 
	 * @param nc1
	 *            the first NodeCluster
	 * @param nc2
	 *            the second NodeCluster
	 * @param linkMethodAndParameters
	 *            an array of strings, starting with the link method name, and
	 *            its required parameters
	 * @return the internal sum, cast to double
	 * @throws InvocationTargetException
	 * @throws IllegalArgumentException
	 * @throws IllegalAccessException
	 */
	public double findInternalSum(NodeCluster nc1, NodeCluster nc2,
			Method linkMethod, Object[] args) {
		// only call if internal flow has not yet been calculated
		double result = 0;
		for (Link l : this.interLinks.values()) {
			try {
				result += (Double) linkMethod.invoke(l, args);
			} catch (IllegalAccessException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IllegalArgumentException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (InvocationTargetException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return result;
	}

	/**
	 * Method called that finds the internal flow for the linkCluster as a
	 * permanent attribute, given by the link method supplied. The flow should
	 * be castable to double.
	 * <p>
	 * 
	 * @param nc1
	 *            the first NodeCluster
	 * @param nc2
	 *            the second NodeCluster
	 * @param newInterlinks 
	 * @param linkMethodAndParameters
	 *            an array of strings, starting with the link method name, and
	 *            its required parameters
	 * @throws InvocationTargetException
	 * @throws IllegalArgumentException
	 * @throws IllegalAccessException
	 */
	private void setInternalFlow(NodeCluster nc1, NodeCluster nc2,
			Method linkMethod, Object[] args, LinkedHashMap<Id,ClusterLink> newInterlinks) {
		// only call if internal flow has not yet been calculated
		if (!Double.isNaN(getInternalFlow()))
			return;
		double result = nc1.getInternalFlow()+nc2.getInternalFlow();
//		if (this.interLinks.size() > 0) {
			for (Link l :newInterlinks.values()) {
				try {
					result += (Double) linkMethod.invoke(l, args);
				} catch (IllegalAccessException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IllegalArgumentException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (InvocationTargetException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
//		}
		this.internalFlow = result;
	}



	public void setInFlowSum(Method linkMethod, Object[] args) {
		double result = 0;
		for (ClusterLink l : this.getInLinks().values()) {
			try {
				result += (Double) linkMethod.invoke(l, args);
			} catch (IllegalAccessException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IllegalArgumentException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (InvocationTargetException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		this.inFlowSum = result;

	}

	public void setOutFlowSum(Method linkMethod, Object[] args) {
		double result = 0;
		for (ClusterLink l : this.getOutLinks().values()) {
			try {
				result += (Double) linkMethod.invoke(l, args);
			} catch (IllegalAccessException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IllegalArgumentException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (InvocationTargetException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		this.outFlowSum = result;

	}


	public Map<Id, ClusterLink> getInLinks() {
		return this.inLinks;
	}

	public Map<Id, ClusterLink> getOutLinks() {
		return this.outLinks;
	}

	public Map<Id, ClusterLink> getInterLinks() {
		return this.interLinks;
	}

	public int getClusterStep() {
		return clusterStep;
	}

	public void setClusterStep(int clusterStep) {
		this.clusterStep = clusterStep;
	}

	public NodeClusteringAlgorithm getAlgo() {
		return algo;
	}

	public int getId() {
		return id;
	}

	public double getInternalFlow() {
		return internalFlow;
	}

	public Map<Id, ClusterNode> getNodes() {
		return nodes;
	}

	public ClusterNode getLeafNode() {
		return leafNode;
	}

	public NodeCluster getChild1() {
		return child1;
	}

	public NodeCluster getChild2() {
		return child2;
	}

	public boolean isLeaf() {
		return isLeaf;
	}

	public void setId(int myId) {
		this.id = myId;
	}

	public NodeCluster getParent() {
		return parent;
	}

	public void setParent(NodeCluster parent) {
		this.parent = parent;
	}

	public double getDeltaFlow() {
		return deltaFlow;
	}

	public HashSet<NodeCluster> getOutBoundClusters(){
		//check first if this set has been calculated yet
//		if(this.outBoundClusters == null){
			this.outBoundClusters = new HashSet<NodeCluster> ();
			for(ClusterLink l:this.outLinks.values()){
//				if(l.getToCluster()==null)
//					continue;
				outBoundClusters.add(l.getToCluster());
			}
//		}
		return this.outBoundClusters;
		
	}
	

	
	
	public double getInFlowSum() {
		// TODO Auto-generated method stub
		return this.inFlowSum;
	}

	public double getOutFlowSum() {
		// TODO Auto-generated method stub
		return this.outFlowSum;
	}
	
	public String toString(){
		
		return  this.getId()+
				(isLeaf ? "" : 
					("["+this.getChild1()+","+this.getChild2()+"]"))
					;
		
	}

	public static long getInvocations() {
		return invocations;
	}

	private static void setInvocations(long invocations) {
		NodeCluster.invocations = invocations;
	}
}
