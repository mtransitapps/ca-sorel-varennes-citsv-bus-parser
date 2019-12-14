package org.mtransit.parser.ca_sorel_varennes_citsv_bus;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.mtransit.parser.CleanUtils;
import org.mtransit.parser.DefaultAgencyTools;
import org.mtransit.parser.Pair;
import org.mtransit.parser.SplitUtils;
import org.mtransit.parser.SplitUtils.RouteTripSpec;
import org.mtransit.parser.Utils;
import org.mtransit.parser.gtfs.data.GCalendar;
import org.mtransit.parser.gtfs.data.GCalendarDate;
import org.mtransit.parser.gtfs.data.GRoute;
import org.mtransit.parser.gtfs.data.GSpec;
import org.mtransit.parser.gtfs.data.GStop;
import org.mtransit.parser.gtfs.data.GTrip;
import org.mtransit.parser.gtfs.data.GTripStop;
import org.mtransit.parser.mt.data.MAgency;
import org.mtransit.parser.mt.data.MRoute;
import org.mtransit.parser.mt.data.MTrip;
import org.mtransit.parser.mt.data.MTripStop;

// https://rtm.quebec/en/about/open-data
// https://rtm.quebec/xdata/citsv/google_transit.zip
public class SorelVarennesCITSVBusAgencyTools extends DefaultAgencyTools {

	public static void main(String[] args) {
		if (args == null || args.length == 0) {
			args = new String[3];
			args[0] = "input/gtfs.zip";
			args[1] = "../../mtransitapps/ca-sorel-varennes-citsv-bus-android/res/raw/";
			args[2] = ""; // files-prefix
		}
		new SorelVarennesCITSVBusAgencyTools().start(args);
	}

	private HashSet<String> serviceIds;

	@Override
	public void start(String[] args) {
		System.out.printf("\nGenerating CITSV bus data...");
		long start = System.currentTimeMillis();
		this.serviceIds = extractUsefulServiceIds(args, this);
		super.start(args);
		System.out.printf("\nGenerating CITSV bus data... DONE in %s.\n", Utils.getPrettyDuration(System.currentTimeMillis() - start));
	}

	@Override
	public boolean excludingAll() {
		return this.serviceIds != null && this.serviceIds.isEmpty();
	}

	@Override
	public boolean excludeCalendar(GCalendar gCalendar) {
		if (this.serviceIds != null) {
			return excludeUselessCalendar(gCalendar, this.serviceIds);
		}
		return super.excludeCalendar(gCalendar);
	}

	@Override
	public boolean excludeCalendarDate(GCalendarDate gCalendarDates) {
		if (this.serviceIds != null) {
			return excludeUselessCalendarDate(gCalendarDates, this.serviceIds);
		}
		return super.excludeCalendarDate(gCalendarDates);
	}

	@Override
	public boolean excludeTrip(GTrip gTrip) {
		if (this.serviceIds != null) {
			return excludeUselessTrip(gTrip, this.serviceIds);
		}
		return super.excludeTrip(gTrip);
	}

	@Override
	public Integer getAgencyRouteType() {
		return MAgency.ROUTE_TYPE_BUS;
	}

	@Override
	public long getRouteId(GRoute gRoute) {
		if (!Utils.isDigitsOnly(gRoute.getRouteId())) {
			return Long.parseLong(gRoute.getRouteShortName());
		}
		return super.getRouteId(gRoute);
	}

	@Override
	public String getRouteLongName(GRoute gRoute) {
		return cleanRouteLongName(gRoute);
	}

	private String cleanRouteLongName(GRoute gRoute) {
		String routeLongName = gRoute.getRouteLongName();
		routeLongName = CleanUtils.SAINT.matcher(routeLongName).replaceAll(CleanUtils.SAINT_REPLACEMENT);
		return CleanUtils.cleanLabel(routeLongName);
	}

	private static final String AGENCY_COLOR = "1F1F1F"; // DARK GRAY (from GTFS)

	@Override
	public String getAgencyColor() {
		return AGENCY_COLOR;
	}

	private static HashMap<Long, RouteTripSpec> ALL_ROUTE_TRIPS2;
	static {
		HashMap<Long, RouteTripSpec> map2 = new HashMap<Long, RouteTripSpec>();
		ALL_ROUTE_TRIPS2 = map2;
	}

	@Override
	public int compareEarly(long routeId, List<MTripStop> list1, List<MTripStop> list2, MTripStop ts1, MTripStop ts2, GStop ts1GStop, GStop ts2GStop) {
		if (ALL_ROUTE_TRIPS2.containsKey(routeId)) {
			return ALL_ROUTE_TRIPS2.get(routeId).compare(routeId, list1, list2, ts1, ts2, ts1GStop, ts2GStop, this);
		}
		return super.compareEarly(routeId, list1, list2, ts1, ts2, ts1GStop, ts2GStop);
	}

