package com.gpergrossi.voronoi.shoreline;

import java.util.Optional;

import com.gpergrossi.util.geom.shapes.Circle;
import com.gpergrossi.util.geom.vectors.Double2D;
import com.gpergrossi.voronoi.graph.Site;
import com.gpergrossi.voronoi.math.VoronoiUtils;

public class ProxySite extends Site {
	
	public ProxySite(Double2D point) {
		super(-1, point);
	}

	@Override
	public String toString() {
		return "ProxySite[x=" + point.x() + ", y=" + point.y() + "]";
	}
	
	public static Optional<Circle> computeEventCircle(ProxySite proxySite, Double2D middle, Double2D right) {		
		/**
		 * If the middle site point is higher than the right, then the middle arc will never vanish:
		 * 
		 * +----------------------------------------SWEEP-LINE---------------------------------------+
		 * |                                                                                         |
		 * |                  MMM                                                                    |
		 * |                MM   MM                                                                  |
		 * |               M       M                                                                 |
		 * |              M         M                                                                |
		 * |             M           M                                                               |
		 * |             M   MIDDLE  M                                                               |
		 * |            M             M              RRRRRRRRRRRRRRRRRRRRRRRRR                       |
		 * |            M             M       RRRRRRR                         RRRRRRR                |
		 * |           M               M RRRRR                                       RRRRR           |
		 * |           M             ###R                      RIGHT                      RRRR       |
		 * |           M          ###                                                         RRR    |
		 * |          M         ##                                                               RR  |
		 * +LLLLLLLLLL==============================PROXY=LINE=======================================+
		 */
		if (middle.y() > right.y() || VoronoiUtils.nearlyEqual(middle.y(), right.y())) {
			return Optional.empty();
		}

		/**
		 * If the middle site's x coordinate is left of the right site's x coordinate, 
		 * then the middle arc (marked by M's) will never vanish. The line made of .'s is the 
		 * projected breakpoint line, this breakpoint will never meet the proxy site's line:
		 * 
		 * +----------------------------------SWEEP-LINE---------------------------------------------+
		 * |                                                   RRR                                   |
		 * |     .                                           RR   RR                                 |
		 * |            .                                   R       R                                |
		 * |                    .                          R         R                               |
		 * |                            .                 R   RIGHT   R                              |
		 * |                                    .         R           R                              |
		 * |                                            .R             R                             |
		 * |                                   MMMMMMMMMM##############R                             |
		 * |                            MMMMMMM                        .@@@@@@                       |
		 * |                       MMMMM                                      .@@@@@                 |
		 * |                   MMMM                     MIDDLE                      @@@@             |
		 * |                MMM                                                         @@@  .       |
		 * |              MM                                                               @@       .|
		 * +LLLLLLLLLLLLLL====================PRO@Y=LINE=============================================+
		 */
		if (middle.x() < right.x() || VoronoiUtils.nearlyEqual(middle.x(), right.x())) {
			return Optional.empty();
		}
		
		/**
		 * Otherwise, the middle arc's site is below and to the right of the right arc's site.
		 * It should be clear from looking at this diagram that the middle arc (M's) is doomed.
		 * 
		 * +----------------------------------SWEEP-LINE---------------------------------------------+
		 * |                                                                                         |
		 * |                                                                                         |
		 * |                           RRR                                                           |
		 * |                         RR   RR                                                         |
		 * |                        R       R                                                        |
		 * |                       R         R                                                       |
		 * |                      R   RIGHT   R                                                      |
		 * |                      R           R@@@@@@@@@@@@@@@@@@@@@@@@                              |
		 * |                     R      #######                        @@@@@@@                       |
		 * |         doomed!     R #####                                      @@@@@@                 |
		 * |                   MM##                     MIDDLE                      @@@@             |
		 * |                MMM                                                         @@@          |
		 * |              MM                                                               @@        |
		 * +LLLLLLLLLLLLLL====================PRO@Y=LINE=============================================+
		 * 
		 * The circle point is the point equidistant from the right and middle site points and the
		 * proxy site's y-coordinate (this is lower than the proxy line itself).
		 */
		return computeEventCircle(proxySite.y(), middle, right);
	}

	public static Optional<Circle> computeEventCircle(Double2D left, ProxySite proxySite, Double2D right) {
		/**
		 * If the order of the arcs is incorrect, then the circle event does not exist
		 */
		if (left.x() > right.x() || VoronoiUtils.nearlyEqual(left.x(), right.x())) {
			return Optional.empty();
		}
		
		/**
		 * If a proxy site "arc" lies between two normal parabola arcs, then it will always vanish
		 */
		return computeEventCircle(proxySite.y(), left, right);
	}

