package edu.metis;

import java.io.BufferedWriter;
import java.io.IOException;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Utility class to work with the metis library.
 */
@SuppressWarnings("preview")
public final class Metis {

	private static boolean AVAILABLE;

	static {
		try {
			if (System.getenv("METIS_LIBRARY") != null) {
				System.load(System.getenv("METIS_LIBRARY"));
			} else
				System.loadLibrary("metis");

			AVAILABLE = true;
		} catch (UnsatisfiedLinkError e) {
			try {
				System.load("/opt/homebrew/lib/libmetis.dylib");
				AVAILABLE = true;
			} catch (UnsatisfiedLinkError e2) {
				AVAILABLE = false;
			}
		}
	}

	/**
	 * Write a graph to a file in the METIS format. Can be used with METIS command line tools.
	 */
	public static void writeGraph(Graph g, Path output) throws IOException {
		try (BufferedWriter writer = Files.newBufferedWriter(output)) {

			writer.write(g.getNumVertices() + " " + g.getNumEdges() + " 001\n");
			for (int i = 0; i < g.getNumVertices(); i++) {
				g.getEdges(i, (vertex, weight) -> {
					try {
						writer.write((vertex + 1) + " " + weight + " ");
					} catch (IOException e) {
						throw new RuntimeException(e);
					}
				});
				writer.write("\n");
			}
		}

	}

	/**
	 * Check if the metis library is available.
	 */
	public static boolean isAvailable() {
		return AVAILABLE;
	}

	/**
	 * Partition a graph into a given number of parts using METIS PartGraphRecursive procedure.
	 *
	 * @param g       graph
	 * @param parts   number of partitions to create
	 * @param options options
	 * @return an array of size g.getNumVertices() containing the partition number for each vertex
	 */
	public static int[] partitionGraphRecursive(Graph g, int parts, MetisOptions options) {
		return partitionInternal(g, parts, options, true);
	}

	public static int[] partitionGraphKway(Graph g, int parts, MetisOptions options) {
		return partitionInternal(g, parts, options, false);
	}

