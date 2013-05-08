package playground.wdoering.grips.scenariomanager.view;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.image.BufferedImage;

import javax.swing.BorderFactory;
import javax.swing.JPanel;

import playground.wdoering.grips.scenariomanager.control.Controller;
import playground.wdoering.grips.scenariomanager.model.imagecontainer.ImageContainerInterface;

public class DefaultRenderPanel extends JPanel implements ComponentListener
{
	private Visualizer visualizer;
	private Controller controller;
	
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
		this.addComponentListener(this);
	}

	@Override
	public void paint(Graphics g)
	{
		this.visualizer.paintLayers();
		g.drawImage((BufferedImage) this.image, this.border, this.border, null);
	}
	
	public void updateImageContainer()
	{
		this.image = visualizer.getBufferedImage();
		this.border = visualizer.getImageContainer().getBorderWidth();
	}

	@Override
	public void componentHidden(ComponentEvent e) {}
	@Override
	public void componentMoved(ComponentEvent e) {}
	@Override
	public void componentResized(ComponentEvent e)
	{
		updateImageContainer();
	}
	@Override
	public void componentShown(ComponentEvent e) {}
}