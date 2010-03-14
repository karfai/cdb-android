package ca.strangeware.cdb;

import ca.strangeware.cdb.Schema.SchemaException;
import ca.strangeware.cdb.Schema.Stop;
import android.app.Activity;
import android.app.ListActivity;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.SimpleCursorAdapter;

public class AtStop extends ListActivity {
	private Stop stop;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		long id = getIntent().getLongExtra(ActivityIds.INTENT_KEY_STOP_ID, 0);
		try {
			this.stop = Schema.instance().lookupStop(id);
			this.setTitle("Waiting at " + this.stop.number + " " + this.stop.name);
			populateStops();
		} catch (SchemaException e) {
		} catch ( Exception e ) {
			e.printStackTrace();
		}	
		final Activity	a = this;
		getListView().setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> lv, View v, int pos, long pickup_id) {
				Intent i = new Intent(a, OnBus.class);
				i.putExtra(ActivityIds.INTENT_KEY_PICKUP_ID, pickup_id);
				try {
					a.startActivity(i);
				} catch ( Exception e ) {
					e.printStackTrace();
				}
			}
		});
	}

	private void populateStops() {
		// http://www.helloandroid.com/tutorials/using-threads-and-progressdialog
		try {
			Cursor cr = this.stop.upcoming_pickups();
			startManagingCursor(cr);
			setListAdapter(buildTripsAdapter(cr));
		} catch (SchemaException e) {
			setListAdapter(null);
		}
	}

	private SimpleCursorAdapter buildTripsAdapter(Cursor cr) {
		String[] fr = new String[] { "bus", "headsign" };
		int[]    to = new int[] { R.id.tr_number, R.id.tr_name };
		return new SimpleCursorAdapter(this, R.layout.travel_row, cr, fr, to);
	}		
}
