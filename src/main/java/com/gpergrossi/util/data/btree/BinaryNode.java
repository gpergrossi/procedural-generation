package com.gpergrossi.util.data.btree;

import java.util.Iterator;

/**
 * An interface that lays out the bare minimum for a binary tree node. Many utility methods are provided 
 * automatically. Their functionality relies only on the implementation of the 6 core methods. 
 * 
 * @param <T> the type of the class that is implementing this interface. Used for type specification on arguments 
 * and return types that deal with nodes. A class implementing this interface should always declare itself in a
 * manner similar to:
 * <pre>
 * public class Example implements IBinaryNode&lt;Example&gt; {
 *    ...
 * }
 * </pre>
 */
public interface BinaryNode<T extends BinaryNode<T>> extends Iterable<T> {
	
	// Class definition for getBreadthAndDepth return type
	
	public static class BreadthAndDepth {
		public final int breadth;
		public final int depth;
		public BreadthAndDepth(int breadth, int depth) {
			this.breadth = breadth;
			this.depth = depth;
		}
	}
	
	
	
	
	
	// Static utility methods
	
	/**
	 * Returns the root of the tree to which the provided node belongs. May be the node itself.
	 * @param node - the node from which the root is to be found
	 * @param <K> a class type that implements the {@code IBinaryTreeNode<K>} 
	 * interface and describes the node argument and return type of this function.
	 * @return the root of the tree
	 */
	public static <K extends BinaryNode<K>> K getRoot(BinaryNode<K> node) {
		@SuppressWarnings("unchecked")
		K result = (K) node;
		while (result.getParent() != null) result = result.getParent();
		return result;
	}
	
	/**
	 * Returns true if the provided node has a non-null parent.
	 * @param node - the node for which the parent should be checked
	 * @return true if the provided node has a parent
	 */
	public static boolean hasParent(BinaryNode<?> node) {
		return (node.getParent() != null);
	}
	
	/**
	 * Returns true if the provided node has a non-null left child, right child, or both.
	 * @param node - the node for which children should be checked
	 * @return true if the provided node has one or more children
	 */
	public static boolean hasChildren(BinaryNode<?> node) {
		return (hasLeftChild(node) || hasRightChild(node));
	}
	
	/**
	 * Returns true if the provided node has a non-null left child.
	 * @param node - the node for which the left child should be checked
	 * @return true if the provided node has a left child
	 */
	public static boolean hasLeftChild(BinaryNode<?> node) {
		return (node.getLeftChild() != null);
	}
	
	/**
	 * Returns true if the provided node has a non-null right child.
	 * @param node - the node for which the right child should be checked
	 * @return true if the provided node has a right child
	 */
	public static boolean hasRightChild(BinaryNode<?> node) {
		return (node.getRightChild() != null);
	}
	
	/**
	 * Returns true if the provided node has a parent and is the left child of that parent.
	 * @param node - the node for which the 'left child' status should be checked
	 * @return true if the provided node is a left child
	 */
	public static boolean isLeftChild(BinaryNode<?> node) {
		return (node.getParent() != null) && (node.getParent().getLeftChild() == node);
	}
	
	/**
	 * Returns true if the provided node has a parent and is the right child of that parent.
	 * @param node - the node for which the 'right child' status should be checked
	 * @return true if the provided node is a right child
	 */
	public static boolean isRightChild(BinaryNode<?> node) {
		return (node.getParent() != null) && (node.getParent().getRightChild() == node);
	}
	
	/**
	 * Returns the sibling of the provided node or null if there isn't one.
	 * @param node - the node for which the sibling should be found
	 * @param <K> a higher level class that implements the {@code IBinaryTreeNode<K>} interface
	 * @return the sibling of the provided node or null
	 */
	public static <K extends BinaryNode<K>> K getSibling(BinaryNode<K> node) {
		if (isLeftChild(node)) return node.getParent().getRightChild();
		if (isRightChild(node)) return node.getParent().getLeftChild();
		if (node.getParent() != null) throw new IllegalStateException("Tree inconsistency detected! Node's parent does not acknowledge node as child.");
		return null;
	}
	
