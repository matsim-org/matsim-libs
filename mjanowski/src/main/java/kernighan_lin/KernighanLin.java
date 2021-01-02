package kernighan_lin;

import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

/**
 * An implentation of the Kernighan-Lin heuristic algorithm for splitting a graph into 
 * two groups where the weights of the edges between groups (cutting cost) is minimised.
 * @author Jake Coxon
 *
 */
public class KernighanLin {
  
  /** Performs kernighan_lin.KernighanLin with only the given vertices **/
  public static KernighanLin processWithVertices(Graph g, Set<Vertex> vertexSet) {
    Graph newG = new Graph();
    
    for (Vertex v : vertexSet) 
      newG.addVertex(v);
    
    for (Edge e : g.getEdges()) {
      Pair<Vertex> endpoints = g.getEndpoints(e);
      
      if (vertexSet.contains(endpoints.first) && 
          vertexSet.contains(endpoints.second))
        newG.addEdge(e, endpoints.first, endpoints.second);
    }
    return process(newG);
  }
  
  /** Performs KerninghanLin on the given graph **/
  public static KernighanLin process(Graph g) {
    return new KernighanLin(g);
  }
  
  public class VertexGroup extends HashSet<Vertex> {  
    public VertexGroup(HashSet<Vertex> clone) { super(clone); }
    public VertexGroup() { }
  }
  
  final private VertexGroup A, B;
  final private VertexGroup unswappedA, unswappedB;
  public VertexGroup getGroupA() { return A; }
  public VertexGroup getGroupB() { return B; }
  
  final private Graph graph;
  public Graph getGraph() { return graph; }
  final private int partitionSize;
  
  private KernighanLin(Graph g) {
    this.graph = g;
    this.partitionSize = g.getVerticesValues().size() / 2;
    
    if (g.getVerticesValues().size() != partitionSize * 2)
      throw new RuntimeException("Size of vertices must be even");
    
    A = new VertexGroup();
    B = new VertexGroup();
    
    // Split vertices into A and B
    int i = 0;
    for (Vertex v : g.getVerticesValues()) {
      (++i > partitionSize ? B : A).add(v);
    }
    unswappedA = new VertexGroup(A);
    unswappedB = new VertexGroup(B);
    
    System.out.println(A.size()+" "+B.size());
    
    doAllSwaps();
  }
  
  /** Performs |V|/2 swaps and chooses the one with least cut cost one **/
  private void doAllSwaps() {

    LinkedList<Pair<Vertex>> swaps = new LinkedList<Pair<Vertex>>();
    double minCost = Double.POSITIVE_INFINITY;
    int minId = -1;
    
    for (int i = 0; i < partitionSize; i++) {
      double cost = doSingleSwap(swaps);
      if (cost < minCost) {
        minCost = cost; minId = i; 
      }
    }
    
    // Unwind swaps
    while (swaps.size()-1 > minId) {
      Pair<Vertex> pair = swaps.pop();
      // unswap
      swapVertices(A, pair.second, B, pair.first);
    }
  }
  
  /** Chooses the least cost swap and performs it **/
  private double doSingleSwap(Deque<Pair<Vertex>> swaps) {
    
    Pair<Vertex> maxPair = null;
    double maxGain = Double.NEGATIVE_INFINITY;
    
    for (Vertex v_a : unswappedA) {
      for (Vertex v_b : unswappedB) {
        
        Edge e = graph.findEdge(v_a, v_b);
        double edge_cost = (e != null) ? e.weight : 0;
        // Calculate the gain in cost if these vertices were swapped
        // subtract 2*edge_cost because this edge will still be an external edge
        // after swapping
        double gain = getVertexCost(v_a) + getVertexCost(v_b) - 2 * edge_cost;
        
        if (gain > maxGain) {
          maxPair = new Pair<Vertex>(v_a, v_b);
          maxGain = gain;
        }
        
      }
    }
    
    swapVertices(A, maxPair.first, B, maxPair.second);
    swaps.push(maxPair);
    unswappedA.remove(maxPair.first);
    unswappedB.remove(maxPair.second);
    
    return getCutCost();
  }

  /** Returns the difference of external cost and internal cost of this vertex.
   *  When moving a vertex from within group A, all internal edges become external 
   *  edges and vice versa. **/
  private double getVertexCost(Vertex v) {
    
    double cost = 0;

    boolean v1isInA = A.contains(v);
    
    for (Vertex v2 : graph.getNeighbors(v)) {
      
      boolean v2isInA = A.contains(v2);
      Edge edge = graph.findEdge(v, v2);
      
      if (v1isInA != v2isInA) // external
        cost += edge.weight;
      else
        cost -= edge.weight;
    }
    return cost;
  }
  
  /** Returns the sum of the costs of all edges between A and B **/
  public double getCutCost() {
    double cost = 0;

    for (Edge edge : graph.getEdges()) {
      Pair<Vertex> endpoints = graph.getEndpoints(edge);
      
      boolean firstInA = A.contains(endpoints.first);
      boolean secondInA= A.contains(endpoints.second);
      
      if (firstInA != secondInA) // external
        cost += edge.weight;
    }
    return cost;
  }
  
  /** Swaps va and vb in groups a and b **/
  private static void swapVertices(VertexGroup a, Vertex va, VertexGroup b, Vertex vb) {
    if (!a.contains(va) || a.contains(vb) ||
        !b.contains(vb) || b.contains(va)) throw new RuntimeException("Invalid swap");
    a.remove(va); a.add(vb);
    b.remove(vb); b.add(va);
  }
  
  

  
}
