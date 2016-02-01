package playground.tschlenther.processing;

import processing.core.PApplet;

public class testapplet extends PApplet{

	final int xsize=600, ysize=600;
	private int ii=0;
	private boolean isAlive =true;
	
	public static void main( String[] args ) {
		PApplet.main( new String[] { "--present", "playground.tschlenther.processing.testapplet"}  );
	}

	@Override
	public void draw() {
		this.strokeWeight(30) ;
		this.stroke( 255, 0, 0, 111) ; // R G B transparency
		this.background(255) ; // "clears" the background

		ii++ ;
		float xx = xsize/2 + ( xsize/2*(float) Math.sin(ii/360.) );
		float yy = ysize/2 + ( ysize/2*(float) Math.cos(ii/360.) );
		
		if(isAlive){
		this.point( xx,yy ) ;
		}
		
		strokeWeight(10);
		if(mousePressed){
			stroke(100,100,100);
			float xhit = mouseX - xx;
			float yhit = mouseY -yy;
			if(xhit<5 && yhit <5){
				isAlive =false;
		}
			point(mouseX,mouseY);
			
		}
		 line(mouseX-10, mouseY, mouseX+10, mouseY);
		 line(mouseX, mouseY-10, mouseX, mouseY+10); 
	}
	
//	public void draw(){
//		background(0);
//		lights();
//
//		noStroke();
//		pushMatrix();
//		translate(130, height/2, 0);
//		rotateY(1.25f);
//		rotateX(-0.4f);
//		box(100);
//		popMatrix();
//
//		noFill();
//		stroke(255);
//		pushMatrix();
//		translate(500f, height*0.35f, -200);
//		sphere(280);
//		popMatrix();
//	}
	
	public void settings(){
		size(xsize,ysize);
	}
	
}
