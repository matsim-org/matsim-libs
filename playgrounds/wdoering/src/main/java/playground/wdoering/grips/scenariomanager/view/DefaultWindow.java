package playground.wdoering.grips.scenariomanager.view;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.image.BufferedImage;

import javax.swing.JFrame;
import javax.swing.JPanel;

import playground.wdoering.grips.scenariomanager.control.Controller;
import playground.wdoering.grips.scenariomanager.model.AbstractToolBox;
import playground.wdoering.grips.scenariomanager.model.Constants;

public class DefaultWindow extends JFrame implements ComponentListener
{
	protected Controller controller;
	protected final Visualizer visualizer;
	
	private int width;
	private int height;
	private int border;
	private BufferedImage image;
	protected JPanel mainPanel;
	private AbstractToolBox toolBox;

	public DefaultWindow(Controller controller)
	{
		if (controller.isMainWindowUndecorated())
			this.setUndecorated(true);

		this.controller = controller;
		this.visualizer = controller.getVisualizer();
		
		updateImageContainer();
		
		this.setLayout(new BorderLayout(this.border/2,this.border/2));
		
		this.mainPanel = new DefaultRenderPanel(this.visualizer, this.image, this.border);
		this.mainPanel.setPreferredSize(new Dimension(this.width, this.height));
		this.mainPanel.setSize(new Dimension(this.width, this.height));
		
		if (this.controller.getActiveToolBox()!=null)
		{
			this.toolBox = this.controller.getActiveToolBox();
			this.add(this.toolBox, BorderLayout.EAST);
		}
		this.add(mainPanel, BorderLayout.CENTER);
		
		int margin = Math.max(1,(this.getComponentCount()-1)) * this.border + 3*this.border;
		
		this.setPreferredSize(new Dimension(this.width + margin, this.height + margin));
		this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		this.setResizable(true);
		this.pack();
		this.validate();
		this.setLocationRelativeTo(null);
		this.setVisible(true);
		
		this.addComponentListener(this);

		
	}

	private void updateImageContainer()
	{
		this.width = this.controller.getImageContainer().getWidth();
		this.height = this.controller.getImageContainer().getHeight();
		this.border = this.controller.getImageContainer().getBorderWidth();
		this.image = this.controller.getImageContainer().getImage();
		
		
	}
	
	public void setToolBox(AbstractToolBox toolBox)
	{
		if (this.toolBox!=null)
			this.remove(this.toolBox);
		
		this.toolBox = toolBox;
		this.add(this.toolBox, BorderLayout.EAST);
		this.toolBox.setVisible(true);
		this.validate();
	}
	
	public AbstractToolBox getToolBox()
	{
		return toolBox;
	}
	
	public JPanel getMainPanel()
	{
		return mainPanel;
	}
	
	public void setMainPanel(JPanel mainPanel)
	{
		if (this.mainPanel != null)
			this.remove(this.mainPanel);
		
		this.mainPanel = mainPanel;
		this.add(mainPanel, BorderLayout.CENTER);
		this.validate();
	}
	
	public void updateMask()
	{
		
	}

	public void setToolBoxVisible(boolean toggle)
	{
		if (this.toolBox !=null)
		this.toolBox.setVisible(toggle);
	}

	@Override
	public void componentResized(ComponentEvent e)
	{
		
		int width, height;
		
		if (mainPanel != null)
		{
			width = mainPanel.getWidth();
			height = mainPanel.getHeight();
		}
		else
		{
			width = this.getContentPane().getWidth();
			height = this.getContentPane().getHeight();
		}
		
		if (width < Constants.FRAME_MIN_WIDTH)
			width = Constants.FRAME_MIN_WIDTH;

		if (height < Constants.FRAME_MIN_HEIGHT)
			height = Constants.FRAME_MIN_HEIGHT;
		
		//if size has changed in any dimension, build a new image and repaint main frame
		if ((controller.getImageContainer().getWidth() != width) || (controller.getImageContainer().getHeight() != height))
		{
			
			BufferedImage image = new BufferedImage(width - border * 2, height - border * 2, BufferedImage.TYPE_INT_ARGB);
			
			controller.getImageContainer().setImage(image);
			controller.updateMapLayerImages();
			updateImageContainer();
			
			if (controller.hasShapeRenderer())
				controller.getVisualizer().getPrimaryShapeRenderLayer().updatePixelCoordinates(true);
			
			if (this.mainPanel instanceof DefaultRenderPanel)
				((DefaultRenderPanel) this.mainPanel).updateImageContainer();
			
			controller.paintLayers();
		}
	}

	@Override
	public void componentMoved(ComponentEvent e) {}
	@Override
	public void componentShown(ComponentEvent e) {}
	@Override
	public void componentHidden(ComponentEvent e) {}

}
