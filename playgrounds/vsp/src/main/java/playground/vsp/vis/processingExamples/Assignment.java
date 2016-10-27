package playground.vsp.vis.processingExamples;

import processing.core.PApplet;

public class Assignment extends PApplet {
	private final int xsize=800, ysize=800 ;

	public static void main( String[] args ) {
		PApplet.main( new String[] { "--present", "playground.vsp.vis.processingExamples.Assignment"}  );
	}
	
	int ii=0 ;
	int cnt = 0 ;

	@Override public void draw() {
		background(0) ;
		lights() ;
		
		// both translate and rotate are with respect to content coordinates

		translate(xsize/2,2*ysize/3,0) ;

		rotateX(-1.3f*HALF_PI) ;
		rotateZ(-0.001f*ii) ;
//		translate(0,0,ysize) ;
		
		ii++;

		float originX = 0 ;
		float originY = 0 ;
		float originZ = 0 ;
		
		this.strokeWeight(1);
		
//		this.stroke(255,0,0) ;
//		this.line(originX, originY, originZ, originX+500, originY, originZ);
//		
//		this.stroke(0,255,0) ;
//		this.line(originX, originY, originZ, originX, originY+500, originZ);
//
//		this.stroke(0,0,255) ;
//		this.line(originX, originY, originZ, originX, originY, originZ-500);

		noStroke() ;
		
		if ( cnt>=1 ) {
			drawFlow(-300, -100, -100, 0, 0, 0xFFFF0000 );
			drawFlow(-100, 0, 100, 0, 0, 0xFFFF0000 );
			drawFlow(100, 0, 300, 100,0, 0xFFFF0000 );
		}

		if ( cnt >=2 ) {
			drawFlow(-300, 100, -100, 0, 0, 0xFF00FF00 );
			drawFlow(-100, 00, 100, 0, 1, 0xFF00FF00 );
			drawFlow(100, 0, 300, -100, 0, 0xFF00FF00 );
		}
		if ( cnt >=3 ) {
			drawFlow(-300,100,-100,0,1, 0xFF0000FF ) ;
			drawFlow(-100,0,100,0,2, 0xFF0000FF ) ;
			drawFlow(100,0,300,100,1, 0xFF0000FF ) ;
		}
		
	}
	
	@Override public void mousePressed() {
		if ( this.mouseButton==LEFT ) {
			cnt++ ;
		} else {
			cnt-- ;
		}
	}

	private void drawFlow(float x0, float y0, float x1, float y1, float level, int color) {
		pushMatrix() ;
		{
			this.fill(color);
			
			translate(x0,y0,0) ;
			rotateZ( atan2(y1-y0, x1-x0) ) ;
			
			float length = sqrt( (x0-x1)*(x0-x1) + (y0-y1)*(y0-y1) ) ;
			
			float boxX = length ;
			float boxY = 10 ;
			float boxZ = 50 ;
			translate(boxX/2,boxY/2,-boxZ/2-boxZ*level) ;
			this.box( boxX, boxY, boxZ ) ;
		}
		popMatrix() ;
	}
	
	@Override
	public void settings() { // setup does not work here when not using the PDE
		size(xsize,ysize, P3D );
	}


}