	/**
	 * Returns true if the provided {@code potentialAncestor} is an ancestor of the provided {@code potentialDescendant}.
	 * @param potentialDescendant - a node that may be a descendant of {@code potentialAncestor}
	 * @param potentialAncestor - a node that may be an ancestor of {@code potentialDescendant}
	 * @return true if {@code potentialAncestor} is an ancestor of {@code potentialDescendant}.
	 */
	public static boolean hasAncestor(BinaryNode<?> potentialDescendant, BinaryNode<?> potentialAncestor) {
		BinaryNode<?> node = potentialDescendant;
		while (node != null) {
			if (node == potentialAncestor) return true;
			node = node.getParent();
		}
		return false;
	}
	
	/**
	 * Returns true if the provided {@code node} and {@code potentialRelative} belong to the same tree (have the same root).
	 * @param node - a node that may be a relative of {@code potentialRelative}
	 * @param potentialRelative - a node that may be a relative of {@code node}
	 * @return true if {@code potentialRelative} is an ancestor of {@code node}.
	 */
	public static boolean hasRelative(BinaryNode<?> node, BinaryNode<?> potentialRelative) {
		return (node.getRoot() == potentialRelative.getRoot());
	}
	
	/**
	 * Returns the ancestor of both the provided node and the provided potentialRelative that is farthest
	 * from the root of the tree (earliest common ancestor). May return null if there is no common ancestor.
	 * @param node - a node that may be a relative of {@code potentialRelative}
	 * @param potentialRelative - a node that may be a relative of {@code node}
	 * @param <K> a class type that implements the {@code IBinaryTreeNode<K>} 
	 * interface and describes the node argument and return type of this function.
	 * @return the closest common ancestor of the provided node and the potentialRelative (local root of the smallest subtree containing both nodes)
	 */
	public static <K extends BinaryNode<K>> K getCommonAncestor(BinaryNode<K> node, BinaryNode<K> potentialRelative) {
		@SuppressWarnings("unchecked")
		K kNode = (K) node;
		while (kNode != null) {
			if (potentialRelative.hasAncestor(kNode)) return kNode;
			kNode = kNode.getParent();
		}
		return null;
	}
	
	/**
	 * Returns the in-order previous node or null if none exists within the tree.
	 * This method assumes the binary tree is ordered with "lower" nodes placed
	 * in the left, and "higher" nodes in the right child of any particular parent.
	 * @param node - the node for which the predecessor should be returned
	 * @param <K> a higher level class that implements the {@code IBinaryTreeNode<K>} interface
	 * @return the predecessor of the provided node or null
	 */
	public static <K extends BinaryNode<K>> K getPredecessor(BinaryNode<K> node) {
		if (node.getLeftChild() != null) {
			return getLastDescendant(node.getLeftChild());
		} else {
			while (node != null && !isRightChild(node)) node = node.getParent();
			if (node != null) return node.getParent();
		}
		return null;
	}
	
	/**
	 * Returns the in-order next node or null if none exists within the tree. 
	 * This method assumes the binary tree is ordered with "lower" nodes placed
	 * in the left, and "higher" nodes in the right child of any particular parent.
	 * @param node - the node for which the successor should be returned
	 * @param <K> a class type that implements the {@code IBinaryTreeNode<K>} 
	 * interface and describes the node argument and return type of this function.
	 * @return the successor of the provided node or null
	 */
	public static <K extends BinaryNode<K>> K getSuccessor(BinaryNode<K> node) {
		if (node.getRightChild() != null) {
			return getFirstDescendant(node.getRightChild());
		} else {
			while (node != null && !isLeftChild(node)) node = node.getParent();
			if (node != null) return node.getParent();
		}
		return null;
	}
	
