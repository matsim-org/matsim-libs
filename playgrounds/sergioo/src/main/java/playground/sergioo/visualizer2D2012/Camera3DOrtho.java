package playground.sergioo.visualizer2D2012;

import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;

public class Camera3DOrtho extends Camera2D implements Camera3D {
	
	//Constantes
	private static final double ZOOM_RATE = 5.0/4.0;
	
	//Attributes
	private Vector3D l;
	private Vector3D d;
	private Vector3D h;

	//Methods
	public Camera3DOrtho() {
		super();
		l = new Vector3D(-1, -1, 1);
		d = new Vector3D(1, 1, -1).normalize();
		h = new Vector3D(1, 1, 2).normalize();
	}
	public Camera3DOrtho(Vector3D l, Vector3D d, Vector3D h) {
		super();
		this.l = l;
		this.d = d;
		this.h = h;
	}
	private Vector3D getCenterCamera() {
		return l;
	}
	public void copyCamera(Camera camera) {
		super.copyCamera(camera);
		this.l = ((Camera3DOrtho)camera).l;
		this.d = ((Camera3DOrtho)camera).d;
		this.h = ((Camera3DOrtho)camera).h;
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
		double[] center0 = getCenter();
		size.scale(1/ZOOM_RATE);
		centerCamera(center0);
	}
	@Override
	public void zoomOut() {
		double[] center0 = getCenter();
		size.scale(ZOOM_RATE);
		centerCamera(center0);
	}
	@Override
	public void zoomIn(double x, double y) {
		double[] center0 = new double[]{x, y, 0};
		size.scale(1/ZOOM_RATE);
		centerCamera(center0);
	}
	@Override
	public void zoomOut(double x, double y) {
		double[] center0 = new double[]{x, y, 0};
		size.scale(ZOOM_RATE);
		centerCamera(center0);
	}
	@Override
	public void setBoundaries(double xMin, double yMin, double xMax, double yMax) {
		double deltaXA=xMax-xMin, deltaYA=yMax-yMin;
		double[][] points = new double[][]{new double[]{xMin,yMin,0}, new double[]{xMin,yMax,0}, new double[]{xMax,yMin,0}, new double[]{xMax,yMax,0}};
		double[][] parameters = new double[][]{getParameters(points[0]), getParameters(points[1]), getParameters(points[2]), getParameters(points[3])};
		xMin = Double.MAX_VALUE;
		yMin = Double.MAX_VALUE;
		xMax = -Double.MAX_VALUE;
		yMax = -Double.MAX_VALUE;
		for(int i=0; i<parameters.length; i++) {
			double[] p = parameters[i];
			if(p[0]<xMin)
				xMin = p[0];
			if(p[1]<yMin)
				yMin = p[1];
			if(p[0]>xMax)
				xMax = p[0];
			if(p[1]>yMax)
				yMax = p[1];
		}
		double deltaX=xMax-xMin, deltaY=yMax-yMin;
		if(deltaXA/deltaYA<=deltaX/deltaY) {
			size.setX(deltaX);
			size.setY(-deltaYA*deltaX/deltaXA);
		}
		else {
			size.setX(deltaXA*deltaY/deltaYA);
			size.setY(-deltaY);
		}		
		l = getVector((xMin+xMax)/2, (yMin+yMax)/2);
	}
	@Override
	public void move(int dx, int dy) {
		l = getVector(dx*size.getX()/width,dy*size.getY()/height);
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
		double t = -getDistanceToCamera(p);
		return d.scalarMultiply(t).add(p);
	}
	public double getDistanceToCamera(Vector3D p) {
		return Vector3D.dotProduct(d, p)-Vector3D.dotProduct(d, l);
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
