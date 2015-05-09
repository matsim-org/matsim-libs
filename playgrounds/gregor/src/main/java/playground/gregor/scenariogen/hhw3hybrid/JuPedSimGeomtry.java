package playground.gregor.scenariogen.hhw3hybrid;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.matsim.api.core.v01.Id;

import playground.gregor.sim2d_v4.cgal.LineSegment;
import playground.gregor.sim2d_v4.scenario.Section;
import playground.gregor.sim2d_v4.scenario.Sim2DEnvironment;

public class JuPedSimGeomtry {

	private static final double TRANSITION_BOX_LENGTH = 2;
	private static final double GOAL_OFFSET = 4;
	
	List<Room> rooms = new ArrayList<>();
	List<Transition> transitions = new ArrayList<>();
	List<Goal> goals = new ArrayList<>();
	
	public void buildFrom2DEnv(Sim2DEnvironment env) {
		Set<String> handledCrossings = new HashSet<>();
		Room r = new Room();
		this.rooms.add(r);
		r.id = "0";
		r.caption = "hall0";
		for (Section sec : env.getSections().values()) {
			Subroom s = new Subroom();
			r.subrooms.add(s);
			s.id = sec.getId().toString();//this won't work this JuPedSim expects int as id
			s.closed = 0;
			s.clazz = "floor";
			for (LineSegment obst : sec.getObstacleSegments()) {
				Polygon p = new Polygon();
				s.polygons.add(p);
				p.caption="wall";
				{
					Vertex v = new Vertex();
					v.px = obst.x0;
					v.py = obst.y0;
					p.vertices.add(v);
				}
				{
					Vertex v = new Vertex();
					v.px = obst.x1;
					v.py = obst.y1;
					p.vertices.add(v);
				}
			}
			for (Id<Section> nId : sec.getNeighbors()) {
				Section nSec = env.getSections().get(nId);
				String invCrId = nSec.getId().toString() + "_" + sec.getId();
				if (handledCrossings.contains(invCrId)) {
					continue;
				} 
				LineSegment crossing = sec.getOpening(nSec);
				Crossing c = new Crossing();
				r.crossings.add(c);
				String crId = sec.getId().toString() + "_" + nSec.getId();
				c.id = crId; //this won't work this JuPedSim expects int as id
				c.subroom1Id = sec.getId().toString();//this won't work this JuPedSim expects int as id
				c.subroom2Id = nSec.getId().toString();//this won't work this JuPedSim expects int as id
				c.v1 = new Vertex();
				c.v1.px = crossing.x0;
				c.v1.py = crossing.y0;
				c.v2 = new Vertex();
				c.v2.px = crossing.x1;
				c.v2.py = crossing.y1;
				handledCrossings.add(crId);
			}

			for (LineSegment open : sec.getOpeningSegments()) {
				Section nSec = sec.getNeighbor(open);
				if (nSec == null) {
					Room ex = new Room();
					this.rooms.add(ex);
					ex.id = sec.getId().toString()+"_exit";
					Subroom subEx = new Subroom();
					ex.subrooms.add(subEx);
					ex.caption = ex.id;
					subEx.id = "0";
					subEx.closed = 0;
					subEx.clazz="floor";
					{
						Polygon p = new Polygon();
						p.caption = "wall";
						subEx.polygons.add(p);
						Vertex v1 = new Vertex();
						v1.px = open.x0;
						v1.py = open.y0;
						p.vertices.add(v1);
						Vertex v2 = new Vertex();
						v2.px = open.x0-TRANSITION_BOX_LENGTH*open.dy;
						v2.py = open.y0+TRANSITION_BOX_LENGTH*open.dx;
						p.vertices.add(v2);
					}
					{
						Polygon p = new Polygon();
						p.caption = "wall";
						subEx.polygons.add(p);
						Vertex v1 = new Vertex();
						v1.px = open.x1-TRANSITION_BOX_LENGTH*open.dy;
						v1.py = open.y1+TRANSITION_BOX_LENGTH*open.dx;
						p.vertices.add(v1);
						Vertex v2 = new Vertex();
						v2.px = open.x1;
						v2.py = open.y1;
						p.vertices.add(v2);
					}
					{
						Transition t = new Transition();
						this.transitions.add(t);
						t.caption = "exit";
						t.id = ex.id+"_"+r.id;
						t.type="emergency";
						t.room1Id =r.id;
						t.room2Id = ex.id;
						t.subRoom1Id = s.id;
						t.subRoom2Id = subEx.id;
						t.v1 = new Vertex();
						t.v1.px = open.x0;
						t.v1.py = open.y0;
						t.v2 = new Vertex();
						t.v2.px = open.x1;
						t.v2.py = open.y1;
					}
					{
						Transition t = new Transition();
						this.transitions.add(t);
						t.caption = "exit";
						t.id = ex.id+"_"+r.id;
						t.type="emergency";
						t.room1Id =ex.id;
						t.room2Id = "-1";
						t.subRoom1Id = subEx.id;
						t.subRoom2Id = "-1";
						t.v1 = new Vertex();
						t.v1.px = open.x0-TRANSITION_BOX_LENGTH*open.dy;
						t.v1.py = open.y0+TRANSITION_BOX_LENGTH*open.dx;
						t.v2 = new Vertex();
						t.v2.px = open.x1-TRANSITION_BOX_LENGTH*open.dy;
						t.v2.py = open.y1+TRANSITION_BOX_LENGTH*open.dx;
						
						
						Goal g = new Goal();
						this.goals.add(g);
						g.caption = "goal";
						g.id = sec.getId().toString();
						g.p = new Polygon();
						double x0 = t.v2.px-(TRANSITION_BOX_LENGTH+GOAL_OFFSET)*open.dy;
						double y0 = t.v2.py+(TRANSITION_BOX_LENGTH+GOAL_OFFSET)*open.dx;
						double x1 = t.v1.px-(TRANSITION_BOX_LENGTH+GOAL_OFFSET)*open.dy;
						double y1 = t.v1.py+(TRANSITION_BOX_LENGTH+GOAL_OFFSET)*open.dx;
						double x2 = t.v1.px-(2*TRANSITION_BOX_LENGTH+GOAL_OFFSET)*open.dy;
						double y2 = t.v1.py+(2*TRANSITION_BOX_LENGTH+GOAL_OFFSET)*open.dx;
						double x3 = t.v2.px-(2*TRANSITION_BOX_LENGTH+GOAL_OFFSET)*open.dy;
						double y3 = t.v2.py+(2*TRANSITION_BOX_LENGTH+GOAL_OFFSET)*open.dx;
						{
							Vertex v = new Vertex();
							v.px = x0;
							v.py = y0;
							g.p.vertices.add(v);
						}
						{
							Vertex v = new Vertex();
							v.px = x1;
							v.py = y1;
							g.p.vertices.add(v);
						}
						{
							Vertex v = new Vertex();
							v.px = x2;
							v.py = y2;
							g.p.vertices.add(v);
						}
						{
							Vertex v = new Vertex();
							v.px = x3;
							v.py = y3;
							g.p.vertices.add(v);
						}
						{
							Vertex v = new Vertex();
							v.px = x0;
							v.py = y0;
							g.p.vertices.add(v);
						}
					}

				} 
			}
		}
		
		fixIds();

	}