	private static int[] partitionInternal(Graph g, int parts, MetisOptions options, boolean recursive) {
		try (Arena arena = Arena.ofConfined()) {

			MemorySegment nvtxs = arena.allocate(metis_h.idx_t.withName("nvtxs"), g.getNumVertices());

			MemorySegment ncon = arena.allocate(metis_h.idx_t.withName("ncon"), 1);

			MemorySegment xadj = arena.allocate(metis_h.idx_t.withName("xadj"), g.getNumVertices() + 1);
			MemorySegment adjncy = arena.allocate(metis_h.idx_t.withName("adjncy"), g.getNumEdges());

			// Vertices computational weights
			MemorySegment vwgt = arena.allocate(metis_h.idx_t.withName("vwgt"), g.getNumVertices());
			// Vertices communication weight
			MemorySegment vsize = arena.allocate(metis_h.idx_t.withName("vsize"), g.getNumVertices());

			// The weight of edges
			MemorySegment adjwgt = arena.allocate(metis_h.idx_t.withName("adjwgt"), g.getNumEdges());

			// Fill xadj and adjncy
			AtomicInteger offset = new AtomicInteger();

			for (int i = 0; i < g.getNumVertices(); i++) {
				xadj.setAtIndex(metis_h.idx_t, i, offset.get());
				int wgt = g.getVertexComputationWeight(i);
				if (wgt < 1)
					throw new IllegalArgumentException("Vertex weight must be greater than 0");

				int size = g.getVertexCommunicationWeight(i);
				// Vsize will be disabled if any communication size is not specified
				if (size == -1) {
					vsize = MemorySegment.NULL;
				} else if (size == 0)
					throw new IllegalArgumentException("Vertex size must be greater than 0");
				else {
					vsize.setAtIndex(metis_h.idx_t, i, size);
				}

				vwgt.setAtIndex(metis_h.idx_t, i, wgt);

				g.getEdges(i, (vertex, weight) -> {
					int pos = offset.getAndIncrement();
					adjncy.setAtIndex(metis_h.idx_t, pos, vertex);
					adjwgt.setAtIndex(metis_h.idx_t, pos, weight);
				});
			}
			xadj.setAtIndex(metis_h.idx_t, g.getNumVertices(), offset.get());

//            System.out.println("nvtxs: " + g.getNumVertices());
//            System.out.println("ncon: " + 1);
//            System.out.println("edges: " + g.getNumEdges());
//            System.out.println("xadj: " + Arrays.toString(xadj.toArray(metis_h.idx_t)));
//            System.out.println("adjncy: " + Arrays.toString(adjncy.toArray(metis_h.idx_t)));
//            System.out.println("vwgt: " + Arrays.toString(vwgt.toArray(metis_h.idx_t)));
//            System.out.println("adjwgt: " + Arrays.toString(adjwgt.toArray(metis_h.idx_t)));

			MemorySegment nparts = arena.allocate(metis_h.idx_t.withName("nparts"), parts);

			// Return values
			MemorySegment objval = arena.allocate(metis_h.idx_t.withName("objval"), 0);
			MemorySegment part = arena.allocate(metis_h.idx_t.withName("part"), g.getNumVertices());

			MemorySegment opt = arena.allocate(metis_h.idx_t.withName("options"), metis_h.METIS_NOPTIONS());
			metis_h.METIS_SetDefaultOptions(opt);

			opt.setAtIndex(metis_h.idx_t, metis_h.METIS_OPTION_NUMBERING(), 0);
			opt.setAtIndex(metis_h.idx_t, metis_h.METIS_OPTION_OBJTYPE(), options.objType.value);
			opt.setAtIndex(metis_h.idx_t, metis_h.METIS_OPTION_CTYPE(), options.coarseningType.value);
			opt.setAtIndex(metis_h.idx_t, metis_h.METIS_OPTION_IPTYPE(), options.initialPartitionType.value);
			opt.setAtIndex(metis_h.idx_t, metis_h.METIS_OPTION_RTYPE(), options.refinementType.value);
			opt.setAtIndex(metis_h.idx_t, metis_h.METIS_OPTION_SEED(), options.seed);

			int ret;
			if (recursive)
				ret = metis_h.METIS_PartGraphRecursive(nvtxs, ncon, xadj, adjncy, vwgt, vsize, adjwgt, nparts,
					MemorySegment.NULL, MemorySegment.NULL,
					opt, objval, part);
			else
				ret = metis_h.METIS_PartGraphKway(nvtxs, ncon, xadj, adjncy, vwgt, vsize, adjwgt, nparts,
					MemorySegment.NULL, MemorySegment.NULL,
					opt, objval, part);

			if (ret != metis_h.METIS_OK())
				throw new RuntimeException("METIS error: " + ret);

			return part.toArray(metis_h.idx_t);
		}
	}

	/**
	 * Graph interface for metis. The interface is designed that minimal data copy is performed.
	 */
	public interface Graph {

		/**
		 * Return the number of vertices (nodes) in the graph.
		 */
		int getNumVertices();

		/**
		 * Return the number of edges (links) in the graph.
		 */
		int getNumEdges();

		/**
		 * The vertex weights are used for ensuring that the computed partitionings satisfy the specified balancing constraints.
		 */
		default int getVertexComputationWeight(int vertex) {
			return 1;
		}

		/**
		 * The vertex sizes are used for determining the total communication volume
		 */
		default int getVertexCommunicationWeight(int vertex) {
			return -1;
		}

		/**
		 * This function must add edges to the builder for the given vertex, via {@link EdgeBuilder#addEdge(int, int)} .
		 */
		void getEdges(int vertex, EdgeBuilder builder);
	}

	@FunctionalInterface
	public interface EdgeBuilder {

		void addEdge(int vertex, int weight);

		default void addEdge(int vertex) {
			addEdge(vertex, 1);
		}

	}

}
