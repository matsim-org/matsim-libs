package playground.dressler.Interval;

import java.util.Iterator;

import playground.dressler.Interval.BinTree.BinTreeNode;
import playground.dressler.Interval.Interval;

public class BinTreeIterator<T extends Interval> implements Iterator {
	BinTreeNode node;
	BinTree tree;
	//SkipListIntervals<T> list;

	public BinTreeIterator(final BinTree tree) {
		this.tree = tree;
		
		for ( node = tree._dummy ;  node.left != null ;  node = node.left )
            ;
	}
	
	public BinTreeIterator(final SearchTree tree, int t) {
		this.tree = tree;
		node = tree.goToNodeAt(t);
	}
	
	/*public void setTo(int t) {	
		node = list.getNodeAt(t);
	}*/
	
	@Override
	public boolean hasNext() {		
		return ( node != tree._dummy ) ;
	}

	@Override
	public T next() {
		T tmp = (T) node.obj;
		
		
		final int LEFT  = 1 ;
    	final int RIGHT = 2 ;
    	final int ABOVE = 3 ;
    	int   origin    = LEFT ;         
    	
    	do                         // while ( origin != LEFT  &&  ! isAtEnd() )
    	{
    		switch ( origin )
    		{
    		case LEFT:
    			if ( node.right != null )
    			{
    				origin = ABOVE ;
    				node  = node.right ;      // we go from above into right
    			}
    			else
    				origin = RIGHT ;  // pretend we finished the right subtree
    			break ;

    		case RIGHT:
    			if ( node.isLeftChild() )
    				origin = LEFT ;
    			node = node.parent ;         // we go from below into parent
    			break ;

    		case ABOVE:
    			if ( node.left != null )
    				node = node.left ;        // we go from above into right
    			else
    				return tmp;
    			break ;

    		default: ;

    		}  // switch ( origin )

    	} while ( origin != LEFT ) ;
    			
		return tmp;
	}

	@Override
	public void remove() {
		throw new UnsupportedOperationException("This Iterator does not implement the remove method");  		
	}
	
	
}