	/**
	 * Returns the first in order descendant of this node. If a node
	 * has no left children then the first descendant is the node itself.
	 * This method assumes the binary tree is ordered with "lower" nodes placed
	 * in the left, and "higher" nodes in the right child of any particular parent.
	 * @param node - the node from which the first descendant should be located
	 * @param <K> a class type that implements the {@code IBinaryTreeNode<K>} 
	 * interface and describes the node argument and return type of this function.
	 * @return first in order descendant of the provided node or the node itself
	 */
	public static <K extends BinaryNode<K>> K getFirstDescendant(BinaryNode<K> node) {
		while (node.getLeftChild() != null) node = node.getLeftChild();
		@SuppressWarnings("unchecked")
		K output = (K) node;
		return output;
	}
	
	/**
	 * Returns the last in order descendant of this node. If a node
	 * has no left children then the first descendant is the node itself.
	 * This method assumes the binary tree is ordered with "lower" nodes placed
	 * in the left, and "higher" nodes in the right child of any particular parent.
	 * @param node - the node from which the last descendant should be located
	 * @param <K> a class type that implements the {@code IBinaryTreeNode<K>} 
	 * interface and describes the node argument and return type of this function.
	 * @return last in order descendant of the provided node or the node itself
	 */
	public static <K extends BinaryNode<K>> K getLastDescendant(BinaryNode<K> node) {
		@SuppressWarnings("unchecked")
		K kNode = (K) node;
		while (kNode.getRightChild() != null) {
			kNode = kNode.getRightChild();
		}
		return kNode;
	}
	
	/**
	 * Returns the breadth and depth of the tree for which this node is the root.
	 * Breadth will be equal to the total number of nodes in the tree. Depth will
	 * be the length of the longest path from this root node to any descendant.
	 * @param node - the node to be treated as root
	 * @param <K> a higher level class that implements the {@code IBinaryTreeNode<K>} interface
	 * @return a BreadthAndDepth object describing the breath and depth.
	 */
	public static <K extends BinaryNode<K>> BreadthAndDepth getTreeBreadthAndDepth(BinaryNode<K> node) {
		BreadthAndDepth left;
		if (node.getLeftChild() != null) {
			left = getTreeBreadthAndDepth(node.getLeftChild());
		} else {
			left = new BreadthAndDepth(0, 0);
		}
		
		BreadthAndDepth right;
		if (node.getRightChild() != null) {
			right = getTreeBreadthAndDepth(node.getRightChild());
		} else {
			right = new BreadthAndDepth(0, 0);
		}
		
		final int breadth = left.breadth + right.breadth + 1;
		final int depth = Math.max(left.depth, right.depth)+1;
		
		return new BreadthAndDepth(breadth, depth);
	}
	
	/**
	 * <p>Rotates the subtree of <code>node</code> and its children to the right and returns the new local root.
	 * <pre>
	 *     4              2     
	 *    / \            / \    
	 *   2   5   --->   1   4   
	 *  / \                / \  
	 * 1   3              3   5 </pre>
	 * rotateRight() on node 4 results in the new tree on the right and returns 2.</p>
	 * 
	 * <p>In the above example, if node 4 had a parent, then node 2 will be correctly attached to node 4's
	 * old parent using the {@code replaceWith} method. If node 4 did not have a parent, the behavior will
	 * depend on the implementation details of the {@code replaceWith} method.</p>
	 * 
	 * <p>In any case, the new local root of the tree (in this example, node 4) will be returned.</p>
	 * 
	 * @param node - a node with a left child representing a subtree to be rotated to the right
	 * @param <K> a class type that implements the {@code IBinaryTreeNode<K>} interface and describes the 
	 * node argument and return type of this function.
	 * @return the new local root (the node that took this node's place after rotation)
	 * @throws IllegalStatException if the provided node does not have a left child.
	 */
	public static <K extends BinaryNode<K>> K rotateRight(BinaryNode<K> node) {
		@SuppressWarnings("unchecked")
		K self = (K) node;
		
		K newTop = self.getLeftChild();
		if (newTop == null) throw new IllegalStateException("Cannot rotate node because there is no appropriate child!");
		
		newTop.removeFromParent();
		self.replaceWith(newTop);
		
		K oldRight = newTop.getRightChild();
		newTop.setRightChild((K) self);
		self.setLeftChild(oldRight);
		
		return newTop;
	}

