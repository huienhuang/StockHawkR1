package com.sam_chordas.android.stockhawk.ui;

import android.app.Activity;
import android.app.LoaderManager;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.graphics.Color;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.db.chart.listener.OnEntryClickListener;
import com.db.chart.model.LineSet;
import com.db.chart.model.Point;
import com.db.chart.view.LineChartView;
import com.sam_chordas.android.stockhawk.R;
import com.sam_chordas.android.stockhawk.data.QuoteExDatabase;
import com.sam_chordas.android.stockhawk.data.QuoteExProvider;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by pk on 5/19/2016.
 */


public class MyChartActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor> {
    MyChartsPagerAdapter mCollectionPagerAdapter;
    ViewPager mViewPager;
    int curIdx = -1;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_chart);

        //Log.i("PKT", savedInstanceState != null ? "savedInstanceState" : "NON");
        if(savedInstanceState == null) {
            curIdx = getIntent().getIntExtra("position", -1);
        } else {
            curIdx = savedInstanceState.getInt("curIdx");
        }

        mCollectionPagerAdapter = new MyChartsPagerAdapter(this, getSupportFragmentManager());
        mViewPager = (ViewPager) findViewById(R.id.pager);

        getLoaderManager().initLoader(0, null, this);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        //Log.i("PKT", "onNewIntent");

        super.onNewIntent(intent);

        int position = intent.getIntExtra("position", -1);
        if(position < 0) return;

        if(curIdx == -1) {
            mViewPager.setCurrentItem(position);
        } else {
            curIdx = position;
        }

    }

    @Override
    public void onSaveInstanceState(Bundle outState, PersistableBundle outPersistentState) {
        outState.putInt("curIdx", curIdx);
        super.onSaveInstanceState(outState, outPersistentState);
    }

    @Override
    protected void onDestroy() {
        //Log.i("PKT", "onDestroy>");
        mViewPager.removeOnPageChangeListener(mCollectionPagerAdapter);

        super.onDestroy();
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        //Log.i("PKT", "onCreateLoader>" + id);
        return new CursorLoader(this,
                QuoteExProvider.getUri_Quotes(),
                null, null, null, null
        );
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        //Log.i("PKT", "onLoaderReset>" + loader.getId());

        mCollectionPagerAdapter.swapCursor(null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        //Log.i("PKT", "Reload>" + loader.getId());

        mCollectionPagerAdapter.swapCursor(data);
        if(curIdx >= 0) {
            mViewPager.setAdapter(mCollectionPagerAdapter);
            mViewPager.addOnPageChangeListener(mCollectionPagerAdapter);
            mViewPager.setCurrentItem(curIdx);
            mCollectionPagerAdapter.onPageSelected(curIdx);
            curIdx = -1;
        }
    }

}
