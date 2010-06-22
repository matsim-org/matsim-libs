package playground.pieter.demandgeneration.emme;

import org.matsim.api.core.v01.Id;

public class ZoneXY {
	private final String x, y;
	private final Id zoneId;

	public ZoneXY(final Id zoneId, final String x, final String y) {
		this.zoneId = zoneId;
		this.x = x;
		this.y = y;
	}

	public String getX() {
		return this.x;
	}

	public String getY() {
		return this.y;
	}

	public Id getZoneId() {
		return this.zoneId;
	}
}