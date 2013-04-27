package playground.pieter.singapore.hits;

import java.util.Set;

import org.matsim.api.core.v01.Coord;
import org.matsim.pt.transitSchedule.api.TransitLine;

public class TransitStageRoutingInput {
	Coord orig;

	public TransitStageRoutingInput(Coord orig, Coord dest,
			Set<TransitLine> lines, boolean busStage) {
		super();
		this.orig = orig;
		this.dest = dest;
		this.lines = lines;
		this.busStage = busStage;
	}

	Coord dest;
	boolean busStage;
	Set<TransitLine> lines;
}