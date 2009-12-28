/* *********************************************************************** *
 * project: org.matsim.*
 * BinTree.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
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


package playground.dressler.Intervall.src.Intervalls ;


/**
 * abstract base class for all sorts of binary trees
 *
 * @author Martin Oellrich modified by Manuel Schneider
 */
abstract class BinTree
{
    /**
     * class for tree nodes
     */
    protected class BinTreeNode
    {
	Intervall     obj ;                          // object referenced by node
	BinTreeNode parent ;                       // parent of node
	BinTreeNode left ;                         // left child of node
	BinTreeNode right ;                        // right child of node

	public BinTreeNode ( )                     // default constructor
	{
	    obj    = null ;
	    parent = left = right = null ;
	}

	public BinTreeNode (Intervall obj )          // init constructor
	{
	    this() ;
	    this.obj = obj ;
	}

	public int numChildren ( )                 // number of children
	{
	    int number = 0 ;

	    if ( left  != null )
		++number ;
	    if ( right != null )
		++number ;

	    return number ;

	}  // numChildren()

	public boolean isLeaf ( )                  // is node a leaf in tree?
	{
	    return ( left == null  &&  right == null ) ;
	}

	public boolean isRoot ( )                  // is node root of tree?
	{
	    return ( this == _getRoot() ) ;
	}

	public boolean isLeftChild ( )         // is node left child of parent?
	{
	    return ( parent != null  &&  parent.left == this ) ;
	}

	@Override
	public String toString ( )                 // conversion to string
	{
	    return ( obj != null  ?  obj.toString()  :  "*" ) ;
	}

    }  // class BinTreeNode


/***  data  ******************************************************************/

    /**
     * flag for debugging
     */
    protected static boolean _checkMode = false ;

    /**
     * dummy node referencing root of tree
     */
    protected BinTreeNode _dummy ;

    /**
     * reference to current node in tree, used for inorder traversal
     */
    protected BinTreeNode _curr ;


/***  constructors  **********************************************************/

    /**
     * default constructor, initializes empty tree
     */
    public BinTree ( )
    {
	_dummy = new BinTreeNode() ;                              // dummy node
	_curr  = null ;
    }


/***  get methods  ***********************************************************/

    /**
     * @return is tree empty?
     */
    public boolean isEmpty ( )
    {
	return ( _dummy.left == null ) ;
    }


    /**
     * @return root node of tree or dummy if tree is empty
     */
    protected BinTreeNode _getRoot ( )
    {
	return ( isEmpty()  ?  _dummy  :  _dummy.left ) ;
    }
    
    /**
     * 
     * @return last Node in INORDER
     */
    protected BinTreeNode _getLast()
    {
    	BinTreeNode save  = _curr ; 
    	BinTreeNode node = _getRoot();
    	while(node.right != null)
    	{
    		node=node.right;
    	}
    	_curr = save;
    	return node;
    }
    
    /**
     * 
     * @param node
     * @return predecessor of node or null if node is first in order
     */
    protected BinTreeNode _getPredecessor(BinTreeNode node){
    	BinTreeNode save  = _curr ; 
    	reset();
    	BinTreeNode pred =null;
    	while(node!= _curr)
    	{
    		pred = _curr;
    		increment();
    	}
    	_curr=save ;
    	return pred;
    }
    /**
     * gets predecessor
     * TODO more Efficient
     * @return
     */
    protected BinTreeNode _getPredecessor(){
    	BinTreeNode save  = _curr ; 
    	reset();
    	BinTreeNode pred =null;
    	while(save!= _curr)
    	{
    		pred = _curr;
    		increment();
    	}
    	_curr=save ;
    	return pred;
    }
    /**
     * returnes next node
     * @return
     */
    protected BinTreeNode getNext(){
    	BinTreeNode save  = _curr ;
    	if(!isAtEnd()){
    	increment();
    	BinTreeNode node = _curr;
    	_curr = save ;
    	return node;
    	}
    	else return null;
    }
    
    /**
     * @return current number of tree nodes
     */
    public int getSize ( )  // REM: uses iterative version, recursive is ok too
    {
	BinTreeNode save  = _curr ;                     // save _curr for later
	int         count = 0 ;

	for ( reset() ;  ! isAtEnd() ;  increment() )   // use _curr implicitly
	    ++count ;

	_curr = save ;                                  // restore _curr

	return count ;

    }  // getSize()


    /**
     * @return height of subtree rooted at node
     */
    private int _height ( BinTreeNode node ) 
    {
	if ( node == null )
	    return -1 ;

	int height1 = _height( node.left  ) ;
	int height2 = _height( node.right ) ;

	return ( height1 >= height2  ?  height1  :  height2 ) + 1 ;

    }  // _height()

    /**
     * @return height of tree
     */
    public int getHeight ( ) 
    {
	return _height( _getRoot() ) ;
    }


/***  set methods  ***********************************************************/

