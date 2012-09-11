package playground.sergioo.NetworksMatcher.kernel;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.swing.JOptionPane;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.router.AStarLandmarks;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;
import org.matsim.core.router.util.PreProcessLandmarks;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.vehicles.Vehicle;

import others.sergioo.util.geometry.Functions2D;
import playground.sergioo.NetworksMatcher.kernel.core.ComposedLink;
import playground.sergioo.NetworksMatcher.kernel.core.ComposedNode;
import playground.sergioo.NetworksMatcher.kernel.core.ComposedNode.Types;
import playground.sergioo.NetworksMatcher.kernel.core.MatchingStep;
import playground.sergioo.NetworksMatcher.kernel.core.NodesMatching;
import playground.sergioo.NetworksMatcher.kernel.core.Region;

public class CrossingMatchingStep extends MatchingStep {


	//Constants

	public static final File MATCHINGS_FILE = new File("./data/matching/matchings.txt");
	
	public static File CAPACITIES_FILE = new File("./data/matching/capacities/linksChanged.txt");
	
	private static final double MAX_ANGLE_DIFFERENCE = Math.PI/3;
	
	private static final double MAX_LENGTH_FRACTION = 0.9;
	
	@SuppressWarnings("unchecked")
	public static final Tuple<Id, Id>[] NEAR_NODES_LOW = new Tuple[] {
		new Tuple<Id, Id>(new IdImpl("17686"), new IdImpl("150029")),
		new Tuple<Id, Id>(new IdImpl("153069"), new IdImpl("153061")),
		new Tuple<Id, Id>(new IdImpl("110489"), new IdImpl("15098")),
		new Tuple<Id, Id>(new IdImpl("198119"), new IdImpl("198111")),
		new Tuple<Id, Id>(new IdImpl("176231"), new IdImpl("176239")),
		new Tuple<Id, Id>(new IdImpl("121279"), new IdImpl("121271")),
		new Tuple<Id, Id>(new IdImpl("128361"), new IdImpl("128369")),
		new Tuple<Id, Id>(new IdImpl("113011"), new IdImpl("113019")),
		new Tuple<Id, Id>(new IdImpl("176409"), new IdImpl("176401")),
		new Tuple<Id, Id>(new IdImpl("144461"), new IdImpl("144469")),
		new Tuple<Id, Id>(new IdImpl("133041"), new IdImpl("10796")),
		new Tuple<Id, Id>(new IdImpl("13491"), new IdImpl("111279")),
		new Tuple<Id, Id>(new IdImpl("184239"), new IdImpl("184231")),
		new Tuple<Id, Id>(new IdImpl("123449"), new IdImpl("123441")),
		new Tuple<Id, Id>(new IdImpl("120251"), new IdImpl("120259")),
		new Tuple<Id, Id>(new IdImpl("167299"), new IdImpl("167291")),
		new Tuple<Id, Id>(new IdImpl("143829"), new IdImpl("143821")),
		new Tuple<Id, Id>(new IdImpl("168131"), new IdImpl("168139")),
		new Tuple<Id, Id>(new IdImpl("127469"), new IdImpl("127461")),
		new Tuple<Id, Id>(new IdImpl("166531"), new IdImpl("166539")),
		new Tuple<Id, Id>(new IdImpl("154309"), new IdImpl("154301")),
		new Tuple<Id, Id>(new IdImpl("10715"), new IdImpl("132079")),
		new Tuple<Id, Id>(new IdImpl("192201"), new IdImpl("192209")),
		new Tuple<Id, Id>(new IdImpl("171049"), new IdImpl("171041")),
		new Tuple<Id, Id>(new IdImpl("154349"), new IdImpl("154341")),
		new Tuple<Id, Id>(new IdImpl("148059"), new IdImpl("148051")),
		new Tuple<Id, Id>(new IdImpl("191099"), new IdImpl("191091")),
		new Tuple<Id, Id>(new IdImpl("165179"), new IdImpl("165171")),
		new Tuple<Id, Id>(new IdImpl("144749"), new IdImpl("144741")),
		new Tuple<Id, Id>(new IdImpl("149079"), new IdImpl("149071")),
		new Tuple<Id, Id>(new IdImpl("128239"), new IdImpl("128231")),
		new Tuple<Id, Id>(new IdImpl("158401"), new IdImpl("158409")),
		new Tuple<Id, Id>(new IdImpl("131139"), new IdImpl("131121")),
		new Tuple<Id, Id>(new IdImpl("159019"), new IdImpl("159011")),
		new Tuple<Id, Id>(new IdImpl("146529"), new IdImpl("146521")),
		new Tuple<Id, Id>(new IdImpl("144441"), new IdImpl("144449")),
		new Tuple<Id, Id>(new IdImpl("153181"), new IdImpl("16927")),
		new Tuple<Id, Id>(new IdImpl("196129"), new IdImpl("196121"))
	};
	
