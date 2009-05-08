package playground.gregor.otf;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.matsim.core.utils.collections.QuadTree;

import playground.gregor.collections.gnuclasspath.TreeMap;

public class InundationData implements Serializable{

	/**
	 * 
	 */
	private static final long serialVersionUID = 6819500627032639679L;

	public double [] powerLookUp;
	public Map<Double,QuadTree<Quad>> floodingData = new HashMap<Double,QuadTree<Quad>>();
//	public float[][] colorTable;

	static final float [] empty = new float [] {1,1,1,0};
	
	static final int RES = 8;
	static final byte [][] walsh16 = new byte [][] {
		{1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1},
		{1,1,1,1,1,1,1,1,-1,-1,-1,-1,-1,-1,-1,-1},
		{1,1,1,1,-1,-1,-1,-1,-1,-1,-1,-1,1,1,1,1},
		{1,1,1,1,-1,-1,-1,-1,1,1,1,1,-1,-1,-1,-1},
		{1,1,-1,-1,-1,-1,1,1,1,1,-1,-1,-1,-1,1,1},
		{1,1,-1,-1,-1,-1,1,1,-1,-1,1,1,1,1,-1,-1},
		{1,1,-1,-1,1,1,-1,-1,-1,-1,1,1,-1,-1,1,1},
		{1,1,-1,-1,1,1,-1,-1,1,1,-1,-1,1,1,-1,-1},
		{1,-1,-1,1,1,-1,-1,1,1,-1,-1,1,1,-1,-1,1},
		{1,-1,-1,1,1,-1,-1,1,-1,1,1,-1,-1,1,1,-1},
		{1,-1,-1,1,-1,1,1,-1,-1,1,1,-1,1,-1,-1,1},
		{1,-1,-1,1,-1,1,1,-1,1,-1,-1,1,-1,1,1,-1},
		{1,-1,1,-1,-1,1,-1,1,1,-1,1,-1,-1,1,-1,1},
		{1,-1,1,-1,-1,1,-1,1,-1,1,-1,1,1,-1,1,-1},
		{1,-1,1,-1,1,-1,1,-1,-1,1,-1,1,-1,1,-1,1},
		{1,-1,1,-1,1,-1,1,-1,1,-1,1,-1,1,-1,1,-1},
	};
	static final byte [][] walsh8 = new byte [][] {
		{1,1,1,1,1,1,1,1},
		{1,1,1,1,-1,-1,-1,-1},
		{1,1,-1,-1,-1,-1,1,1},
		{1,1,-1,-1,1,1,-1,-1},
		{1,-1,-1,1,1,-1,-1,1},
		{1,-1,-1,1,-1,1,1,-1},
		{1,-1,1,-1,-1,1,-1,1},
		{1,-1,1,-1,1,-1,1,-1},
	};	
	
	static final byte [][] walsh4 = new byte [][] {
		{1,1,1,1},
		{1,1,-1,-1},
		{1,-1,-1,1},
		{1,-1,1,-1},
	};	
	static final byte [][] walsh2 = new byte [][] {
		{1,1},
		{1,-1},
	};
	static final byte [][] walsh1 = new byte [][] {
		{1}
	};
	
//	public HashMap<Integer, byte[]> tableMapping;

	public TreeMap<Double,float[]> colorMapping;
	
	public int seriesLength;
	
	
	public static class Quad implements Serializable {
		/**
		 * 
		 */
		private static final long serialVersionUID = -3119790587775248913L;
		
		
		
		
		//		public HashMap<Double, float[]> acol = new HashMap<Double, float[]>();
		//		public HashMap<Double, float[]> dcol = new HashMap<Double, float[]>();
		//		public HashMap<Double, float[]> bcol = new HashMap<Double, float[]>();
		//		public HashMap<Double, float[]> ccol = new HashMap<Double, float[]>();
		//		public double flTime;
		
		float x;
		float y;
		float diff;
		//		Coordinate a;
		//		Coordinate b;
		//		Coordinate c;
		//		Coordinate d;
//		public int col;
//		public int acol;
//		public int bcol;
//		public int ccol;
//		public int dcol;

		float [] walsh;
		float [] awalsh;
		float [] bwalsh;
		float [] cwalsh;
		float [] dwalsh;




		public float xa;




		public float ya;




		public float xb;




		public float yb;




		public float xc;




		public float yc;
		
		//		public String getId() {
		//			ArrayList<Double> list = new ArrayList<Double>();
		//			list.add(this.a.x);
		//			list.add(this.b.x);
		//			list.add(this.c.x);
		//			list.add(this.d.x);
		//			list.add(this.a.y);
		//			list.add(this.b.y);
		//			list.add(this.c.y);
		//			list.add(this.d.y);
		//			Collections.sort(list);
		//			return list.toString();
		//
		//		}
	}




	public float[] getColor(short timeSlotIdx, float[] coef) {
		
		int idx = Math.round(((timeSlotIdx-coef[RES])/(this.seriesLength-coef[RES])) * (RES-1));
		double restored = 0;
		for (int k = 0; k < RES; k++) {
			restored += coef[k] * walsh8[k][idx]; 
		}
		
		if (restored < 0) {
			return empty;
		}
		if (Double.isNaN(restored)) {
			return empty;
		}
		
//		System.out.println("restored:" + restored);
		Entry e = this.colorMapping.floorEntry(restored);
		if (e == null) {
			return empty;
		}
		float [] ret = (float[]) e.getValue(); 
		if (ret == null) {
			return empty;
		}
		return ret;
	}
	
}
