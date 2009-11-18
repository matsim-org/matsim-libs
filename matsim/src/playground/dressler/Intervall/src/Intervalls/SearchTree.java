/* *********************************************************************** *
 * project: org.matsim.*
 * SearchTree.java
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
 * class for "normal" binary search trees
 *
 * @author Martin Oellrich modified by Manuel Schneider
 */

public class SearchTree
extends      BinTree
//implements   TraversableSortedContainer<Intervall>
{
/***  data  ******************************************************************/

    /**
     * current size of tree
     */
    protected int _size ;


/***  constructors  **********************************************************/

    /**
     * default constructor: create empty tree
     */
    public SearchTree ( )
    {
	super() ;                                      // create BinTree object
	_size = 0 ;
    }


/***  get methods  ***********************************************************/

    /**
     * @return current number of tree nodes
     */
    @Override
		public int getSize ( )
    {
	return _size ;
    }


/***  get methods  ***********************************************************/

    /**
     * @return current height of tree
     */
    public int maxSearch ( )
    {
	return getHeight() ;
    }


    /**
     * protected: search tree for an object with same key value as obj
     *
     * @param obj object whose key is to be searched for
     * @return reference to real or would-be parent node of obj
     */
    protected BinTreeNode _findParentFor ( Intervall obj )
    {
	BinTreeNode node = _dummy ;
	int         comp = -1 ;                    // root is left son of dummy
	
	while ( true )
	    if ( comp < 0 ) {
		if ( node.left == null )           // only virtual parent found
		    break ;
		comp = obj.compareTo(node.left.obj ) ;
		if ( comp == 0 )                   // real parent found
		    break ;
		
		node = node.left ;
	    }
	    else if ( comp > 0 ) {
		if ( node.right == null )          // only virtual parent found
		    break ;
		comp = obj.compareTo( node.right.obj ) ;
		if ( comp == 0 )                   // real parent found
		    break ;

		node = node.right ;
	    }
		
	return node ;                              // return "parent of object"
	
    }  // _findParentFor()
    

    
    /**
     * @return object in tree with same key as given object, null otherwise
     */
    public Intervall contains ( Intervall obj )
    {
	if ( isEmpty() )
	    return null ;
	
	BinTreeNode parent = _findParentFor( obj ) ;               // "parent" of obj
	
	BinTreeNode node   = (parent==_dummy || obj.compareTo( parent.obj ) < 0
			       ?  parent.left  :  parent.right ) ;
	if(node != null && node.obj.compareTo(obj) == 0){
		_curr=node;
	}
	return ( node != null && node.obj.compareTo(obj) == 0  ?  node.obj  :  null ) ;

    }  // contains()



    /**
     * insert object into tree if no other object with same key is present
     *
     * @param object to be inserted
     * @return the object itself if it is the first one with its key,
     *         otherwise the found object with same key
     */
    public Intervall insert ( Intervall obj )
    {
	BinTreeNode parent = _findParentFor( obj ) ;               // "parent" of obj
	BinTreeNode node ;

	if (     parent == _dummy            // REM: compareTo() not applicable
		 ||  obj.compareTo( parent.obj ) < 0 ) {
		if( parent.left != null ) {    // object with same key found in tree  => nothing to do
		return parent.left.obj;
	    }
	    node        = new BinTreeNode( obj ) ;
	    parent.left = node ;
	}
	else {
	    if ( parent.right != null ) {  // object with same key found in tree => nothing to do
		return parent.right.obj;
	    }
	    node         = new BinTreeNode( obj ) ;
	    parent.right = node ;
	}

	node.parent = parent ;
	++_size ;                                        // count nodes in tree

	/***  last things  ***/
	
	_checkTree() ;                     // check tree properties (debugging)
	
	return obj ;
	
    }  // insert()
    

    /**
     * protected: search tree for an object containing t
     *
     * @param obj object whose key is to be searched for
     * @return reference to real or would-be parent node of obj
     */
    protected BinTreeNode _findParentAt (int t )
    {
	BinTreeNode node = _dummy ;
	int         comp = -1 ;                    // root is left son of dummy
	
	while ( true )
	    if ( comp < 0 ) {
		if ( node.left == null )           // only virtual parent found
		    break ;
		comp = t-node.left.obj.getLowBound();
		//comp = obj.compareTo(node.left.obj ) ;
		if ( comp == 0 ||node.left.obj.contains(t))                   // real parent found
		    break ;
		
		node = node.left ;
	    }
	    else if ( comp > 0 ) {
		if ( node.right == null )          // only virtual parent found
		    break ;
		comp = t-node.right.obj.getLowBound();
		//comp = obj.compareTo( node.right.obj ) ;
		if ( comp == 0 ||node.right.obj.contains(t))                   // real parent found
		    break ;

		node = node.right ;
	    }
		
	return node ;                              // return "parent of object"
	
    }  // _findParentFor()
    
    /**
     * @return object in tree with same key as given object, null otherwise
     */
    public BinTreeNode goToNodeAt ( int t )
    {
	if ( isEmpty() )
	    return null ;
	
	BinTreeNode parent = _findParentAt( t ) ;               // "parent" of obj
	//TODO intervall finden
	BinTreeNode node   = (parent==_dummy || (t- parent.obj.getLowBound()) < 0
			       ?  parent.left  :  parent.right ) ;
	if(node !=null && node.obj.contains(t)){
		_curr=node;
	}
	return ( node != null && node.obj.contains(t)  ?  node  :  null ) ;
		//TODO may want to throw Exception
    }  // contains()

    /**
     * @return object in tree with same key as given object, null otherwise
     */
    public Intervall contains ( int t )
    {
	if ( isEmpty() )
	    return null ;
	
	BinTreeNode parent = _findParentAt( t ) ;               // "parent" of obj
	//TODO intervall finden
	BinTreeNode node   = (parent==_dummy || (t- parent.obj.getLowBound()) < 0
			       ?  parent.left  :  parent.right ) ;

	return ( node != null && node.obj.contains(t)  ?  node.obj  :  null ) ;

    }  // contains()


    /**
     * protected: locate tree node to be removed
     *
     * @param obj whose key is searched
     * @return tree node to be removed
     */
    protected BinTreeNode _nodeToDelete ( Intervall obj )
    {
	BinTreeNode parent = _findParentFor( obj ) ;               // "parent" of obj
	BinTreeNode node   = (        parent == _dummy 
						     ||  obj.compareTo( parent.obj ) < 0
			       ?  parent.left  :  parent.right ) ;
	if ( node == null )                    // no object with same key found
	    return null ;                      // in tree => nothing to do

	if (     node.left  == null            // node has at most one child
	     ||  node.right == null )
	    return node ;
	else
	{
	    BinTreeNode delNode = node.right ;
	    while ( delNode.left != null )    // find inorder successor of node
		delNode = delNode.left ;

	    Intervall tempobj = node.obj ;       // interchange objects
	    node.obj       = delNode.obj ;
	    delNode.obj    = tempobj ;

	    return delNode ;
	}

    }  // _nodeToDelete()


    /**
     * protected: remove given node from tree;
     *            it is expected that node has at most one child
     *
     * @param node tree node to be removed
     */
    protected void _remove ( BinTreeNode node )
    {
	BinTreeNode child  = (    node.left != null
			       ?  node.left  :  node.right ) ;
	BinTreeNode parent = node.parent ;

	if ( child != null )
	    child.parent = parent ;
	if ( node.isLeftChild() )
	    parent.left  = child ;
	else
	    parent.right = child ;

	node.left = node.right = node.parent = null ;
	node.obj  = null ;                                      // abandon node
	--_size ;                                        // count nodes in tree
	
    }  // _remove()


    /**
     * remove object with same key from tree if present
     *
     * @param object whose key is searched
     * @return removed object if any was found, null otherwise
     */
    public Intervall remove ( Intervall obj )
    {
	BinTreeNode node = _nodeToDelete( obj ) ;        // locate node to be removed

	if ( node == null )            // no object with same key found in tree => nothing to do
	    return null ;
	
	obj = node.obj;             // save object for return
	_remove( node ) ;           // remove and abandon node
	
    /***  last things  ***/
	
	_checkTree() ;                     // check tree properties (debugging)
	
	return obj ;

    }  // remove()


/***  debugging methods  *****************************************************/

    /**
      * protected: check consistency of search tree property
      *
      * @return are properties consistent in tree?
      */
    protected boolean _checkTree ( )
    {
	boolean retval = _checkLinks() ;          // check links in entire tree

	if (     ! _checkMode                             // no checking wanted
	     ||  isEmpty() )                              // nothing to do
	    return true ;

    /***  check whether tree keys are ascending in inorder sequence  ***/

	BinTreeNode save = _curr ;                      // save _curr for later
	reset() ;
	BinTreeNode prev = _curr ;

	if ( ! isAtEnd() )
	{
	    int size = 1 ;

	    for ( increment() ;  ! isAtEnd() ;  increment() )      // use _curr
	    {
		if ( ! ( prev.obj instanceof Comparable ) )
		    throw new ClassCastException(   "SearchTree._checkTree():"
						  + " cannot cast object to "
						  + "needed Comparable<T>!" ) ;

		if ( prev.obj.compareTo( _curr.obj ) > 0 )
		{
		    System.err.println(   "SearchTree: key order between "
					+ "nodes " + prev + " and " + _curr
					+ " is inconsistent!" ) ;
		    retval = false ;
		}

		++size ;
		prev = _curr ;

	    }  // for ( _curr )

	    if ( size != _size )
	    {
		System.err.println(   "SearchTree: size count of " + size
				    + " is inconsistent with variable "
				    + "_size = " + _size + " !" ) ;
		retval = false ;
	    }
	}  // if ( ! isAtEnd() )

	_curr = save ;                                         // restore _curr

	return retval ;

    }  // _checkTree()

}  // class SearchTree