	private void fixIds() {
		Map<String,String> rooms = new HashMap<>();
		rooms.put("-1", "-1");
		Map<String,Map<String,String>> rs = new HashMap<>();
		Map<String,String> tmp = new HashMap<>();
		tmp.put("-1", "-1");
		rs.put("-1", tmp);
		int rIds = 0;
		for (Room r : this.rooms) {
			String newId = rIds+++"";
			rooms.put(r.id, newId);
			Map<String,String> subs = new HashMap<>();
			subs.put("-1", "-1");
			r.id = newId;
			rs.put(r.id, subs);
			
			int sIds = 0;
			for (Subroom s : r.subrooms) {
				String newSId = sIds+++"";
				subs.put(s.id,newSId);
				s.id = newSId;
			}
			int cIds = 0;
			for (Crossing cr : r.crossings) {
				cr.id = cIds+++"";
				cr.subroom1Id = subs.get(cr.subroom1Id);
				cr.subroom2Id = subs.get(cr.subroom2Id);
			}
		}
		int trIds = 0;
		for (Transition tr : this.transitions) {
			tr.id = trIds+++"";
			tr.room1Id = rooms.get(tr.room1Id);
			Map<String, String> subs1 = rs.get(tr.room1Id);
			tr.subRoom1Id = subs1.get(tr.subRoom1Id);
			tr.room2Id = rooms.get(tr.room2Id);
			Map<String, String> subs2 = rs.get(tr.room2Id);
			tr.subRoom2Id = subs2.get(tr.subRoom2Id);
		}
		
	}

	static final class Room {
		String id;
		String caption;
		List<Subroom> subrooms = new ArrayList<>();
		List<Crossing> crossings = new ArrayList<>();

	}

	static final class Subroom {
		String id;
		int closed;
		String clazz;
		List<Polygon> polygons = new ArrayList<>();
	}

	static final class Polygon {
		String caption;
		List<Vertex> vertices = new ArrayList<>();
	}

	static final class Vertex {
		double px;
		double py;
	}

	static final class Transition {
		String id;
		String caption = "exit";
		String type = "emergency";

		String room1Id;
		String room2Id;
		String subRoom1Id;
		String subRoom2Id;

		Vertex v1;
		Vertex v2;
	}

	static final class Crossing {
		String id;
		String subroom1Id;
		String subroom2Id;
		Vertex v1;
		Vertex v2;
	}
	
	static final class Goal {
		String id;
		String finalGoal = "true";
		String caption;
		Polygon p;
	}

}
