package playground.wrashid.tryouts.processing;

public class ExampleUsingMovingAndZoom extends MovableAndZoomable {

	public void setup() {
		super.setup();
		smooth();
	}	
		
	
	@Override
	public void draw(){
		super.draw();
		
		fill(255, 255, 255);
		rect(-1000, -1000, 10000, 10000);

		fill(255, 0, 0);
		rect(100, 100, 100, 100);
		fill(0, 255, 0);
		rect(800, 800, 100, 100);
	}
	
}
