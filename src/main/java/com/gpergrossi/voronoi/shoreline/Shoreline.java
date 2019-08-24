package com.gpergrossi.voronoi.shoreline;

import java.util.Optional;

import com.gpergrossi.util.data.btree.AbstractBinaryNode;
import com.gpergrossi.util.data.btree.BinaryNode;
import com.gpergrossi.util.data.btree.BinaryTree;
import com.gpergrossi.util.geom.vectors.Double2D;
import com.gpergrossi.voronoi.event.CircleEvent;
import com.gpergrossi.voronoi.graph.Site;

public class Shoreline {
	
	public class Entry extends AbstractBinaryNode<Shoreline.Entry> {
		public final boolean isBreakpoint;
		public final Arc arc;
		public final Breakpoint breakpoint;
		
		private Entry(Arc arc) {
			this.isBreakpoint = false;
			this.arc = arc;
			this.breakpoint = null;
		}
		
		private Entry(Breakpoint breakpoint) {
			this.isBreakpoint = true;
			this.arc = null;
			this.breakpoint = breakpoint;
		}
		
		@Override
		public void replaceWith(Entry node) {
			super.replaceWith(node);
			if (this == root) {
				root = node;
			}
		}

		@Override
		public BinaryTree<Entry> getTree() {
			// TODO
			return null;
		}
		
		@Override
		public String toString() {
			if (this.isBreakpoint) {
				return ""+breakpoint;
			} else {
				return ""+arc;
			}
		}
	}
	
	protected Entry root;
	
	public Shoreline() {
		root = null;
	}
	
	protected Optional<Arc> getArcBelowSite(Site site, Sweepline sweepline) {
		if (root == null) return Optional.empty();
		Entry scout = root;
		//System.out.println("getArcBelowSite: Starting with " + scout.toString());
		while (scout.isBreakpoint) {
			final Entry left = scout.getLeftChild();
			final Entry right = scout.getRightChild();
			final Double2D breakpointLocation = scout.breakpoint.computeLocation(sweepline);
			final Double2D siteLocation = site.getPoint();
			
			//System.out.print("getArcBelowSite: Query Site["+site.getID()+"] at position ("+siteLocation.x()+","+siteLocation.y()+"), Breakpoint is at (" + breakpointLocation.x() + "," + breakpointLocation.y() + ")");
			if (siteLocation.x() <= breakpointLocation.x()) {
				scout = left;
				System.out.println(" going LEFT");
			} else {
				scout = right;
				System.out.println(" going RIGHT");
			}
			//System.out.println("getArcBelowSite: Now looking at " + scout.toString());
		}
		return Optional.of(scout.arc);
	}

	public InsertResult insertArc(Site newSite, Sweepline sweepline) {
		Optional<Arc> below = getArcBelowSite(newSite, sweepline);
		
		if (!below.isPresent()) {
			if (root != null) return InsertResult.createErrorResult("Found no arc below site, but there should have been one!");
			Arc newArc = new Arc(newSite);
			root = new Entry(newArc);
			newArc.shorelineEntry = root;
			return InsertResult.createFirstArcResult(newArc);			
		} else {
			final Arc oldArc = below.get();
			Optional<Breakpoint> farLeftBreakpoint = oldArc.getLeftBreakpoint();
			Optional<Breakpoint> farRightBreakpoint = oldArc.getRightBreakpoint(); 
			
			// Create a new arc inside the old arc
			Arc leftArc = new Arc(oldArc.getSite());
			Arc newArc = new Arc(newSite);
			Arc rightArc = new Arc(oldArc.getSite());
			
			Breakpoint leftBreakpoint = new Breakpoint(leftArc, newArc);
			Breakpoint rightBreakpoint = new Breakpoint(newArc, rightArc);
			
			farLeftBreakpoint.ifPresent(bp -> bp.setRightArc(leftArc));
			
			leftArc.setLeftBreakpoint(farLeftBreakpoint);
			leftArc.setRightBreakpoint(Optional.of(leftBreakpoint));
			
			newArc.setLeftBreakpoint(Optional.of(leftBreakpoint));
			newArc.setRightBreakpoint(Optional.of(rightBreakpoint));
			
		    rightArc.setLeftBreakpoint(Optional.of(rightBreakpoint));
			rightArc.setRightBreakpoint(farRightBreakpoint);
			
			farRightBreakpoint.ifPresent(bp -> bp.setLeftArc(rightArc));
			
			// Update the tree entries
			Entry leftArcEntry = new Entry(leftArc);
			Entry leftBreakpointEntry = new Entry(leftBreakpoint);
			Entry newArcEntry = new Entry(newArc);
			Entry rightBreakpointEntry = new Entry(rightBreakpoint);
			Entry rightArcEntry = new Entry(rightArc);
			
			leftBreakpointEntry.setLeftChild(leftArcEntry);
			leftBreakpointEntry.setRightChild(rightBreakpointEntry);
			
			rightBreakpointEntry.setLeftChild(newArcEntry);
			rightBreakpointEntry.setRightChild(rightArcEntry);
			
			oldArc.shorelineEntry.replaceWith(leftBreakpointEntry);
			
			// Link arcs/breakpoints to entries
			leftArc.shorelineEntry = leftArcEntry;
			leftBreakpoint.shorelineEntry = leftBreakpointEntry;
			newArc.shorelineEntry = newArcEntry;
			rightBreakpoint.shorelineEntry = rightBreakpointEntry;
			rightArc.shorelineEntry = rightArcEntry;
			
			return InsertResult.createNormalResult(oldArc, leftArc, leftBreakpoint, newArc, rightBreakpoint, rightArc);
		}
	}
	
