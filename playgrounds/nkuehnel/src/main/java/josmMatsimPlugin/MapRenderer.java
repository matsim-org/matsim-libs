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
import org.openstreetmap.josm.data.Preferences.PreferenceChangeEvent;
import org.openstreetmap.josm.data.Preferences.PreferenceChangedListener;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.osm.visitor.paint.StyledMapRenderer;
import org.openstreetmap.josm.gui.NavigatableComponent;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.gui.mappaint.LabelCompositionStrategy;
import org.openstreetmap.josm.gui.mappaint.TextElement;

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
			Network network = ((NetworkLayer) layer).getMatsimNetwork();
			if (network.getLinks().containsKey(id)) {
				Link link = network.getLinks().get(id);
				if (!way.isSelected()) {
					if (Properties.showIds) {
						drawTextOnPath(
								way,
								new TextElement(Properties.getInstance(),
										Properties.FONT, 0, textOffset(network,
												link), Properties.MATSIMCOLOR,
										0.f, null));
					}
					super.drawWay(way, Properties.MATSIMCOLOR, line, dashes,
							dashedColor, wayOffset(network, link),
							showOrientation, showHeadArrowOnly, showOneway,
							onewayReversed);
					return;
				} else {
					if (Properties.showIds) {
						drawTextOnPath(
								way,
								new TextElement(Properties.getInstance(),
										Properties.FONT, 0, textOffset(network,
												link), selectedColor, 0.f, null));
					}
				}
			}
		}
		super.drawWay(way, color, line, dashes, dashedColor, offset,
				showOrientation, showHeadArrowOnly, showOneway, onewayReversed);
	}

	private float wayOffset(Network network, Link link) {
		return Properties.wayOffset;
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

	static class Properties extends LabelCompositionStrategy implements
			PreferenceChangedListener {

		private final static Properties INSTANCE = new Properties();
		final static Font FONT = new Font(Font.SANS_SERIF, Font.PLAIN, 14);
		final static Color MATSIMCOLOR = new Color(80, 145, 190);
		static boolean showIds = Main.pref.getBoolean("matsim_showIds", false);
		static float wayOffset = ((float) Main.pref.getDouble(
				"matsim_wayOffset", 0));

		public static void initialize() {
			Main.pref.addPreferenceChangeListener(INSTANCE);
		}

		@Override
		public void preferenceChanged(PreferenceChangeEvent e) {
			if (e.getKey().equalsIgnoreCase("matsim_showIds")) {
				showIds = Main.pref.getBoolean("matsim_showIds");
			}
			if (e.getKey().equalsIgnoreCase("matsim_wayOffset")) {
				wayOffset = ((float) (Main.pref
						.getDouble("matsim_wayOffset", 0)));
			}
		}

		public static Properties getInstance() {
			return INSTANCE;
		}

		@Override
		public String compose(OsmPrimitive prim) {
			Layer layer = Main.main.getActiveLayer();
			Id id = new IdImpl(prim.getUniqueId());
			return ((LinkImpl) ((NetworkLayer) layer).getMatsimNetwork()
					.getLinks().get(id)).getOrigId();
		}
	}
}
