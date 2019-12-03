package org.matsim.osmNetworkReader;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.matsim.api.core.v01.Coord;

@RequiredArgsConstructor
@Getter
class LightOsmNode {

	private final long id;
	private final boolean preserve;
	private final Coord coord;
}