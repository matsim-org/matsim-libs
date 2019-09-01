package org.matsim.guice;

import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.TypeLiteral;
import com.google.inject.grapher.*;
import org.jgrapht.DirectedGraph;
import org.jgrapht.Graphs;
import org.jgrapht.alg.ConnectivityInspector;
import org.jgrapht.graph.DirectedMaskSubgraph;
import org.jgrapht.graph.ListenableDirectedGraph;
import org.jgrapht.graph.MaskFunctor;
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

public class JGraphTGrapher extends AbstractInjectorGrapher {

	private ListenableDirectedGraph<Node, Edge> g;
	private final Map<NodeId, Node> nodes = new HashMap<>();
	private Writer writer;

	public JGraphTGrapher(GrapherParameters options, Writer writer) {
		super(options);
		this.writer = writer;
		g = new ListenableDirectedGraph<>(Edge.class);
	}

	@Override
	protected void reset() {
		g = new ListenableDirectedGraph<>(Edge.class);
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
		DirectedGraph<Node, Edge> filteredGraph = new DirectedMaskSubgraph<>(g, new MaskFunctor<Node, Edge>() {
			@Override
			public boolean isEdgeMasked(Edge edge) {
				return false;
			}
			@Override
			public boolean isVertexMasked(Node node) {
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
			}
		});
		ConnectivityInspector<Node, Edge> ci = new ConnectivityInspector<>(filteredGraph);
		filteredGraph = new ListenableDirectedGraph<>(Edge.class);
		Graphs.addGraph(filteredGraph, new DirectedMaskSubgraph<>(g, new MaskFunctor<Node, Edge>() {
			@Override
			public boolean isEdgeMasked(Edge edge) {
				return false;
			}
			@Override
			public boolean isVertexMasked(Node node) {
				Node vertex = JGraphTGrapher.this.nodes.get(NodeId.newTypeId(Key.get(ControlerI.class)));
				return !ci.connectedSetOf(vertex).contains(node);
			}
		}));

		GraphvizRenderer graphvizRenderer = new GraphvizRenderer();
		graphvizRenderer.setRankdir("LR");
		graphvizRenderer.setOut((PrintWriter) writer);
		graphvizRenderer.render(filteredGraph);
	}
}
