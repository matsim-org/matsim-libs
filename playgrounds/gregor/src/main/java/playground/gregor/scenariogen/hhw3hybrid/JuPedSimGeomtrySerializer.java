package playground.gregor.scenariogen.hhw3hybrid;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import playground.gregor.scenariogen.hhw3hybrid.JuPedSimGeomtry.Crossing;
import playground.gregor.scenariogen.hhw3hybrid.JuPedSimGeomtry.Goal;
import playground.gregor.scenariogen.hhw3hybrid.JuPedSimGeomtry.Polygon;
import playground.gregor.scenariogen.hhw3hybrid.JuPedSimGeomtry.Room;
import playground.gregor.scenariogen.hhw3hybrid.JuPedSimGeomtry.Subroom;
import playground.gregor.scenariogen.hhw3hybrid.JuPedSimGeomtry.Transition;
import playground.gregor.scenariogen.hhw3hybrid.JuPedSimGeomtry.Vertex;

public class JuPedSimGeomtrySerializer {
	
	private String file;
	private JuPedSimGeomtry geo;
	private BufferedWriter bw;
	
	private int indent = 0;
	private String goalsFile;

	public JuPedSimGeomtrySerializer(String file, JuPedSimGeomtry geo, String goalsFile) {
		this.file = file;
		this.geo = geo;
		this.goalsFile = goalsFile;
		
	}
	
	public void  serialize() throws IOException{
		this.bw = new BufferedWriter(new FileWriter(new File(file)));
		writeHeader();
		this.indent++;
		writeRooms();
		writeTransitions();
		this.indent--;
		writeFooter();
		this.bw.close();
		dumpGoals();
	}

	private void dumpGoals() throws IOException {
		this.bw = new BufferedWriter(new FileWriter(new File(this.goalsFile)));
		this.indent = 1;
		String ind = getIndent();
		this.bw.append(ind+"<routing>\n");
		this.indent++;
		ind = getIndent();
		this.bw.append(ind+"<goals>\n");
		this.indent++;
		for (Goal g : this.geo.goals){
			writeGoal(g);
		}
		this.indent--;
		this.bw.append(ind+"</goals>\n");
		this.indent--;
		ind = getIndent();
		this.bw.append(ind+"</routing>\n");
		dumpAgents();
		
		bw.close();
	}

	private void dumpAgents() throws IOException {
		String ind = getIndent();
		this.bw.append(ind+"<agents operational_model_id=\"2\">\n");
		this.indent++;
		ind = getIndent();
		this.bw.append(ind+"<agents_distribution>\n");
		this.indent++;
		dumpAgentsDistribution();
		this.indent--;
		this.bw.append(ind+"</agents_distribution>\n");
		this.bw.append(ind+"<agents_sources>\n");
		this.indent++;
		dumpAgentsSources();
		this.indent--;
		this.bw.append(ind+"</agents_sources>\n");
		this.indent--;
		ind = getIndent();
		this.bw.append(ind+"</agents>\n");
	}

	private void dumpAgentsSources() throws IOException {
		String ind = getIndent();
		for (Goal g : this.geo.goals) {
			String id = g.id;
			this.bw.append(ind+"<source id=\"" + id + "\" frequency=\"1\" agents_max=\"5\" "
					+ "group_id=\""+id+"\" caption=\"source "+ id +"\" />\n");
		}
		
	}

	private void dumpAgentsDistribution() throws IOException {
		String ind = getIndent();
		for (Goal g : this.geo.goals) {
			String id = g.id;
			this.bw.append(ind+"<group group_id=\"" + id + "\" room_id=\"" + id 
					+ "\" subroom_id=\"0\" number=\"0\" router_id=\"1\" agent_parameter=\"1\" />\n");
		}
		
	}

	private void writeGoal(Goal g) throws IOException {
		String ind = getIndent();
		this.bw.append(ind+"<goal id=\""+g.id+"\" final=\"true\" caption =\"" + g.caption + "\">\n");
		this.indent++;
		ind = getIndent();
		this.bw.append(ind+"<polygon>\n");
		this.indent++;
		writeVertices(g.p);
		this.indent--;
		ind = getIndent();
		this.bw.append(ind+"</polygon>\n");
		this.indent--;
		ind = getIndent();
		this.bw.append(ind+"</goal>\n");
	}

