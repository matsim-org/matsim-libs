package josmMatsimPlugin;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;

import org.matsim.api.core.v01.Id;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.network.LinkImpl;
import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.osm.visitor.paint.StyledMapRenderer;
import org.openstreetmap.josm.gui.NavigatableComponent;
import org.openstreetmap.josm.gui.layer.Layer;

public class MapRenderer extends StyledMapRenderer {

    public MapRenderer(Graphics2D arg0, NavigatableComponent arg1, boolean arg2) {
	super(arg0, arg1, arg2);
    }

    /**
     * draw way
     * 
     * @param showOrientation
     *            show arrows that indicate the technical orientation of the way
     *            (defined by order of nodes)
     * @param showOneway
     *            show symbols that indicate the direction of the feature, e.g.
     *            oneway street or waterway
     * @param onewayReversed
     *            for oneway=-1 and similar
     */
    public void drawWay(Way way, Color color, BasicStroke line,
	    BasicStroke dashes, Color dashedColor, float offset,
	    boolean showOrientation, boolean showHeadArrowOnly,
	    boolean showOneway, boolean onewayReversed) {

	Layer layer = Main.main.getActiveLayer();
	Id id = new IdImpl(way.getUniqueId());
	if (layer instanceof NetworkLayer) {
	    if (((NetworkLayer) layer).getMatsimNetwork().getLinks()
		    .containsKey(id)) {
		Color matsimColor = new Color(80, 145, 190);
		super.drawWay(way, matsimColor, line, dashes, dashedColor,
			offset, showOrientation, showHeadArrowOnly, showOneway,
			onewayReversed);
	    } else
		super.drawWay(way, color, line, dashes, dashedColor, offset,
			showOrientation, showHeadArrowOnly, showOneway,
			onewayReversed);
	} else
	    super.drawWay(way, color, line, dashes, dashedColor, offset,
		    showOrientation, showHeadArrowOnly, showOneway,
		    onewayReversed);

    }
}
