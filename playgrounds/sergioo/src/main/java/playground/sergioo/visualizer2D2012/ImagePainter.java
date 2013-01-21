package playground.sergioo.visualizer2D2012;

import java.awt.Graphics2D;
import java.awt.Image;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;


public class ImagePainter extends Painter {
	
	//Attributes
	private final Image image;
	private int[] imagePosition;
	private double[] upLeft;
	private double[] downRight;
	               
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
		else {
			int[] upLeftP = layersPanel.getScreenXY(upLeft);
			int[] downRightP = layersPanel.getScreenXY(downRight);
			g.drawImage(image, upLeftP[0], upLeftP[1], downRightP[0], downRightP[1], 0, 0, image.getWidth(layersPanel), image.getHeight(layersPanel), layersPanel);
		}
	}
	public double[] getUpLeft() {
		return upLeft;
	}
	public double[] getDownRight() {
		return downRight;
	}
	public void setImagePosition(int dx1, int dy1, int dx2, int dy2, int sx1, int sy1, int sx2, int sy2) {
		imagePosition = new int[]{dx1, dy1, dx2, dy2, sx1, sy1, sx2, sy2};
		upLeft = null;
		downRight = null;
	}
	public void setImageCoordinates(double[] upLeft, double[] downRight) {
		imagePosition = null;
		this.upLeft = upLeft;
		this.downRight = downRight;
	}
}
