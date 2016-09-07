/* *********************************************************************** *
 * project: org.matsim.*												   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */
package playground.vsptelematics.jamfromnowhere;

import java.awt.Canvas;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;

import org.matsim.api.core.v01.Id;
import org.matsim.vehicles.Vehicle;

/**
 * @author nagel
 *
 */
class KNJam {
	private double density = 0.2 ;
	private final int LEN = 1000 ;

	int XX=1000 ;
	int YY=1000 ;
	int sqr = 2 ;

	private class Car {
		Id<Vehicle> id ;
		double pos ;
		double vel ;
		double oldVel ;
	}

	private Car[] cars = new Car[(int)( LEN * density )] ;

	private final BufferedImage pixels = new BufferedImage(XX*sqr, YY*sqr, BufferedImage.TYPE_INT_RGB);

	private final Canvas canvas ;

	@SuppressWarnings("serial")
	KNJam() {
		for ( int cc = 0 ; cc<cars.length-1 ; cc++ ) {
			Car car = new Car() ;
			car.pos = (int) (cc/density) ;
			car.vel = 0. ;
			car.oldVel = 0. ;
			car.id = Id.create( cc, Vehicle.class ) ;
			cars[cc] = car ;
		}

		canvas = new Canvas(){
			@Override
			public void paint(Graphics g)
			{
				g.drawImage(pixels, 0, 0, Color.red, null);
			}	  
		};

		Frame f = new Frame( "paint Example" );
		f.add("Center", canvas);
		f.setSize(new Dimension(XX*sqr,YY*sqr+22));
		f.setVisible(true);

	}

	void run() {
		for ( long time=0 ; time<1000 ; time++ ) {
			for ( int cc = 0 ; cc<cars.length-1 ; cc++ ) {
				cars[cc].oldVel = cars[cc].vel ;
			}
			cars[ cars.length-1 ] = cars[0] ;

			for ( int cc=0 ; cc<cars.length-1 ; cc++ ) {
				cars[cc].vel ++ ;

				if ( cars[cc].vel > 5 ) {
					cars[cc].vel = 5 ;
				}

				double gap = cars[cc+1].pos - cars[cc].pos - 1 ;
				if ( gap < 0. ) {
					gap += LEN ;
				}

				if ( gap < cars[cc].vel ) {
					cars[cc].vel = gap ;
				}

				if ( cars[cc].vel >=1 && Math.random() < 0.5 ) {
					cars[cc].vel -- ;
				}
			}
			for ( int cc=0 ; cc<cars.length-1 ; cc++ ) {
				cars[cc].pos += cars[cc].vel ;
				if ( cars[cc].pos > LEN ) {
					cars[cc].pos -= LEN ;
				}
			}

			{
				int[] road = new int[80] ;
				for ( int ii=0 ; ii<road.length ; ii++ ) {
					road[ii] = -1 ;
				}
				for ( int cc=0 ; cc < cars.length-1 ; cc++ ) {
					if ( cars[cc].pos < 80 ) {
						road[ (int) cars[cc].pos ] = (int) cars[cc].vel ;
					}
				}
				StringBuilder strb = new StringBuilder() ;
				for ( int ii=0 ; ii<road.length ; ii++ ) {
					if ( road[ii]==-1 ) {
						strb.append('.') ;
					} else {
						strb.append( road[ii] ) ;
					}
				}
				System.out.println( strb.toString() ) ;
			}

			for ( int cc = 0 ; cc < cars.length-1 ; cc++ ) {
				Car car = cars[cc] ;

				double idAsInt = Double.parseDouble( car.id.toString() ) ;

				float brightness = 1 ;
				float saturation = 1 ;
				float hue = (float) (idAsInt/10.) ; 
				int color = Color.HSBtoRGB( hue, saturation, brightness) ;

				final int xx = (int) (sqr*car.pos);
				final int yy = (int) (sqr*time);
				for ( int ii=0 ; ii<sqr ; ii++ ) {
					for ( int jj=0 ; jj<sqr ; jj++ ) {
						int xxx = xx + ii ; int yyy = yy + jj ;
						if ( xxx < XX*sqr && yyy < YY*sqr ) {
							pixels.setRGB( xxx, yyy, color ) ;
						}
					}
				}

				// move pixels up ... how?
				WritableRaster raster = pixels.getRaster() ;
				if ( time > 100 ) {
					for ( int ii=0 ; ii<XX*sqr ; ii++ ) {
						for ( int jj=0 ; jj<YY*sqr ; jj++ ) {
//														raster.setPixels(x, y, w, h, iArray);
						}
					}
				}
			}

			canvas.repaint(); 

		}
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		new KNJam().run() ;
	}

}
