package ca.strangeware.cdb;

import ca.strangeware.cdb.Schema.SchemaException;
import android.app.Activity;
import android.app.ListActivity;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.AdapterView;
import android.widget.SimpleCursorAdapter;

public abstract class TravelActivity extends ListActivity {
	private Handler handler = new Handler();
	private long 	startTime = 0;

	private class Update implements Runnable {
		private TravelActivity act;
		public Update(TravelActivity act) {
			this.act = act;
		}
		
		public void run() {
			this.act.populate();
		}
	}

	protected abstract Cursor	query() throws SchemaException;
	protected abstract Intent	intent(long id);
	protected abstract String[] columnNames();
	protected abstract String	title();
	protected abstract void		updateObjects();
	protected abstract int		arrivalIndex();
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.startTime = System.currentTimeMillis();
		final Activity a = this;
		getListView().setOnItemClickListener(new AdapterView.OnItemClickListener() {

			public void onItemClick(AdapterView<?> lv, View v, int pos, long pickup_id) {
				try {
					a.startActivity(intent(pickup_id));
				} catch ( Exception e ) {
					e.printStackTrace();
				}
			}
		});
		populate();
	}
	
	protected void populate() {
		// http://www.helloandroid.com/tutorials/using-threads-and-progressdialog
		updateObjects();
		setTitle(title() + formatExpired());
		try {
			Cursor cr = query();
			startManagingCursor(cr);
			setListAdapter(buildTripsAdapter(cr));
		} catch (SchemaException e) {
			setListAdapter(null);
		}
		this.handler.postDelayed(new Update(this), 60000);
	}

	private String formatExpired() {
		long   mins = secondsRunning() / 60;
		String rv = "";
		if ( mins > 0 ) {
			rv = " (" + mins + " minute" + ((mins > 1) ? "s" : "") + ")";
		}
		return rv;
	}
	
	protected long secondsRunning() {
		return (System.currentTimeMillis() - this.startTime) / 1000;
	}
	
	private SimpleCursorAdapter buildTripsAdapter(Cursor cr) {
		String[] fr = columnNames();
		int[]    to = new int[] { R.id.tr_number, R.id.tr_name };
		return new TravelCursorAdapter(this, R.layout.travel_row, cr, fr, to, this.arrivalIndex());
	}		
}