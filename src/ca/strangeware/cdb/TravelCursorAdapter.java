package ca.strangeware.cdb;

import android.content.Context;
import android.database.Cursor;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

public class TravelCursorAdapter extends SimpleCursorAdapter {
	private Cursor cr;
	private int arrival_index;

	public TravelCursorAdapter(Context c, int layout, Cursor cr, String[] fr, int[] to, int arrival_index) {
		super(c, layout, cr, fr, to);
		this.cr = cr;
		this.arrival_index = arrival_index;
	}

	@Override
	public View getView(int position, View v, ViewGroup parent) {
		View rv = super.getView(position, v, parent);
		TextView tv = (TextView) rv.findViewById(R.id.tr_desc);
		if ( null != rv ) {
			tv.setText(formatArrival(position));
		}
		return rv;
	}

	private String formatArrival(int position) {
		String rv = "Now";
		this.cr.moveToPosition(position);
		long mins = (this.cr.getInt(this.arrival_index) - Schema.secsElapsedToday()) / 60;
		if ( mins < 0 ) {
			rv = "Expected " + Math.abs(mins) + " minutes ago";
		} else if ( mins > 0 ) {
			rv = "Arrives in " + Math.abs(mins) + " minutes";			
		}
		return rv;
	}
}