	/**
	 * <p>Rotates the subtree of <code>node</code> and its children to the left and returns the new local root.
	 * <pre>
	 *     2              4    
	 *    / \            / \   
	 *   1   4   --->   2   5  
	 *      / \        / \     
	 *     3   5      1   3    </pre>
	 * rotateLeft() on node 2 results in the new tree on the right and returns 4.</p>
	 * 
	 * <p>In the above example, if node 2 had a parent, then node 4 will be correctly attached to node 2's
	 * old parent using the {@code replaceWith} method. If node 2 did not have a parent, the behavior will
	 * depend on the implementation details of the {@code replaceWith} method.</p>
	 * 
	 * <p>In any case, the new local root of the tree (in this example, node 2) will be returned.</p>
	 * 
	 * @param node - a node with a right child representing a subtree to be rotated to the left
	 * @param <K> a class type that implements the {@code IBinaryTreeNode<K>} interface and describes the 
	 * node argument and return type of this function.
	 * @return the new local root (the node that took this node's place after rotation)
	 * @throws IllegalStatException if the provided node does not have a right child.
	 */
	public static <K extends BinaryNode<K>> K rotateLeft(BinaryNode<K> node) {
		@SuppressWarnings("unchecked")
		K self = (K) node;
		
		K newTop = self.getRightChild();
		if (newTop == null) throw new IllegalStateException("Cannot rotate node because there is no appropriate child!");
		
		newTop.removeFromParent();
		self.replaceWith(newTop);
		
		K oldLeft = newTop.getLeftChild();
		newTop.setLeftChild(self);
		self.setRightChild(oldLeft);
		
		return newTop;
	}
	
	/**
	 * <p>Replaces {@code oldNode} with {@code newNode} using {@code setLeftChild} or {@code setRightChild}
	 * as appropriate. This method is used to attach new subtrees or nodes in the place of nodes already
	 * existing in the tree.</p>
	 * 
	 * <p>If {@code oldNode} has no parent, no action is taken because there is no action to be performed.
	 * It may be necessary to override this behavior to keep track of your tree's root reference.
	 * You should extend the "super" method's behavior by calling this static method.<p>
	 * 
	 * <p>If {@code newNode} is non-null, then the following conditions apply:<ol>
	 * <li>{@code newNode} must not have a parent, and</li>
	 * <li>{@code newNode} must not be an ancestor of {@code oldNode} (because that would cause a cycle)</li></ol></p>
	 * 
	 * @param oldNode - a node to be replaced
	 * @param newNode - a parent-less node to become the left child or right child of the oldNode's parent.
	 * @param <K> a class type that implements the {@code IBinaryTreeNode<K>} interface and describes the 
	 * node arguments of this function. 
	 * @throws IllegalStateException when the newNode argument is non-null and one of the following is true:<ol>
	 * <li>{@code newNode} is has a parent node, or</li>
	 * <li>{@code newNode} is an ancestor of {@code oldNode} (causing a cycle).</li><ol>
	 * @throws IllegalStateException if the {@code oldNode}'s parent does not acknowledge {@code oldNode} as a child.
	 */
	public static <K extends BinaryNode<K>> void replaceWith(BinaryNode<K> oldNode, BinaryNode<K> newNode) {
		@SuppressWarnings("unchecked")
		K kNewNode = (K) newNode;
		
		if (!oldNode.hasParent()) return;
		
		if (kNewNode != null) {
			if (kNewNode.hasParent()) throw new IllegalStateException("Argument node -- to become a child -- must not already have a parent!");
			if (oldNode.hasAncestor(kNewNode)) throw new IllegalStateException("Argument node -- to become a child -- must not be an ancestor of the node to which it will be attached. This would cause a cycle!");
		}
		
		if (oldNode.isLeftChild()) oldNode.getParent().setLeftChild(kNewNode);
		else if (oldNode.isRightChild()) oldNode.getParent().setRightChild(kNewNode);
		else throw new IllegalStateException("Parent node does not reference this node as a child");
	}
	