	@Override
	public ArrayList<MTrip> splitTrip(MRoute mRoute, GTrip gTrip, GSpec gtfs) {
		if (ALL_ROUTE_TRIPS2.containsKey(mRoute.getId())) {
			return ALL_ROUTE_TRIPS2.get(mRoute.getId()).getAllTrips();
		}
		return super.splitTrip(mRoute, gTrip, gtfs);
	}

	@Override
	public Pair<Long[], Integer[]> splitTripStop(MRoute mRoute, GTrip gTrip, GTripStop gTripStop, ArrayList<MTrip> splitTrips, GSpec routeGTFS) {
		if (ALL_ROUTE_TRIPS2.containsKey(mRoute.getId())) {
			return SplitUtils.splitTripStop(mRoute, gTrip, gTripStop, routeGTFS, ALL_ROUTE_TRIPS2.get(mRoute.getId()), this);
		}
		return super.splitTripStop(mRoute, gTrip, gTripStop, splitTrips, routeGTFS);
	}

	@Override
	public void setTripHeadsign(MRoute mRoute, MTrip mTrip, GTrip gTrip, GSpec gtfs) {
		if (ALL_ROUTE_TRIPS2.containsKey(mRoute.getId())) {
			return; // split
		}
		mTrip.setHeadsignString(cleanTripHeadsign(gTrip.getTripHeadsign()), gTrip.getDirectionId());
	}

	private static final Pattern DIRECTION = Pattern.compile("(direction )", Pattern.CASE_INSENSITIVE);
	private static final String DIRECTION_REPLACEMENT = "";

	@Override
	public String cleanTripHeadsign(String tripHeadsign) {
		tripHeadsign = DIRECTION.matcher(tripHeadsign).replaceAll(DIRECTION_REPLACEMENT);
		tripHeadsign = CleanUtils.cleanStreetTypesFRCA(tripHeadsign);
		return CleanUtils.cleanLabelFR(tripHeadsign);
	}

	@Override
	public boolean mergeHeadsign(MTrip mTrip, MTrip mTripToMerge) {
		List<String> headsignsValues = Arrays.asList(mTrip.getHeadsignValue(), mTripToMerge.getHeadsignValue());
		if (mTrip.getRouteId() == 370L) {
			if (Arrays.asList( //
					"St-Amable", //
					"Ste-Julie", //
					"Nord" //
			).containsAll(headsignsValues)) {
				mTrip.setHeadsignString("Nord", mTrip.getHeadsignId());
				return true;
			}
		}
		if (mTrip.getRouteId() == 700L) {
			if (Arrays.asList( //
					"Longueuil", // <>
					"Sorel-Tracy" //
			).containsAll(headsignsValues)) {
				mTrip.setHeadsignString("Sorel-Tracy", mTrip.getHeadsignId());
				return true;
			}
		}
		if (mTrip.getRouteId() == 720L) {
			if (Arrays.asList( //
					"Varennes", //
					"Nord" //
			).containsAll(headsignsValues)) {
				mTrip.setHeadsignString("Nord", mTrip.getHeadsignId());
				return true;
			}
			if (Arrays.asList( //
					"Longueuil", //
					"Sud" //
			).containsAll(headsignsValues)) {
				mTrip.setHeadsignString("Sud", mTrip.getHeadsignId());
				return true;
			}
		}
		if (mTrip.getRouteId() == 721L) {
			if (Arrays.asList( //
					"Varennes", //
					"Nord" //
			).containsAll(headsignsValues)) {
				mTrip.setHeadsignString("Nord", mTrip.getHeadsignId());
				return true;
			}
			if (Arrays.asList( //
					"Longueuil", //
					"Sud" //
			).containsAll(headsignsValues)) {
				mTrip.setHeadsignString("Sud", mTrip.getHeadsignId());
				return true;
			}
		}
		if (mTrip.getRouteId() == 722L) {
			if (Arrays.asList( //
					"Varennes", //
					"Nord" //
			).containsAll(headsignsValues)) {
				mTrip.setHeadsignString("Nord", mTrip.getHeadsignId());
				return true;
			}
			if (Arrays.asList( //
					"Longueuil", //
					"Sud" //
			).containsAll(headsignsValues)) {
				mTrip.setHeadsignString("Sud", mTrip.getHeadsignId());
				return true;
			}
		}
		if (mTrip.getRouteId() == 723L) {
			if (Arrays.asList( //
					"Varennes (IREQ)", //
					"Nord" //
			).containsAll(headsignsValues)) {
				mTrip.setHeadsignString("Nord", mTrip.getHeadsignId());
				return true;
			}
			if (Arrays.asList( //
					"Longueuil", //
					"Sud" //
			).containsAll(headsignsValues)) {
				mTrip.setHeadsignString("Sud", mTrip.getHeadsignId());
				return true;
			}
		}
		if (mTrip.getRouteId() == 724L) {
			if (Arrays.asList( //
					"Varennes", //
					"Nord" //
			).containsAll(headsignsValues)) {
				mTrip.setHeadsignString("Nord", mTrip.getHeadsignId());
				return true;
			}
			if (Arrays.asList( //
					"Longueuil", //
					"Sud" //
			).containsAll(headsignsValues)) {
				mTrip.setHeadsignString("Sud", mTrip.getHeadsignId());
				return true;
			}
		}
		if (mTrip.getRouteId() == 731L) {
			if (Arrays.asList( //
					"Longueuil", //
					"Sud" //
			).containsAll(headsignsValues)) {
				mTrip.setHeadsignString("Sud", mTrip.getHeadsignId());
				return true;
			}
		}
		System.out.printf("\nUnexpected trips to merge: %s & %s!\n", mTrip, mTripToMerge);
		System.exit(-1);
		return false;
	}