	public RemoveResult removeArc(Arc arc) {
		Optional<CircleEvent> optCircleEvent = arc.getCircleEvent();
		if (!optCircleEvent.isPresent()) {
			throw new IllegalStateException("Arc does not have a circle event!");
		}
		CircleEvent circleEvent = optCircleEvent.get();
		if (!circleEvent.isValid()) {
			throw new IllegalStateException("Circle event is invalid!");
		}
		
		Optional<Breakpoint> optLeftBreakpoint = arc.getLeftBreakpoint();
		Optional<Breakpoint> optRightBreakpoint = arc.getRightBreakpoint();
		if (!optLeftBreakpoint.isPresent() || !optRightBreakpoint.isPresent()) {
			throw new IllegalStateException("Removal of Arc without both breakpoints is impossible!");
		}
		Breakpoint leftBreakpoint = optLeftBreakpoint.get();
		Breakpoint rightBreakpoint = optRightBreakpoint.get();
		
		// Easy part: update the shoreline objects 
		
		// Make a new breakpoint
		Arc leftArc = leftBreakpoint.getLeftArc();
		Arc rightArc = rightBreakpoint.getRightArc();
		Breakpoint newBreakpoint = new Breakpoint(leftArc, rightArc);
		
		// Update surrounding arcs
		leftArc.setRightBreakpoint(Optional.of(newBreakpoint));
		rightArc.setLeftBreakpoint(Optional.of(newBreakpoint));
		
		// Hard part: fix the shore tree
		
		// The removed arc will always have its parent as one of its breakpoints
		Entry parent = arc.shorelineEntry.getParent();
		Entry sibling = arc.shorelineEntry.getSibling();
		
		// The other breakpoint will be an ancestor (predecessor or successor, depending on relationship to parent)
		Entry ancestor;
		if (arc.shorelineEntry.isLeftChild()) {
			ancestor = arc.shorelineEntry.getPredecessor();
		} else {
			ancestor = arc.shorelineEntry.getSuccessor();
		}
		
		// The ancestor will be replaced with a new entry for the new breakpoint
		Entry newBreakpointEntry = new Entry(newBreakpoint);
		arc.shorelineEntry.replaceWith(newBreakpointEntry); // TODO: this is a hack because swapNodes only works on nodes from the same tree. The reasoning behind this limitation was I didn't want to deal with root nodes for trees.
		BinaryNode.swapNodes(newBreakpointEntry, ancestor); // ReplaceWith handles roots correctly but can't be used twice because it will remove both nodes from their parents and lose that reference. This should be fixed
		
		// Removing the parent is straight-forward; replace the parent with the sibling
		sibling.removeFromParent();
		parent.replaceWith(sibling);
		
		return new RemoveResult(arc, leftBreakpoint, rightBreakpoint, newBreakpoint);
	}
	
	@Override
	public String toString() {
		if (root == null) return "null";
		return "\n"+root.treeString();
	}
	
}