package ca.strangeware.cdb;

import ca.strangeware.cdb.Schema.PickUp;
import ca.strangeware.cdb.Schema.SchemaException;
import ca.strangeware.cdb.Schema.Trip;
import android.app.Activity;
import android.app.ListActivity;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.SimpleCursorAdapter;

public class OnBus extends ListActivity {
	private Trip trip;
	private PickUp pickup;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		long pickup_id = getIntent().getLongExtra(ActivityIds.INTENT_KEY_PICKUP_ID, 0);
		try {
			this.pickup = Schema.instance().lookupPickUp(pickup_id);
			this.trip = Schema.instance().lookupTrip(pickup.trip_id);
			this.setTitle("Riding " + this.trip.number + " " + this.trip.headsign);
			populateTrips();
		} catch (SchemaException e) {
			e.printStackTrace();
		} catch ( Exception e ) {
			e.printStackTrace();
		}		
		final Activity	a = this;
		getListView().setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> lv, View v, int pos, long stop_id) {
				Intent i = new Intent(a, AtStop.class);
				i.putExtra(ActivityIds.INTENT_KEY_STOP_ID, stop_id);
				try {
					a.startActivity(i);
				} catch ( Exception e ) {
					e.printStackTrace();
				}
			}
		});
	}

	private void populateTrips() {
		// http://www.helloandroid.com/tutorials/using-threads-and-progressdialog
		try {
			Cursor cr = this.trip.upcomingStopsFromSeq(this.pickup.sequence);
			startManagingCursor(cr);
			setListAdapter(buildAdapter(cr));
		} catch (SchemaException e) {
			setListAdapter(null);
		}
	}

	private SimpleCursorAdapter buildAdapter(Cursor cr) {
		String[] fr = new String[] { "number", "name" };
		int[]    to = new int[] { R.id.tr_number, R.id.tr_name };
		return new SimpleCursorAdapter(this, R.layout.travel_row, cr, fr, to);
	}		
}
