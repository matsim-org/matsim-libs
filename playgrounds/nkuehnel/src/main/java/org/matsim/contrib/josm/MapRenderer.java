package org.matsim.contrib.josm;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.network.LinkImpl;
import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.Preferences.PreferenceChangeEvent;
import org.openstreetmap.josm.data.Preferences.PreferenceChangedListener;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.osm.visitor.paint.MapRendererFactory;
import org.openstreetmap.josm.data.osm.visitor.paint.StyledMapRenderer;
import org.openstreetmap.josm.gui.NavigatableComponent;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.gui.mappaint.LabelCompositionStrategy;
import org.openstreetmap.josm.gui.mappaint.TextElement;

/**
 * The MATSim MapRenderer. Draws ways that correspond to existing MATSim link(s)
 * in a MATSim-blue color. Also offers offset for overlapping links as well as
 * the option to show MATSim ids on ways
 * 
 * @author Nico
 */
public class MapRenderer extends StyledMapRenderer {

	/**
	 * Creates a new MapRenderer. Initialized by
	 * <strong>MapRendererFactory</strong>
	 * 
	 * @see StyledMapRenderer
	 * @see MapRendererFactory
	 */
	public MapRenderer(Graphics2D arg0, NavigatableComponent arg1, boolean arg2) {
		super(arg0, arg1, arg2);
	}

	/**
	 * Maps links to their corresponding way.
	 */
	private static Map<Way, List<Link>> way2Links = new HashMap<Way, List<Link>>();

	public static void setWay2Links(Map<Way, List<Link>> way2LinksTmp) {
		way2Links = way2LinksTmp;
	}

	/**
	 * Draws a <code>way</code>. Ways that are mapped in <code>way2Links</code>
	 * and represent MATSim links are drawn in a blue color. If "show Ids" is
	 * turned on, Ids of the links are drawn on top or below the
	 * <code>way</code>.
	 * 
	 * @see #textOffset(Way)
	 * @param showOrientation
	 *            show arrows that indicate the technical orientation of the way
	 *            (defined by order of nodes)
	 * @param showOneway
	 *            show symbols that indicate the direction of the feature, e.g.
	 *            oneway street or waterway
	 * @param onewayReversed
	 *            for oneway=-1 and similar
	 */
	@Override
	public void drawWay(Way way, Color color, BasicStroke line,
			BasicStroke dashes, Color dashedColor, float offset,
			boolean showOrientation, boolean showHeadArrowOnly,
			boolean showOneway, boolean onewayReversed) {

		// could be used as filter for non converted ways in future releases
		// if(!way2Links.containsKey(way)) {
		// return;
		// }

		Layer layer = Main.main.getActiveLayer();
		if (layer instanceof OsmDataLayer) {
			if (way2Links.containsKey(way)) {
				if (!way2Links.get(way).isEmpty()) {
					if (!way.isSelected()) {
						if (Properties.showIds) { // draw id on path
							drawTextOnPath(way,
									new TextElement(Properties.getInstance(),
											Properties.FONT, 0,
											textOffset(way),
											Properties.MATSIMCOLOR, 0.f, null));
						}
						if (way.hasTag("modes", TransportMode.pt)) { // draw
																		// dashed
																		// lines
																		// for
																		// pt
																		// links
							float[] dashPhase = { 9.f };
							BasicStroke trainDashes = new BasicStroke(2, 0, 1,
									10.f, dashPhase, 9.f);
							super.drawWay(way, Properties.MATSIMCOLOR, line,
									trainDashes, Color.white,
									Properties.wayOffset * -1, showOrientation,
									showHeadArrowOnly, !way.hasTag("highway",
											OsmConvertDefaults.getDefaults()
													.keySet()), onewayReversed);
						} else { // draw simple blue lines for other links, if
									// way is not converted by highway tag, draw
									// direction arrow for directed edge
							super.drawWay(way, Properties.MATSIMCOLOR, line,
									dashes, dashedColor, Properties.wayOffset
											* -1, showOrientation,
									showHeadArrowOnly, !way.hasTag("highway",
											OsmConvertDefaults.getDefaults()
													.keySet()), onewayReversed);
						}
						return;
					} else {
						if (Properties.showIds) { // draw ids on selected ways
													// also
							drawTextOnPath(way,
									new TextElement(Properties.getInstance(),
											Properties.FONT, 0,
											textOffset(way), selectedColor,
											0.f, null));
						}
					}
				}
			}
		}
		super.drawWay(way, color, line, dashes, dashedColor,
				Properties.wayOffset * -1, showOrientation, showHeadArrowOnly,
				showOneway, onewayReversed);
	}

	/**
	 * Returns the text <code>offset</code> for the given <code>way</code>.
	 * <strong>Positive</strong>, if the <code>id</code> of the <code>way</code>
	 * 's first node is less than it's last node's <code>id</code>.
	 * <strong>Negative</strong> otherwise.
	 * 
	 * @param way
	 *            The way which offset is to be calculated
	 * @return The text offset for the given <code>way</code>
	 */
	private int textOffset(Way way) {
		int offset = -15;

		if (way.firstNode().getUniqueId() < way.lastNode().getUniqueId()) {
			offset *= -1;
		}
		return offset;
	}

	/**
	 * The properties for the text elements used to visualize the Ids of the
	 * MATSim links.
	 * 
	 * @author Nico
	 * 
	 */
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

		@SuppressWarnings("rawtypes")
		@Override
		// listen for changes in preferences that concern renderer adjustments
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

		/**
		 * Composes the MATSim Id text for the OsmPrimitive <code>prim</code>.
		 * Multiple MATSim Ids are consecutively appended. <br>
		 * <br>
		 * Example: <br>
		 * [{@code Id1}] [{@code Id2}] [{@code Id3}]
		 * 
		 * 
		 * @param prim
		 *            The given Primitive. Only Ways can represent MATSim
		 *            link-Ids
		 * @return The [id]s of the links represented by the given Primitive
		 *         <code>prim</code> or an empty string if no link is
		 *         represented.
		 */
		@Override
		public String compose(OsmPrimitive prim) {
			StringBuilder sB = new StringBuilder();
			if (way2Links.containsKey(prim)) {
				for (Link link : way2Links.get(prim)) {
					sB.append(" [").append(((LinkImpl) link).getOrigId())
							.append("] ");
				}
			}
			return sB.toString();
		}
	}
}
