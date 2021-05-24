package org.matsim.guice;

import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.TypeLiteral;
import com.google.inject.grapher.AbstractInjectorGrapher;
import com.google.inject.grapher.BindingEdge;
import com.google.inject.grapher.DependencyEdge;
import com.google.inject.grapher.Edge;
import com.google.inject.grapher.ImplementationNode;
import com.google.inject.grapher.InstanceNode;
import com.google.inject.grapher.InterfaceNode;
import com.google.inject.grapher.Node;
import com.google.inject.grapher.NodeId;
import org.jgrapht.Graph;
import org.jgrapht.Graphs;
import org.jgrapht.alg.connectivity.ConnectivityInspector;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.MaskSubgraph;
import org.matsim.analysis.IterationStopWatch;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.controler.ControlerI;
import org.matsim.core.controler.MatsimServices;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.corelisteners.DumpDataAtEnd;
import org.matsim.core.controler.corelisteners.EventsHandling;
import org.matsim.core.controler.corelisteners.PlansDumping;
import org.matsim.core.mobsim.framework.listeners.MobsimListener;
import org.matsim.core.replanning.ReplanningContext;
import org.matsim.core.router.SingleModeNetworksCache;
import org.matsim.core.scoring.ExperiencedPlansService;
import org.matsim.core.trafficmonitoring.TravelTimeCalculator;
import org.matsim.facilities.ActivityFacilities;

