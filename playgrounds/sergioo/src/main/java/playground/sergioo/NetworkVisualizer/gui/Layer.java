package playground.sergioo.NetworkVisualizer.gui;

import java.awt.Graphics2D;

public class Layer {
	
	//Attributes
	private boolean visible;
	private final Painter painter;
	
	//Method
	protected Layer(Painter painter) {
		super();
		this.painter = painter;
		visible = true;
	}
	public Painter getPainter() {
		return painter;
	}
	public void paint(Graphics2D g2, Camera camera) throws Exception {
		if(visible)
			painter.paint(g2, camera);
	}
	public void changeVisible() {
		visible = !visible;
	}
	
}
