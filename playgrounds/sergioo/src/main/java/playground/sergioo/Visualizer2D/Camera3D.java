package playground.sergioo.Visualizer2D;

import org.apache.commons.math.geometry.Vector3D;

public class Camera3D extends Camera2D implements Camera {
	
	//Constantes
	private static final double ZOOM_RATE = 5.0/4.0;
	
	//Attributes
	private Vector3D l;
	private Vector3D d;
	private Vector3D h;
	
	//Methods
	public Camera3D() {
		super();
		l = new Vector3D(0, 0, 1);
		d = new Vector3D(0, 0, -1);
		h = new Vector3D(0, 1, 0);
	}
	public Camera3D(Vector3D l, Vector3D d, Vector3D h) {
		super();
		this.l = l;
		this.d = d;
		this.h = h;
	}
	public Vector3D getL() {
		return l;
	}
	public Vector3D getD() {
		return d;
	}
	public Vector3D getH() {
		return h;
	}
	private Vector3D getCenterCamera() {
		Vector3D u = Vector3D.crossProduct(d, h);
		Vector3D v = h;
		return u.scalarMultiply(size.getX()/2).add(v.scalarMultiply(size.getY()/2)).add(l);
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
		l = l.add(d.scalarMultiply(ZOOM_RATE));
		size.scale(1/ZOOM_RATE);
		centerCamera(center0);
	}
	@Override
	public void zoomOut() {
		l = l.subtract(d.scalarMultiply(ZOOM_RATE));
		size.scale(ZOOM_RATE);
	}
	@Override
	public void setBoundaries(double xMin, double yMin, double xMax, double yMax) {
		double deltaXA=xMax-xMin, deltaYA=yMax-yMin;
		double[][] points = new double[][]{new double[]{xMin,yMin,0}, new double[]{xMin,yMax,0}, new double[]{xMax,yMin,0}, new double[]{xMax,yMax,0}};
		double[][] parameters = new double[][]{getParameters(points[0]), getParameters(points[1]), getParameters(points[2]), getParameters(points[3])};
		xMin = Double.MAX_VALUE;
		yMin = Double.MAX_VALUE;
		xMax = 0;
		yMax = 0;
		int xMinI=0, yMaxI=0;
		for(int i=0; i<parameters.length; i++) {
			double[] p = parameters[i];
			if(p[0]<xMin) {
				xMin=p[0];
				xMinI = i;
			}
			if(p[1]<yMin)
				yMin=p[1];
			if(p[0]>xMax)
				xMax=p[0];
			if(p[1]>yMax) {
				yMax=p[1];
				yMaxI = i;
			}
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
		l = getVector(parameters[xMinI][0],parameters[yMaxI][1]);
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
		if(ele<0)
			ele = 0;
		else if(ele>Math.PI)
			ele = Math.PI;
		Vector3D nr = new Vector3D(azi, ele).scalarMultiply(r.getNorm());
		center = center0.add(nr);
		d = nr.negate().normalize();
		if(d.getZ()==1 || d.getZ()==-1)
			h = new Vector3D(0, 1, 0);
		else
			h = new Vector3D(d.getX(), d.getY(), d.getZ()-(1/d.getZ())).normalize();
		Vector3D u = Vector3D.crossProduct(d, h);
		Vector3D v = h;
		l = center.subtract(u.scalarMultiply(size.getX()/2).add(v.scalarMultiply(size.getY()/2)));
	}
	public void centerCamera(double x, double y) {
		l = getPointInCamera(new double[]{x, y, 0});
		l = getVector(-size.getX()/2, -size.getY()/2);
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
		double t = (Vector3D.dotProduct(d, l)-Vector3D.dotProduct(d, p));
		return d.scalarMultiply(t).add(p);
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
		return new int[]{(int)(u*width/size.getX())+frameSize, (int)(v*height/size.getY())+frameSize};
	}
	private Vector3D getVector(double paramU, double paramV) {
		Vector3D u = Vector3D.crossProduct(d, h);
		Vector3D v = h;
		return u.scalarMultiply(paramU).add(v.scalarMultiply(paramV)).add(l);
	}
	@Override
	public double[] getWorld(int x, int y) {
		Vector3D s = getVector((x-frameSize)*size.getX()/width,(y-frameSize)*size.getY()/height);
		Vector3D s0 = d.scalarMultiply(-s.getZ()/d.getZ()).add(s);
		return new double[]{s0.getX(), s0.getY(), s0.getZ()};
	}
	@Override
	public boolean isInside(double[] point) {
		double[] params = getParameters(point);
		return params[0]<size.getX() && params[0]>0 && params[1]>size.getY() && params[1]<0;
	}

}
