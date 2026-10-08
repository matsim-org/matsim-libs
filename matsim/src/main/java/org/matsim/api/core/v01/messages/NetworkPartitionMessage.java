package org.matsim.api.core.v01.messages;

import org.matsim.api.core.v01.Message;

/**
 * Distributes the network partitioning computed on the head node to all other nodes.
 *
 * @param rank           rank of the sending node
 * @param nodePartitions partition of each node, indexed by the node id index. Empty if not sent by the head node.
 */
public record NetworkPartitionMessage(int rank, int[] nodePartitions) implements Message {
}
