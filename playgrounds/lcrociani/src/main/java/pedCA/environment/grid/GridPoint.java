package pedCA.environment.grid;

import java.io.Serializable;

public class GridPoint implements Serializable{

	private static final long serialVersionUID = 1L;
	private int x;
	private int y;
	
	public GridPoint(int x, int y){
		setX(x);
		setY(y);
	}

	public int getX() {
		return x;
	}

	public void setX(int x) {
		this.x = x;
	}

	public int getY() {
		return y;
	}

	public void setY(int y) {
		this.y = y;
	}
	
	@Override
	public final boolean equals(Object gp){
		return y == ((GridPoint)gp).getY() && x == ((GridPoint)gp).getX();
	}
	
	@Override
	public int hashCode(){
		return toString().hashCode();
	}
	
	public String toString(){
		return getY()+" "+getX();
	}
}
