package org.mjanowski;

import org.graphstream.graph.implementations.SingleGraph;
import org.graphstream.ui.view.Viewer;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class NetworkPartitioner {

    private final Network network;

    public NetworkPartitioner(Network network) {
        this.network = network;
    }

    public Map<Integer, Partition> partition(int partitionsNumber) {
        Map<Id<Node>, ? extends Node> nodes = network.getNodes();
        int nodesNumber = nodes.size();
        int nodesPerPartition = nodesNumber / partitionsNumber;
        int reminderNodesNumber = nodesNumber % partitionsNumber;
        Map<Integer, Partition> partitions = IntStream.range(0, partitionsNumber)
                .boxed()
                .collect(Collectors.toMap(Function.identity(),
                        k -> k < reminderNodesNumber ? new Partition(nodesPerPartition + 1) : new Partition(nodesPerPartition)));

        TreeSet<Id<Node>> unvisitedNodesIds = nodes.values()
                .stream()
                .sorted(Comparator.comparingLong((Node n) -> n.getInLinks().size() + n.getOutLinks().size()).reversed())
                .map(Node::getId)
                .collect(Collectors.toCollection(TreeSet::new));
        LinkedList<Id<Node>> nodesToVisitIds = new LinkedList<>();

        Iterator<Partition> partitionsIterator = partitions.values().iterator();
        Partition currentPartition = partitionsIterator.next();
        int nodesCounter = 0;
        while (unvisitedNodesIds.size() > 0) {
            Id<Node> sourceNodeId = unvisitedNodesIds.first();
            Node source = nodes.get(sourceNodeId);
            nodesToVisitIds.add(source.getId());

            while (nodesToVisitIds.size() > 0) {
                Id<Node> firstNodeId = nodesToVisitIds.pollLast();
                if (!unvisitedNodesIds.contains(firstNodeId))
                    continue;
                Node first = nodes.get(firstNodeId);
                Collection<? extends Link> outLinks = source.getOutLinks().values();
                List<Id<Node>> nextNodesIds = outLinks.stream()
                        .map(Link::getToNode)
                        .map(Node::getId)
                        .filter(unvisitedNodesIds::contains)
                        .collect(Collectors.toList());
                nodesToVisitIds.addAll(nextNodesIds);
                unvisitedNodesIds.remove(firstNodeId);

                if (nodesCounter == currentPartition.getMaxSize()) {
                    nodesCounter = 0;
                    currentPartition = partitionsIterator.next();
                }
                currentPartition.addNode(first);
                nodesCounter++;
            }
        }

//        List<String> colours = Arrays.asList("#e6194B", "#3cb44b", "#ffe119", "#4363d8", "#f58231", "#911eb4", "#42d4f4", "#f032e6",
//                "#bfef45", "#fabed4", "#469990", "#dcbeff", "#9A6324", "#fffac8", "#800000", "#aaffc3", "#808000",
//                "#ffd8b1", "#000075", "#a9a9a9", "#000000");
//
//        Map<Integer, String> coloursMap = IntStream.range(0, colours.size())
//                .boxed()
//                .collect(Collectors.toMap(Function.identity(), colours::get));
//
//        SingleGraph displayGraph = new SingleGraph("");
//        for (int i = 0; i < partitions.values().size(); i++) {
//            Partition partition = partitions.get(i);
//            for (Node node : partition.getNodes()) {
//                Id<Node> nodeId = node.getId();
//                org.graphstream.graph.Node graphNode = displayGraph.addNode(nodeId.toString());
//                Coord coord = network.getNodes().get(nodeId).getCoord();
//                graphNode.setAttribute("xy", coord.getX(), coord.getY());
//                String colour = colours.get(i);
//                graphNode.setAttribute("ui.style", String.format("fill-color: %s;", colour));
//            }
//        }
//
//        Map<Id<Link>, ? extends Link> links = network.getLinks();
//        for (Link link : links.values()) {
//            String fromNodeId = link.getFromNode().getId().toString();
//            String toNodeId = link.getToNode().getId().toString();
//            try {
//                org.graphstream.graph.Edge edge = displayGraph.addEdge(link.getId().toString(), fromNodeId, toNodeId);
//            } catch (Exception e) {
//                //todo?
//                Logger.getRootLogger().info("ups");
//            }
////			edge.addAttribute("ui.label", entry.getKey().weight);
//        }
//
//		System.setProperty("org.graphstream.ui.renderer", "org.graphstream.ui.j2dviewer.J2DGraphRenderer");
//		Viewer display = displayGraph.display(false);
//
//		try {
//			Thread.currentThread().join();
//		} catch (InterruptedException e) {
//			e.printStackTrace();
//		}
//
//		System.exit(0);

        return partitions;
    }
}
