/*
 * Copyright 2015 Gunnar Flötteröd
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * contact: gunnar.floetteroed@abe.kth.se
 *
 */ 
package floetteroed.utilities.visualization;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.geom.AffineTransform;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

/**
 * <u><b>The entire utilitis.visualization package is experimental!</b></u>
 * 
 * @author Gunnar Flötteröd
 * 
 */
public class NetVis {

	// -------------------- MEMBERS --------------------

	private JFrame vizFrame;

	private NetVisScrollPane networkScrollPane;

	private NetJComponent networkComponent;

	private ControlToolbar buttonComponent;

	// -------------------- CONSTRUCTION --------------------

	public NetVis(final VisConfig visConfig, final VisNetwork net,
			final RenderableDynamicData<VisLink> data) {

		/*
		 * (1) create renderers
		 */
		final AbstractRenderer linkSetRenderer = new LinkRenderer(visConfig,
				net, data);
		final AbstractRenderer backgroundRenderer = new BackgroundRenderer(
				visConfig, net);
		final NodeRenderer nodeSetRenderer = new NodeRenderer(visConfig, net);
		final AbstractRenderer mainRenderer = new LabelRenderer(visConfig, net);
		mainRenderer.append(linkSetRenderer);
		linkSetRenderer.append(nodeSetRenderer);
		nodeSetRenderer.append(backgroundRenderer);

		/*
		 * (2) create frame
		 */
		this.vizFrame = new JFrame(net.getId());
		this.vizFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		JFrame.setDefaultLookAndFeelDecorated(true);

		/*
		 * (3) create control bar
		 */
		this.buttonComponent = new ControlToolbar(this, data, visConfig);
		this.vizFrame.getContentPane().add(this.buttonComponent,
				BorderLayout.NORTH);

		/*
		 * (4) create drawing area
		 */
		this.networkComponent = new NetJComponent(mainRenderer, visConfig);
		mainRenderer.setComponent(this.networkComponent);
		this.networkScrollPane = new NetVisScrollPane(this.networkComponent);
		this.vizFrame.getContentPane().add(this.networkScrollPane,
				BorderLayout.CENTER);
		final NetVisMouseHandler mouseHandler = new NetVisMouseHandler(this);
		this.networkScrollPane.addMouseMotionListener(mouseHandler);
		this.networkScrollPane.addMouseListener(mouseHandler);
		this.networkScrollPane.getViewport().addChangeListener(mouseHandler);
	}

	// -------------------- IMPLEMENTATION -------------------------

	void scaleNetwork(double scale) {
		this.networkScrollPane.scaleNetwork((float) scale);
	}

	void repaintForMovie() {
		this.networkComponent.repaint();
		this.buttonComponent.repaintForMovie();
	}

	Component networkComponent() {
		return this.networkComponent;
	}

	NetVisScrollPane networkScrollPane() {
		return this.networkScrollPane;
	}

	ControlToolbar buttonComponent() {
		return this.buttonComponent;
	}

	public void paintNow() {
		if (this.vizFrame != null)
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					vizFrame.paint(vizFrame.getGraphics());
				}
			});
	}

	public void run() {
		this.vizFrame.pack();
		this.vizFrame.setVisible(true);
	}

	// -------------------- STATIC IMPLEMENTATION --------------------

	// TODO should probably move this elsewhere

	/*
	 * maps (x,y) onto world coordinates, where x is distance from this link's
	 * startPoint and y is a normal shift to this link.
	 * 
	 * this transform allows to draw anything "from left to right" onto the link
	 * without having to worry about the link's true direction.
	 */
	static AffineTransform newLinear2PlaneTransform(double offset_m,
			double displayedLength_m, double startEasting,
			double startNorthing, double endEasting, double endNorthing,
			double length_m) {

		// 3. translate link onto original position
		double tx = startEasting;
		double ty = startNorthing;
		AffineTransform result = AffineTransform.getTranslateInstance(tx, ty);

		// 2. rotate link into original direction
		double dx = endEasting - startEasting;
		double dy = endNorthing - startNorthing;
		double theta = Math.atan2(dy, dx);
		result.rotate(theta);

		// 1. scale link
		double sx = displayedLength_m / length_m;
		double sy = 1;
		result.scale(sx, sy);

		// 0. translate link by target offset
		tx = offset_m * length_m / displayedLength_m;
		ty = 0;
		result.translate(tx, ty);

		// result = 3.translate o 2.rotate o 1.scale o 0.translate
		return result;
	}

	static AffineTransform newLinear2PlaneTransform(final VisLink link) {

		final double startEasting = link.getFromNode().getEasting();
		final double startNorthing = link.getFromNode().getNorthing();
		final double endEasting = link.getToNode().getEasting();
		final double endNorthing = link.getToNode().getNorthing();

		final double deltaNorthing = endNorthing - startNorthing;
		final double deltaEasting = endEasting - startEasting;
		double result = deltaNorthing * deltaNorthing;
		result += deltaEasting * deltaEasting;
		double nodeDist_m = Math.sqrt(result);

		double offset_m = 0; // 5;
		double length_m = nodeDist_m - 2.0 * offset_m;
		if (length_m <= 0) {
			length_m = nodeDist_m / 2.0;
			offset_m = (nodeDist_m - length_m) / 2.0;
		}
		return NetVis.newLinear2PlaneTransform(offset_m, length_m,
				startEasting, startNorthing, endEasting, endNorthing,
				link.getLength_m());
	}

	// static AffineTransform newLinear2PlaneTransform(final BasicLink link,
	// final VisNodeData fromNodeData, final VisNodeData toNodeData,
	// final double lengthAttribute_m) {
	//
	// final double startEasting = fromNodeData.getEasting();
	// final double startNorthing = fromNodeData.getNorthing();
	// final double endEasting = toNodeData.getEasting();
	// final double endNorthing = toNodeData.getNorthing();
	//
	// final double deltaNorthing = endNorthing - startNorthing;
	// final double deltaEasting = endEasting - startEasting;
	// double result = deltaNorthing * deltaNorthing;
	// result += deltaEasting * deltaEasting;
	// double nodeDist_m = Math.sqrt(result);
	//
	// double offset_m = 0; // 5;
	// double length_m = nodeDist_m - 2.0 * offset_m;
	// if (length_m <= 0) {
	// length_m = nodeDist_m / 2.0;
	// offset_m = (nodeDist_m - length_m) / 2.0;
	// }
	// return newLinear2PlaneTransform(offset_m, length_m, startEasting,
	// startNorthing, endEasting, endNorthing, lengthAttribute_m);
	// }

	// -------------------- MAIN - FUNCTION --------------------

}
