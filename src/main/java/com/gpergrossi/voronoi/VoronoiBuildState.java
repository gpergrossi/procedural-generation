package com.gpergrossi.voronoi;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.function.LongSupplier;

import com.gpergrossi.util.data.queue.PriorityMultiQueue;
import com.gpergrossi.util.geom.ranges.Double2DRange;
import com.gpergrossi.util.geom.shapes.Circle;
import com.gpergrossi.util.geom.vectors.Double2D;
import com.gpergrossi.util.task.IncrementalTaskTimedExecutor;
import com.gpergrossi.util.task.TimedIncrementalTask;
import com.gpergrossi.voronoi.event.CircleEvent;
import com.gpergrossi.voronoi.event.SiteEvent;
import com.gpergrossi.voronoi.event.VoronoiEvent;
import com.gpergrossi.voronoi.event.VoronoiEventType;
import com.gpergrossi.voronoi.graph.Site;
import com.gpergrossi.voronoi.shoreline.Arc;
import com.gpergrossi.voronoi.shoreline.InsertResult;
import com.gpergrossi.voronoi.shoreline.PartialEdge;
import com.gpergrossi.voronoi.shoreline.ProxySite;
import com.gpergrossi.voronoi.shoreline.RemoveResult;
import com.gpergrossi.voronoi.shoreline.Shoreline;
import com.gpergrossi.voronoi.shoreline.Sweepline;

public class VoronoiBuildState implements TimedIncrementalTask {

	private static enum BuildProgressStep {
		INITIALIZATION, PROCESSING_EVENTS, FINISHING, FINISHED;
	}
	
	BuildProgressStep progressStep;
	TimedIncrementalTask timedTask;
	
	Site[] sites;
	Double2DRange bounds;
	Sweepline sweepline;
	Shoreline shoreline;
	Queue<SiteEvent> siteEventsQueue;
	Queue<CircleEvent> circleEventsQueue;
	PriorityMultiQueue<VoronoiEvent> multiQueue;
	List<VoronoiEvent> completedEvents;
	List<PartialEdge> partialEdges;
	
	public VoronoiBuildState(Sweepline sweepline, List<Double2D> sitePoints) {
		this.sweepline = sweepline;
		this.sites = new Site[sitePoints.size()+1];
		
		int siteID = 0;
		this.bounds = null;
		for (Double2D point : sitePoints) {
			sites[siteID] = new Site(siteID, point);
			siteID++;
			
			if (bounds == null) {
				bounds = new Double2DRange(point, point);
			} else {
				bounds = bounds.union(new Double2DRange(point, point));
			}
		}
		if (bounds == null) {
			bounds = new Double2DRange(0,0,0,0);
		}
		bounds = bounds.grow(10);
		
		// Insert a proxy site to avoid edge cases with the first sites being parallel
		double proxyY = bounds.minY - bounds.height * 2.0;
		double proxyX = (bounds.minX + bounds.maxX) / 2.0;
		this.sites[sites.length-1] = new ProxySite(new Double2D(proxyX, proxyY));
		
		this.progressStep = BuildProgressStep.INITIALIZATION;
		this.timedTask = new IncrementalTaskTimedExecutor(this);
	}

	private void initialize() {
		// Initialize state
		this.shoreline = new Shoreline();
		this.progressStep = BuildProgressStep.PROCESSING_EVENTS;
		this.completedEvents = new ArrayList<>();
		
		// Build site event queue in pre-sorted order
		Arrays.sort(sites, Comparator.comparing(Site::getPoint, sweepline));
		this.siteEventsQueue = new LinkedList<>();
		for (Site site : sites) {
			SiteEvent event = new SiteEvent(site);
			siteEventsQueue.offer(event);
		}

		// Prepare a priority queue for circle events
		this.circleEventsQueue = new PriorityQueue<>(Comparator.comparing(VoronoiEvent::getEventPoint, sweepline));
		
		// Combine queues into a multi-queue that selects the best element off of either queue
		this.multiQueue = new PriorityMultiQueue<>(Comparator.comparing(VoronoiEvent::getEventPoint, sweepline));
		this.multiQueue.addQueue(this.siteEventsQueue);
		this.multiQueue.addQueue(this.circleEventsQueue);
		
		// Prepare other data structures
		this.partialEdges = new ArrayList<>();
	}

	private void processEvent() {
		if (!this.multiQueue.isEmpty()) {
			VoronoiEvent nextEvent = this.multiQueue.poll();
			
			System.out.println("processEvent: Processing event " + nextEvent + "...");

			sweepline.advance(nextEvent.getEventPoint());
			
			if (nextEvent.getType() == VoronoiEventType.CIRCLE) {
				CircleEvent circleEvent = (CircleEvent) nextEvent;
				if (circleEvent.isValid()) processCircleEvent(circleEvent);
			} else if (nextEvent.getType() == VoronoiEventType.SITE) {
				processSiteEvent((SiteEvent) nextEvent);
			}
			
			completedEvents.add(nextEvent);
						
			//System.out.println("processEvent: Event complete! Shoreline looks like this:" + shoreline.toString());
			//System.out.println("\n\n");
		} else {
			this.progressStep = BuildProgressStep.FINISHING;
		}
	}
	
