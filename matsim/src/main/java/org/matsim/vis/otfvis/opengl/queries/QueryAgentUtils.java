package org.matsim.vis.otfvis.opengl.queries;

import java.awt.Color;
import java.util.List;

import org.apache.log4j.Logger;
import org.jfree.util.Log;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.pt.PtConstants;
import org.matsim.vis.snapshots.writers.AgentSnapshotInfo;
import org.matsim.vis.snapshots.writers.AgentSnapshotInfoFactory;

public class QueryAgentUtils {

	private static final Logger log = Logger.getLogger(QueryAgentUtils.class);


	enum Level { ROUTES, PLANELEMENTS } 
	static private Level myLevel ;

	/**
	 * @param plan
	 * @param result
	 * @param agentId
	 * @param net
	 * @param level -- level at which plan should be plotted, i.e. if route should be included or not.
	 */
	static void buildRoute(Plan plan, QueryAgentPlan.Result result, Id agentId, Network net, Level level) {
		// reducing visibility to avoid proliferation.  kai, jan'11
		
		myLevel = level ;
		
		int count = countLines(plan);
		if (count == 0)
			return;

		int pos = 0;
		result.vertex = new float[count * 2];
		result.colors = new byte[count * 4];

		Color carColor = Color.ORANGE;
		Color actColor = Color.BLUE;
		Color ptColor = Color.YELLOW;
		Color walkColor = Color.MAGENTA;
		Color otherColor = Color.PINK;

		for (Object o : plan.getPlanElements()) {
			if (o instanceof Activity) {
				Activity act = (Activity) o;
				if ( myLevel==Level.PLANELEMENTS && PtConstants.TRANSIT_ACTIVITY_TYPE.equals( act.getType() ) ) {
					continue ; // skip
				}
				Coord coord = act.getCoord();
				if (coord == null) {
					if ( net==null ) {
						log.info("no net info; not drawing activity location ") ;
						continue ;
					}
					Link link = net.getLinks().get(act.getLinkId());
					if ( link==null ) {
						log.info("can't find link with given id in net; not drawing activity location") ;
						continue ;
					}
					AgentSnapshotInfo pi = AgentSnapshotInfoFactory.staticCreateAgentSnapshotInfo(agentId, link);
					if ( pi.getEasting()==0. && pi.getNorthing()==0. ) {
						log.info("both coordinates are zero; this is implausible; not drawing activity location") ;
						continue ;
					}
					coord = new CoordImpl(pi.getEasting(), pi.getNorthing());
					log.info( " east: " + pi.getEasting() + " north: " + pi.getNorthing() );
				}
				setCoord(pos++, coord, actColor, result);
			} else if (o instanceof Leg) {
				Leg leg = (Leg) o;
				if (leg.getMode().equals(TransportMode.car)) {
					if ( level==Level.ROUTES ) {
						Node last = null;
						for (Id linkId : ((NetworkRoute) leg.getRoute()).getLinkIds()) {
							Link driven = net.getLinks().get(linkId);
							Node node = driven.getFromNode();
							last = driven.getToNode();
							setCoord(pos++, node.getCoord(), carColor, result);
						}
						if (last != null) {
							setCoord(pos++, last.getCoord(), carColor, result);
						}
					} else if ( level==Level.PLANELEMENTS ) {
						setColor( pos - 1, carColor, result ) ;
					} else {
						throw new RuntimeException("should not happen");
					}
				} else if (leg.getMode().equals(TransportMode.pt)) {
					setColor(pos - 1, ptColor, result);
				} else if (leg.getMode().equals(TransportMode.walk)) {
					setColor(pos - 1, walkColor, result);
				} else {
					setColor(pos - 1, otherColor, result); // replace act Color with pt
													// color... here we need
													// walk etc too
				}
			} // end leg handling
		}
	}

	private static void setColor(int pos, Color col, QueryAgentPlan.Result result) {
		result.colors[pos * 4 + 0] = (byte) col.getRed();
		result.colors[pos * 4 + 1] = (byte) col.getGreen();
		result.colors[pos * 4 + 2] = (byte) col.getBlue();
		result.colors[pos * 4 + 3] = (byte) 128;
	}

	private static void setCoord(int pos, Coord coord, Color col, QueryAgentPlan.Result result) {
		result.vertex[pos * 2 + 0] = (float) coord.getX();
		result.vertex[pos * 2 + 1] = (float) coord.getY();
		setColor(pos, col, result);
	}


	private static int countLines(Plan plan) {
		int count = 0;
		for (Object o : plan.getPlanElements()) {
			if (o instanceof Activity) {
				Activity act = (Activity) o ;
				if ( myLevel==Level.PLANELEMENTS && PtConstants.TRANSIT_ACTIVITY_TYPE.equals( act.getType() ) ) {
					continue ; // skip
				}
				count++;
			} else if (o instanceof Leg) {
				if ( myLevel==Level.PLANELEMENTS ) {
					continue ; // skip
				}
				Leg leg = (Leg) o;
				if (leg.getMode().equals(TransportMode.car)) {
					List<Id> route = ((NetworkRoute) leg.getRoute()).getLinkIds();
					count += route.size();
					if (route.size() != 0)
						count++; // add last position if there is a path
				}
			}
		}
		return count;
	}

}