	@SuppressWarnings("unchecked")
	public static final Tuple<Id, Id>[] NEAR_NODES_HIGH = new Tuple[] {
		new Tuple<Id, Id>(new IdImpl("12499_1380029209_0"), new IdImpl("12500_1380009607_0")),
		new Tuple<Id, Id>(new IdImpl("1380019534"), new IdImpl("57073_1380019532_0")),
		new Tuple<Id, Id>(new IdImpl("1380022856"), new IdImpl("71392_1380022854_0")),
		new Tuple<Id, Id>(new IdImpl("1380023386"), new IdImpl("69854_1380023385_0")),
		new Tuple<Id, Id>(new IdImpl("1380033194"), new IdImpl("28695_1380033195_0")),
		new Tuple<Id, Id>(new IdImpl("1380034578"), new IdImpl("23989_1380034579_0")),
		new Tuple<Id, Id>(new IdImpl("1380041933"), new IdImpl("76007_1380034709_0")),
		new Tuple<Id, Id>(new IdImpl("1655_1380025784_0"), new IdImpl("1656_1380023078_0")),
		new Tuple<Id, Id>(new IdImpl("19346_1380038781_0"), new IdImpl("19347_1380038244_0")),
		new Tuple<Id, Id>(new IdImpl("25732_1380037754_1"), new IdImpl("25733_1380025451_0")),
		new Tuple<Id, Id>(new IdImpl("74819_1380016402_0"), new IdImpl("74820_1380016401_0")),
		new Tuple<Id, Id>(new IdImpl("BP1"), new IdImpl("NS4")),
		new Tuple<Id, Id>(new IdImpl("EW16/NE3"), new IdImpl("NE3")),
		new Tuple<Id, Id>(new IdImpl("NE17"), new IdImpl("PTC"))
	};
	
	
	//Attributes
	
	private double radius;
	
	private Map<Link, Tuple<Link,Double>> linksChanged = new HashMap<Link, Tuple<Link,Double>>();
	private Set<Link> linksA = new HashSet<Link>();
	private Set<Link> linksB = new HashSet<Link>();

	//Methods

	public CrossingMatchingStep(Region region, double radius, double minAngle) {
		super("Crossing matching step", region);
		nodesMatchings = new HashSet<NodesMatching>();
		this.radius = radius;
		ComposedNode.radius = radius;
		IncidentLinksNodesMatching.minAngle = minAngle;
		networkSteps.add(new NodeTypeReductionStep(region));
		internalStepPosition = 1;
	}

	public void setRadius(double radius) {
		this.radius = radius;
		ComposedNode.radius = radius;
	}

	public void setMinAngle(double minAngle) {
		IncidentLinksNodesMatching.minAngle = minAngle;
	}

	private Set<Set<Node>> getSubsetsOfSize(Set<Node> nodesSet, int n) {
		Set<Set<Node>> nodesSubsets = new HashSet<Set<Node>>();
		getSubsetsOfSize(nodesSet, n, new HashSet<Node>(), nodesSubsets);
		return nodesSubsets;
	}

	private void getSubsetsOfSize(Set<Node> nodesSet, int n, Set<Node> nodesSubset, Set<Set<Node>> nodesSubsets) {
		if(n==0) {
			Set<Node> subSet = new HashSet<Node>(nodesSubset);
			nodesSubsets.add(subSet);
		}
		else {
			Set<Node> newNodeSet = new HashSet<Node>(nodesSet);
			for(Node node:nodesSet) {
				nodesSubset.add(node);
				newNodeSet.remove(node);
				getSubsetsOfSize(newNodeSet, n-1, nodesSubset, nodesSubsets);
				nodesSubset.remove(node);
			}
		}
	}
	
