/* *********************************************************************** *
 * project: org.matsim.*
 * SoldnerToWGS84.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 *              This file includes code (C) by Michael Loesler, see below  *
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

package playground.mrieser;

import org.matsim.api.core.v01.Coord;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.geometry.CoordinateTransformation;

public class SoldnerBerlinToWGS84 implements CoordinateTransformation {

	private final CoordsConverter converter = new CoordsConverter(new Ellipsoid("Bessel", 3, 52.4186482777778, 13.6272036666667));

	@Override
	public Coord transform(final Coord coord) {

		return this.converter.rh2bl(coord.getX()-40000, coord.getY()-10000);
	}

	/* The following inner classes are taken from
   * CoordsCalculator (http://coordscalc.sourceforge.net/)
	 * The classes were minimally changed to return MATSim-Coord objects
	 * instead of the original PointRHBL objects.
	 */

	/* **********************************************************************
	 *             CoordsCalculator - CoordsConverter v1.1                  *
	 ************************************************************************
	 * Copyright (C) 2007-08 by Michael Loesler, http://derletztekick.com   *
	 *                                                                      *
	 * This program is free software; you can redistribute it and/or modify *
	 * it under the terms of the GNU General Public License as published by *
	 * the Free Software Foundation; either version 2 of the License, or    *
	 * (at your option) any later version.                                  *
	 *                                                                      *
	 * This program is distributed in the hope that it will be useful,      *
	 * but WITHOUT ANY WARRANTY; without even the implied warranty of       *
	 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the        *
	 * GNU General Public License for more details.                         *
	 *                                                                      *
	 * You should have received a copy of the GNU General Public License    *
	 * along with this program; if not, write to the                        *
	 * Free Software Foundation, Inc.,                                      *
	 * 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.            *
	 ************************************************************************/


	private static class CoordsConverter {
	  private Ellipsoid ell = null;
	  private static final double rho=180.0/Math.PI;

	  public CoordsConverter(){  }

	  public CoordsConverter(final Ellipsoid ell){
	    this.ell = ell;
	  }

	  //Meridianbogenlaenge G aus ellipsoidischen Breite B
	  private double calcMeridianArcLength(final double B, final int index){
	    double E[] = new double[6];
	    double e2 = this.ell.getE2();
	    double c = this.ell.getC();
	    E[0] = c / rho * (1.0-3.0/4.0*Math.pow(e2,2) +  45.0/64.0*Math.pow(e2,4) -  175.0/256.0*Math.pow(e2,6) + 11025.0/16384.0*Math.pow(e2,8) -  43659.0/65536.0*Math.pow(e2,10));
	    E[1] = c            * (   -3.0/8.0*Math.pow(e2,2) +  15.0/32.0*Math.pow(e2,4) - 525.0/1024.0*Math.pow(e2,6) +   2205.0/4096.0*Math.pow(e2,8) - 72765.0/131072.0*Math.pow(e2,10));
	    E[2] = c            * (                             15.0/256.0*Math.pow(e2,4) - 105.0/1024.0*Math.pow(e2,6) +  2205.0/16384.0*Math.pow(e2,8) -  10395.0/65536.0*Math.pow(e2,10));
	    E[3] = c            * (                                                       -  35.0/3072.0*Math.pow(e2,6) +   315.0/12288.0*Math.pow(e2,8) - 31185.0/786432.0*Math.pow(e2,10));
	    E[4] = c            * (                                                                                        315.0/131072.0*Math.pow(e2,8) -  3465.0/524288.0*Math.pow(e2,10));
	    E[5] = c            * (                                                                                                                      -  693.0/1310720.0*Math.pow(e2,10));
	    if (index < 0 || index >= E.length)
	      return E[0]*B + E[1]*Math.sin(2.0*B/rho) + E[2]*Math.sin(4.0*B/rho) + E[3]*Math.sin(6.0*B/rho) + E[4]*Math.sin(8.0*B/rho) + + E[5]*Math.sin(10.0*B/rho);
	    else
	      return E[index];
	  }
	  //ellipsoidische Breite B aus Meridianbogenlaenge G
	  private double calcEllLatitude(final double G, final double B) {
	    //double sigma = (G+B)/this.calcMeridianArcLength(B,0);
	    double sigma = G/this.calcMeridianArcLength(B,0);
	    double e2 = this.ell.getE2();
	    double F[] = new double[3];
	    F[0] = rho * (3.0/8.0*Math.pow(e2,2) - 3.0/16.0*Math.pow(e2,4) + 213.0/2048.0*Math.pow(e2,6) -  255.0/4096.0*Math.pow(e2,8));
	    F[1] = rho * (                       21.0/256.0*Math.pow(e2,4) -   21.0/256.0*Math.pow(e2,6) +  533.0/8192.0*Math.pow(e2,8));
	    F[2] = rho * (                                                   151.0/6144.0*Math.pow(e2,6) - 453.0/12288.0*Math.pow(e2,8));

	    return sigma + F[0]*Math.sin(2*sigma/rho) + F[1]*Math.sin(4*sigma/rho) + F[2]*Math.sin(6*sigma/rho);
	  }

	  public Coord bl2rh(final double b, final double l){
	  	Coord p = new CoordImpl(0, 0);
	    if (!this.ell.isSoldner()) {
	      int kz = (int)((l+(this.ell.getDEG()==3?0.5:1)*this.ell.getDEG())/this.ell.getDEG());
	      //int kz = (int)((P.getLongitude()+0.5*this.Ell.getDEG())/this.Ell.getDEG());
	      double N = this.ell.getN(b);
	      double eta2 = Math.pow(this.ell.getEta(b),2);
	      double L0 = kz*this.ell.getDEG()-(this.ell.getDEG()==3?0:3);
	      double dL = l - L0;
	      double t = Math.tan(b/rho);
	      double x[] = new double[4];
	      double y[] = new double[4];
	      x[0] = this.calcMeridianArcLength(l, -1);
	      x[1] = 1.0/(    2.0*Math.pow(rho,2)) * N * Math.pow(Math.cos(l/rho),2) * t;
	      x[2] = 1.0/(   24.0*Math.pow(rho,4)) * N * Math.pow(Math.cos(l/rho),4) * t * (5.0-      Math.pow(t,2) + 9.0*eta2);
	      x[3] = 1.0/(  720.0*Math.pow(rho,6)) * N * Math.pow(Math.cos(l/rho),6) * t * (61.0-58.0*Math.pow(t,2) + Math.pow(t,4) + 270.0*eta2 - 330.0*Math.pow(t,2)*eta2);

	      y[0] = 1.0/rho                      * N * Math.cos(l/rho);
	      y[1] = 1.0/(   6.0*Math.pow(rho,3)) * N * Math.pow(Math.cos(l/rho),3) * (1.0-       Math.pow(t,2) + eta2);
	      y[2] = 1.0/( 120.0*Math.pow(rho,5)) * N * Math.pow(Math.cos(l/rho),5) * (5.0-18.0  *Math.pow(t,2) + Math.pow(t,4));
	      y[3] = 1.0/(5040.0*Math.pow(rho,7)) * N * Math.pow(Math.cos(l/rho),7) * (61.0-479.0*Math.pow(t,2) + 179.0*Math.pow(t,4) - Math.pow(t,6));

	      p.setY(this.ell.getScale()*(x[0]    + x[1]*Math.pow(dL,2) + x[2]*Math.pow(dL,4) + x[3]*Math.pow(dL,6)));
	      p.setX( this.ell.getScale()*(y[0]*dL + y[1]*Math.pow(dL,3) + y[2]*Math.pow(dL,5) + y[3]*Math.pow(dL,7)) + 500000.0 + kz*1000000.0);
	    }

	    else {
	      double x,y,xB,N,t,cosB,eta,B0,L0,dl;
	      B0 = this.ell.getB0();
	      L0 = this.ell.getL0();
	      dl = l-L0;
	      xB = this.calcMeridianArcLength(b,-1) - this.calcMeridianArcLength(B0,-1);
	      t  = Math.tan(b/rho);
	      cosB = Math.cos(b/rho);
	      eta  = this.ell.getEta(b);
	      N    = this.ell.getN(b);

	      x  = 0.5*N*Math.pow(cosB,2)*t*Math.pow(dl,2)/Math.pow(rho,2);
	      x += N*Math.pow(cosB,4)*t*(5.0-Math.pow(t,2)+5.0*Math.pow(eta,2))*Math.pow(dl,4)/(24.0*Math.pow(rho,4));
	      x += xB;

	      y  = N*cosB*dl/rho;
	      y -= N*Math.pow(cosB,3)*Math.pow(t,2)*Math.pow(dl,3)/(6.0*Math.pow(rho,3));

	      p.setY( x ); // jep, that's correct, y := x
	      p.setX( y );
	    }
	    return p;
	  }

	  public Coord rh2bl(final double r, final double h){
	  	Coord p = new CoordImpl(0, 0);

	    if (!this.ell.isSoldner()) {
	      int kz;

	      double G, y0, N, eta2;
	      double B[] = new double[4];
	      double L[] = new double[4];
	      G  = h/this.ell.getScale();
	      kz = (int)(r/1000000.0);
	      y0 = (r-500000.0-1000000.0*kz)/this.ell.getScale();

	      B[0] =  this.calcEllLatitude(G, 0.0);

	      N = this.ell.getN(B[0]);
	      eta2 = Math.pow(this.ell.getEta(B[0]),2);
	      B[1] = -rho/(  2*Math.pow(N,2)) * Math.tan(B[0]/rho) * (    1+eta2);
	      B[2] =  rho/( 24*Math.pow(N,4)) * Math.tan(B[0]/rho) * (  5+3*Math.pow(Math.tan(B[0]/rho),2)+ 6*eta2*(1-Math.pow(Math.tan(B[0]/rho),2)));
	      B[3] = -rho/(720*Math.pow(N,6)) * Math.tan(B[0]/rho) * (61+90*Math.pow(Math.tan(B[0]/rho),2)+45*Math.pow(Math.tan(B[0]/rho),4));

	      L[0] =  (kz-(this.ell.isUTM()?30:0))*this.ell.getDEG()-(this.ell.getDEG()-3);
	      L[1] =  rho/(             N   *Math.cos(B[0]/rho));
	      L[2] = -rho/(  6*Math.pow(N,3)*Math.cos(B[0]/rho)) * (1+ 2*Math.pow(Math.tan(B[0]/rho),2)+eta2);
	      L[3] =  rho/(120*Math.pow(N,5)*Math.cos(B[0]/rho)) * (5+28*Math.pow(Math.tan(B[0]/rho),2)+24*Math.pow(Math.tan(B[0]/rho),4));

	      p.setY( B[0] + B[1]*Math.pow(y0,2) + B[2]*Math.pow(y0,4) + B[3]*Math.pow(y0,6));
	      p.setX(L[0] + L[1]*         y0    + L[2]*Math.pow(y0,3) + L[3]*Math.pow(y0,5));
	    }

	    else {
	      double GF, BF, VF, B, L, NF, B0, L0, x=h, y=r, tF, etaF, l;
	      B0 = this.ell.getB0();
	      L0 = this.ell.getL0();
	      GF = this.calcMeridianArcLength(B0,-1) + x;
	      BF = this.calcEllLatitude(GF, this.calcMeridianArcLength(B0,0));

	      tF = Math.tan(BF/rho);
	      VF = this.ell.getV(BF);
	      NF = this.ell.getN(BF);

	      etaF = this.ell.getEta(BF);

	      B  = 0.5 * Math.pow(VF,2) * tF * rho / Math.pow(NF,2) * Math.pow(y,2);
	      B -= Math.pow(VF,2) * tF * (1.0 + 3.0*Math.pow(tF,2) + Math.pow(etaF,2) - 9.0*Math.pow(etaF,2)*Math.pow(tF,2)) * rho / (24.0*Math.pow(NF,4)) * Math.pow(y,4);
	      B  = BF-B;


	      l  = rho/(NF*Math.cos(BF/rho)) * y - Math.pow(tF,2)*rho/(3.0*Math.pow(NF,3)*Math.cos(BF/rho)) * Math.pow(y,3);
	      l += Math.pow(tF,2)*rho*(1.0 + 3.0*Math.pow(tF,2))/(15.0*Math.pow(NF,5)*Math.cos(BF/rho)) * Math.pow(y,5);
	      L = L0 + l;

	      p.setY( B );
	      p.setX( L );
	    }

	    return p;
	  }

	}

	private static class Ellipsoid {
	  public String ellName;
	  private double a = 0;
	  private double b = 0;
	  private int deg = 6;
	  private boolean isUTM = false, isSoldner = false;
	  private static final double roh=180.0/Math.PI;
	  private double b0 = 0.0, l0 = 0.0;
	  public Ellipsoid(final String ellName){
	    this(ellName, 6, false);
	  }

	  public Ellipsoid(final String ellName, final int deg){
	    this(ellName, deg, false);
	  }

	  public Ellipsoid(final String ellName, final int deg, final double b0, final double l0){
	    this(ellName, deg, false);
	    this.isSoldner = true;
	    this.b0 = b0;
	    this.l0 = l0;
	  }

	  public Ellipsoid(final String ellName, final int deg, final boolean isUTM){
	    this.ellName = ellName;
	    this.deg = (deg==3||deg==6)?deg:6;
	    this.isUTM = isUTM;
	    if (this.ellName.equalsIgnoreCase("Bessel")){
	      this.a = 6377397.15507605;
	      this.b = 6356078.96289778;
	    }
	    else if(this.ellName.equalsIgnoreCase("Krassowski")){
	      this.a = 6378245.0;
	      this.b = 6356863.01877304;
	    }
	    else if(this.ellName.equalsIgnoreCase("WGS84") || this.ellName.equalsIgnoreCase("GRS80")) {
	      this.a = 6378137.0;
	      this.b = 6356752.31419275;
	    }
	    else if(this.ellName.equalsIgnoreCase("Hayford")){
	      this.a = 6378388.0;
	      this.b = 6356911.94612796;
	    }
	    else
	      return;
	  }
	  public boolean isUTM(){
	    return this.isUTM;
	  }
	  //Gradstreifen-System
	  public int getDEG(){
	    return this.deg;
	  }
	  //Massstab
	  public double getScale(){
	    return this.isUTM?0.9996:1.0;
	  }
	  //Grosse Halbachse
	  public double getA(){
	    return this.a;
	  }
	  // Kleine Halbachse
	  public double getB(){
	    return this.b;
	  }
	  // Erste num Exzentrizitaet e
	  public double getE1(){
	    return Math.sqrt((Math.pow(this.a,2) - Math.pow(this.b,2)) / Math.pow(this.a,2));
	  }
	  // Zweite num Exzentrizitaet e'
	  public double getE2(){
	    return Math.sqrt((Math.pow(this.a,2) - Math.pow(this.b,2)) / Math.pow(this.b,2));
	  }
	  //Polarkruemmungshalbmesser c
	  public double getC(){
	    return Math.pow(this.a,2) / this.b;
	  }
	  //Querkruemmungsradius N
	  public double getN(final double B){
	    return this.getC() / Math.sqrt(1.0+Math.pow(this.getEta(B),2));
	  }
	  //Hilfsgroesse Eta
	  public double getEta(final double B){
	    return this.getE2() * Math.cos(B/this.roh);
	  }
	  //Hilfsgroesse V
	  public double getV(final double B){
	    return Math.sqrt(1+Math.pow(this.getEta(B),2));
	  }
	  //Soldnerkoordinaten
	  public boolean isSoldner() {
	    return this.isSoldner;
	  }
	  public void isSoldner(final boolean isSoldner) {
	    this.isSoldner = isSoldner;
	  }
	  //Nullpunkt des Soldnersytems B
	  public double getB0() {
	    return this.b0;
	  }
	  //Nullpunkt des Soldnersytems L
	  public double getL0() {
	    return this.l0;
	  }
	  public void setB0(final double b0) {
	    this.b0 = b0;
	  }
	  public void setL0(final double l0) {
	    this.l0 = l0;
	  }

	  @Override
		public String toString(){
	    return this.deg + "° - " + this.ellName;
	  }
	}

}