    /**
     * switch debugging mode
     */
    public static void setCheck ( boolean mode )
    {
	_checkMode = mode ;
    }



/***  methods for current node  **********************************************/

    /**
     * reset current node to first node in inorder sequence
     */
    public void reset ( )
    {
        for ( _curr = _dummy ;  _curr.left != null ;  _curr = _curr.left )
            ;
    }


    /**
     * @return does current node stand at end of inorder sequence?
     */
    public boolean isAtEnd ( )
    {
	return ( _curr == _dummy ) ;
    }

    
    
    /**
     *  reset current node to successor in inorder sequence
     */
    public void increment ( )
    {
    	Intervall old = this._curr.obj;
	if ( isAtEnd() )                             // stay in place if at end
	    return ;

        final int LEFT  = 1 ;
	final int RIGHT = 2 ;
	final int ABOVE = 3 ;
	int   origin    = LEFT ;                          // informal invariant

	do                         // while ( origin != LEFT  &&  ! isAtEnd() )
	{
	    switch ( origin )
	    {
	    case LEFT:
		 if ( _curr.right != null )
		 {
		     origin = ABOVE ;
		     _curr  = _curr.right ;      // we go from above into right
		 }
		 else
		     origin = RIGHT ;  // pretend we finished the right subtree
		 break ;

	    case RIGHT:
	         if ( _curr.isLeftChild() )
		     origin = LEFT ;
		 _curr = _curr.parent ;         // we go from below into parent
		 break ;
		    
	    case ABOVE:
		 if ( _curr.left != null )
		     _curr = _curr.left ;        // we go from above into right
		 else
		     return ;
		 break ;

	    default: ;

	    }  // switch ( origin )

	} while ( origin != LEFT ) ;
	Intervall newOne = this._curr.obj;
	if(old != null && newOne != null && old.getHighBound() != newOne.getLowBound())
	{
		throw new RuntimeException("foobar!");
	}
	
    }  // increment()


    /**
     * @return object referenced by current node
     */
    public Intervall currentData ( )
    {
	return _curr.obj ;
    }


    /**
     * @return is current node a leaf?
     */
    public boolean isAtLeaf ( )
    {
    	return _curr.isLeaf() ;
    }


/***  conversion methods  ****************************************************/

    /**
     * private: recursively traverse the tree, append output of level
     * to given StringBuffer
     *
     * @param strbuf StringBuffer to append to
     * @param level  current level in tree (for indentation)
     * @param node   current tree node
     */
    private void _tree2string ( StringBuffer strbuf, int level,
				BinTreeNode  node )
    {
	if ( node.left != null )
	    _tree2string( strbuf, level + 1, node.left ) ;

	for ( int i = 0 ;  i < level ;  ++i )
	    strbuf.append( "    " ) ;
	if ( ! node.isRoot() )
	    strbuf.append( node.isLeftChild() ? "/" : "\\" ) ;
	strbuf.append( node + "\n" ) ;

	if ( node.right != null )
	    _tree2string( strbuf, level + 1, node.right ) ;

    }  // _tree2string()

    /**
     * convert tree to string
     *
     * @return string representation of tree
     */
    @Override
		public String toString ( )
    {
	StringBuffer strbuf = new StringBuffer( this.getClass() + ": " ) ;

	if ( isEmpty() )
	    strbuf.append( "EMPTY\n" ) ;
	else
	{
	    strbuf.append( "\n" ) ;
	    _tree2string( strbuf, 0, _getRoot() ) ;
	}

	return strbuf.toString() ;

    }  // toString()


/***  debugging methods  *****************************************************/

    /**
      * private: recursively check consistency of links in subtree
      *
      * @param node root of subtree to be checked
      * @return are links consistent in subtree rooted at given node?
      */
    private boolean _checkLink ( BinTreeNode node )
    {
	boolean retval = true ;

	if ( node.left != null )
	    if ( node.left.parent != node )
	    {
		System.err.println(   "BinTree: Verlinkung zwischen Knoten "
				    + node
				    + " und linkem Kind inkonsistent!" ) ;
		retval = false ;
	    }
	    else if ( ! _checkLink( node.left ) )
		retval = false ;

	if ( node.right != null )
	    if ( node.right.parent != node )
	    {
		System.err.println(   "BinTree: Verlinkung zwischen Knoten "
				   + node
				   + " und rechtem Kind inkonsistent!" ) ;
		retval = false ;
	    }
	    else if ( ! _checkLink( node.right ) )
		retval = false ;

	return retval ;

    }  // _checkLink()


    /**
      * check consistency of links in entire tree
      *
      * @return are links consistent in tree?
      */
    protected boolean _checkLinks ( )
    {
	boolean retval = true ;

	if ( _checkMode  &&  ! isEmpty() )                   // checking wanted
	    retval = _checkLink( _getRoot() ) ;

	return retval ;

    }  // _checkLinks()

}  // class BinTree