	private void processSiteEvent(SiteEvent siteEvent) {
		InsertResult result = shoreline.insertArc(siteEvent.getSite(), sweepline);
		
		if (result.isError()) {
			throw new IllegalStateException(result.getErrorMessage());
		} else if (!result.isFirstArc()) {
			// Invalidate old event
			Optional<CircleEvent> oldCircleEvent = result.oldArc.getCircleEvent();
			oldCircleEvent.ifPresent(CircleEvent::markInvalid);

			// Check for new circle events
			Optional<Circle> leftEventCircle = result.leftArc.computeEventCircle();
			leftEventCircle.ifPresent(circle -> {
				CircleEvent event = new CircleEvent(result.leftArc, circle.getCentroid(), circle.radius());
				result.leftArc.setCircleEvent(Optional.of(event));
				circleEventsQueue.add(event);
			});
			Optional<Circle> middleEventCircle = result.newArc.computeEventCircle();
			middleEventCircle.ifPresent(circle -> {
				CircleEvent event = new CircleEvent(result.newArc, circle.getCentroid(), circle.radius());
				result.newArc.setCircleEvent(Optional.of(event));
				circleEventsQueue.add(event);
			});
			Optional<Circle> rightEventCircle = result.rightArc.computeEventCircle();
			rightEventCircle.ifPresent(circle -> {
				CircleEvent event = new CircleEvent(result.rightArc, circle.getCentroid(), circle.radius());
				result.rightArc.setCircleEvent(Optional.of(event));
				circleEventsQueue.add(event);
			});
			
			// Create a new edge in the diagram
			PartialEdge newEdge = new PartialEdge(result.leftBreakpoint, result.rightBreakpoint);
			result.leftBreakpoint.setEdge(newEdge);
			result.rightBreakpoint.setEdge(newEdge);
			partialEdges.add(newEdge);
		}
	}

	private void processCircleEvent(CircleEvent circleEvent) {
		RemoveResult result = shoreline.removeArc(circleEvent.arc);

		Arc left = result.newBreakpoint.getLeftArc();
		Arc right = result.newBreakpoint.getRightArc();
		
		// Invalidate the event for the now vanished arc
		Optional<CircleEvent> oldEvent = result.oldArc.getCircleEvent();
		oldEvent.ifPresent(event -> {
			event.markInvalid();
		});
		
		// Invalidate the events for the affected arcs
		Optional<CircleEvent> leftEvent = left.getCircleEvent();
		leftEvent.ifPresent(event -> {
			event.markInvalid();
		});
		Optional<CircleEvent> rightEvent = right.getCircleEvent();
		rightEvent.ifPresent(event -> {
			event.markInvalid();
		});
		
		// Check for new circle events
		Optional<Circle> leftEventCircle = left.computeEventCircle();
		leftEventCircle.ifPresent(circle -> {
			CircleEvent event = new CircleEvent(left, circle.getCentroid(), circle.radius());
			left.setCircleEvent(Optional.of(event));
			circleEventsQueue.add(event);
		});
		Optional<Circle> rightEventCircle = right.computeEventCircle();
		rightEventCircle.ifPresent(circle -> {
			CircleEvent event = new CircleEvent(right, circle.getCentroid(), circle.radius());
			right.setCircleEvent(Optional.of(event));
			circleEventsQueue.add(event);
		});
		
		// Finish two related edges
		result.oldLeftBreakpoint.getEdge().addEndpoint(circleEvent);
		result.oldRightBreakpoint.getEdge().addEndpoint(circleEvent);
		
		// Create new edge
		PartialEdge newEdge = new PartialEdge(circleEvent, result.newBreakpoint);
		result.newBreakpoint.setEdge(newEdge);
		partialEdges.add(newEdge);
	}

	public void step() {
		this.doWork();
	}
	
	@Override
	public void doWork() {
		switch (progressStep) {
			case INITIALIZATION:
				initialize();
				break;
				
			case PROCESSING_EVENTS:
				processEvent();
				break;
				
			case FINISHED:				
			default:
				break;
		}
	}

	@Override
	public boolean isFinished() {
		return (progressStep == BuildProgressStep.FINISHED);
	}

	@Override
	public void doTimedWork(LongSupplier timeSupplierMS, int suggestedMaxWorkTimeMS) {
		timedTask.doTimedWork(timeSupplierMS, suggestedMaxWorkTimeMS);		
	}
	
	public void doTimedWork(int suggestedMaxWorkTimeMS) {
		timedTask.doTimedWork(() -> System.currentTimeMillis(), suggestedMaxWorkTimeMS);		
	}

}
