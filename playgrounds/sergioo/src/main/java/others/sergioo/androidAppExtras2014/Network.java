package others.sergioo.androidAppExtras2014;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class Network {

	public class Node {
	
		private final String id;
		private final LatLng position;
	
		public Node(String id, LatLng position) {
			super();
			this.id = id;
			this.position = position;
		}
		public String getId() {
			return id;
		}
		public LatLng getPosition() {
			return position;
		}
	
	}
	public class Link {
	
		private final String id;
		private final Node start;
		private final Node end;
		private final double value;
	
		public Link(String id, Node start, Node end, double value) {
			super();
			this.id = id;
			this.start = start;
			this.end = end;
			this.value = value;
		}
		public String getId() {
			return id;
		}
		public Node getStart() {
			return start;
		}
		public Node getEnd() {
			return end;
		}
		public double getValue() {
			return value;
		}
	
	}

	private static final String LINKS = "Links";
	private static final String NODES = "Nodes";

	private final Map<String, Node> nodes = new HashMap<String, Node>();
	private final Map<String, Link> links = new HashMap<String, Link>();

	public Network(String file) throws IOException {
		BufferedReader reader = new BufferedReader(new FileReader(file));
		reader.readLine();
		String line = reader.readLine();
		while(!line.equals(LINKS)) {
			String[] parts = line.split(" ");
			nodes.put(parts[0], new Node(parts[0], new LatLng(Double.parseDouble(parts[1]), Double.parseDouble(parts[2]))));
			line = reader.readLine();
		}
		line = reader.readLine();
		while(line!=null) {
			String[] parts = line.split(" ");
			links.put(parts[0]+"-"+parts[1], new Link(parts[0]+"-"+parts[1], nodes.get(parts[0]), nodes.get(parts[1]), Double.parseDouble(parts[2])));
			line = reader.readLine();
		}
		reader.close();
	}
	public Map<String, Node> getNodes() {
		return nodes;
	}
	public Map<String, Link> getLinks() {
		return links;
	}

}
