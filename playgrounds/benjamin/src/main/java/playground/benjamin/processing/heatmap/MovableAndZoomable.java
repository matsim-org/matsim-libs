//package playground.benjamin.processing.heatmap;
//
//import java.awt.event.MouseEvent;
//import java.awt.event.MouseListener;
//import java.awt.event.MouseMotionListener;
//import java.awt.event.MouseWheelEvent;
//import java.awt.event.MouseWheelListener;
//import java.util.LinkedList;
//
//import org.apache.log4j.varia.ReloadingPropertyConfigurator;
//
//import processing.core.PApplet;
//
///**
// * functionalities:
// * - can move (drag)
// * - can double click to zoom in
// * - can right click to reset zoom and view
// * - scan scroll up to zoom in, scroll down to zoom away
// * @author wrashid
// *
// */
//
//public class MovableAndZoomable extends PApplet {
//
//	float scaler;
//
//	LinkedList<Float> xValues;
//	LinkedList<Float> yValues;
//
//	private float centerCoordinateX;
//	private float centerCoordinateY;
//
//	public void setup() {
//		xValues = new LinkedList<Float>();
//		yValues = new LinkedList<Float>();
//
//		size(1000, 1000);
//
//		resetCenterCoordinateAndScaling();
//
//		
//		
//		//smooth();
//
//		addMouseWheelListener(new MouseWheelListener() {
//
//			@Override
//			public void mouseWheelMoved(MouseWheelEvent e) {
//				relocateWindowCenter();
//
//				if (e.getWheelRotation() < 0) {
//					scaler *= 1.5f;
//				}
//
//				if (e.getWheelRotation() > 0) {
//					if (scaler > 1) {
//						scaler *= 0.5f;
//					}
//				}
//
//			}
//
//			
//		});
//
//		addMouseMotionListener(new MouseMotionListener() {
//
//			@Override
//			public void mouseMoved(MouseEvent e) {
//				mouseX = e.getX();
//				mouseY = e.getY();
//			}
//
//			@Override
//			public void mouseDragged(MouseEvent e) {
//				centerCoordinateX -= (e.getX() - mouseX) / scaler;
//				centerCoordinateY -= (e.getY() - mouseY) / scaler;
//				mouseX = e.getX();
//				mouseY = e.getY();
//			}
//		});
//
//		addMouseListener(new MouseListener() {
//
//			@Override
//			public void mouseReleased(MouseEvent e) {
//				// TODO Auto-generated method stub
//
//			}
//
//			@Override
//			public void mousePressed(MouseEvent e) {
//				// TODO Auto-generated method stub
//
//			}
//
//			@Override
//			public void mouseExited(MouseEvent e) {
//				// TODO Auto-generated method stub
//
//			}
//
//			@Override
//			public void mouseEntered(MouseEvent e) {
//				// TODO Auto-generated method stub
//
//			}
//
//			@Override
//			public void mouseClicked(MouseEvent e) {
//				
//				
//				if (mouseEvent.getClickCount() == 2) {
//					relocateWindowCenter();
//					
//
//					scaler *= 2f;
//				}
//				
//				if (mouseButton == RIGHT){
//					resetCenterCoordinateAndScaling();
//				}
//				
//				
//
//			}
//		});
//
//	}
//
//	private void resetCenterCoordinateAndScaling() {
//		scaler=1;
//		centerCoordinateX = width / 2;
//		centerCoordinateY = height / 2;
//	}
//
//	public void draw() {
//		translate(-(centerCoordinateX - width / 2 / scaler) * scaler, -(centerCoordinateY - height / 2 / scaler) * scaler);
//
//		scale(scaler);
//
//	}
//
//	private void relocateWindowCenter() {
//		centerCoordinateX += mouseX / scaler - width / 2 / scaler;
//		centerCoordinateY += mouseY / scaler - height / 2 / scaler;
//	}
//	
//}
