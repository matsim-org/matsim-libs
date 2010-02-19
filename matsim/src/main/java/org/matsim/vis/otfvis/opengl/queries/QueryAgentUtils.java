package org.matsim.vis.otfvis.opengl.queries;

import java.awt.Color;
import java.util.List;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.vis.snapshots.writers.PositionInfo;

public class QueryAgentUtils {
	

	public static void buildRoute(Plan plan, QueryAgentPlan.Result result, Id agentId, Network net) {
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
				Color col = actColor;
				Activity act = (Activity) o;
				Coord coord = act.getCoord();
				if (coord == null) {
					assert (net != null);
					Link link = net.getLinks().get(act.getLinkId());
					PositionInfo pi = new PositionInfo(agentId, link);
					coord = new CoordImpl(pi.getEasting(), pi.getNorthing());
				}
				setCoord(pos++, coord, col, result);
			} else if (o instanceof LegImpl) {
				Leg leg = (Leg) o;
				if (leg.getMode().equals(TransportMode.car)) {
					Node last = null;
					for (Id linkId : ((NetworkRoute) leg.getRoute())
							.getLinkIds()) {
						Link driven = net.getLinks().get(linkId);
						Node node = driven.getFromNode();
						last = driven.getToNode();
						setCoord(pos++, node.getCoord(), carColor, result);
					}
					if (last != null) {
						setCoord(pos++, last.getCoord(), carColor, result);
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
			if (o instanceof ActivityImpl) {
				count++;
			} else if (o instanceof LegImpl) {
				LegImpl leg = (LegImpl) o;
				if (leg.getMode().equals(TransportMode.car)) {
					List<Id> route = ((NetworkRoute) leg.getRoute())
							.getLinkIds();
					count += route.size();
					if (route.size() != 0)
						count++; // add last position if there is a path
				}
			}
		}
		return count;
	}

}
