package josmMatsimPlugin;

import org.matsim.api.core.v01.Coord;

/**
 * filter to be used when exporting Osm-Data
 * @author nkuehnel
 * 
 */
public class OsmFilter
{
	private final Coord coordNW;
	private final Coord coordSE;
	private final int hierarchy;

	public OsmFilter(final Coord coordNW, final Coord coordSE,
			final int hierarchy)
	{
		this.coordNW = coordNW;
		this.coordSE = coordSE;
		this.hierarchy = hierarchy;
	}

	public boolean coordInFilter(final Coord coord, final int hierarchyLevel)
	{
		if (this.hierarchy < hierarchyLevel)
		{
			return false;
		}

		return ((this.coordNW.getX() < coord.getX() && coord.getX() < this.coordSE
				.getX()) && (this.coordNW.getY() > coord.getY() && coord
				.getY() > this.coordSE.getY()));
	}
}
