package com.gpergrossi.voronoi;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import com.gpergrossi.util.data.Iterators;
import com.gpergrossi.util.geom.vectors.Double2D;
import com.gpergrossi.util.spacial.SpacialIndex2D;
import com.gpergrossi.voronoi.shoreline.Sweepline;

public class VoronoiBuilder {

	private static final double MIN_SITE_DISTANCE = 1.0;
	
	protected boolean sanitizeInputPoints;
	protected SpacialIndex2D siteSpacialIndex;
	protected List<Double2D> sites;
	
	public VoronoiBuilder() {
		sanitizeInputPoints = true;
		siteSpacialIndex = new SpacialIndex2D(MIN_SITE_DISTANCE);
		sites = new ArrayList<>();
	}
	
	/**
	 * Add a single site to this VoronoiBuilder. The site may fail to be added if the sanitize inputs 
	 * option is enabled and the new site is too close to a previously added site.
	 * @param site - site to be added 
	 * @return true if site was added, false if the site failed the enabled sanitize inputs check
	 */
	public boolean addSite(Double2D site) {
		// Optional: sanitize check
		if (sanitizeInputPoints) {
			if (siteSpacialIndex.getNearbyPoints(site, MIN_SITE_DISTANCE, Optional.empty()) > 0) {
				// Sanitize check failed
				return false;
			} else {
				// Sanitize check passed
				siteSpacialIndex.add(site);
				sites.add(site);
				return true;
			}
		} else {
			// Sanitize check off
			sites.add(site);
			return true;
		}
	}
	
	/**
	 * Add a collection of sites to this VoronoiBuilder. Some sites may fail to be added if the sanitize inputs 
	 * option is enabled and the new site is too close to a previously added site. In this case you may optionally
	 * provide a list in which to collect the failed sites as output. Items will be added to the list when they 
	 * fail the enabled sanitize inputs check.
	 * @param sites - the collection of sites to be added
	 * @param failedSitesOutput - an optional list to which all failed sites will be added
	 * @return number of sites successfully added
	 */
	public int addSites(Collection<Double2D> sites, Optional<List<Double2D>> failedSitesOutput) {
		int numAdded = 0;		
		for (Double2D site : sites) {
			if (addSite(site)) {
				numAdded++;
			} else if (failedSitesOutput.isPresent()) {
				failedSitesOutput.get().add(site);
			}
		}
		return numAdded;
	}
	
	/**
	 * Remove a previously added site from the sites list
	 * @param site
	 * @return true if the site was removed, false if it was not present.
	 */
	public boolean removeSite(Double2D site) {
		if (sites.remove(site)) {
			siteSpacialIndex.remove(site);
			return true;
		} else {		
			return false;
		}
	}
	
	public Iterator<Double2D> getSites() {
		return Iterators.withRemoveCallback(sites.iterator(), pt -> {
			return false;
		});
	}
	
	public void clear() {
		this.sites.clear();
		this.siteSpacialIndex.clear();
	}

	public VoronoiBuildState createBuildState() {
		return new VoronoiBuildState(new Sweepline(), sites);
	}

	
}
