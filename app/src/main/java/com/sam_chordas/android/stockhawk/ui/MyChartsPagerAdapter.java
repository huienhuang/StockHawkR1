package com.sam_chordas.android.stockhawk.ui;

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Rect;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.db.chart.listener.OnEntryClickListener;
import com.db.chart.model.LineSet;
import com.db.chart.model.Point;
import com.sam_chordas.android.stockhawk.R;
import com.sam_chordas.android.stockhawk.data.QuoteExDatabase;
import com.sam_chordas.android.stockhawk.data.QuoteExProvider;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by pk on 5/24/2016.
 */

public class MyChartsPagerAdapter extends FragmentStatePagerAdapter implements ViewPager.OnPageChangeListener {
    Context mContext;
    Cursor mCursor;

    public MyChartsPagerAdapter(Context context, FragmentManager fm) {
        super(fm);
        mContext = context;
    }

    @Override
    public Fragment getItem(int i) {
        if(i >= getCount()) return null;
        if(!mCursor.moveToPosition(i)) return null;

        Fragment fragment = new ObjectFragment();
        Bundle args = new Bundle();
        args.putLong(QuoteExDatabase.QuoteColumns.UPDATED_TS,
                mCursor.getLong(mCursor.getColumnIndex(QuoteExDatabase.QuoteColumns.UPDATED_TS)));
        args.putString(QuoteExDatabase.QuoteColumns.BIDPRICE,
                mCursor.getString(mCursor.getColumnIndex(QuoteExDatabase.QuoteColumns.BIDPRICE)));
        args.putString(QuoteExDatabase.QuoteColumns.CHANGE,
                mCursor.getString(mCursor.getColumnIndex(QuoteExDatabase.QuoteColumns.CHANGE)));
        args.putString(QuoteExDatabase.QuoteColumns.PERCENT_CHANGE,
                mCursor.getString(mCursor.getColumnIndex(QuoteExDatabase.QuoteColumns.PERCENT_CHANGE)));
        args.putString(QuoteExDatabase.QuoteColumns.SYMBOL,
                mCursor.getString(mCursor.getColumnIndex(QuoteExDatabase.QuoteColumns.SYMBOL)));

        fragment.setArguments(args);
        return fragment;
    }

    public Cursor swapCursor(Cursor newCursor) {
        Cursor old = mCursor;
        mCursor = newCursor;
        notifyDataSetChanged();

        return old;
    }