	/**
	 * Creates an iterator that traverses the nodes in this node's subtree.
	 * The iterator does not support or detect concurrent modifications!
	 * @param node
	 * @return
	 */
	public static <K extends BinaryNode<K>> Iterator<K> iterator(BinaryNode<K> node) {
		@SuppressWarnings("unchecked")
		K self = (K) node;
		return new TreeNodeIterator<K>(self);
	}
	
	/**
	 * <p>In order to avoid cycles in the tree, the following conditions must be met by any non-null child
	 * being attached to a node:<ol>
	 * <li>{@code child} must not have a parent, and</li>
	 * <li>{@code child} must not be an ancestor of this {code node} (because that would cause a cycle)</li></ol></p>
	 * @param child - the child to be attached to this node
	 * @param <K> a class type that implements the {@code IBinaryTreeNode<K>} interface and describes the 
	 * node arguments of this function.
	 * @throws IllegalStateException when the {@code child} argument is non-null and one of the following 
	 * is true:<ol>
	 * <li>{@code child} has a parent node, or</li>
	 * <li>{@code child} is an ancestor of {@code node} (causing a cycle).</li><ol>
	 */
	public static <K extends BinaryNode<K>> void assertChildNode(BinaryNode<K> node, BinaryNode<K> child) throws IllegalStateException {
		@SuppressWarnings("unchecked")
		K kChild = (K) child;
		
		if (kChild.hasParent()) throw new IllegalStateException("Argument node -- to become a child -- must not already have a parent!");
		if (node.hasAncestor(kChild)) throw new IllegalStateException("Argument node -- to become a child -- must not be an ancestor of the node to which it will be attached. This would cause a cycle!");
	}
	
