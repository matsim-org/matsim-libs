package playground.wrashid.tryouts.processing;

import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.util.LinkedList;

import processing.core.PApplet;

// cont: zoom at specific point, where you double click (center that point).
// be able to do that twice!

public class MovingAndZooming extends PApplet {
	
	private float scaler = 1;

	LinkedList<Float> xValues;
	LinkedList<Float> yValues;
	
	private float moveTranslateX=0;
	private float moveTranslateY=0;
	
	private float scaleTranslateX=0;
	private float scaleTranslateY=0;
	
	public void setup() {
		xValues=new LinkedList<Float>();
		yValues=new LinkedList<Float>();
		
		size(1000,1000);  
		smooth();
		
		addMouseWheelListener(new MouseWheelListener() {

			@Override
			public void mouseWheelMoved(MouseWheelEvent e) {
				if (e.getWheelRotation() < 0) {
					scaler+= 0.1f;
					
					int xTranslate = (width/2-mouseX);
					int yTranslate = (height/2-mouseY);
					
					xValues.add(xTranslate*scaler);
					yValues.add(yTranslate*scaler);
					
					xTranslate=0;
					yTranslate=0;
					
					for (Float f:xValues){
						xTranslate+=f;
					}
					
					for (Float f:yValues){
						yTranslate+=f;
					}
					
					scaleTranslateX=xTranslate;
					scaleTranslateX=yTranslate;
					//System.out.println(xTranslate);
					
					scaleTranslateX=(width/2-mouseX);
					scaleTranslateY=(height/2-mouseY);
				}

				if (e.getWheelRotation() > 0) {
					scaler-= 0.1f;
					
				}
				
				
				
				
				/*
				if (mouseX>width/2){
					moveTranslateX=(width/2-mouseX)*scaler;
				}
				
				if (mouseY>height/2){
					moveTranslateY=(height/2-mouseY)*scaler;
				}
				*/
			}
		});
		
		addMouseMotionListener(new MouseMotionListener() {
			
			@Override
			public void mouseMoved(MouseEvent e) {
				mouseX = e.getX();
				mouseY = e.getY();
			}
			
			@Override
			public void mouseDragged(MouseEvent e) {
				//System.out.println(moveTranslateX);
				//System.out.println(moveTranslateY);
				moveTranslateX+=e.getX()-mouseX;
				moveTranslateY+=e.getY()-mouseY;
				mouseX = e.getX();
				mouseY = e.getY();
				scaleTranslateX=mouseX;
				scaleTranslateY=mouseY;
			}
		});
		
		
		addMouseListener(new MouseListener() {
			
			@Override
			public void mouseReleased(MouseEvent e) {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public void mousePressed(MouseEvent e) {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public void mouseExited(MouseEvent e) {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public void mouseEntered(MouseEvent e) {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public void mouseClicked(MouseEvent e) {
				if (mouseEvent.getClickCount()==2) {
					scaler*= 2f;
				
					moveTranslateX-=mouseX-width/(scaler*scaler);
					moveTranslateY-=mouseY-height/(scaler*scaler);
				}
				
			}
		});
		
	}
	

	
	
	
	
	
	
	
	
	
	
	
	
	public void draw() {
		//System.out.println(moveTranslateX);
		//translate(-xTranslate, -yTranslate);
		//scale(scaler);

		translate(moveTranslateX*scaler,moveTranslateY*scaler);
		//translate(-scaler*width/2,-scaler*height/2);
		
		//translate(moveTranslateX, moveTranslateY);
		scale(scaler);
		
		fill(255, 255, 255);
		rect(-1000, -1000, 10000, 10000);
		
		fill(255, 0, 0);
		rect(100, 100, 100, 100); 
		fill(0, 255, 0);
		rect(800, 800, 100, 100); 
	}

}
