package playground.wrashid.tryouts.processing;

import processing.core.PApplet;

// see http://processing.org/reference/libraries/video/MovieMaker.html
// see http://processing.org/reference/saveFrame_.html
public class AnimationCharging extends PApplet {

	int i=0;
	
	public void setup() {

		size(1000, 1000);
		 frameRate(24);
		 
//		 MovieMaker mm = new MovieMaker(this, width, height, "movie.mov", 24,
//				  MovieMaker.JPEG, MovieMaker.BEST);
	}
	
	public void draw(){
		
		// this is the way to save pngs for the frame
		//saveFrame("c:/tmp/a"+i+".png");
		
		
		
		
		if (i<100){
			
			fill(255, 0, 0);
			rect(100, 100, 50, 150);
			fill(0, 255, 0);
			rect(100, 200-i, 50, 100+i);
			i++;
		}
		//System.out.println(frameCount);
		
		
		
	}
	
}
