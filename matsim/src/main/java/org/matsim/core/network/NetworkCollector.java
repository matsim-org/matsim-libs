package org.matsim.core.network;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.groups.NetworkConfigGroup;

import java.util.Collection;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.Stream;

/**
 * Collector to transform a {@link Stream<Link>} into a {@link Network}. The collector makes shallow copies of nodes
 * attached to the supplied links.
 * <p>
 * The collector works fine with parallel streams.
 */
public class NetworkCollector implements Collector<Link, Collection<Link>, Network> {
	private final NetworkConfigGroup networkConfigGroup;
	
    /**
     * Deliberately package private
     */
    NetworkCollector(NetworkConfigGroup networkConfigGroup) {
    	this.networkConfigGroup = networkConfigGroup;
    }

    private static void addNodeIfNecessary(Network network, Node node) {
        if (!network.getNodes().containsKey(node.getId())) {

            // nodes keep internal state of in- and out-links. Since we don't know, whether this state is stale at this
            // point we create a shallow copy with empty in-out-links mappings. Simply clearing the mappings would alter the
            // state in the original network, which would be unexpected behaviour of a collector.
            var copy = NetworkUtils.createNode(node.getId());
            copy.setCoord(node.getCoord());

            for (var entry : node.getAttributes().getAsMap().entrySet()) {
                copy.getAttributes().putAttribute(entry.getKey(), entry.getValue());
            }
            network.addNode(copy);
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

            var result = NetworkUtils.createNetwork(networkConfigGroup);
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
