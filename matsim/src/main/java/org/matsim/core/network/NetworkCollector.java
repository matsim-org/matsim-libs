package org.matsim.core.network;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;

import java.util.Collection;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;

public class NetworkCollector implements Collector<Link, Collection<Link>, Network> {

    /**
     * Deliberately package private
     */
    NetworkCollector() {
    }

    private static void addNodeIfNecessary(Network network, Node node) {
        if (!network.getNodes().containsKey(node.getId())) {

            // usually this collector is used with links, which have been inside a network before,
            // therefore the nodes already have in and out links. If in a previous stream operation
            // links from the original network are filtered out, nodes still might have a reference
            // to those links in their in and out lists
            clearInAndOutLinks(node);
            network.addNode(node);
        }
    }

    private static void clearInAndOutLinks(Node node) {

        for (var linkId : node.getInLinks().keySet()) {
            node.removeInLink(linkId);
        }

        for (var linkId : node.getOutLinks().keySet()) {
            node.removeOutLink(linkId);
        }
    }

    @Override
    public Supplier<Collection<Link>> supplier() {
        // return a concurrent set because we want 'concurrent' characteristics
        return ConcurrentHashMap::newKeySet;
    }

    @Override
    public BiConsumer<Collection<Link>, Link> accumulator() {
        return Collection::add;
    }

    @Override
    public BinaryOperator<Collection<Link>> combiner() {
        return (links, links2) -> {
            links.addAll(links2);
            return links;
        };
    }

    @Override
    public Function<Collection<Link>, Network> finisher() {
        return links -> {

            var result = NetworkUtils.createNetwork();
            for (var link : links) {
                addNodeIfNecessary(result, link.getFromNode());
                addNodeIfNecessary(result, link.getToNode());
                result.addLink(link);
            }
            return result;
        };
    }

    @Override
    public Set<Characteristics> characteristics() {
        return Set.of(Characteristics.UNORDERED, Characteristics.CONCURRENT);
    }
}