	public static Optional<Circle> computeEventCircle(Double2D left, Double2D middle, ProxySite proxySite) {		
		/**
		 * If the middle site point is higher than the left, then the middle arc will never vanish:
		 * 
		 * +---------------------------------------SWEEP-LINE----------------------------------------+
		 * |                                                                                         |
		 * |                                                                    MMM                  |
		 * |                                                                  MM   MM                |
		 * |                                                                 M       M               |
		 * |                                                                M         M              |
		 * |                                                               M           M             |
		 * |                                                               M  MIDDLE   M             |
		 * |                       LLLLLLLLLLLLLLLLLLLLLLLLL              M             M            |
		 * |                LLLLLLL                         LLLLLLL       M             M            |
		 * |           LLLLL                                       LLLLL M               M           |
		 * |       LLLL                       LEFT                      L###             M           |
		 * |    LLL                                                         ###          M           |
		 * |  LL                                                               ##         M          |
		 * +=======================================PROXY=LINE==============================RRRRRRRRRR+
		 */
		if (middle.y() > left.y() || VoronoiUtils.nearlyEqual(middle.y(), left.y())) {
			return Optional.empty();
		}

		/**
		 * If the middle site's x coordinate is right of the left site's x coordinate, 
		 * then the middle arc (marked by M's) will never vanish. The line made of .'s is the 
		 * projected breakpoint line, this breakpoint will never meet the proxy site's line:
		 * 
		 * +---------------------------------------------SWEEP-LINE----------------------------------+
		 * |                                   LLL                                                   |
		 * |                                 LL   LL                                           .     |
		 * |                                L       L                                   .            |
		 * |                               L         L                          .                    |
		 * |                              L   LEFT    L                 .                            |
		 * |                              L           L         .                                    |
		 * |                             L             L.                                            |
		 * |                             L##############MMMMMMMMMM                                   |
		 * |                       @@@@@@.                        MMMMMMM                            |
		 * |                 @@@@@.                                      MMMMM                       |
		 * |             @@@@                      MIDDLE                     MMMM                   |
		 * |       .  @@@                                                         MMM                |
		 * |.       @@                                                               MM              |
		 * +=============================================PROXY=LINE====================RRRRRRRRRRRRRR+
		 */
		if (middle.x() > left.x() || VoronoiUtils.nearlyEqual(middle.x(), left.x())) {
			return Optional.empty();
		}
		
		/**
		 * Otherwise, the middle arc's site is below and to the left of the left arc's site.
		 * It should be clear from looking at this diagram that the middle arc (M's) is doomed.
		 * 
		 * +---------------------------------------------SWEEP-LINE----------------------------------+
		 * |                                                                                         |
		 * |                                                                                         |
		 * |                                                           LLL                           |
		 * |                                                         LL   LL                         |
		 * |                                                        L       L                        |
		 * |                                                       L         L                       |
		 * |                                                      L    LEFT   L                      |
		 * |                              @@@@@@@@@@@@@@@@@@@@@@@@L           L                      |
		 * |                       @@@@@@@                        #######      L                     |
		 * |                 @@@@@@                                      ##### L     doomed!         |
		 * |             @@@@                      MIDDLE                     ##MM                   |
		 * |          @@@                                                         MMM                |
		 * |        @@                                                               MM              |
         * +=============================================PROXY=LINE====================RRRRRRRRRRRRRR+
		 */
		return computeEventCircle(proxySite.y(), left, middle);
	}
	
	private static Optional<Circle> computeEventCircle(double proxySiteY, Double2D leftArcSitePoint, Double2D rightArcSitePoint) {
		final double leftX = leftArcSitePoint.x();
		final double leftY = leftArcSitePoint.y();
		final double rightX = rightArcSitePoint.x();
		final double rightY = rightArcSitePoint.y();
		
		final double midpointX = (leftX + rightX) * 0.5;
		final double midpointY = (leftY + rightY) * 0.5;
		
		final double orthoX, orthoY, distance;
		{
			final double deltaX = rightX - leftX;
			final double deltaY = rightY - leftY;
			distance = Math.sqrt(deltaX * deltaX + deltaY * deltaY);
			orthoX = -deltaY / distance;
			orthoY = deltaX / distance;
		}
		
		if (VoronoiUtils.nearlyEqual(orthoY, 0)) return Optional.empty();

		final double t = (proxySiteY - midpointY) / orthoY;
		final double x = midpointX + orthoX * t;
		final double y = midpointY + orthoY * t;
		
		final double radius;
		{
			final double deltaX = x - leftX;
			final double deltaY = y - leftY;
			radius = Math.sqrt(deltaX * deltaX + deltaY * deltaY);
		}
		
		return Optional.of(new Circle(x, y, radius));
	}
}