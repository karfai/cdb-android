package ca.strangeware.cdb;

import android.content.Intent;
import android.database.Cursor;
import ca.strangeware.cdb.Schema.SchemaException;
import ca.strangeware.cdb.Schema.Stop;

public class AtStop extends TravelActivity {
	private Stop stop = null;
	
	@Override
	protected String[] columnNames() {
		return new String[] { "bus", "headsign" };
	}

	@Override
	protected Intent intent(long id) {
		Intent i = new Intent(this, OnBus.class);
		i.putExtra(ActivityIds.INTENT_KEY_PICKUP_ID, id);
		return i;
	}

	@Override
	protected Cursor query() throws SchemaException {
		return this.stop.upcoming_pickups();
	}

	@Override
	protected void updateObjects() {
		if ( null == this.stop ) {
			long id = getIntent().getLongExtra(ActivityIds.INTENT_KEY_STOP_ID, 0);
			try {
				this.stop = Schema.instance().lookupStop(id);
			} catch (SchemaException e) {
			} catch ( Exception e ) {
				e.printStackTrace();
			}
		}
	}

	@Override
	protected String title() {
		return this.stop.number + " " + this.stop.name;
	}

	@Override
	protected int arrivalIndex() {
		return 1;
	}
}
