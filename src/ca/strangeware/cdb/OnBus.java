package ca.strangeware.cdb;

import android.content.Intent;
import android.database.Cursor;
import ca.strangeware.cdb.Schema.PickUp;
import ca.strangeware.cdb.Schema.SchemaException;
import ca.strangeware.cdb.Schema.Trip;

public class OnBus extends TravelActivity {
	private Trip	trip = null;
	private PickUp	pickup = null;

	@Override
	protected void updateObjects() {
		try {
			if ( null == this.pickup ) {
				long pickup_id = getIntent().getLongExtra(ActivityIds.INTENT_KEY_PICKUP_ID, 0);
				this.pickup = Schema.instance().lookupPickUp(pickup_id);
				this.trip = Schema.instance().lookupTrip(pickup.trip_id);
			} else {
				// verify that the elapsed time has not put us past when we EXPECTED to get to
				// the next stop
				PickUp next = Schema.instance().lookupNextPickUp(this.pickup.trip_id, this.pickup.sequence);
				if ( this.pickup.arrival + this.secondsRunning() >= next.arrival ) {
					this.pickup = next;
				}
			}
		} catch (SchemaException e) {
			e.printStackTrace();
		} catch ( Exception e ) {
			e.printStackTrace();
		}
	}

	@Override
	protected String[] columnNames() {
		return new String[] { "number", "name" };
	}

	@Override
	protected Intent intent(long id) {
		Intent i = new Intent(this, AtStop.class);
		i.putExtra(ActivityIds.INTENT_KEY_STOP_ID, id);
		return i;
	}

	@Override
	protected Cursor query() throws SchemaException {
		return this.trip.upcomingStopsFromSeq(this.pickup.sequence);
	}

	@Override
	protected String title() {
		return this.trip.number + " " + this.trip.headsign;
	}
	
	@Override
	protected int arrivalIndex() {
		return 3;
	}
}
