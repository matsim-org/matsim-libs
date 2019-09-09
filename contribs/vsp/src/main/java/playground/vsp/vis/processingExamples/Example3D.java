package playground.vsp.vis.processingExamples;

import processing.core.PApplet;

public class Example3D extends PApplet {
	private final int xsize=600, ysize=600 ;

	private int ii =0 ;
	private boolean flag = true ;

	public static void main( String[] args ) {
		PApplet.main( new String[] { "--present", "playground.vsp.vis.processingExamples.Example3D"}  );
	}

	@Override
	public void draw() {
		background(0) ;
		lights() ;
		
		noStroke();
		pushMatrix();
		translate(130, ii, 0);
		rotateY(1.25f);
		rotateX(-0.4f);
		box(100);
		popMatrix();


		if ( flag ) {
			ii++ ;
			if ( ii > ysize ) flag = false ;
		} else {
			ii -- ;
			if ( ii < 0 ) flag = true ;
		}
	}
	
	@Override
	public void settings() { // setup does not work here when not using the PDE
		size(xsize,ysize, P3D );
	}


}
