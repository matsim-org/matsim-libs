package org.matsim.guice;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.inject.Key;
import com.google.inject.grapher.BindingEdge;
import com.google.inject.grapher.DependencyEdge;
import com.google.inject.grapher.Edge;
import com.google.inject.grapher.ImplementationNode;
import com.google.inject.grapher.InstanceNode;
import com.google.inject.grapher.InterfaceNode;
import com.google.inject.grapher.NameFactory;
import com.google.inject.grapher.Node;
import com.google.inject.grapher.NodeId;
import com.google.inject.grapher.ShortNameFactory;
import com.google.inject.grapher.graphviz.ArrowType;
import com.google.inject.grapher.graphviz.CompassPoint;
import com.google.inject.grapher.graphviz.EdgeStyle;
import com.google.inject.grapher.graphviz.GraphvizEdge;
import com.google.inject.grapher.graphviz.GraphvizNode;
import com.google.inject.grapher.graphviz.NodeStyle;
import com.google.inject.grapher.graphviz.PortIdFactory;
import com.google.inject.grapher.graphviz.PortIdFactoryImpl;
import com.google.inject.spi.InjectionPoint;
import org.jgrapht.Graph;

import java.io.PrintWriter;
import java.lang.reflect.Member;
import java.util.List;
import java.util.Map;

public class GraphvizRenderer {

	private boolean fields = false;

	private final Map<NodeId, GraphvizNode> nodes = Maps.newHashMap();
	private final List<GraphvizEdge> edges = Lists.newArrayList();
	private final NameFactory nameFactory = new ShortNameFactory();
	private final PortIdFactory portIdFactory = new PortIdFactoryImpl();

	private PrintWriter out;
	private String rankdir = "TB";

	public void render(Graph<Node, Edge> graph) {
		for (Node node : graph.vertexSet()) {
			if (node instanceof InstanceNode) {
				newInstanceNode(((InstanceNode) node));
			} else if (node instanceof ImplementationNode) {
				newImplementationNode(((ImplementationNode) node));
			} else if (node instanceof InterfaceNode) {
				newInterfaceNode(((InterfaceNode) node));
			}
		}
		for (Edge edge : graph.edgeSet()) {
			if (edge instanceof BindingEdge) {
				newBindingEdge(((BindingEdge) edge));
			} else if (edge instanceof DependencyEdge) {
				newDependencyEdge(((DependencyEdge) edge));
			}
		}
		out.println("digraph injector {");

		Map<String, String> attrs = Maps.newHashMap();
		attrs.put("rankdir", rankdir);
		out.println("graph " + getAttrString(attrs) + ";");

		for (GraphvizNode node : nodes.values()) {
			renderNode(node);
		}

		for (GraphvizEdge edge : edges) {
			renderEdge(edge);
		}

//		out.println("{");
//		out.println("rank=same;");
//		out.println(nodes.get(NodeId.newTypeId(Key.get(Mobsim.class))).getIdentifier()+";");
//		out.println(nodes.get(NodeId.newTypeId(Key.get(TripRouter.class))).getIdentifier()+";");
//		out.println(nodes.get(NodeId.newTypeId(Key.get(PlansScoring.class))).getIdentifier()+";");
//		out.println(nodes.get(NodeId.newTypeId(Key.get(PrepareForSim.class))).getIdentifier()+";");
//		out.println(nodes.get(NodeId.newTypeId(Key.get(PlansReplanning.class))).getIdentifier()+";");
//		out.println("}");
//
//		out.println("{");
//		out.println("rank=same;");
//		out.println(nodes.get(NodeId.newTypeId(Key.get(RoutingModule.class, Names.named("car")))).getIdentifier()+";");
//		out.println(nodes.get(NodeId.newTypeId(Key.get(TravelTime.class, Names.named("car")))).getIdentifier()+";");
//		out.println(nodes.get(NodeId.newTypeId(Key.get(TravelDisutilityFactory.class, Names.named("car")))).getIdentifier()+";");
//		out.println("}");

		finish();

		out.flush();
	}