	@Override
	public void process(Network oldNetworkA, Network oldNetworkB) {
		int res = JOptionPane.showConfirmDialog(null,"Do you want to load the matchings?");
		if(res == JOptionPane.YES_OPTION) {
			loadNodesMatchings();
		}
		else {
			Set<Node> alreadyReducedA = new HashSet<Node>();
			Set<Node> alreadyReducedB = new HashSet<Node>();
			List<Node> sortedNodes = sortNodesA(); 
			for(Node node:sortedNodes)
				SEARCH_MATCH:
					if(region.isInside(node) && ((ComposedNode)node).getType().equals(Types.CROSSING) && !alreadyReducedA.contains(node)) {
						Set<Node> nearestNodesToA = new HashSet<Node>();
						for(Node nodeA:networkA.getNodes().values())
							if(((ComposedNode)nodeA).getType().equals(Types.CROSSING) && ((CoordImpl)node.getCoord()).calcDistance(nodeA.getCoord())<radius && !alreadyReducedA.contains(nodeA))
								nearestNodesToA.add(nodeA);
						Set<Node> nearestNodesToB = new HashSet<Node>();
						for(Node nodeB:networkB.getNodes().values())
							if(((ComposedNode)nodeB).getType().equals(Types.CROSSING) && ((CoordImpl)node.getCoord()).calcDistance(nodeB.getCoord())<radius && !alreadyReducedB.contains(nodeB))
								nearestNodesToB.add(nodeB);
						for(int n=1; n<=nearestNodesToA.size(); n++) {
							Set<Set<Node>> nodesSubsetsA = getSubsetsOfSize(nearestNodesToA, n);
							for(Set<Node> nodeSubsetA:nodesSubsetsA)
								for(int m=nearestNodesToB.size(); m>=1; m--) {
									Set<Set<Node>> nodesSubsetsB = getSubsetsOfSize(nearestNodesToB, m);
									for(Set<Node> nodeSubsetB:nodesSubsetsB) {
										IncidentLinksNodesMatching nodesMatching = null;
										try {
											nodesMatching = new IncidentLinksNodesMatching(nodeSubsetA, nodeSubsetB);
										} catch (Exception e) {
											//Not possible
										}
										if(nodesMatching.linksAnglesMatches()) {
											nodesMatchings.add(nodesMatching);
											for(Node nodeMatched:nodeSubsetA)
												alreadyReducedA.add(nodeMatched);
											for(Node nodeMatched:nodeSubsetB)
												alreadyReducedB.add(nodeMatched);
											break SEARCH_MATCH;
										}
									}
								}
						}
					}
		}
		/*LayersWindow windowHR2 = new DoubleNetworkMatchingWindow("Networks reduced", networkA, networkB, nodesMatchings);
		windowHR2.setVisible(true);
		while(!windowHR2.isReadyToExit())
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}*/
		loadSimpleLinks();
		applyCapacitiesSimples();
		JOptionPane.showMessageDialog(null,"Simples done!");
		TravelDisutility travelMinCost = new TravelDisutility() {
			@Override
			public double getLinkTravelDisutility(final Link link, final double time, final Person person, final Vehicle vehicle) {
				return getLinkMinimumTravelDisutility(link);
			}
			@Override
			public double getLinkMinimumTravelDisutility(Link link) {
				return link.getLength();
			}
		};
		TravelTime timeFunction = new TravelTime() {	


			@Override
			public double getLinkTravelTime(Link link, double time, Person person, Vehicle vehicle) {
				return link.getLength();
			}
		};
		PreProcessLandmarks preProcessData = new PreProcessLandmarks(travelMinCost);
		preProcessData.run(networkB);
		AStarLandmarks aStarLandmarks = new AStarLandmarks(networkB, preProcessData, timeFunction);
		applyCapacitiesASimple(aStarLandmarks);
		JOptionPane.showMessageDialog(null,"SimplesA done!");
		preProcessData = new PreProcessLandmarks(travelMinCost);
		preProcessData.run(networkA);
		aStarLandmarks = new AStarLandmarks(networkA, preProcessData, timeFunction);
		applyCapacitiesBSimple(aStarLandmarks);
		JOptionPane.showMessageDialog(null,"SimplesB done!");
		try {
			PrintWriter writer = new PrintWriter(CAPACITIES_FILE);
			for(Entry<Link, Tuple<Link,Double>> linkE:linksChanged.entrySet())
				writer.println(linkE.getKey().getId()+":::"+linkE.getValue().getFirst().getId()+":::"+linkE.getValue().getSecond());
			writer.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		networkSteps.add(new CrossingReductionStep(region, nodesMatchings));
		networkSteps.add(new EdgeDeletionStep(region));
	}
	
	private void loadSimpleLinks() {
		try {
			BufferedReader bufferedReader = new BufferedReader(new FileReader("./data/matching/capacities/simpleLinksA.txt"));
			String line = bufferedReader.readLine();
			while(line!=null) {
				linksA.add(networkA.getLinks().get(new IdImpl(line)));
				line = bufferedReader.readLine();
			}
			bufferedReader.close();
			bufferedReader = new BufferedReader(new FileReader("./data/matching/capacities/simpleLinksB.txt"));
			line = bufferedReader.readLine();
			while(line!=null) {
				linksB.add(networkB.getLinks().get(new IdImpl(line)));
				line = bufferedReader.readLine();
			}
			bufferedReader.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void loadNodesMatchings() {
		try {
			nodesMatchings.clear();
			BufferedReader reader = new BufferedReader(new FileReader(MATCHINGS_FILE));
			String line = reader.readLine();
			while(line!=null) {
				NodesMatching nodesMatching = NodesMatching.parseNodesMatching(line, networkA, networkB);
				if(nodesMatching!=null)
					nodesMatchings.add(nodesMatching);
				line = reader.readLine();
			}
			reader.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private void applyCapacitiesSimples() {
		for(Link linkA: linksA) {
			NodesMatching nodesMatchingFrom = null, nodesMatchingTo = null;
			FROM_NODE:
				for(NodesMatching nodesMatching:nodesMatchings)
					for(Node nodeA:nodesMatching.getComposedNodeA().getNodes())
						if(linkA.getFromNode().getId().equals(nodeA.getId())) {
							nodesMatchingFrom = nodesMatching;
							break FROM_NODE;
						}
			TO_NODE:
				for(NodesMatching nodesMatching:nodesMatchings)
					for(Node nodeA:nodesMatching.getComposedNodeA().getNodes())
						if(linkA.getToNode().getId().equals(nodeA.getId())) {
							nodesMatchingTo = nodesMatching;
							break TO_NODE;
						}
			for(Node nodeFrom:nodesMatchingFrom.getComposedNodeB().getNodes())
				for(Node nodeTo:nodesMatchingTo.getComposedNodeB().getNodes())
					for(Link linkB: linksB)
						if(linkB.getFromNode().getId().equals(nodeFrom.getId()) && linkB.getToNode().getId().equals(nodeTo.getId()))
							applyCapacities(((ComposedLink)linkA).getLinks(), ((ComposedLink)linkB).getLinks());
		}
	}
	
	private void applyCapacitiesASimple(AStarLandmarks aStarLandmarks) {
		for(Link linkA:linksA) {
			NodesMatching nodesMatchingFrom = null, nodesMatchingTo = null;
			FROM_NODE:
				for(NodesMatching nodesMatching:nodesMatchings)
					for(Node nodeA:nodesMatching.getComposedNodeA().getNodes())
						if(linkA.getFromNode().getId().equals(nodeA.getId())) {
							nodesMatchingFrom = nodesMatching;
							break FROM_NODE;
						}
			TO_NODE:
				for(NodesMatching nodesMatching:nodesMatchings)
					for(Node nodeA:nodesMatching.getComposedNodeA().getNodes())
						if(linkA.getToNode().getId().equals(nodeA.getId())) {
							nodesMatchingTo = nodesMatching;
							break TO_NODE;
						}
			for(Node nodeFrom:nodesMatchingFrom.getComposedNodeB().getNodes())
				for(Node nodeTo:nodesMatchingTo.getComposedNodeB().getNodes()) {
					List<Link> linksTo = new ArrayList<Link>();
					Path path = aStarLandmarks.calcLeastCostPath(nodeFrom, nodeTo, 0, null, null);
					if(path!=null && path.links.size()>0) {
						double pathLength = 0;
						for(Link link:path.links)
							pathLength+=ComposedLink.getLength(link);
						if(Functions2D.getAnglesDifference(ComposedLink.getAngle(path.links.get(0)),ComposedLink.getAngle(linkA))<MAX_ANGLE_DIFFERENCE &&
								Functions2D.getAnglesDifference(ComposedLink.getAngle(path.links.get(path.links.size()-1)),ComposedLink.getAngle(linkA))<MAX_ANGLE_DIFFERENCE &&
								pathLength<(1+MAX_LENGTH_FRACTION)*ComposedLink.getLength(linkA) && pathLength>(1-MAX_LENGTH_FRACTION)*ComposedLink.getLength(linkA)) {
							for(Link linkPath:path.links)
								linksTo.addAll(((ComposedLink)linkPath).getLinks());
							applyCapacities(((ComposedLink)linkA).getLinks(), linksTo);
						}
					}
				}
		}
	}
	
	private void applyCapacitiesBSimple(AStarLandmarks aStarLandmarks) {
		for(Link linkB:linksB) {
			NodesMatching nodesMatchingFrom = null, nodesMatchingTo = null;
			FROM_NODE:
				for(NodesMatching nodesMatching:nodesMatchings)
					for(Node nodeB:nodesMatching.getComposedNodeB().getNodes())
						if(linkB.getFromNode().getId().equals(nodeB.getId())) {
							nodesMatchingFrom = nodesMatching;
							break FROM_NODE;
						}
			TO_NODE:
				for(NodesMatching nodesMatching:nodesMatchings)
					for(Node nodeB:nodesMatching.getComposedNodeB().getNodes())
						if(linkB.getToNode().getId().equals(nodeB.getId())) {
							nodesMatchingTo = nodesMatching;
							break TO_NODE;
						}
			for(Node nodeFrom:nodesMatchingFrom.getComposedNodeA().getNodes())
				for(Node nodeTo:nodesMatchingTo.getComposedNodeA().getNodes()) {
					List<Link> linksFrom = new ArrayList<Link>();
					Path path = aStarLandmarks.calcLeastCostPath(nodeFrom, nodeTo, 0, null, null);
					if(path!=null && path.links.size()>0) {
						double pathLength = 0;
						for(Link link:path.links)
							pathLength+=ComposedLink.getLength(link);
						if(Functions2D.getAnglesDifference(ComposedLink.getAngle(path.links.get(0)),ComposedLink.getAngle(linkB))<MAX_ANGLE_DIFFERENCE &&
								Functions2D.getAnglesDifference(ComposedLink.getAngle(path.links.get(path.links.size()-1)),ComposedLink.getAngle(linkB))<MAX_ANGLE_DIFFERENCE &&
								pathLength<(1+MAX_LENGTH_FRACTION)*ComposedLink.getLength(linkB) && pathLength>(1-MAX_LENGTH_FRACTION)*ComposedLink.getLength(linkB)) {
							for(Link linkPath:path.links)
								linksFrom.addAll(((ComposedLink)linkPath).getLinks());
							applyCapacities(linksFrom, ((ComposedLink)linkB).getLinks());
						}
					}
				}
		}
	}
	
	private void applyCapacities(List<Link> linksFrom, List<Link> linksTo) {
		for(Link linkTo:linksTo) {
			if(!linksChanged.containsKey(linkTo)) {
				double shortestDistance=Double.MAX_VALUE;
				Link nearest = linksFrom.iterator().hasNext()?linksFrom.iterator().next():null;
				for(Link linkFrom:linksFrom) {
					double distance = CoordUtils.calcDistance(nearest.getCoord(), linkFrom.getCoord());
					if(distance<shortestDistance && nearest.getCapacity()!=0) {
						shortestDistance = distance;
						nearest = linkFrom;
					}
				}
				if(nearest!=null && nearest.getCapacity()!=0) {
					linksChanged.put(linkTo, new Tuple<Link,Double>(nearest,nearest.getCapacity()));
					//System.out.println(linksChanged.size());
				}
			}
		}
	}

	private List<Node> sortNodesA() {
		List<Node> sortedNodes = new ArrayList<Node>(networkA.getNodes().values());
		for(int i=0; i<sortedNodes.size()-1; i++) {
			Node node = sortedNodes.get(i);
			for(int j=i+1; j<sortedNodes.size(); j++)
				if(((ComposedNode)node).getAnglesDeviation()>((ComposedNode)sortedNodes.get(j)).getAnglesDeviation()) {
					sortedNodes.set(i, sortedNodes.get(j));
					sortedNodes.set(j, node);
					node = sortedNodes.get(i);
				}
		}
		return sortedNodes;
	}

}
