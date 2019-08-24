package com.gpergrossi.voronoi.event;

import com.gpergrossi.util.geom.vectors.Double2D;
import com.gpergrossi.voronoi.graph.Site;

public class SiteEvent implements VoronoiEvent {

	private final Site site;
	
	public SiteEvent(Site site) {
		this.site = site;
	}
	
	@Override
	public VoronoiEventType getType() {
		return VoronoiEventType.SITE;
	}
	
	@Override
	public Double2D getEventPoint() {
		return site.getPoint();
	}

	public Site getSite() {
		return site;
	}
	
	@Override
	public String toString() {
		return "SiteEvent[Site=" + site.getID() + "]";
	}

}