	protected void reset() {
		nodes.clear();
		edges.clear();
	}

	public void setOut(PrintWriter out) {
		this.out = out;
	}

	void setRankdir(String rankdir) {
		this.rankdir = rankdir;
	}

	protected void finish() {
		out.println("}");
	}

	private void renderNode(GraphvizNode node) {
		Map<String, String> attrs = getNodeAttributes(node);
		out.println(node.getIdentifier() + " " + getAttrString(attrs));
	}

	private Map<String, String> getNodeAttributes(GraphvizNode node) {
		Map<String, String> attrs = Maps.newHashMap();

		attrs.put("label", getNodeLabel(node));
		// remove most of the margin because the table has internal padding
		attrs.put("margin", "\"0.02,0\"");
		attrs.put("shape", node.getShape().toString());
		attrs.put("style", node.getStyle().toString());

		return attrs;
	}

	/**
	 * Creates the "label" for a node. This is a string of HTML that defines a
	 * table with a heading at the top and (in the case of
	 * {@link ImplementationNode}s) rows for each of the member fields.
	 */
	private String getNodeLabel(GraphvizNode node) {
		String cellborder = node.getStyle() == NodeStyle.INVISIBLE ? "1" : "0";

		StringBuilder html = new StringBuilder();
		html.append("<");
		html.append("<table cellspacing=\"0\" cellpadding=\"5\" cellborder=\"");
		html.append(cellborder).append("\" border=\"0\">");

		html.append("<tr>").append("<td align=\"left\" port=\"header\" ");
		html.append("bgcolor=\"" + node.getHeaderBackgroundColor() + "\">");

		String subtitle = Joiner.on("<br align=\"left\"/>").join(node.getSubtitles());
		if (subtitle.length() != 0) {
			html.append("<font color=\"").append(node.getHeaderTextColor());
			html.append("\" point-size=\"10\">");
			html.append(subtitle).append("<br align=\"left\"/>").append("</font>");
		}

		html.append("<font color=\"" + node.getHeaderTextColor() + "\">");
		html.append(htmlEscape(node.getTitle())).append("<br align=\"left\"/>");
		html.append("</font>").append("</td>").append("</tr>");

		for (Map.Entry<String, String> field : node.getFields().entrySet()) {
			html.append("<tr>");
			html.append("<td align=\"left\" port=\"").append(htmlEscape(field.getKey())).append("\">");
			html.append(htmlEscape(field.getValue()));
			html.append("</td>").append("</tr>");
		}

		html.append("</table>");
		html.append(">");
		return html.toString();
	}

	private void renderEdge(GraphvizEdge edge) {
		Map<String, String> attrs = getEdgeAttributes(edge);

		String tailId = getEdgeEndPoint(nodes.get(edge.getTailNodeId()).getIdentifier(),
				edge.getTailPortId(), edge.getTailCompassPoint());

		String headId = getEdgeEndPoint(nodes.get(edge.getHeadNodeId()).getIdentifier(),
				edge.getHeadPortId(), edge.getHeadCompassPoint());

		out.println(tailId + " -> " + headId + " " + getAttrString(attrs));
	}

	private Map<String, String> getEdgeAttributes(GraphvizEdge edge) {
		Map<String, String> attrs = Maps.newHashMap();

		attrs.put("arrowhead", getArrowString(edge.getArrowHead()));
		attrs.put("arrowtail", getArrowString(edge.getArrowTail()));
		attrs.put("style", edge.getStyle().toString());

		return attrs;
	}

	private String getAttrString(Map<String, String> attrs) {
		List<String> attrList = Lists.newArrayList();

		for (Map.Entry<String, String> attr : attrs.entrySet()) {
			String value = attr.getValue();

			if (value != null) {
				attrList.add(attr.getKey() + "=" + value);
			}
		}

		return "[" + Joiner.on(", ").join(attrList) + "]";
	}

	/**
	 * Turns a {@link List} of {@link ArrowType}s into a {@link String} that
	 * represents combining them. With Graphviz, that just means concatenating
	 * them.
	 */
	private String getArrowString(List<ArrowType> arrows) {
		return Joiner.on("").join(arrows);
	}