    @Override
    public int getCount() {
        if(mCursor == null) return 0;
        return mCursor.getCount();
    }

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

    }

    @Override
    public void onPageScrollStateChanged(int state) {

    }

    @Override
    public void onPageSelected(int position) {
        ((Activity)mContext).setTitle(getPageTitle(position));
    }

    @Override
    public CharSequence getPageTitle(int position) {
        if(mCursor == null || !mCursor.moveToPosition(position)) return null;
        return mCursor.getString(mCursor.getColumnIndex(QuoteExDatabase.QuoteColumns.SYMBOL));
    }


    public static class ObjectFragment extends Fragment {

        static class TimeInterval {
            int mInterval;
            int mFactor;
            String mPostfix;

            TimeInterval(int interval, int factor, String postfix) {
                mInterval = interval;
                mFactor = factor;
                mPostfix = postfix;
            }
        }
        final static TimeInterval[] sTimeIntervals = new TimeInterval[] {
                new TimeInterval(300, 5, "M"),
                new TimeInterval(3600, 1, "H"),
                new TimeInterval(3600 * 24, 1, "D"),
        };


        TextView mTextViewTooltip;
        MyLineChartView mLineChartView;
        int mTimeIdx;


        @Override
        public View onCreateView(LayoutInflater inflater,
                                 ViewGroup container, Bundle savedInstanceState) {
            //Log.i("PKT", "onCreateView");
            View rootView = inflater.inflate(R.layout.line_chart, container, false);

            Bundle args = getArguments();
            mLineChartView = (MyLineChartView)rootView.findViewById(R.id.linechart);
            mTextViewTooltip = (TextView)rootView.findViewById(R.id.textView_tooltip);

            InitChart(args);

            return rootView;
        }

        private int getTimeIdx(long ts) {
            return (int)(ts / 1000) / sTimeIntervals[mTimeIdx].mInterval;
        }

        private void updateChart(Bundle args) {
            TimeInterval ti = sTimeIntervals[mTimeIdx];

            int offset;
            long ts;

            MyPoint[] timelines = new MyPoint[20];

            int intval = QuoteExProvider.HISTORY_INTERVAL;
            int start_idx = getTimeIdx(new Date().getTime());

            String symbol = args.getString(QuoteExDatabase.QuoteColumns.SYMBOL);
            String bidprice = args.getString(QuoteExDatabase.QuoteColumns.BIDPRICE);
            String change = args.getString(QuoteExDatabase.QuoteColumns.CHANGE);
            String percent = args.getString(QuoteExDatabase.QuoteColumns.PERCENT_CHANGE);
            ts = args.getLong(QuoteExDatabase.QuoteColumns.UPDATED_TS);

            int cur_idx = getTimeIdx(ts);
            offset = start_idx - cur_idx;
            if(offset >= 0 && offset < 20)
                timelines[offset] = new MyPoint(offset * ti.mFactor + ti.mPostfix, bidprice, change, percent, ts);

            Cursor cursor = getActivity().getContentResolver().query(
                    QuoteExProvider.getUri_HistoryBySymbol(symbol),
                    null,
                    null, null,
                    "_id desc"
            );
            if(cursor != null) {
                try {
                    while (cursor.moveToNext()) {
                        //DatabaseUtils.dumpCursor(cursor);

                        ts = cursor.getLong(cursor.getColumnIndex(QuoteExDatabase.QuoteColumns.UPDATED_TS));
                        offset = start_idx - getTimeIdx(ts);
                        //Log.i("PKT", ">>>>>" + offset + "|" + getTimeIdx(ts));
                        MyPoint pt = new MyPoint(
                                offset * ti.mFactor + ti.mPostfix,
                                cursor.getString(cursor.getColumnIndex(QuoteExDatabase.QuoteColumns.BIDPRICE)),
                                cursor.getString(cursor.getColumnIndex(QuoteExDatabase.QuoteColumns.CHANGE)),
                                cursor.getString(cursor.getColumnIndex(QuoteExDatabase.QuoteColumns.PERCENT_CHANGE)),
                                ts
                        );

                        if (offset >= 0 && offset < 20)
                            if (timelines[offset] == null)
                                timelines[offset] = pt;
                            else if (offset >= 20)
                                break;
                    }
                } finally {
                    cursor.close();
                }
            }

            LineSet dataset = new LineSet();
            dataset.setColor(Color.parseColor("#51b11d")).setThickness(4);

            for(int i = timelines.length - 1; i >= 0; i--) {
                MyPoint pt = timelines[i];
                if(pt == null) {
                    pt = timelines[i] = new MyPoint(
                            i * ti.mFactor + ti.mPostfix,
                            "0.0",
                            "0.0",
                            "0.0",
                            0
                    );
                    pt.setColor(Color.parseColor("#ff0000"));
                } else
                    pt.setColor(Color.parseColor("#3847bb"));

                dataset.addPoint(pt);
            }

            mLineChartView.getData().clear();
            mLineChartView.addData(dataset);
            mLineChartView.setStep(50);

            mLineChartView.notifyDataUpdate();
            mLineChartView.show();
        }

        private void InitChart(final Bundle args) {

            mLineChartView.setOnEntryClickListener(new OnEntryClickListener(){
                @Override
                public void onClick(int setIndex, int entryIndex, Rect entryRect) {
                    MyPoint pt = (MyPoint)mLineChartView.getData().get(setIndex).getEntry(entryIndex);

                    if(pt.mUpdatedTs == 0) {
                        mTextViewTooltip.setText("No Data Recorded");
                    } else {
                        Date dt = new Date();
                        dt.setTime(pt.mUpdatedTs);
                        mTextViewTooltip.setText(
                                String.format("$%.2f - %s",
                                        pt.getValue(),
                                        new SimpleDateFormat("MMM d, HH:mm").format(dt)
                                )
                        );
                    }
                }
            });


            mLineChartView.setOnScale(new MyLineChartView.OnScaleListener() {
                @Override
                public void OnScale(float factor) {
                    if(factor < 1) {
                        if(mTimeIdx + 1 < sTimeIntervals.length) {
                            mTimeIdx++;
                            updateChart(args);
                        }
                    } else if(factor > 1) {
                        if(mTimeIdx - 1 >= 0) {
                            mTimeIdx--;
                            updateChart(args);
                        }
                    }

                    //Log.i("PKT", factor + "");
                }
            });




            updateChart(args);
        }

        static class MyPoint extends Point {

            String mChange;
            String mPercent;
            long mUpdatedTs;

            MyPoint(String label, String bidprice, String change, String percent, long updated_ts) {
                super(label, Float.parseFloat(bidprice));

                mChange = change;
                mPercent = percent;
                mUpdatedTs = updated_ts;

            }

        }

    }


}
