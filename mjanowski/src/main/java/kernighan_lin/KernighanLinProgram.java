package kernighan_lin;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;
import java.util.Scanner;
import java.util.regex.MatchResult;

public class KernighanLinProgram {
  static public void main(String[] args) {
    
    try {
      new KernighanLinProgram();
    } catch (IOException e) {
      e.printStackTrace();
    }
    
  }
  
  public KernighanLinProgram() throws IOException {

    KernighanLin k = KernighanLin.process(graphFromFile("graph.txt"));

    System.out.print("Group A: ");
    for (Vertex x : k.getGroupA())
      System.out.print(x);
    System.out.print("\nGroup B: ");
    for (Vertex x : k.getGroupB())
      System.out.print(x);
    System.out.println("");
    System.out.println("Cut cost: "+k.getCutCost());
  }
  
  public static Graph graphFromFile(String filename) throws IOException {
    FileReader fileReader = new FileReader(filename);
    BufferedReader bufferedReader = new BufferedReader(fileReader);
    
    Graph g = fromReadable(bufferedReader);
    bufferedReader.close();
    
    return g;
  }
  
  public static Graph fromReadable(Readable readable) {
    Graph graph = new Graph();
    HashMap<String, Vertex> names = new HashMap<String, Vertex>();
    
    Scanner s = new Scanner(readable);

    while(s.hasNext("\r|\n")) s.next("\r|\n");
    
    s.skip("vertices:");
    while (s.findInLine("([A-Z])") != null) {
      MatchResult match = s.match();
      
      String name = match.group(1);
      Vertex v = new Vertex(name);
      graph.addVertex(v);
      names.put(name, v);
    }

    s.skip("\nedges:");
    while (s.findInLine("([A-Z])([A-Z])\\(([0-9]+(?:\\.[0-9]+)?)\\)") != null) {
      MatchResult match = s.match();
      
      Vertex first = names.get(match.group(1));
      Vertex second = names.get(match.group(2));
      Double weight = Double.parseDouble(match.group(3));
      graph.addEdge(new Edge(weight), first, second);
    }
    return graph;
  }
  
  /** Adds a random vertex on an edge if the number of 
   *  vertices in the given graph isn't even */
  public static void makeVerticesEven(Graph g) {
    if (g.getVerticesValues().size() % 2 == 0) return;
    
    ArrayList<Vertex> vlist = new ArrayList<Vertex>();
    for (Vertex v : g.getVerticesValues()) vlist.add(v);
    Random r = new Random();
    Vertex randomV = vlist.get(r.nextInt(vlist.size()));
    Vertex newV = new Vertex("?");
    Edge newE = new Edge(0);
    
    g.addVertex(newV);
    g.addEdge(newE, newV, randomV);
  }
  
  public static void printGraph(Graph g) {
    for (Vertex v : g.getVerticesValues())
      System.out.print(v+" ");
    System.out.println();
    
    for (Edge e : g.getEdges()) {
      Pair<Vertex> endpoints = g.getEndpoints(e);
      System.out.print(endpoints.first+""+endpoints.second+"("+e.weight+") ");
    }
    System.out.println();
    
  }
  
}
