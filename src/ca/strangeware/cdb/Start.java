package ca.strangeware.cdb;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;

public class Start extends Activity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        setContentView(R.layout.main);
        setupListeners();
   }

	private void setupListeners() {
		final Activity a = this;
		getSearchButton().setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				populateStops();
			}
		});
		getStopsView().setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> lv, View v, int position, long id) {
				Intent i = new Intent(a, AtStop.class);
				i.putExtra(ActivityIds.INTENT_KEY_STOP_ID, id);
				try {
					a.startActivity(i);
				} catch ( Exception e ) {
					e.printStackTrace();
				}
			}
		});
	}

	private View getSearchButton() {
		return this.findViewById(R.id.find);
	}

	private ListView getStopsView() {
		return (ListView) this.findViewById(R.id.stops);
	}
	
	private String getSearchText() {
		return ((EditText) this.findViewById(R.id.search)).getText().toString();
	}
	
	private void populateStops() {
		try {
	        Cursor cr = Schema.instance().stopViaSearch(getSearchText());
	        startManagingCursor(cr);
	        getStopsView().setAdapter(buildStopsAdapter(cr));
		} catch ( Schema.SchemaException e ) {
			
		}
	}

	private SimpleCursorAdapter buildStopsAdapter(Cursor cr) {
		String[] fr = new String[] { "number", "name" };
		int[]    to = new int[] { R.id.number, R.id.name };
		return new SimpleCursorAdapter(this, R.layout.stops_row, cr, fr, to);
	}		
}