	private void writeFooter() throws IOException {
		this.bw.append("</geometry>\n");
		
	}

	private void writeTransitions() throws IOException {
		String ind = getIndent();
		this.bw.append(ind+"<transitions>\n");
		this.indent++;
		for (Transition t : this.geo.transitions){
			writeTransition(t);
		}
		this.indent--;
		this.bw.append(ind+"</transitions>\n");
	}

	private void writeTransition(Transition t) throws IOException {
		String ind = getIndent();
		this.bw.append(ind+"<transition id=\""+ t.id +"\" caption=\""+t.caption+"\" type=\""+t.type+"\" room1_id=\"" + t.room1Id+"\""
				+ " subroom1_id=\""+t.subRoom1Id+"\" room2_id=\""+ t.room2Id+"\" subroom2_id=\""+t.subRoom2Id+"\">\n");
		this.indent++;
		writeVertex(t.v1);
		writeVertex(t.v2);
		this.indent--;
		this.bw.append(ind+"</transition>\n");
	}

	private void writeRooms() throws IOException {
		String ind = getIndent();
		this.bw.append(ind+"<rooms>\n");
		this.indent++;
		ind = getIndent();
		for (Room r : this.geo.rooms){
			this.bw.append(ind + "<room id=\"" + r.id + "\" caption=\"" + r.caption + "\">\n");
			this.indent++;
			writeSubrooms(r);
			writeCrossings(r);
			this.indent--;
			ind = getIndent();
			this.bw.append(ind + "</room>\n");
		}
		this.indent--;
		ind = getIndent();
		this.bw.append(ind+"</rooms>\n");
	}

	private void writeCrossings(Room r) throws IOException {
		String ind = getIndent();
		this.bw.append(ind+"<crossings>\n");
		this.indent++;
		for (Crossing c : r.crossings){
			writeCrossing(c);
		}
		this.indent--;
		this.bw.append(ind+"</crossings>\n");
	}

	private void writeCrossing(Crossing c) throws IOException {
		String ind = getIndent();
		this.bw.append(ind + "<crossing id=\"" + c.id + "\" subroom1_id=\"" + c.subroom1Id + "\" subroom2_id=\"" + c.subroom2Id + "\">\n");
		this.indent++;
		writeVertex(c.v1);
		writeVertex(c.v2);
		this.indent--;
		this.bw.append(ind + "</crossing>\n");
	}

	private void writeSubrooms(Room r) throws IOException {
		String ind = getIndent();
		for (Subroom sub : r.subrooms) {
			this.bw.append(ind + "<subroom id=\"" + sub.id + "\" closed=\"" + sub.closed + "\" class=\""+sub.clazz+"\">\n");
			this.indent++;
			writePolygons(sub);
			this.indent--;
			ind = getIndent();
			this.bw.append(ind + "</subroom>\n");
		}
		
		
	}

	private void writePolygons(Subroom sub) throws IOException {
		String ind = getIndent();
		for (Polygon p : sub.polygons) {
			this.bw.append(ind+"<polygon caption=\""+p.caption+"\">\n");
			this.indent++;
			writeVertices(p);
			this.indent--;
			this.bw.append(ind+"</polygon>\n");
		}
		
	}

	private void writeVertices(Polygon p) throws IOException {
		for (Vertex vert : p.vertices){
			writeVertex(vert);
		}
		
	}

	private void writeVertex(Vertex vert) throws IOException {
		String ind = getIndent();
		this.bw.append(ind+"<vertex px=");
		this.bw.append("\""+vert.px+"\"");
		this.bw.append(" py=");
		this.bw.append("\""+vert.py+"\"");
		this.bw.append(" />\n");
	}

	private String getIndent() {
		StringBuffer buff = new StringBuffer();
		for (int i = 0 ; i < this.indent; i++) {
			buff.append(' ');
		}
		return buff.toString();
	}

	private void writeHeader() throws IOException {
		bw.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n");
		bw.append("<geometry version=\"0.5\" caption=\"hybrid test\" unit=\"m\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:noNamespaceSchemaLocation=\"http://xsd.jupedsim.org/0.6/jps_geometry.xsd\">\n");
	}

}