	private String getEdgeEndPoint(String nodeId, String portId, CompassPoint compassPoint) {
		List<String> portStrings = Lists.newArrayList(nodeId);

		if (portId != null) {
			portStrings.add(portId);
		}

		if (compassPoint != null) {
			portStrings.add(compassPoint.toString());
		}

		return Joiner.on(":").join(portStrings);
	}

	private String htmlEscape(String str) {
		return str.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
	}

	private void newInterfaceNode(InterfaceNode node) {
		NodeId nodeId = node.getId();
		GraphvizNode gnode = new GraphvizNode(nodeId);
		gnode.setStyle(NodeStyle.DASHED);
		Key<?> key = nodeId.getKey();
		gnode.setTitle(nameFactory.getClassName(key));
		gnode.addSubtitle(0, nameFactory.getAnnotationName(key));
		addNode(gnode);
	}

	private void newImplementationNode(ImplementationNode node) {
		NodeId nodeId = node.getId();
		GraphvizNode gnode = new GraphvizNode(nodeId);
		gnode.setStyle(NodeStyle.SOLID);

		gnode.setHeaderBackgroundColor("#000000");
		gnode.setHeaderTextColor("#ffffff");
		gnode.setTitle(nameFactory.getClassName(nodeId.getKey()));

		if (fields) {
			for (Member member : node.getMembers()) {
				gnode.addField(portIdFactory.getPortId(member), nameFactory.getMemberName(member));
			}
		}

		addNode(gnode);
	}

	private void newInstanceNode(InstanceNode node) {
		NodeId nodeId = node.getId();
		GraphvizNode gnode = new GraphvizNode(nodeId);
		gnode.setStyle(NodeStyle.SOLID);

		gnode.setHeaderBackgroundColor("#000000");
		gnode.setHeaderTextColor("#ffffff");
		gnode.setTitle(nameFactory.getClassName(nodeId.getKey()));

		gnode.addSubtitle(0, nameFactory.getSourceName(node.getSource()));

		gnode.setHeaderBackgroundColor("#aaaaaa");
		gnode.setHeaderTextColor("#ffffff");
		gnode.setTitle(nameFactory.getInstanceName(node.getInstance()));

		if (fields) {
			for (Member member : node.getMembers()) {
				gnode.addField(portIdFactory.getPortId(member), nameFactory.getMemberName(member));
			}
		}
		addNode(gnode);
	}

	private void newDependencyEdge(DependencyEdge edge) {
		GraphvizEdge gedge = new GraphvizEdge(edge.getFromId(), edge.getToId());
		InjectionPoint fromPoint = edge.getInjectionPoint();
		if (fromPoint == null) {
			gedge.setTailPortId("header");
		} else {
			gedge.setTailPortId(portIdFactory.getPortId(fromPoint.getMember()));
		}
		gedge.setArrowHead(ImmutableList.of(ArrowType.NORMAL));
		gedge.setTailCompassPoint(CompassPoint.EAST);

		edges.add(gedge);
	}

	private void newBindingEdge(BindingEdge edge) {
		GraphvizEdge gedge = new GraphvizEdge(edge.getFromId(), edge.getToId());
		gedge.setStyle(EdgeStyle.DASHED);
		switch (edge.getType()) {
			case NORMAL:
				gedge.setArrowHead(ImmutableList.of(ArrowType.NORMAL_OPEN));
				break;

			case PROVIDER:
				gedge.setArrowHead(ImmutableList.of(ArrowType.NORMAL_OPEN, ArrowType.NORMAL_OPEN));
				break;

			case CONVERTED_CONSTANT:
				gedge.setArrowHead(ImmutableList.of(ArrowType.NORMAL_OPEN, ArrowType.DOT_OPEN));
				break;
		}
		edges.add(gedge);
	}

	private void addNode(GraphvizNode node) {
		node.setIdentifier("x" + nodes.size());
		nodes.put(node.getNodeId(), node);
	}

}
