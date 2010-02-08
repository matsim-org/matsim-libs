package playground.dressler.Interval;

public class Intervals<T extends Interval > {
//------------------------FIELDS----------------------------------//

	/**
	 * internal binary search tree holding distinct VertexInterval instances
	 */
	AVLTree _tree;
	
	/**
	 * reference to the last VertexInterval
	 */
	 T _last; 
	
	/**
	 * flag for debug mode
	 */
	@SuppressWarnings("unused")
	private static int _debug = 0;
	
	//-----------------------METHODS----------------------------------//
	//****************************************************************//
		
		 
	//----------------------CONSTRUCTORS------------------------------//	
		
		/**
		 * Default Constructor Constructs an object containing only 
		 * one EdgeInterval [0,Integer.MAX_VALUE) with flow equal to 0
		 */
		public Intervals(T first){
			T interval = first;
			_tree = new AVLTree();
			_tree.insert(interval);
			_last =  interval;
		}
		//------------------------------GETTER-----------------------//
		
		
		/**
		 * Finds the VertexInterval containing t in the collection
		 * @param t time
		 * @return  VertexInterval  containing t
		 */
		public T getIntervalAt(int t){
			if(t<0){
				throw new IllegalArgumentException("negative time: "+ t);
			}
			T i = (T) _tree.contains(t);
			if(i==null)throw new IllegalArgumentException("there is no Interval containing "+t);
			return i;
		}
		
		/**
		 * Returns the number of stored intervals
		 * @return the number of stored intervals
		 */
		public int getSize() {		
			return this._tree._size;
		}
		/**
		 * Gives the last stored VertexInterval
		 * @return VertexInterval with maximal lowbound
		 */
		public T getLast(){
			return _last;
		}
		

		/**
		 * checks whether last is referenced right
		 * @return true iff everything is OK
		 */
		public boolean checkLast(){
			return _last==_tree._getLast().obj;
		}
		
		/**
		 * Checks whether the given VertexInterval is the last
		 * @param o EgeInterval which it test for 
		 * @return true if getLast.equals(o)
		 */
		public boolean isLast(T o){
			return (_last.equals(o));
		}
		
		
		
		/**
		 * gives the next VertexInterval with respect to the order contained 
		 * @param o should be contained
		 * @return next VertexInterval iff o is not last and contained
		 */
		public T getNext(T o){
			_tree.goToNodeAt(o.getLowBound());
			T j = (T) _tree._curr.obj;
			if(j.equals(o)){
				_tree.increment();
				if(!_tree.isAtEnd()){
					T i = (T) _tree._curr.obj;
					_tree.reset();
					return i;
				}else 	throw new IllegalArgumentException("Interval was already last");
			}
			else throw new IllegalArgumentException("Interval was not contained");
		}
		
		
	//------------------------SPLITTING--------------------------------//	
		
		/**
		 * Finds the VertexInterval containing t and splits this at t 
		 * giving it the same flow as the flow as the original 
		 * it inserts the new VertexInterval after the original
		 * @param t time point to split at
		 * @return the new VertexInterval for further modification
	 	 */
		public T splitAt(int t){
			boolean found = false;
			T j = null;
			T i = this.getIntervalAt(t);
				if (i != null){
					found = true;
					//update last
					if(i == _last){
						j =(T) i.splitAt(t);
						_last = j;
					}else {
						j =(T) i.splitAt(t);
					}
				}
			if (found){
				_tree.insert(j);
				return j;
			}
			else throw new IllegalArgumentException("there is no Interval that can be split at "+t);
		}

		
		/**
		 * setter for debug mode
		 * @param debug debug mode true is on
		 */
		public static void debug(int debug){
			Intervals._debug=debug;
		}
		

		/**
		 * Gives a String representation of all stored Intervals
		 * @return String representation
		 */
		
		public String toString(){
			
			StringBuilder str = new StringBuilder();
			for(_tree.reset();!_tree.isAtEnd();_tree.increment()){
				T i= (T) _tree._curr.obj;
				str.append(i.toString()+" \n");
			}
			return str.toString();	
		}
}
