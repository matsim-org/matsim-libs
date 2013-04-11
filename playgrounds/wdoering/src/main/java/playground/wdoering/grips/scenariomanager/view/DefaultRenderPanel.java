package playground.wdoering.grips.scenariomanager.view;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;

import javax.swing.BorderFactory;
import javax.swing.JPanel;

import playground.wdoering.grips.scenariomanager.control.Controller;

public class DefaultRenderPanel extends JPanel
{
	private Visualizer visualizer;
	private int border;
	private BufferedImage image;

	public DefaultRenderPanel(Controller controller)
	{
		this(controller.getVisualizer(), (BufferedImage) controller.getImageContainer().getImage(), controller.getImageContainer().getBorderWidth());
	}

	public DefaultRenderPanel(Visualizer visualizer, BufferedImage image, int border)
	{
		this.visualizer = visualizer;
		this.image = image;
		this.border = border;
		this.setBorder(BorderFactory.createLineBorder(Color.pink, 15));
	}

	@Override
	public void paint(Graphics g)
	{
		this.visualizer.paintLayers();
		// g.setColor(Color.black);
		// g.fillRect(0, 0, this.getParent().getWidth(),
		// this.getParent().getHeight());
		g.drawImage((BufferedImage) this.image, this.border, this.border, null);
	}
}