	/**
	 * <p>Node A and Node B will trade places within the same tree.</p>
	 * <p>Will fail if:<ul>
	 * <li>The tree has cycles (A is an ancestor of B and B is also an ancestor of A)</li>
	 * <li>The nodes are not from the same tree</li>
	 * </ul></p>
	 * <p>All other cases are handled, including when:<ul>
	 * <li>A and B are the same node (no swap is done)</li>
	 * <li>A is B's child</li>
	 * <li>B is A's child</li>
	 * <li>A is the root of the tree</li>
	 * <li>B is the root of the tree</li> 
	 * </ul></p>
	 * <p>This method swaps ONLY the two nodes. Not their children. This is useful primarily when you
	 * are about to delete a parent node and wish to keep your tree in order. You may swap a predecessor
	 * or successor into the place of the parent node, then remove the parent node.
	 * 
	 * TODO: I'd like to be able to swap any two nodes, even between different trees. Hence the concept
	 * of the "Tree" reference that I'm currently working out the details for.
	 * 
	 * @param a - any node
	 * @param b - another node from the same tree as <code>a</code>
	 * @param <T> the class type of nodes a and b
	 */
	public static <K extends BinaryNode<K>> void swapNodes(BinaryNode<K> a, BinaryNode<K> b) {
		// Convert a and b into K types
		@SuppressWarnings("unchecked")
		K ka = (K) a;
		@SuppressWarnings("unchecked")
		K kb = (K) b;
		
		// Special case: nodes are the same, do nothing 
		if (ka == kb) {
			return;
		}
		
		// Special case: nodes belong to the same tree
		if (hasAncestor(ka, kb)) {
			if (hasAncestor(kb, ka)) {
				// Error condition: tree has a cycle
				throw new IllegalStateException("Tree has a cycle. A and B are ancestors of each other."); 
			} else {
				// Make sure node a is closer to root than node b, makes another special case easier to deal with
				swapNodes(kb, ka);
				return;
			}
		}
		if (a.getRoot() != b.getRoot()) throw new IllegalArgumentException("Nodes must belong to the same tree!");
		
		// Remember all connections
		K aLeft = ka.getLeftChild();
		K aRight = ka.getRightChild();
		
		K bLeft = kb.getLeftChild();
		K bRight = kb.getRightChild();
		K bParent = kb.getParent();
		boolean bIsRightChild = kb.isRightChild();
		
		// Remove children from both nodes
		if (bLeft != null) bLeft.removeFromParent();
		if (bRight != null) bRight.removeFromParent();
		if (aLeft != null) aLeft.removeFromParent();
		if (aRight != null) aRight.removeFromParent();
		
		// Remove b from its parent, replace a with b
		kb.removeFromParent();
		ka.replaceWith((K) kb); // replaceWith should be overridden to handle replacing of the tree's root node
		
		// Reconnect b's old children to a
		ka.setLeftChild(bLeft);
		ka.setRightChild(bRight);
		
		// Reconnect a's old children to b
		// Special cases for when b is child of a
		if (aLeft != kb) kb.setLeftChild(aLeft);
		if (aRight != kb) kb.setRightChild(aRight);
		
		// Reconnect a to parent
		// Special case for when b is child of a
		if (bParent == ka) bParent = kb; 
		if (bIsRightChild) {
			bParent.setRightChild(ka);
		} else {
			bParent.setLeftChild(ka);
		}
	}
	
	
	
	// Core interface methods
	
	/**
	 * Gets the tree to which this binary node belongs
	 * @return a binary tree object, or null if this node does not belong to a tree
	 */
	public BinaryTree<T> getTree();
	
	/**
	 * Get the parent of this node. May be null.
	 * @return parent node or null
	 */
	public T getParent();
	
	/**
	 * Disconnects this node from its parent. If this node has no parent, no changes are made.
	 * If this node is a root node and this node has a Tree object, 
	 */
	public void removeFromParent();
	
	/**
	 * Get the left child of this node. May be null.
	 * @return left child or null
	 */
	public T getLeftChild();
	
	/**
	 * <p>Set the left child of this node, removing the old left child if there was one.</p>
	 * 
	 * <p>If the child argument is non-null, then the following conditions apply:<ol>
	 * <li>The child node must not have a parent, and</li>
	 * <li>The child node must not be an ancestor of this node (because that would cause a cycle)</li></ol></p>
	 * 
	 * <p>The child argument is allowed to have children in which case an entire subtree will be
	 * assigned as the left child of this node.</p>
	 * 
	 * <p>The newly assigned child should be updated to have this node as its parent.</p>
	 * 
	 * <p>The newly-orphaned child (if any) should be updated to have no parent.</p>
	 * 
	 * @param child - a parent-less node to become the left child, or null. 
	 * @throws IllegalStateException when the child argument is non-null and one of the following is true:<ol>
	 * <li>The child argument has a parent node, or</li>
	 * <li>The child argument is an ancestor of this node (causing a cycle)</li></ul>
	 */
	public void setLeftChild(T child) throws IllegalStateException;
	
	/**
	 * Get the right child of this node. May be null.
	 * @return right child or null
	 */
	public T getRightChild();
	
