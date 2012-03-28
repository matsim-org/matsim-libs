package playground.wrashid.tryouts.processing;

import processing.core.PApplet;

public class AnimationCharging extends PApplet {

	int i=0;
	
	public void setup() {

		size(1000, 1000);
		 frameRate(60);
	}
	
	public void draw(){
		
		
		if (i<100){
			
			fill(255, 0, 0);
			rect(100, 100, 50, 150);
			fill(0, 255, 0);
			rect(100, 200-i, 50, 100+i);
			i++;
		}
		
		
		
		
	}
	
}
