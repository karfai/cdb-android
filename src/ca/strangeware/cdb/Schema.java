package ca.strangeware.cdb;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.regex.*;

import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

public class Schema {
	public class PickUp {
		public long id;
		public int	sequence;
		public long	trip_id;
		
		public PickUp(long id, int seq, long trip_id) {
			this.id = id;
			this.sequence = seq;
			this.trip_id = trip_id;
		}
	}
	
	public class Stop {
		public String	name;
		public int		number;
		public String	label;
		private long	id;
		
		public Stop(long id, String name, int num, String label) {
			this.id = id;
			this.name = name;
			this.number = num;
			this.label = label;
		}
		
		public Cursor upcoming_pickups() throws SchemaException {
			long secs = Schema.secsElapsedToday();
			ServicePeriod sp = Schema.instance().currentServicePeriod();

			if ( null == sp ) {
				throw new SchemaException();
			}
			return Schema.instance().pickupsAtStop(this.id, sp.id, secs - 300, secs + 300);
		}
	}
	
	public class Trip {
		public  String	number;
		public  String  headsign;
		public  long    id;

		public Trip(long id, String number, String headsign) {
			this.id = id;
			this.number = number;
			this.headsign = headsign;
		}

		public Cursor upcomingStopsFromSeq(int seq) throws SchemaException {
			return Schema.instance().pickupsOnTripFromSeq(this.id, seq, 5);
		}
}
	
	public class ServicePeriod {
		private int days;
		private Calendar start;
		private Calendar finish;
		public long id;

		public ServicePeriod(long id, int days, String st, String fin) {
			this.id = id;
			this.days = days;
			this.start = dateStringToCalendar(st);
			this.finish = dateStringToCalendar(fin);
		}
		
		public boolean servesDay(Calendar c) {
			int shift = 0;
			int dow = c.get(Calendar.DAY_OF_WEEK);
			if ( dow == Calendar.SUNDAY ) {
				shift = 6;
			} else {
				shift = dow - 1;
			}

			return (this.days & (1 << shift)) > 0;
		}
		
		public boolean inService(Calendar c) {
			return servesDay(c) && (c.equals(this.start) || c.after(this.start)) && ((c.equals(this.finish) || c.before(this.finish)));
		}
	}
	
	static private Schema the_instance = null;
	
	private SQLiteDatabase db;
	private List<SearchBuilder> searches;

	static protected Calendar dateStringToCalendar(String s) {
		int y = Integer.parseInt(s.substring(0, 4));
		int m = Integer.parseInt(s.substring(4, 6));
		int d = Integer.parseInt(s.substring(6));
		Calendar rv = Calendar.getInstance();
		rv.clear();
		rv.set(y, m - 1, d);
		return rv;
	}
	
	static protected long secsElapsedToday() {
		Calendar now = Calendar.getInstance();
		Calendar midnight = currentDay();
								
		return ((now.getTimeInMillis() - midnight.getTimeInMillis()) / 1000);
	}
	
	static protected Calendar currentDay() {
		Calendar now = Calendar.getInstance();
		Calendar day = Calendar.getInstance();
		
		day.clear();
		day.set(now.get(Calendar.YEAR), now.get(Calendar.MONTH), now.get(Calendar.DATE), 0, 0);
		return day;
	}
	
	class NoMatchException extends Exception {
		private static final long serialVersionUID = 5164809425478019384L;
	};
	
	class SchemaException extends Exception {
		private static final long serialVersionUID = -17366214209793334L;
	};
	
	private abstract class SearchBuilder {
		private Pattern pat;

		public SearchBuilder(String re) {
			try {
				this.pat = Pattern.compile(re);
			} catch ( PatternSyntaxException e ) {
				
			}
		}
		
		protected abstract String interpret(String[] parts) throws SchemaException;
		
		protected String toSearchString(String s) {
			return "%" + s.toUpperCase() + "%";
		}

		public String build(String srch) throws NoMatchException, SchemaException {
			Matcher m = this.pat.matcher(srch);
			if ( !m.matches() ) {
				throw new NoMatchException();
			}
			
			String[] grps = new String[m.groupCount()];
			for ( int i = 0; i < m.groupCount(); i++ ) {
				grps[i]= m.group(i + 1);
			}
			return interpret(grps);
		}
	}

	private class Intersection extends SearchBuilder {
		public Intersection() {
			super("(\\w+) and (\\w+)");
		}

		@Override
		protected String interpret(String[] parts) throws SchemaException {
			if ( parts.length < 2 ) {
				throw new SchemaException();
			}
			String a = toSearchString(parts[0]);
			String b = toSearchString(parts[1]);
		    return String.format("stops.name LIKE \"%s/%s\" OR stops.name LIKE \"%s/%s\"", a, b, b, a);
		}
	}			
	
	private class Number extends SearchBuilder {
		public Number() {
			super("([0-9]{4})");
		}

		@Override
		protected String interpret(String[] parts) throws SchemaException {
			if ( parts.length < 1 ) {
				throw new SchemaException();
			}
		    return String.format("stops.number=%s", parts[0]);
		}
}
	