	/**
	 * <p>Set the right child of this node, removing the old right child if there was one.</p>
	 * 
	 * <p>If the child argument is non-null, then the following conditions apply:<ol>
	 * <li>The child node must not have a parent, and</li>
	 * <li>The child node must not be an ancestor of this node (because that would cause a cycle)</li></ol></p>
	 * 
	 * <p>The child argument is allowed to have children in which case an entire subtree will be
	 * assigned as the right child of this node.</p>
	 * 
	 * <p>The newly assigned child should be updated to have this node as its parent.</p>
	 * 
	 * <p>The newly-orphaned child (if any) should be updated to have no parent.</p>
	 * 
	 * @param child - a parent-less node to become the right child, or null. 
	 * @throws IllegalStateException when the child argument is non-null and one of the following is true:<ol>
	 * <li>The child argument has a parent node, or</li>
	 * <li>The child argument is an ancestor of this node (causing a cycle)</li></ul>
	 */
	public void setRightChild(T child) throws IllegalStateException;
	
	
	

	
	// Default utility method implementations
	
	/**
	 * Get the root of the tree to which this node belongs.
	 * @return root node (may be self)
	 */
	public default T getRoot() {
		return BinaryNode.getRoot(this);
	}
	
	/**
	 * Returns true if the provided node has a non-null parent.
	 * @return true if the provided node has a parent
	 */
	public default boolean hasParent() {
		return BinaryNode.hasParent(this);
	}
	
	/**
	 * <p>Replaces {@code this} node with {@code node} using {@code setLeftChild} or {@code setRightChild}
	 * as appropriate. This method is used to attach new subtrees or nodes in the place of nodes already
	 * existing in the tree.</p>
	 * 
	 * <p>If {@code this} node has no parent, no action is taken because there is no action to be performed.
	 * It may be necessary to override this behavior to keep track of your tree's root reference.
	 * You should extend the "super" method's behavior by calling the static {@code replaceWith} method.<p>
	 * 
	 * <p>If {@code node} is non-null, then the following conditions apply:<ol>
	 * <li>{@code node} must not have a parent, and</li>
	 * <li>{@code node} must not be an ancestor of this node (because that would cause a cycle)</li></ol></p>
	 * 
	 * @param node - a parent-less node to become the left child or right child of the oldNode's parent. 
	 * @throws IllegalStateException when the newNode argument is non-null and one of the following is true:<ol>
	 * <li>{@code newNode} is has a parent node, or</li>
	 * <li>{@code newNode} is an ancestor of {@code oldNode} (causing a cycle).</li><ol>
	 * @throws IllegalStateException if the {@code oldNode}'s parent does not acknowledge {@code oldNode} as a child.
	 */
	public default void replaceWith(T node) {
		BinaryNode.replaceWith(this, node);
	}
	
	/**
	 * Returns true if the provided node has a non-null left child, right child, or both.
	 * @return true if the provided node has one or more children
	 */
	public default boolean hasChildren() {
		return BinaryNode.hasChildren(this);
	}
	
	/**
	 * Returns true if the provided node has a non-null left child.
	 * @param node - the node for which the left child should be checked
	 * @return true if the provided node has a left child
	 */
	public default boolean hasLeftChild() {
		return BinaryNode.hasLeftChild(this);
	}
	
	/**
	 * Returns true if the provided node has a non-null right child.
	 * @param node - the node for which the right child should be checked
	 * @return true if the provided node has a right child
	 */
	public default boolean hasRightChild() {
		return BinaryNode.hasRightChild(this);
	}
	
	/**
	 * Returns true if the provided node has a parent and is the left child of that parent.
	 * @param node - the node for which the 'left child' status should be checked
	 * @return true if the provided node is a left child
	 */
	public default boolean isLeftChild() {
		return BinaryNode.isLeftChild(this);
	}
	
	/**
	 * Returns true if the provided node has a parent and is the right child of that parent.
	 * @param node - the node for which the 'right child' status should be checked
	 * @return true if the provided node is a right child
	 */
	public default boolean isRightChild() {
		return BinaryNode.isRightChild(this);
	}
	
