package playground.gregor.spatial;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;


/**
 * 2D-KDTree (2DTree) implementation
 * @author laemmel
 *
 */
public class KDTree2D {

	private final KDNode root;
	private final List<double[]> points;

	public KDTree2D(List<double[]> points) {

		List<Integer> x = new ArrayList<Integer>();
		List<Integer> y = new ArrayList<Integer>();
		for (int i = 0; i < points.size(); i++) {
			x.add(i);
			y.add(i);

		}

		Collections.sort(x, new XComp(points));
		Collections.sort(y, new YComp(points));
		this.points = points;
		P p = new P(x,y);

		int splitPoint = x.size()/2;
		double splitVal = this.points.get(x.get(splitPoint))[0];
		this.root = new KDNode(p,0,splitVal,null);

		buildKDTree(this.root,0);
	}

	public KDNode getRoot() {
		return this.root;
	}

	public double[] getNearestNeighbor(double x, double y) {
		KDNode n = getNearestNeighbor(x, y,this.root);
		return this.points.get(n.p.getXs().get(0));
	}

	private KDNode getNearestNeighbor(double x, double y, KDNode root) {
		KDNode leaf = goDownToLeaf(x,y,0,root);

		KDNode best = goUpToRoot(x,y,leaf,leaf, root.depth);
		return best;
	}

	private KDNode goUpToRoot(double x, double y, KDNode current, KDNode currentBest, int rootDepth) {
		if (current.depth == rootDepth) {
			return currentBest;
		}

		KDNode parent = current.parent;

		KDNode nodeToCheck;
		if (parent.left == current) {
			nodeToCheck = parent.right;
		} else {
			nodeToCheck = parent.left;
		}
		if (needToCheck(parent,x,y,currentBest)) {
			KDNode tmpBest = getNearestNeighbor(x,y,nodeToCheck);
			double [] tmpVal = this.points.get(tmpBest.p.getXs().get(0));
			double sqrDistTmp = (x-tmpVal[0]) * (x-tmpVal[0]) + (y-tmpVal[1]) * (y-tmpVal[1]);

			double [] val = this.points.get(currentBest.p.getXs().get(0));
			double bestSqrDist = (x- val[0]) * ( x - val[0]) + (y - val[1]) * (y - val[1]);

			if (sqrDistTmp < bestSqrDist) {
				currentBest  = tmpBest;
			}
		}

		return goUpToRoot(x,y,parent,currentBest,rootDepth);
	}

	private boolean needToCheck(KDNode node, double x, double y,
			KDNode currentBest) {

		double [] val = this.points.get(currentBest.p.getXs().get(0));
		double bestDist = Math.sqrt((x- val[0]) * ( x - val[0]) + (y - val[1]) * (y - val[1]));

		if (node.depth % 2 == 0) {
			if (Math.abs(node.splitVal - x) < bestDist) {
				return true;
			}
		} else {
			if (Math.abs(node.splitVal - y) < bestDist) {
				return true;
			}			
		}
		return false;
	}

	private KDNode goDownToLeaf(double x, double y, int i, KDNode current) {


		if (current.p.isLeaf()) {
			return current;
		}

		boolean goLeft;
		if (current.depth % 2 == 0) {
			if (x <= current.splitVal) {
				goLeft = true;
			} else {
				goLeft = false;
			}
		} else {
			if (y <= current.splitVal) {
				goLeft = true;
			} else {
				goLeft = false;
			}
		}

		KDNode child = goLeft ? current.left : current.right;
		return goDownToLeaf(x,y,++i,child);
	}

