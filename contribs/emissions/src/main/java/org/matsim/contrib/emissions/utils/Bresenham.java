package org.matsim.contrib.emissions.utils;

import javax.swing.*;
import java.awt.*;

public class Bresenham {

	public static void main(String[] args) {
		SwingUtilities.invokeLater(Bresenham::run);
	}

	private static void run() {
		JFrame f = new JFrame();
		f.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		f.setTitle("Bresenham");

		f.getContentPane().add(new BresenhamPanel());
		f.pack();

		f.setLocationRelativeTo(null);
		f.setVisible(true);
	}
}

class BresenhamPanel extends JPanel {

	private final int pixelSize = 10;

	BresenhamPanel() {
		setPreferredSize(new Dimension(600, 500));
		setBackground(Color.WHITE);
	}

	@Override
	public void paintComponent(Graphics g) {
		super.paintComponent(g);

		int w = (getWidth() - 1) / pixelSize;
		int h = (getHeight() - 1) / pixelSize;
		int maxX = (w - 1) / 2;
		int maxY = (h - 1) / 2;
		int x1 = -maxX, x2 = maxX * -2 / 3, x3 = maxX * 2 / 3, x4 = maxX;
		int y1 = -maxY, y2 = maxY * -2 / 3, y3 = maxY * 2 / 3, y4 = maxY;

//		drawLine(g, 0, 0, x3, y1); // NNE
//		drawLine(g, 0, 0, x4, y2); // ENE
//		drawLine(g, 0, 0, x4, y3); // ESE
//		drawLine(g, 0, 0, x3, y4); // SSE
//		drawLine(g, 0, 0, x2, y4); // SSW
//		drawLine(g, 0, 0, x1, y3); // WSW
//		drawLine(g, 0, 0, x1, y2); // WNW
//		drawLine(g, 0, 0, x2, y1); // NNW
		drawLineShort(g, 0, 0, x1, y1);
	}

	private void plot(Graphics g, int x, int y) {
		int w = (getWidth() - 1) / pixelSize;
		int h = (getHeight() - 1) / pixelSize;
		int maxX = (w - 1) / 2;
		int maxY = (h - 1) / 2;

		int borderX = getWidth() - ((2 * maxX + 1) * pixelSize + 1);
		int borderY = getHeight() - ((2 * maxY + 1) * pixelSize + 1);
		int left = (x + maxX) * pixelSize + borderX / 2;
		int top = (y + maxY) * pixelSize + borderY / 2;

		g.setColor(Color.black);
		g.drawRect(left, top, pixelSize, pixelSize);
	}

	private void drawLineShort(Graphics g, int x0, int y0, int x1, int y1) {

		int dx = Math.abs(x1 - x0);
		int dy = -Math.abs(y1 - y0);
		int err = dx + dy, e2;

		int sx = (x0 < x1 ? 1 : -1);
		int sy = (y0 < y1 ? 1 : -1);

		do {
			plot(g, x0, y0);
			e2 = err + err;

			if (e2 >= dy) {
				err += dy;
				x0 += sx;
			}
			if (e2 <= dx) {
				err += dx;
				y0 += sy;
			}
		} while (x0 != x1 || y0 != y1);
	}
}