import java.io.PrintWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class MatsimGrapher extends AbstractInjectorGrapher {

	private Graph<Node, Edge> g;
	private final Map<NodeId, Node> nodes = new HashMap<>();
	private Writer writer;

	public MatsimGrapher(GrapherParameters options, Writer writer) {
		super(options);
		this.writer = writer;
		g = new DefaultDirectedGraph<>(Edge.class);
	}

	@Override
	protected void reset() {
		g = new DefaultDirectedGraph<>(Edge.class);
		nodes.clear();
	}

	@Override
	protected void newInterfaceNode(InterfaceNode node) {
		g.addVertex(node);
		nodes.put(node.getId(), node);
	}

	@Override
	protected void newImplementationNode(ImplementationNode node) {
		g.addVertex(node);
		nodes.put(node.getId(), node);
	}

	@Override
	protected void newInstanceNode(InstanceNode node) {
		g.addVertex(node);
		nodes.put(node.getId(), node);
	}

	@Override
	protected void newDependencyEdge(DependencyEdge edge) {
		g.addEdge(nodes.get(edge.getFromId()), nodes.get(edge.getToId()), edge);
	}

	@Override
	protected void newBindingEdge(BindingEdge edge) {
		g.addEdge(nodes.get(edge.getFromId()), nodes.get(edge.getToId()), edge);
	}

	@Override
	protected void postProcess() {
		// More or less arbitrarily filter out graph nodes that I think are unhelpful clutter, like config
		// groups, scenario elements, and things that aren't usually overridden.
		Graph<Node, Edge> filteredGraph = filterGraph();

		Graph<Node, Edge> graphComponentReachableFromControler = findGraphComponentReachableFromControler(filteredGraph);

		// Render a dot file. Can be converted to a PDF with
		//     dot -Tpdf modules.dot > modules.pdf
		GraphvizRenderer graphvizRenderer = new GraphvizRenderer();
		graphvizRenderer.setRankdir("LR");
		graphvizRenderer.setOut((PrintWriter) writer);
		graphvizRenderer.render(graphComponentReachableFromControler);
	}

	private Graph<Node, Edge> filterGraph() {
		return new MaskSubgraph<>(g, node -> {
			if (ConfigGroup.class.isAssignableFrom(node.getId().getKey().getTypeLiteral().getRawType())) {
				return true;
			}
			if (Network.class.isAssignableFrom(node.getId().getKey().getTypeLiteral().getRawType())) {
				return true;
			}
			if (Population.class.isAssignableFrom(node.getId().getKey().getTypeLiteral().getRawType())) {
				return true;
			}
			if (DumpDataAtEnd.class.isAssignableFrom(node.getId().getKey().getTypeLiteral().getRawType())) {
				return true;
			}
			if (OutputDirectoryHierarchy.class.isAssignableFrom(node.getId().getKey().getTypeLiteral().getRawType())) {
				return true;
			}
			if (MatsimServices.class.isAssignableFrom(node.getId().getKey().getTypeLiteral().getRawType())) {
				return true;
			}
			if (Injector.class.isAssignableFrom(node.getId().getKey().getTypeLiteral().getRawType())) {
				return true;
			}
			if (PopulationFactory.class.isAssignableFrom(node.getId().getKey().getTypeLiteral().getRawType())) {
				return true;
			}
			if (Scenario.class.isAssignableFrom(node.getId().getKey().getTypeLiteral().getRawType())) {
				return true;
			}
			if (Config.class.isAssignableFrom(node.getId().getKey().getTypeLiteral().getRawType())) {
				return true;
			}
			if (IterationStopWatch.class.isAssignableFrom(node.getId().getKey().getTypeLiteral().getRawType())) {
				return true;
			}
			if (EventsManager.class.isAssignableFrom(node.getId().getKey().getTypeLiteral().getRawType())) {
				return true;
			}
			if (ReplanningContext.class.isAssignableFrom(node.getId().getKey().getTypeLiteral().getRawType())) {
				return true;
			}
			if (PlansDumping.class.isAssignableFrom(node.getId().getKey().getTypeLiteral().getRawType())) {
				return true;
			}
			if (ActivityFacilities.class.isAssignableFrom(node.getId().getKey().getTypeLiteral().getRawType())) {
				return true;
			}
			if (EventsHandling.class.isAssignableFrom(node.getId().getKey().getTypeLiteral().getRawType())) {
				return true;
			}
			if (TravelTimeCalculator.class.isAssignableFrom(node.getId().getKey().getTypeLiteral().getRawType())) {
				return true;
			}
			if (SingleModeNetworksCache.class.isAssignableFrom(node.getId().getKey().getTypeLiteral().getRawType())) {
				return true;
			}
			if (ExperiencedPlansService.class.equals(node.getId().getKey().getTypeLiteral().getRawType())) {
				return true;
			}
			if (node.getId().getKey().getTypeLiteral().toString().contains("ExperiencedPlansServiceImpl")) {
				return true;
			}
			if (node.getId().getKey().getTypeLiteral().toString().contains("ControlerListener")) {
				return true;
			}
			if (node.getId().getKey().getTypeLiteral().toString().contains("EventsToActivities")) {
				return true;
			}
			if (node.getId().getKey().getTypeLiteral().toString().contains("EventsToLegs")) {
				return true;
			}
			if (node.getId().getKey().getTypeLiteral().toString().contains("LeastCostPathCalculatorFactory")) {
				return true;
			}
			if (node.getId().getKey().getTypeLiteral().toString().contains("MainModeIdentifier")) {
				return true;
			}
			if (node.getId().getKey().getTypeLiteral().toString().contains("TerminationCriterion")) {
				return true;
			}
			if (node.getId().getKey().getTypeLiteral().equals(new TypeLiteral<Set<MobsimListener>>(){})) {
				return true;
			}
			return false;
		}, edge -> false);
	}

	private Graph<Node, Edge> findGraphComponentReachableFromControler(Graph<Node, Edge> filteredGraph) {
		Graph<Node, Edge> graphComponentReachableFromControler = new DefaultDirectedGraph<>(Edge.class);
		ConnectivityInspector<Node, Edge> ci = new ConnectivityInspector<>(filteredGraph);
		Node controlerNode = nodes.get(NodeId.newTypeId(Key.get(ControlerI.class)));
		Graphs.addGraph(graphComponentReachableFromControler, new MaskSubgraph<>(g, node -> !ci.connectedSetOf(controlerNode).contains(node), edge -> false));
		return graphComponentReachableFromControler;
	}
}
