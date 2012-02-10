package playground.sergioo.Visualizer2D.NetworkVisualizer;

import java.awt.Graphics2D;
import java.awt.Image;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import org.matsim.api.core.v01.Coord;

import playground.sergioo.Visualizer2D.LayersPanel;
import playground.sergioo.Visualizer2D.Painter;

public class ImagePainter extends Painter {
	
	//Attributes
	private final Image image;
	private int[] imagePosition;
	private Coord upLeft;
	private Coord downRight;
	               
	//Constructors
	public ImagePainter(Image image, LayersPanel layersPanel) {
		this.image = image;
		imagePosition = new int[]{0, 0, layersPanel.getWidth(), layersPanel.getHeight(), 0, 0, image.getWidth(layersPanel), image.getHeight(layersPanel)};
	}
	public ImagePainter(File file, LayersPanel layersPanel) throws IOException {
		image = ImageIO.read(file);
		imagePosition = new int[]{0, 0, layersPanel.getWidth(), layersPanel.getHeight(), 0, 0, image.getWidth(layersPanel), image.getHeight(layersPanel)};
	}
	
	//Methods
	@Override
	public void paint(Graphics2D g, LayersPanel layersPanel) {
		if(imagePosition!=null)
			g.drawImage(image, imagePosition[0], imagePosition[1], imagePosition[2], imagePosition[3], imagePosition[4], imagePosition[5], imagePosition[6], imagePosition[7], layersPanel);
		else
			g.drawImage(image, layersPanel.getScreenX(upLeft.getX()), layersPanel.getScreenY(upLeft.getY()), layersPanel.getScreenX(downRight.getX()), layersPanel.getScreenY(downRight.getY()), 0, 0, image.getWidth(layersPanel), image.getHeight(layersPanel), layersPanel);
	}
	public Coord getUpLeft() {
		return upLeft;
	}
	public Coord getDownRight() {
		return downRight;
	}
	public void setImagePosition(int dx1, int dy1, int dx2, int dy2, int sx1, int sy1, int sx2, int sy2) {
		imagePosition = new int[]{dx1, dy1, dx2, dy2, sx1, sy1, sx2, sy2};
		upLeft = null;
		downRight = null;
	}
	public void setImageCoordinates(Coord upLeft, Coord downRight) {
		imagePosition = null;
		this.upLeft = upLeft;
		this.downRight = downRight;
	}
}
