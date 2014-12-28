package org.mtransit.parser.ca_sorel_varennes_citsv_bus;

import java.util.regex.Pattern;

import org.mtransit.parser.DefaultAgencyTools;
import org.mtransit.parser.Utils;
import org.mtransit.parser.gtfs.data.GRoute;
import org.mtransit.parser.gtfs.data.GStop;
import org.mtransit.parser.gtfs.data.GTrip;
import org.mtransit.parser.mt.data.MRoute;
import org.mtransit.parser.mt.data.MSpec;
import org.mtransit.parser.mt.data.MTrip;

// https://amt.qc.ca/en/about/open-data
// http://www.amt.qc.ca/xdata/citsv/google_transit.zip
public class SorelVarennesCITSVBusAgencyTools extends DefaultAgencyTools {

	public static final String ROUTE_TYPE_FILTER = "3"; // bus only

	public static void main(String[] args) {
		if (args == null || args.length == 0) {
			args = new String[3];
			args[0] = "input/gtfs.zip";
			args[1] = "../../mtransitapps/ca-sorel-varennes-citsv-bus-android/res/raw/";
			args[2] = ""; // files-prefix
		}
		new SorelVarennesCITSVBusAgencyTools().start(args);
	}

	@Override
	public void start(String[] args) {
		System.out.printf("Generating CITSV bus data...\n");
		long start = System.currentTimeMillis();
		super.start(args);
		System.out.printf("Generating CITSV bus data... DONE in %s.\n", Utils.getPrettyDuration(System.currentTimeMillis() - start));
	}

	@Override
	public boolean excludeRoute(GRoute gRoute) {
		if (ROUTE_TYPE_FILTER != null && !gRoute.route_type.equals(ROUTE_TYPE_FILTER)) {
			return true;
		}
		return super.excludeRoute(gRoute);
	}

	@Override
	public String getRouteLongName(GRoute gRoute) {
		String routeLongName = gRoute.route_long_name;
		routeLongName = MSpec.SAINT.matcher(routeLongName).replaceAll(MSpec.SAINT_REPLACEMENT);
		return MSpec.cleanLabel(routeLongName);
	}

	private static final String ROUTE_COLOR = "77A22E";

	@Override
	public String getRouteColor(GRoute gRoute) {
		return ROUTE_COLOR;
	}

	private static final String ROUTE_TEXT_COLOR = "FFFFFF";

	@Override
	public String getRouteTextColor(GRoute gRoute) {
		return ROUTE_TEXT_COLOR;
	}

	@Override
	public void setTripHeadsign(MRoute route, MTrip mTrip, GTrip gTrip) {
		String stationName = cleanTripHeadsign(gTrip.trip_headsign);
		int directionId = Integer.valueOf(gTrip.direction_id);
		mTrip.setHeadsignString(stationName, directionId);
	}

	private static final Pattern DIRECTION = Pattern.compile("(direction )", Pattern.CASE_INSENSITIVE);
	private static final String DIRECTION_REPLACEMENT = "";

	@Override
	public String cleanTripHeadsign(String tripHeadsign) {
		tripHeadsign = DIRECTION.matcher(tripHeadsign).replaceAll(DIRECTION_REPLACEMENT);
		return MSpec.cleanLabelFR(tripHeadsign);
	}

	private static final Pattern START_WITH_FACE_A = Pattern.compile("^(face à )", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
	private static final Pattern START_WITH_FACE_AU = Pattern.compile("^(face au )", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
	private static final Pattern START_WITH_FACE = Pattern.compile("^(face )", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

	private static final Pattern SPACE_FACE_A = Pattern.compile("( face à )", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
	private static final Pattern SPACE_WITH_FACE_AU = Pattern.compile("( face au )", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
	private static final Pattern SPACE_WITH_FACE = Pattern.compile("( face )", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

	private static final Pattern[] START_WITH_FACES = new Pattern[] { START_WITH_FACE_A, START_WITH_FACE_AU, START_WITH_FACE };

	private static final Pattern[] SPACE_FACES = new Pattern[] { SPACE_FACE_A, SPACE_WITH_FACE_AU, SPACE_WITH_FACE };

	private static final Pattern AVENUE = Pattern.compile("( avenue)", Pattern.CASE_INSENSITIVE);
	private static final String AVENUE_REPLACEMENT = " av.";

	@Override
	public String cleanStopName(String gStopName) {
		gStopName = AVENUE.matcher(gStopName).replaceAll(AVENUE_REPLACEMENT);
		gStopName = Utils.replaceAll(gStopName, START_WITH_FACES, MSpec.SPACE);
		gStopName = Utils.replaceAll(gStopName, SPACE_FACES, MSpec.SPACE);
		return super.cleanStopNameFR(gStopName);
	}

	@Override
	public String getStopCode(GStop gStop) {
		if ("0".equals(gStop.stop_code)) {
			if ("BOU29C".equals(gStop.stop_id)) {
				return "77029";
			} else if ("BOU30A".equals(gStop.stop_id)) {
				return "77030";
			} else if ("BOU30C".equals(gStop.stop_id)) {
				return "77130";
			} else if ("LON6A".equals(gStop.stop_id)) {
				return "77060";
			} else if ("LON6C".equals(gStop.stop_id)) {
				return "77160";
			} else if ("LON30A".equals(gStop.stop_id)) {
				return "77530";
			} else if ("LON30C".equals(gStop.stop_id)) {
				return "77630";
			} else if ("LON219A".equals(gStop.stop_id)) {
				return "77219";
			} else if ("LON242C".equals(gStop.stop_id)) {
				return "77242";
			} else if ("VAR109B".equals(gStop.stop_id)) {
				return "77109";
			} else if ("VAR146A".equals(gStop.stop_id)) {
				return "77146";
			} else if ("SAM77A".equals(gStop.stop_id)) {
				return "77077";
			} else if ("SAM77C".equals(gStop.stop_id)) {
				return "77177";
			} else {
				System.out.println("Stop doesn't have an ID! " + gStop);
				System.exit(-1);
			}
		}
		return super.getStopCode(gStop);
	}

	@Override
	public int getStopId(GStop gStop) {
		return Integer.valueOf(getStopCode(gStop)); // using stop code as stop ID
	}

}
