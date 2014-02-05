package josmMatsimPlugin;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.network.LinkImpl;
import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.osm.visitor.paint.StyledMapRenderer;
import org.openstreetmap.josm.gui.NavigatableComponent;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.gui.mappaint.LabelCompositionStrategy;
import org.openstreetmap.josm.gui.mappaint.TextElement;

public class MapRenderer extends StyledMapRenderer {
	private final Font FONT = new Font(Font.SANS_SERIF, Font.PLAIN, 14);
	private final CompositionStrategy STRATEGY = new CompositionStrategy();
	private final Color MATSIMCOLOR = new Color(80, 145, 190);
	protected static float wayOffset = ((float) Main.pref.getDouble("matsim_wayOffset", 1.5)); 

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

		if (Main.pref.getBoolean("matsim_renderer", true)) {
			Layer layer = Main.main.getActiveLayer();
			Id id = new IdImpl(way.getUniqueId());
			if (layer instanceof NetworkLayer) {
				Network network = ((NetworkLayer) layer).getMatsimNetwork();
				if (network.getLinks().containsKey(id)) {
					Link link = network.getLinks().get(id);
					if (Main.pref.getBoolean("matsim_showIds", false)) {
						drawTextOnPath(way, new TextElement(STRATEGY, FONT, 0,
								textOffset(network, link), MATSIMCOLOR, 0.f,
								null));
					}
					if (!way.isSelected()) {
						super.drawWay(way, MATSIMCOLOR, line, dashes,
								dashedColor, wayOffset(network, link),
								showOrientation, showHeadArrowOnly, showOneway, onewayReversed);
						return;
					}
				}
			}
		}
		super.drawWay(way, color, line, dashes, dashedColor, offset,
				showOrientation, showHeadArrowOnly, showOneway, onewayReversed);
	}

	private float wayOffset(Network network, Link link) {
		for (Link link2 : network.getLinks().values()) {
			if (link2.getFromNode().equals(link.getToNode())
					&& link2.getToNode().equals(link.getFromNode())) {
				
				return wayOffset;
			}
		}
		return 0.f;
	}

	private int textOffset(Network network, Link link) {
		int offset = -10;
		for (Link link2 : network.getLinks().values()) {
			if (link2.getFromNode().equals(link.getToNode())
					&& link2.getToNode().equals(link.getFromNode())) {
				if (Long.parseLong(link.getToNode().getId().toString()) < Long
						.parseLong(link.getFromNode().getId().toString())) {
					offset *= -1;
				}
				break;
			}
		}
		return offset;
	}

	private class CompositionStrategy extends LabelCompositionStrategy {

		@Override
		public String compose(OsmPrimitive prim) {
			Layer layer = Main.main.getActiveLayer();
			Id id = new IdImpl(prim.getUniqueId());
			return ((LinkImpl) ((NetworkLayer) layer).getMatsimNetwork()
					.getLinks().get(id)).getOrigId();
		}

	}
	
}
