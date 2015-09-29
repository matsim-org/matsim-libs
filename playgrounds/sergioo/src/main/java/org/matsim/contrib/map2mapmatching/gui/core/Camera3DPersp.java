package org.matsim.contrib.map2mapmatching.gui.core;

import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;

public class Camera3DPersp extends Camera2D implements Camera3D {
	
	//Constantes
	private static final double ZOOM_RATE = 1000;
	private static final double WIDTH_CAM = 100;
	
	//Attributes
	private Vector3D l;
	private Vector3D d;
	private Vector3D h;
	private double tanTetaOverTwo;
	//Methods
	public Camera3DPersp() {
		super();
		size.setY(WIDTH_CAM/getAspectRatio());
		size.setX(WIDTH_CAM);
		l = new Vector3D(-100, -100, 100);
		d = new Vector3D(1, 1, -1).normalize();
		h = new Vector3D(1, 1, 2).normalize();
		tanTetaOverTwo = Math.tan(Math.PI/4);
	}
	public Camera3DPersp(Vector3D l, Vector3D d, Vector3D h, double teta) {
		super();
		this.l = l;
		this.d = d;
		this.h = h;
		tanTetaOverTwo = Math.tan(teta/2);
	}
	private Vector3D getCenterCamera() {
		return l;
	}
	public void copyCamera(Camera camera) {
		super.copyCamera(camera);
		this.l = ((Camera3DPersp)camera).l;
		this.d = ((Camera3DPersp)camera).d;
		this.h = ((Camera3DPersp)camera).h;
		this.tanTetaOverTwo = ((Camera3DPersp)camera).tanTetaOverTwo;
	}
	@Override
	public double[] getCenter() {
		Vector3D center = getCenterCamera();
		Vector3D center0 = d.scalarMultiply(-center.getZ()/d.getZ()).add(center);
		return new double[]{center0.getX(), center0.getY()};
	}
	@Override
	public void setFrameSize(int frameSize) {
		this.frameSize = frameSize;
	}
	@Override
	public void zoomIn() {
		l = l.add(d.scalarMultiply(ZOOM_RATE));
	}
	@Override
	public void zoomOut() {
		l = l.subtract(d.scalarMultiply(ZOOM_RATE));
	}
	@Override
	public void zoomIn(double x, double y) {
		double[] center0 = new double[]{x, y, 0};
		l = l.add(d.scalarMultiply(ZOOM_RATE));
		centerCamera(center0);
	}
	@Override
	public void zoomOut(double x, double y) {
		double[] center0 = new double[]{x, y, 0};
		l = l.add(d.scalarMultiply(ZOOM_RATE));
		centerCamera(center0);
	}
	@Override
	public void setBoundaries(double xMin, double yMin, double xMax, double yMax) {
		double[][] points = new double[][]{new double[]{xMin,yMin,0}, new double[]{xMin,yMax,0}, new double[]{xMax,yMin,0}, new double[]{xMax,yMax,0}};
		double[][] parameters = new double[][]{getParametersPlane(points[0]), getParametersPlane(points[1]), getParametersPlane(points[2]), getParametersPlane(points[3])};
		xMin = Double.MAX_VALUE;
		yMin = Double.MAX_VALUE;
		xMax = -Double.MAX_VALUE;
		yMax = -Double.MAX_VALUE;
		int xMinI = 0, xMaxI=0;
		for(int i=0; i<parameters.length; i++) {
			double[] p = parameters[i];
			if(p[0]<xMin) {
				xMin = p[0];
				xMinI = i;
			}
			if(p[0]>xMax) {
				xMax = p[0];
				xMaxI = i;
			}
			if(p[1]<yMin)
				yMin = p[1];
			if(p[1]>yMax)
				yMax = p[1];
		}
		double deltaX=xMax-xMin, deltaY=yMax-yMin;
		size.setX(WIDTH_CAM);
		size.setY(-deltaY*WIDTH_CAM/deltaX);
		Vector3D pA = new Vector3D(points[xMinI][0], points[xMinI][1], points[xMinI][2]);
		Vector3D pC = new Vector3D(points[xMaxI][0], points[xMaxI][1], points[xMaxI][2]);
		double xB = (pC.getX()*h.getY()/h.getX()+pA.getX()*h.getX()/h.getY()-pC.getY()+pA.getY())/(h.getY()/h.getX()+h.getX()/h.getY());
		double yB = -h.getY()*xB/h.getX()+pA.getY()+pA.getX()*h.getX()/h.getY();
		Vector3D pB = new Vector3D(xB, yB, 0);
		Vector3D m = pA.add(pB.subtract(pA).scalarMultiply(0.5));
		double r = pA.subtract(m).getNorm()/tanTetaOverTwo;
		Vector3D o = new Vector3D(m.getX()-r*Math.sin(h.getDelta())*Math.cos(d.getAlpha()), m.getY()-r*Math.sin(h.getDelta())*Math.sin(d.getAlpha()), r*Math.cos(h.getDelta()));
		l = o.add(d.scalarMultiply(WIDTH_CAM/(2*tanTetaOverTwo)));
	}
	@Override
	public void move(int dx, int dy) {
		double[] center = getCenter();
		double ratio = new Vector3D(center[0],center[1],0).subtract(getOrigin()).getNorm()/l.subtract(getOrigin()).getNorm();
		l = getVector(dx*size.getX()*ratio/width, dy*size.getY()*ratio/height);
	}
	@Override
	public void move2(int dx, int dy) {
		Vector3D center = getCenterCamera();
		Vector3D center0 = d.scalarMultiply(-center.getZ()/d.getZ()).add(center);
		Vector3D r = center.subtract(center0);
		double azi = r.getAlpha()+dx*2*Math.PI/width;
		double ele = r.getDelta()-dy*Math.PI/height;
		if(ele<Math.PI/360)
			ele = Math.PI/360;
		else if(ele>179*Math.PI/360)
			ele = 179*Math.PI/360;
		Vector3D nr = new Vector3D(azi, ele).scalarMultiply(r.getNorm());
		center = center0.add(nr);
		d = nr.negate().normalize();
		if(d.getZ()==1 || d.getZ()==-1)
			h = new Vector3D(h.getX(), h.getY(), 0).normalize();
		else if(d.getZ()==0)
			h = new Vector3D(0, 0, 1);
		else
			h = new Vector3D(d.getX(), d.getY(), d.getZ()-(1/d.getZ())).normalize();
		l = center;
	}
	@Override
	public void centerCamera(double[] p) {
		l = getPointInCamera(new double[]{p[0], p[1], 0});
	}
	@Override
	public int[] getScreenXY(double[] point) {
		double[] parameters = getParameters(point);
		return getScreenXY(parameters[0], parameters[1]);
	}
	private Vector3D getPointInCamera(double[] point) {
		Vector3D p;
		if(point.length<3)
			p = new Vector3D(point[0], point[1], 0);
		else
			p = new Vector3D(point[0], point[1], point[2]);
		double t = getDistanceToCamera(p);
		return getOrigin().subtract(p).normalize().scalarMultiply(t).add(p);
	}
	private Vector3D getPointInCameraPlane(double[] point) {
		Vector3D p;
		if(point.length<3)
			p = new Vector3D(point[0], point[1], 0);
		else
			p = new Vector3D(point[0], point[1], point[2]);
		double t = -getDistanceToCamera(p);
		return d.scalarMultiply(t).add(p);
	}
	public double getDistanceToCamera(Vector3D p) {
		return (Vector3D.dotProduct(d, l)-Vector3D.dotProduct(d, p))/Vector3D.dotProduct(d, getOrigin().subtract(p).normalize());
	}
	public Vector3D getOrigin() {
		return d.scalarMultiply(-WIDTH_CAM/(2*tanTetaOverTwo)).add(l);
	}
	private double[] getParameters(double[] point) {
		Vector3D s = getPointInCamera(point); 
		Vector3D u = Vector3D.crossProduct(d, h);
		Vector3D v = h;
		Vector3D r = s.subtract(l);
		double det = u.getX()*v.getY()-v.getX()*u.getY();
		double xDet = -v.getX()*r.getY()+r.getX()*v.getY();
		double yDet = u.getX()*r.getY()-r.getX()*u.getY();
		return new double[]{xDet/det, yDet/det};
	}
	private double[] getParametersPlane(double[] point) {
		Vector3D s = getPointInCameraPlane(point); 
		Vector3D u = Vector3D.crossProduct(d, h);
		Vector3D v = h;
		Vector3D r = s.subtract(l);
		double det = u.getX()*v.getY()-v.getX()*u.getY();
		double xDet = -v.getX()*r.getY()+r.getX()*v.getY();
		double yDet = u.getX()*r.getY()-r.getX()*u.getY();
		return new double[]{xDet/det, yDet/det};
	}
	private int[] getScreenXY(double u, double v) {
		return new int[]{(int)(u*width/size.getX())+width/2+frameSize, (int)(v*height/size.getY())+height/2+frameSize};
	}
	private Vector3D getVector(double paramU, double paramV) {
		Vector3D u = Vector3D.crossProduct(d, h);
		Vector3D v = h;
		return u.scalarMultiply(paramU).add(v.scalarMultiply(paramV)).add(l);
	}
	@Override
	public double[] getWorld(int x, int y) {
		Vector3D s = getVector((x-width/2-frameSize)*size.getX()/width,(y-height/2-frameSize)*size.getY()/height);
		Vector3D s0 = d.scalarMultiply(-s.getZ()/d.getZ()).add(s);
		return new double[]{s0.getX(), s0.getY(), s0.getZ()};
	}
	@Override
	public boolean isInside(double[] point) {
		double[] params = getParameters(point);
		return params[0]<size.getX()/2 && params[0]>-size.getX()/2 && params[1]>size.getY()/2 && params[1]<-size.getY()/2;
	}
	@Override
	public double getDistanceToCamera(double[] point) {
		Vector3D vector = new Vector3D(point[0], point[1], point.length>2?point[2]:0); 
		return getDistanceToCamera(vector);
	}

}
