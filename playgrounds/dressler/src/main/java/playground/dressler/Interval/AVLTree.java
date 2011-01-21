/* *********************************************************************** *
 * project: org.matsim.*
 * AVLTree.java
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


package playground.dressler.Interval;

/**
 * class for AVL trees
 *
 * @author Martin Oellrich modified by Manuel Schneider
 */

public class AVLTree
extends      SearchTree

{
    /**
     * class for tree nodes with balances
     */
    protected class AVLTreeNode
    extends BinTree.BinTreeNode
    {
	int balance ;               // height balance of subtree rooted at node

	public AVLTreeNode ( Interval obj )                   // init constructor
	{
	    super( obj ) ;
	    balance = 0 ;
	}

	@Override
	public String toString ( )
	{
	    StringBuffer strbuf = new StringBuffer( super.toString() ) ;

	    strbuf.append( "(" + balance + ")" ) ;

	    return strbuf.toString() ;
	}
    }  // class AVLTreeNode


/***  constants  *************************************************************/

    /**
     * flag for runtime comments from insert()
     */
    private static final boolean INSERT = false ;

    /**
     * flag for runtime comments from remove()
     */
    private static final boolean REMOVE = false ;

    /**
     * flag for runtime comments from _rebalance()
     */
    private static final boolean REBALANCE = false ;

    /**
     * flag for runtime comments from _rotateLeft() and _rotateRight()
     */
    private static final boolean ROTATE = false ;


/***  constructors  **********************************************************/

    /**
     * default constructor: create empty tree
     */
    public AVLTree ( )
    {
	_dummy = new AVLTreeNode( null ) ;                        // dummy node
    }


/***  rebalancing methods  ***************************************************/

    /**
     * private: rotate subtrees rooted at node to the left
     *
     * @param node tree node rotate at
     */
    private void _rotateLeft ( BinTreeNode node )
    {
    //	if ( ROTATE )
    //		System.out.println( "      _rotateLeft ( " + node + " )" ) ;

	BinTreeNode rson = node.right ;

	node.right = rson.left ;
	if ( rson.left != null )
	    rson.left.parent = node ;
	rson.left = node ;

	if ( node.isLeftChild() )
	    node.parent.left  = rson ;
	else
	    node.parent.right = rson ;    
	rson.parent = node.parent ;
	node.parent = rson ;

    }  // _rotateLeft()


    /**
     * private: rotate subtrees rooted at node to the right 
     *
     * @param node tree node rotate at
     */
    private void _rotateRight ( BinTreeNode node )
    {
//if ( ROTATE )
 // System.out.println( "      _rotateRight( " + node + " )" ) ;

	BinTreeNode lson = node.left ;

	node.left = lson.right ;
	if ( lson.right != null )
	    lson.right.parent = node ;
	lson.right = node ;

	if ( node.isLeftChild() )
	    node.parent.left  = lson ;
	else
	    node.parent.right = lson ;    
	lson.parent = node.parent ;
	node.parent = lson ;

    }  // _rotateRight()


    /**
     * private: rebalance the tree at given parent cascade
     *
     * @param upNode  upper  node in parent cascade
     * @return whether any rotations were carried out
     */
    private boolean _rebalance ( AVLTreeNode upNode )
    {
	//if ( REBALANCE )
	//    System.out.print( "   _rebalance( up = " + upNode ) ;

	if ( upNode.balance == -2 )
	    {
		AVLTreeNode midNode = (AVLTreeNode)( upNode.left ) ;
		//if ( REBALANCE )
		//    System.out.print( ", mid = " + midNode ) ;

		if ( midNode.balance <= 0 )
		    {
			//if ( REBALANCE )
			//    System.out.println( " )" ) ;
			_rotateRight( upNode ) ;
			++( midNode.balance ) ;
			upNode.balance = -midNode.balance ;
		    }
		else
		    {
			AVLTreeNode lowNode = (AVLTreeNode)( midNode.right ) ;
			//if ( REBALANCE )
			//    System.out.println( ", low = " + lowNode + " )" ) ;
			
			_rotateLeft ( midNode ) ;
			_rotateRight( upNode  ) ;
			upNode .balance = ( lowNode.balance >= 0  ?  0  :   1 ) ;
			midNode.balance = ( lowNode.balance <= 0  ?  0  :  -1 ) ;
			lowNode.balance = 0 ;
		    }

		return true ;

	    }  // if ( upNode.balance == -2 )
	
	if ( upNode.balance == 2 )
	    {
		AVLTreeNode midNode = (AVLTreeNode)( upNode.right ) ;
		//if ( REBALANCE )
		//    System.out.print( ", mid = " + midNode ) ;
		
		if ( midNode.balance >= 0 )
		    {
			//if ( REBALANCE )
			//    System.out.println( " )" ) ;
			_rotateLeft( upNode ) ;
			--( midNode.balance ) ;
			upNode.balance = -midNode.balance ;
		    }
		else
		    {
			AVLTreeNode lowNode = (AVLTreeNode)( midNode.left ) ;
			//if ( REBALANCE )
			//    System.out.println( ", low = " + lowNode + " )" ) ;
			
			_rotateRight( midNode ) ;
			_rotateLeft ( upNode  ) ;
			upNode .balance = ( lowNode.balance <= 0  ?  0  :  -1 ) ;
			midNode.balance = ( lowNode.balance >= 0  ?  0  :   1 ) ;
			lowNode.balance = 0 ;
		    }
		
		return true ;
		
	    }  // if ( upNode.balance == 2 )
	
	//if ( REBALANCE )
	//    System.out.println( " )" ) ;
	
	return false ;
	
    }  // _rebalance()
    

/***  set methods  ***********************************************************/

    /**
     * insert object into tree if no other object with same key is present
     *
     * @param object to be inserted
     * @return the object itself if it is the first one with its key,
     *         otherwise the found object with same key
     */
    @Override
		public Interval insert ( Interval obj )
    {
    /* insert object normally */

	AVLTreeNode parent = (AVLTreeNode)_findParentFor( obj ) ;  // "parent" of obj
	AVLTreeNode node ;
	
	if (     parent == _dummy            // REM: compareTo() not applicable
		 ||  obj.compareTo(parent.obj ) < 0 )
	    {
		if ( parent.left != null )    // object with same key found in tree
		    {                             // => nothing to do
			//if ( INSERT )
			//    System.out.println( "insert( " + obj + " ): other object found!" ) ;
			
			return parent.left.obj;
		    }
		//if ( INSERT )
		//    System.out.print( "insert( " + obj + " ): before insertion: " + this ) ;
		
		node        = new AVLTreeNode( obj ) ;
		parent.left = node ;
	    }
	else
	    {
		if ( parent.right != null )   // object with same key found in tree
		    {                             // => nothing to do
			//if ( INSERT )
			//    System.out.println( "insert(): other object " + obj + " found!" ) ;

			return parent.right.obj;
		    }
		//if ( INSERT )
		//    System.out.print( "insert( " + obj + " ): before insertion: " + this ) ;
		
		node         = new AVLTreeNode( obj ) ;
		parent.right = node ;
	    }
	
	node.parent = parent ;
	++_size ;                                        // count nodes in tree

	/* rebalance the tree */

	while ( parent != _dummy )
	    {
		if ( node.isLeftChild() )
		    --( parent.balance ) ;
		else
		    ++( parent.balance ) ;
		
		if (     parent.balance == 0                // no further need
			 ||  _rebalance( parent ) )             // just rebalanced tree
		    break ;
		
		node   = parent ;
		parent = (AVLTreeNode)( node.parent ) ;
		
	    }  // while ( parent != _dummy )

	/* last things */
	
	//if ( INSERT )
	//    System.out.println( "insert( " + obj + " ): after insertion:" + this ) ;
	
	//checkBalances() ;                 // check balance property (debugging)
	
	return obj ;

    }  // insert()


    /**
     * remove object with same key from tree if present
     *
     * @param object whose key is searched
     * @return removed object if any was found, null otherwise
     */
    @Override
		public Interval remove ( Interval obj )
    {
	/* locate node to be deleted */

        AVLTreeNode node = (AVLTreeNode)_nodeToDelete( obj ) ;

	if ( node == null )            // no object with same key found in tree
	{                              // => nothing to do
	   // if ( REMOVE )
		//System.out.println( "remove( " + obj + " ): object not found!" ) ;

            return null ;
	}
	
	//if ( REMOVE )
	//    System.out.print( "remove( obj = " + obj + " ):  before removal: " + this ) ;

	/* rebalance the tree */

	AVLTreeNode saveNode = node ;                 // save for removal later
	AVLTreeNode parent   = (AVLTreeNode)( node.parent ) ;

	while ( parent != _dummy )
	{
	    if ( node.isLeftChild() )
		++( parent.balance ) ;
	    else
		--( parent.balance ) ;

	    node = (    _rebalance( parent )
			?  (AVLTreeNode)( parent.parent )  :  parent ) ;
	    
	    if ( node.balance == 1  ||  node.balance == -1 )
		break ;

	    parent = (AVLTreeNode)( node.parent ) ;

	}  // while ( parent != _dummy )

	/* remove node from tree */

	obj = saveNode.obj;          // save object for return
	_remove( saveNode ) ;        // remove and abandon node now

	/* last things */

	//if ( REMOVE )
	//    System.out.println(   "remove(): return object " + obj
	//	      + ", after removal: " + this ) ;

	//checkBalances() ;                 // check balance property (debugging)

	return obj ;

    }  // remove()

    
/***  debugging methods  *****************************************************/

    /**
     * flag indicating whether checkBalances() has found balance property
     * consistent in entire tree
     */
    private boolean _retval ;

    /**
      * private: recursively check consistency of balances in subtree
      *
      * @param node root of subtree to be checked
      * @return height of subtree rooted at node
      */
    private int _checkBal ( BinTreeNode node ) 
    {
	if ( node == null )
	    return -1 ;

	int height1 = _checkBal( node.left  ) ;
	int height2 = _checkBal( node.right ) ;

	if ( Math.abs( height1 - height2 ) > 1 )
	{
	    _retval = false ;
	    System.err.println(   "AVLTree.checkBalances(): balance property"
			        + " violated in node " + node + ": balance = "
			        + ( height1 - height2 ) ) ;
        }
	else if ( height2 - height1 != ( (AVLTreeNode)node ).balance )
	{
	    _retval = false ;
	    System.err.println(   "AVLTree.checkBalances(): balance property"
				+ " inconsistent in node " + node
				+ ": real balance = " + ( height2 - height1 )
			        + " != " + ( (AVLTreeNode)node ).balance
			        + " = node.balance" ) ;
        }

	return ( height1 >= height2  ?  height1  :  height2 ) + 1 ;

    }  // _checkBal()

    /**
      * private: check consistency of search tree property
      *
      * @return are properties consistent in tree?
      */
    public boolean checkBalances ( )
    {
    	throw new UnsupportedOperationException( );
    	
    	/*
	_retval = _checkTree() ;                  // check search tree property

	if ( _checkMode  &&  ! isEmpty() )                   // checking wanted
	    _checkBal( _getRoot() ) ;

	return _retval ;*/

    }  // checkBalances()

}  // class AVLTree