	/**
	 * Returns the sibling of the provided node or null if there isn't one.
	 * @param node - the node for which the sibling should be found
	 * @param <K> a higher level class that implements the {@code IBinaryTreeNode<K>} interface
	 * @return the sibling of the provided node or null
	 */
	public default T getSibling() {
		return BinaryNode.getSibling(this);
	}
	
	/**
	 * Return true if the provided potentialAncestor is an ancestor of this node.
	 * @param potentialAncestor - the node that may potentially be an ancestor of this node
	 * @return true if the potentialAncestor is an ancestor of this node.
	 */
	public default boolean hasAncestor(T potentialAncestor) {
		return BinaryNode.hasAncestor(this, potentialAncestor);
	}
	
	/**
	 * Return true if the provided potentialRelative belongs to the same tree as this node (same root).
	 * @param potentialRelative - the node that may potentially be an ancestor of this node
	 * @return true if the potentialAncestor is an ancestor of this node.
	 */
	public default boolean hasRelative(T potentialRelative) {
		return BinaryNode.hasAncestor(this, potentialRelative);
	}
	
	/**
	 * Return the ancestor of both this node and the provided potentialRelative that is farthest from
	 * the root (earliest common ancestor). May return null if there is no common ancestor.
	 * @param potentialRelative - a node that may be a relative of this node
	 * @return the closest common ancestor of this node and the potentialRelative (smallest subtree containing both)
	 */
	public default T getCommonAncestor(T potentialRelative) {
		return BinaryNode.getCommonAncestor(this, potentialRelative);
	}
	
	/**
	 * Return the in-order previous node or null if none exists within the tree.
	 * 
	 * <p><b>Ordered Tree</b><br/>
	 * This method assumes the binary tree is ordered with "lower" nodes found in the
	 * left child, and "higher" nodes in the right child.</p>
	 * 
	 * @return the predecessor of this node or null
	 */
	public default T getPredecessor() {
		return BinaryNode.getPredecessor(this);
	}
	
	/**
	 * Return the in-order next node or null if none exists within the tree.
	 * 
	 * <p><b>Ordered Tree</b><br/>
	 * This method assumes the binary tree is ordered with "lower" nodes found in the
	 * left child, and "higher" nodes in the right child.</p>
	 * 
	 * @return the successor of this node or null
	 */
	public default T getSuccessor() {
		return BinaryNode.getSuccessor(this);
	}
	
	/**
	 * Return the first in order descendant of this node. If a node
	 * has no left children then the first descendant is the node itself.
	 * 
	 * <p><b>Ordered Tree</b><br/>
	 * This method assumes the binary tree is ordered with "lower" nodes found in the
	 * left child, and "higher" nodes in the right child.</p>
	 * 
	 * @return first in order descendant or self
	 */
	public default T getFirstDescendant() {
		return BinaryNode.getFirstDescendant(this);
	}
	
	/**
	 * Return the last in order descendant of this node. If a node
	 * has no left children then the first descendant is the node itself.
	 * 
	 * <p><b>Ordered Tree</b><br/>
	 * This method assumes the binary tree is ordered with "lower" nodes found in the
	 * left child, and "higher" nodes in the right child.</p>
	 * 
	 * @return last in order descendant or self
	 */
	public default T getLastDescendant() {
		return BinaryNode.getLastDescendant(this);
	}
	
	/**
	 * Returns the breadth and depth of the tree for which this node is the root.
	 * Breadth will be equal to the total number of nodes in the tree. Depth will
	 * be the length of the longest path from this root node to any descendant.
	 * @return a BreadthAndDepth object describing the breath and depth.
	 */
	public default BreadthAndDepth getTreeBreadthAndDepth() {
		return BinaryNode.getTreeBreadthAndDepth(this);
	}
	
	/**
	 * Creates an iterator that traverses the nodes in this node's subtree.
	 * The iterator does not support or detect concurrent modifications!
	 */
	@Override
	public default Iterator<T> iterator() {
		return BinaryNode.iterator(this);
	}
	
}
