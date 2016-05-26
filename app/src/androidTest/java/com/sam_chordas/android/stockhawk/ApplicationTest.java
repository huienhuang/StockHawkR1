package com.sam_chordas.android.stockhawk;

import android.app.Application;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.DataSetObserver;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.provider.Settings;
import android.test.ApplicationTestCase;
import android.util.Log;

import com.sam_chordas.android.stockhawk.data.QuoteExDatabase;
import com.sam_chordas.android.stockhawk.data.QuoteExProvider;

import java.util.Date;

/**
 * <a href="http://d.android.com/tools/testing/testing_android.html">Testing Fundamentals</a>
 */
public class ApplicationTest extends ApplicationTestCase<Application> {

  int x = 0;

  public ApplicationTest() {
    super(Application.class);

    //getContext()



  }

  public void testDB() {

    Cursor cursor = getContext().getContentResolver().query(QuoteExProvider.getUri_Quotes(), null, null, null, null);
    cursor.registerDataSetObserver(new NotifyingDataSetObserver());
    while(cursor.moveToNext()) {
      Log.d("PKT", String.format(
              "%s | %s | %s | %s | %s | %s",
              cursor.getString(0),
              cursor.getString(1),
              cursor.getString(2),
              cursor.getString(3),
              cursor.getString(4),
              cursor.getString(5)
      ));
    }

    SQLiteDatabase db = QuoteExDatabase.getInstance(getContext()).getWritableDatabase();

    db.beginTransaction();

    cursor = db.rawQuery("update quotes set bid_price=300 where _id=18", null);
    cursor.getCount();

    db.beginTransaction();
    cursor = db.rawQuery("update quotes set bid_price=400 where _id=25", null);
    cursor.getCount();
    db.setTransactionSuccessful();

    db.endTransaction();

    db.setTransactionSuccessful();
    db.endTransaction();
    /*

    cursor.moveToPosition(-1);
    getContext().getContentResolver().notifyChange(QuoteExProvider.getUri_Quotes(), null);

    ContentValues val = new ContentValues();
    long ts = new Date().getTime();
    val.put(QuoteExDatabase.QuoteColumns.SYMBOL, "GOOG");
    val.put(QuoteExDatabase.QuoteColumns.UPDATED_TS, ts);
    val.put(QuoteExDatabase.QuoteColumns.BIDPRICE, "178");
    val.put(QuoteExDatabase.QuoteColumns.CHANGE, "2.899");
    val.put(QuoteExDatabase.QuoteColumns.PERCENT_CHANGE, "1.99%");
    int count = getContext().getContentResolver().update(QuoteExProvider.getUri_Quotes(),
            val, null, null
    );
    Log.d("PKT", "Count: " + count + "|" + ts);

    getContext().getContentResolver().delete(QuoteExProvider.getUri_QuoteBySymbol("GOOG"), null, null);

    cursor = getContext().getContentResolver().query(QuoteExProvider.getUri_HistoryBySymbol("GOOG"), null, null, null, null);
    while(cursor != null && cursor.moveToNext()) {
      Log.d("PKT", String.format(
              "%s | %s | %s | %s | %s | %s",
              cursor.getString(0),
              cursor.getString(1),
              cursor.getString(2),
              cursor.getString(3),
              cursor.getString(4),
              cursor.getString(5)
      ));
    }

    Log.d("PKT", "x: " + x);

    */
  }

  private class NotifyingDataSetObserver extends DataSetObserver {
    @Override public void onChanged() {
      Log.d("PKT", ">>>>>>>>>>onChanged");
      super.onChanged();

      x++;
    }

    @Override public void onInvalidated() {
      Log.d("PKT", ">>>>>>>>>>onInvalidated");
      super.onInvalidated();
      x++;
    }
  }

}