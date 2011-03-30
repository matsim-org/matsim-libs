package util.geometry;

import java.util.ArrayList;
import java.util.List;

public class Rectangle2D {
	//Attributes
	private Point2D center;
	private Point2D[] corners;
	private Line2D[] sides;
	private double width;
	private double height;
	private double ang;
	//Methods
	public Rectangle2D(Point2D center, double width, double height, double ang) {
		super();
		this.center = center;
		this.width = width;
		this.height = height;
		this.ang = ang;
		this.corners = new Point2D[4];
		for(int i=0; i<4; i++) {
			this.corners[i]=this.calculatePoint(i);
		}
		this.sides = new Line2D[4];
		for(int i=0; i<4; i++) {
			this.sides[i]=this.calculateLine(i);
		}
	}
	public Rectangle2D(Line2D side, double length) {
		super();
		this.center = new Point2D(side.getCenter().getX()+length*Math.sin(side.getAngle())/2,
				side.getCenter().getY()-length*Math.cos(side.getAngle())/2);
		this.width = side.getLength();
		this.height = length;
		this.ang = side.getAngle();
		this.corners = new Point2D[4];
		for(int i=0; i<4; i++) {
			this.corners[i]=this.calculatePoint(i);
		}
		this.sides = new Line2D[4];
		for(int i=0; i<4; i++) {
			this.sides[i]=this.calculateLine(i);
		}
	}
	public Point2D getCenter() {
		return center;
	}
	public void setCenter(Point2D center) {
		this.center = center;
	}
	public double getWidth() {
		return width;
	}
	public void setWidth(double width) {
		this.width = width;
	}
	public double getHeight() {
		return height;
	}
	public void setHeight(double height) {
		this.height = height;
	}
	public double getAng() {
		return ang;
	}
	public void setAng(double ang) {
		this.ang = ang;
	}
	public Point2D getCorner(int pos) {
		if(pos>=0 && pos<4)
			return corners[pos];
		return null;
	}
	public Point2D calculatePoint(int pos) {
		Vector2D v = null;
		if(pos==0)
			v=new Vector2D(width/2,height/2);
		else if(pos==1)
			v=new Vector2D(-width/2,height/2);
		else if(pos==2)
			v=new Vector2D(-width/2,-height/2);
		else
			v=new Vector2D(width/2,-height/2);
		v.rotate(ang);
		return new Point2D(center.getX()+v.getX(),center.getY()+v.getY());
	}
	public Line2D getSide(int pos) {
		if(pos<4 && pos>=0)
			return sides[pos];
		return null;
	}
	public Line2D calculateLine(int pos) {
		if(pos<3 && pos>=0)
			return new Line2D(this.calculatePoint(pos),this.calculatePoint(pos+1));
		else if(pos==3)
			return new Line2D(this.calculatePoint(pos),this.calculatePoint(0));
		else
			return null;
			
	}
	public boolean isInCollision(Rectangle2D r2) {
		for(int i=0; i<4; i++)
			for(int j=0; j<4; j++)
				if(this.getSide(i).isIntersected(r2.getSide(j)))
					return true;
		return false;
	}
	public List<int[]> getDiscreteRectangle2D(double tamCell) {
		int maxX=0, maxY=0, minX=Integer.MAX_VALUE, minY=Integer.MAX_VALUE;
		List<int[]> cells=new ArrayList<int[]>();
		for(int i=0; i<4; i++) {
			List<int[]> res=sides[i].getDiscreteLine2D(tamCell);
			if(res!=null && res.size()>0) {
				if(i==0)
					cells.add(res.get(0));
				for(int j=1; j<res.size()-1; j++)
					cells.add(res.get(j));
				if(i!=3)
					cells.add(res.get(res.size()-1));
				if(res.get(0)[0]>maxY)
					maxY=res.get(0)[0];
				if(res.get(0)[1]>maxX)
					maxX=res.get(0)[1];
				if(res.get(0)[0]<minY)
					minY=res.get(0)[0];
				if(res.get(0)[1]<minX)
					minX=res.get(0)[1];
				if(res.get(res.size()-1)[0]>maxY)
					maxY=res.get(res.size()-1)[0];
				if(res.get(res.size()-1)[1]>maxX)
					maxX=res.get(res.size()-1)[1];
				if(res.get(res.size()-1)[0]<minY)
					minY=res.get(res.size()-1)[0];
				if(res.get(res.size()-1)[1]<minX)
					minX=res.get(res.size()-1)[1];
			}
			else {
				return null;
			}
		}
		if(cells!=null) {
			for(int i=minY; i<=maxY; i++) {
				int ini1=-1;
				int fin1=-1;
				int ini2=-1;
				for(int j=minX; ini2==-1 && j<=maxX; j++) {
					boolean a=false;
					for(int c=0; c<cells.size(); c++)
						if(cells.get(c)[0]==j && cells.get(c)[1]==i)
							a=true;
					if(a && ini1==-1)
						ini1=j;
					else if(ini1!=-1 && fin1==-1 && !a)
						fin1=j;
					else if(a && fin1!=-1)
						ini2=j;
				}
				for(int j=fin1; j<ini2; j++) {
					int[] e={j,i};
					cells.add(e);
				}
			}
		}
		return cells;
	}
}
