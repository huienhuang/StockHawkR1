package com.sam_chordas.android.stockhawk.ui;

import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Binder;
import android.preference.PreferenceManager;
import android.text.format.DateUtils;
import android.util.Log;
import android.widget.AdapterView;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import com.sam_chordas.android.stockhawk.R;
import com.sam_chordas.android.stockhawk.data.QuoteExDatabase;
import com.sam_chordas.android.stockhawk.data.QuoteExProvider;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by pk on 5/24/2016.
 */
public class AppWidgetStockService extends RemoteViewsService {

    static final String TAG = "PKT"; //AppWidgetStock.class.getSimpleName();

    @Override
    public RemoteViewsFactory onGetViewFactory(Intent intent) {
        return new RemoteViewsFactory() {
            Cursor mCursor;

            @Override
            public void onCreate() {
            }

            @Override
            public int getCount() {
                return mCursor == null ? 0 : mCursor.getCount();
            }

            @Override
            public void onDataSetChanged() {
                //Log.i(TAG, "RemoteViewsFactory -> onDataSetChanged");
                if(mCursor != null) mCursor.close();

                //Log.i("PKT", "onDataSetChanged" + android.os.Process.myPid());
                final long identityToken = Binder.clearCallingIdentity();
                mCursor = getContentResolver().query(QuoteExProvider.getUri_Quotes(),
                        new String[]{
                                QuoteExDatabase.QuoteColumns._ID,
                                QuoteExDatabase.QuoteColumns.SYMBOL,
                                QuoteExDatabase.QuoteColumns.BIDPRICE,
                                QuoteExDatabase.QuoteColumns.PERCENT_CHANGE,
                                QuoteExDatabase.QuoteColumns.CHANGE,
                                QuoteExDatabase.QuoteColumns.UPDATED_TS
                        },
                        null,
                        null,
                        null
                );
                Binder.restoreCallingIdentity(identityToken);

            }

            @Override
            public int getViewTypeCount() {
                return 1;
            }

            @Override
            public RemoteViews getViewAt(int position) {
                if (position == AdapterView.INVALID_POSITION ||
                        mCursor == null || !mCursor.moveToPosition(position)) {
                    return null;
                }

                //Log.i("PKT", "getViewAt" + android.os.Process.myPid());
                RemoteViews views = new RemoteViews(getPackageName(), R.layout.list_item_quote);

                views.setTextViewText(R.id.stock_symbol,
                        mCursor.getString(mCursor.getColumnIndex(QuoteExDatabase.QuoteColumns.SYMBOL)));
                views.setTextViewText(R.id.bid_price,
                        mCursor.getString(mCursor.getColumnIndex(QuoteExDatabase.QuoteColumns.BIDPRICE)));

                String change = mCursor.getString(mCursor.getColumnIndex(QuoteExDatabase.QuoteColumns.CHANGE));
                views.setTextViewText(R.id.change, change);
                if(Float.parseFloat(change) < 0) {
                    //views.setTextColor(R.id.change, getResources().getColor(R.color.material_red_700));
                    views.setInt(R.id.change, "setBackgroundColor", getResources().getColor(R.color.material_red_700));

                } else {
                    //views.setTextColor(R.id.change, getResources().getColor(R.color.material_green_700));
                    views.setInt(R.id.change, "setBackgroundColor", getResources().getColor(R.color.material_green_700));
                }

                Intent fillInIntent = new Intent();
                fillInIntent.putExtra("position", position);
                views.setOnClickFillInIntent(R.id.list_item_quote, fillInIntent);

                return views;
            }

            @Override
            public long getItemId(int position) {
                if(mCursor != null && mCursor.move(position))
                    return (int)mCursor.getInt(
                            mCursor.getColumnIndex(QuoteExDatabase.QuoteColumns._ID));

                return -1;
            }

            @Override
            public void onDestroy() {
                if(mCursor != null) {
                    mCursor.close();
                    mCursor = null;
                }
            }

            @Override
            public RemoteViews getLoadingView() {
                return null;
            }

            @Override
            public boolean hasStableIds() {
                return true;
            }


        };
    }


}
