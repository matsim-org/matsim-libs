package playground.gregor.scenariogen.hhw3hybrid;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import playground.gregor.scenariogen.hhw3hybrid.JuPedSimGeomtry.Crossing;
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

	public JuPedSimGeomtrySerializer(String file, JuPedSimGeomtry geo) {
		this.file = file;
		this.geo = geo;
		
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
	}

	private void writeFooter() throws IOException {
		this.bw.append("<geomtry>\n");
		
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
		this.bw.append(ind + "<crossing>\n");
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
