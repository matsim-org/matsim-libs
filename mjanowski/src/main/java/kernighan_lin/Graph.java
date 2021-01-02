package kernighan_lin;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;


public class Graph {
  final private Map<Vertex, Map<Vertex, Edge>> vertices;
  final private Map<Edge, Pair<Vertex>> edges;
  
  public Graph() {
    // Maps vertices to a map of vertices to incident edges
    vertices = new HashMap<Vertex, Map<Vertex, Edge>>();
    // Maps edges to edge endpoints
    edges = new HashMap<Edge, Pair<Vertex>>();
  }
  
  public boolean addVertex(Vertex v) {
    if (containsVertex(v)) return false;
    vertices.put(v, new HashMap<Vertex, Edge>());
    return true;
  }
  
  public boolean addEdge(Edge edge, Vertex v1, Vertex v2) {
    
    if (!containsVertex(v1) || !containsVertex(v2)) return false;
    if (findEdge(v1, v2) != null) return false;
    
    Pair<Vertex> pair = new Pair<Vertex>(v1, v2);
    edges.put(edge, pair);
    vertices.get(v1).put(v2, edge);
    vertices.get(v2).put(v1, edge);
    
    return true;
  }
  
  public boolean containsVertex(Vertex v) {
    return vertices.containsKey(v);
  }
  public boolean containsEdge(Edge e) {
    return edges.containsKey(e);
  }
  
  /** Finds an edge if any between v1 and v2 **/
  public Edge findEdge(Vertex v1, Vertex v2) {
    if (!containsVertex(v1) || !containsVertex(v2)) 
      return null;
    return vertices.get(v1).get(v2);
  }
  
  /** Gets the vertices directly connected to v **/
  public Collection<Vertex> getNeighbors(Vertex v) {
    if (!containsVertex(v)) return null;
    return vertices.get(v).keySet();
  }

  public Collection<Vertex> getVertices(Vertex v) {
    if (!containsVertex(v)) return null;
    return vertices.get(v).keySet();
  }
  
  public Set<Edge> getEdges() {
    return edges.keySet();
  }

  public Map<Vertex, Map<Vertex, Edge>> getVerticesMap() {
    return vertices;
  }

  public Map<Edge, Pair<Vertex>> getEdgesMap() {
    return edges;
  }
  
  public Set<Vertex> getVerticesValues() {
    return vertices.keySet();
  }
  
  /** Returns a pair of vertices that connects by edge e **/
  public Pair<Vertex> getEndpoints(Edge e) {
    return edges.get(e);
  }
  
}