	private KDNode buildKDTree(KDNode current, int i) {


		if (current.p.isLeaf()) {
			return current;
		}

		List<Integer> x = current.p.getXs();
		List<Integer> y = current.p.getYs();

		int splitPoint = x.size()/2;

		double splitR, splitL;
		P p1;
		P p2;

		if (i % 2 == 0) { // x-split

			List<Integer> xLeft = x.subList(0, splitPoint);
			List<Integer> xRight = x.subList(splitPoint, x.size());


			boolean [] lr = new boolean[this.points.size()];


			for (int j = 0; j < splitPoint; j++) {
				int idx = x.get(j);
				lr[idx] = false;
			}
			for (int j = splitPoint; j < x.size(); j++) {
				int idx = x.get(j);
				lr[idx] = true;
			}

			List<Integer> yLeft = new ArrayList<Integer>();
			List<Integer> yRight = new ArrayList<Integer>();
			for (int j = 0; j < x.size(); j++) {//BUG
				int idx0 = y.get(j);
				if (lr[idx0]) {
					yRight.add(idx0);
				} else {
					yLeft.add(idx0);
				}
			}



			int splitPointR = xRight.size()/2;
			int splitPointL = xLeft.size()/2;
			splitR = this.points.get(yRight.get(splitPointR))[1];
			splitL = this.points.get(yLeft.get(splitPointL))[1];

			p1 = new P(xLeft,yLeft);
			p2 = new P(xRight,yRight);

		} else { // y-split

			List<Integer> yLeft = y.subList(0, splitPoint);
			List<Integer> yRight = y.subList(splitPoint, y.size());


			boolean [] lr = new boolean[this.points.size()];


			for (int j = 0; j < splitPoint; j++) {
				int idx = y.get(j);
				lr[idx] = false;
			}
			for (int j = splitPoint; j < x.size(); j++) {
				int idx = y.get(j);
				lr[idx] = true;
			}

			List<Integer> xLeft = new ArrayList<Integer>();
			List<Integer> xRight = new ArrayList<Integer>();
			for (int j = 0; j < x.size(); j++) { //BUG
				int idx0 = x.get(j);
				if (lr[idx0]) {
					xRight.add(idx0);
				} else {
					xLeft.add(idx0);
				}
			}



			int splitPointR = xRight.size()/2;
			int splitPointL = (int) 0.5+xLeft.size()/2;
			splitR = this.points.get(xRight.get(splitPointR))[0];
			splitL = this.points.get(xLeft.get(splitPointL))[0];

			p1 = new P(xLeft,yLeft);
			p2 = new P(xRight,yRight);
		}
		i++;

		KDNode right = new KDNode(p2, i, splitR, current); 

		buildKDTree(right,i);
		KDNode left = new KDNode(p1,i,splitL,current);
		buildKDTree(left,i);

		KDNode ret = current;
		ret.left = left;
		ret.right = right;
		return ret;
	}

	private static class KDNode {
		private final int depth;
		private final double splitVal;
		private final KDNode parent;
		public KDNode(P p, int i, double splitVal, KDNode parent) {
			this.p = p;
			this.depth = i;
			this.splitVal = splitVal;
			this.parent = parent;
		}

		private KDNode left;
		private KDNode right;
		private final P p;
	}

	private static class P {

		private final List<Integer> x;
		private final List<Integer> y;
		public P(List<Integer> x2, List<Integer> y2) {
			this.x = x2;
			this.y = y2;
		}

		public List<Integer> getXs() {
			return this.x;
		}
		public List<Integer> getYs() {
			return this.y;
		}

		public boolean isLeaf() {
			return this.x.size() == 1;
		}


	}

	private static class XComp implements Comparator<Integer> {

		private final List<double[]> points;

		public XComp(List<double[]> points) {
			this.points = points;
		}

		@Override
		public int compare(Integer a0, Integer a1) {
			double p0 = this.points.get(a0)[0];
			double p1 = this.points.get(a1)[0];
			if (p0 < p1) {
				return -1;
			} else if (p0 > p1) {
				return 1;
			}
			throw new RuntimeException("Not implemented yet");
		}

	}

	private static class YComp implements Comparator<Integer> {

		private final List<double[]> points;

		public YComp(List<double[]> points) {
			this.points = points;
		}

		@Override
		public int compare(Integer a0, Integer a1) {
			double p0 = this.points.get(a0)[1];
			double p1 = this.points.get(a1)[1];
			if (p0 < p1) {
				return -1;
			} else if (p0 > p1) {
				return 1;
			}
			throw new RuntimeException("Not implemented yet");
		}

	}

}