	private class TranspoId extends SearchBuilder {
		public TranspoId() {
			super("[a-zA-Z]{2}([0-9]{3})");
		}

		@Override
		protected String interpret(String[] parts) throws SchemaException {
			if ( parts.length < 1 ) {
				throw new SchemaException();
			}
		    return String.format("stops.label=%s", parts[0].toUpperCase());
		}
	}
	
	private class Any extends SearchBuilder {
		public Any() {
			super("(\\w+)");
		}

		@Override
		protected String interpret(String[] parts) throws SchemaException {
			if ( parts.length < 1 ) {
				throw new SchemaException();
			}
		    return String.format("stops.name like \"%s\"", toSearchString(parts[0]));
		}
	}

	static public Schema instance() throws SchemaException {
		if ( null == the_instance ) {
			the_instance = new Schema();
			the_instance.open();
		}
		
		return the_instance;
	}
	
    private Schema() {
    	this.searches = new ArrayList<SearchBuilder>();
    	this.searches.add(new Intersection());
    	this.searches.add(new Number());
    	this.searches.add(new TranspoId());
    	this.searches.add(new Any());
	}
	
	private String buildSearch(String srch) throws SchemaException {
		for ( SearchBuilder b: this.searches) {
			try {
				return b.build(srch);
			} catch ( NoMatchException e ) {
				// do nothing
			}
		}

		return null;
	}

	private Schema open() throws SchemaException {
		try {
			db = SQLiteDatabase.openDatabase("/sdcard/transit.db", null, SQLiteDatabase.OPEN_READONLY);
		} catch ( SQLException e) {
			throw new SchemaException();
		}
		return this;
	}
	
	public Cursor stopViaSearch(String srch) throws SchemaException {
		String wh = buildSearch(srch); 
		return this.db.query(
				"stops",
				new String[] { "id AS _id", "label", "number", "name" },
				wh,
				null, null, null, null, null);
	}

	public Stop lookupStop(long id) throws SchemaException {
		String wh = "stops.id=" + id;
		Cursor cr = this.db.query("stops", new String[] { "name", "number", "label" }, wh, null, null, null, null);
		if ( null == cr || cr.getCount() == 0 ) {
			throw new SchemaException();
		}
		cr.moveToFirst();
		return new Stop(id, cr.getString(0), cr.getInt(1), cr.getString(2));
	}

	public Trip lookupTrip(long id) throws SchemaException {
		Cursor cr = this.db.rawQuery(
				"SELECT t.id, r.name, t.headsign FROM trips t LEFT JOIN routes r ON r.id=t.route_id WHERE t.id="
				+ id,
				null);
		if ( null == cr || cr.getCount() == 0 ) {
			throw new SchemaException();
		}
		cr.moveToFirst();
		return new Trip(cr.getLong(0), cr.getString(1), cr.getString(2));
	}
	
	public PickUp lookupPickUp(long id) throws SchemaException {
		String wh = "pickups.id=" + id;
		Cursor cr = this.db.query("pickups", new String[] { "id", "sequence", "trip_id" }, wh, null, null, null, null);
		if ( null == cr || cr.getCount() == 0 ) {
			throw new SchemaException();
		}
		cr.moveToFirst();
		return new PickUp(cr.getLong(0), cr.getInt(1), cr.getLong(2));
	}
	
	public Cursor pickupsAtStop(long id, long spid, long st, long en) {
		return this.db.rawQuery(
				"SELECT pickups.id AS _id, arrival, departure, sequence, trip_id , stop_id, headsign, r.name AS bus FROM pickups LEFT JOIN trips t ON trip_id=t.id LEFT JOIN routes r ON r.id=t.route_id WHERE stop_id="
				+ id + " AND t.service_period_id=" + spid + " AND arrival>=" + st + " AND arrival<=" + en,
				null);
	}

	public Cursor pickupsOnTripFromSeq(long trip_id, int seq, int limit) {
		Log.d("TIMING", "starting");
		Cursor rv = this.db.rawQuery(
				"SELECT s.id AS _id, s.number AS number, s.name AS name, p.arrival FROM pickups p LEFT JOIN stops s ON p.stop_id=s.id WHERE p.trip_id="
				+ trip_id + " AND p.sequence>" + seq + " ORDER BY p.sequence LIMIT " + limit,
				null);
		Log.d("TIMING", "done (size=" + rv.getCount() + ")");
		return rv;
	}
	
	public ServicePeriod currentServicePeriod() throws SchemaException {
		Calendar now = currentDay();
		ServicePeriod rv = null;
		// TODO_donk: service exceptions
		Cursor cr = this.db.query("service_periods", new String[] { "id", "days", "start", "finish" }, null, null, null, null, null);
		while ( null == rv && cr.moveToNext() ) {
			ServicePeriod sp = new ServicePeriod(cr.getLong(0), cr.getInt(1), cr.getString(2), cr.getString(3));
			if ( sp.inService(now) ) {
				rv = sp;
			}
		}

		if ( null == rv ) {
			throw new SchemaException();
		}
		return rv;
	}	
}