	private static final Pattern START_WITH_FACE_A = Pattern.compile("^(face à )", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
	private static final Pattern START_WITH_FACE_AU = Pattern.compile("^(face au )", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
	private static final Pattern START_WITH_FACE = Pattern.compile("^(face )", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

	private static final Pattern SPACE_FACE_A = Pattern.compile("( face à )", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
	private static final Pattern SPACE_WITH_FACE_AU = Pattern.compile("( face au )", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
	private static final Pattern SPACE_WITH_FACE = Pattern.compile("( face )", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

	private static final Pattern[] START_WITH_FACES = new Pattern[] { START_WITH_FACE_A, START_WITH_FACE_AU, START_WITH_FACE };

	private static final Pattern[] SPACE_FACES = new Pattern[] { SPACE_FACE_A, SPACE_WITH_FACE_AU, SPACE_WITH_FACE };

	@Override
	public String cleanStopName(String gStopName) {
		gStopName = Utils.replaceAll(gStopName, START_WITH_FACES, CleanUtils.SPACE);
		gStopName = Utils.replaceAll(gStopName, SPACE_FACES, CleanUtils.SPACE);
		gStopName = CleanUtils.cleanStreetTypesFRCA(gStopName);
		return CleanUtils.cleanLabelFR(gStopName);
	}

	private static final String ZERO = "0";

	@Override
	public String getStopCode(GStop gStop) {
		if (ZERO.equals(gStop.getStopCode())) {
			return null;
		}
		return super.getStopCode(gStop);
	}

	private static final Pattern DIGITS = Pattern.compile("[\\d]+");

	@Override
	public int getStopId(GStop gStop) {
		String stopCode = getStopCode(gStop);
		if (stopCode != null && stopCode.length() > 0 && Utils.isDigitsOnly(stopCode)) {
			return Integer.valueOf(stopCode); // using stop code as stop ID
		}
		Matcher matcher = DIGITS.matcher(gStop.getStopId());
		if (matcher.find()) {
			int digits = Integer.parseInt(matcher.group());
			int stopId;
			if (gStop.getStopId().startsWith("BOU")) {
				stopId = 100000;
			} else if (gStop.getStopId().startsWith("LON")) {
				stopId = 200000;
			} else if (gStop.getStopId().startsWith("VAR")) {
				stopId = 300000;
			} else if (gStop.getStopId().startsWith("SAM")) {
				stopId = 400000;
			} else if (gStop.getStopId().startsWith("VCH")) {
				stopId = 500000;
			} else if (gStop.getStopId().startsWith("CON")) {
				stopId = 600000;
			} else if (gStop.getStopId().startsWith("LON")) {
				stopId = 700000;
			} else {
				System.out.printf("\nStop doesn't have an ID (start with) %s!\n", gStop);
				System.exit(-1);
				return -1;
			}
			if (gStop.getStopId().endsWith("A")) {
				stopId += 1000;
			} else if (gStop.getStopId().endsWith("B")) {
				stopId += 2000;
			} else if (gStop.getStopId().endsWith("C")) {
				stopId += 3000;
			} else if (gStop.getStopId().endsWith("D")) {
				stopId += 4000;
			} else {
				System.out.printf("\nStop doesn't have an ID (end with) %s!\n", gStop);
				System.exit(-1);
				return -1;
			}
			return stopId + digits;
		} else {
			System.out.printf("\nUnexpected stop ID %s!\n", gStop);
			System.exit(-1);
			return -1;
		}
	}
}
