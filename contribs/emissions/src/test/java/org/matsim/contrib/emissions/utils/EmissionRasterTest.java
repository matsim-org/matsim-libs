package org.matsim.contrib.emissions.utils;

import org.junit.Test;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.network.NetworkUtils;
import org.matsim.examples.ExamplesUtils;

import javax.swing.*;
import java.awt.*;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;

public class EmissionRasterTest {

	@Test
	public void addLink() throws InterruptedException, MalformedURLException {

		URL equil = URI.create(ExamplesUtils.getTestScenarioURL("equil").toString() + "network.xml").toURL();
		Network network = NetworkUtils.readNetwork(equil.toString());

		EmissionRaster emissionRaster = new EmissionRaster(100, network);
		//displayOnPanel(emissionRaster, network);

		Thread.sleep(20000);
	}

	// little hack to visualize the network and the raster. This will go away, or move somewhere else
	private void displayOnPanel(EmissionRaster emissionRaster, Network network) {
		JFrame frame = new JFrame();
		frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		frame.setTitle("Raster");
		frame.getContentPane().add(new RasterPanel(emissionRaster, network));
		frame.pack();
		frame.setLocationRelativeTo(null);
		frame.setVisible(true);
	}

	private static class RasterPanel extends JPanel {

		private final EmissionRaster emissionRaster;
		private final Network network;

		public RasterPanel(EmissionRaster emissionRaster, Network network) {

			setPreferredSize(new Dimension(1000, 1000));
			setBackground(Color.WHITE);
			this.emissionRaster = emissionRaster;
			this.network = network;
		}

		@Override
		protected void paintComponent(Graphics g) {
			super.paintComponent(g);

			int divideBy = 20;
			int offset = 20 * divideBy;

			g.setColor(Color.BLACK);
			for (EmissionRaster.Cell cell : emissionRaster.getCells()) {

				double left = offset + cell.getCoord().getX() / divideBy;
				double top = offset + cell.getCoord().getY() / divideBy;
				g.drawRect((int) left, (int) top, emissionRaster.getCellSize() / divideBy, emissionRaster.getCellSize() / divideBy);
			}

			g.setColor(Color.RED);
			for (Link link : network.getLinks().values()) {
				g.drawLine(
						offset + (int) link.getFromNode().getCoord().getX() / divideBy,
						offset + (int) link.getFromNode().getCoord().getY() / divideBy,
						offset + (int) link.getToNode().getCoord().getX() / divideBy,
						offset + (int) link.getToNode().getCoord().getY() / divideBy
				);
			}
		}
	}
}