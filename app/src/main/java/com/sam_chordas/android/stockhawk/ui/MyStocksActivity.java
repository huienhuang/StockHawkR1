package com.sam_chordas.android.stockhawk.ui;

import android.app.LoaderManager;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.ActionBar;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.text.InputType;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import com.afollestad.materialdialogs.MaterialDialog;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.sam_chordas.android.stockhawk.R;
import com.sam_chordas.android.stockhawk.data.QuoteExDatabase;
import com.sam_chordas.android.stockhawk.data.QuoteExProvider;
import com.sam_chordas.android.stockhawk.rest.QuoteCursorAdapter;
import com.sam_chordas.android.stockhawk.rest.RecyclerViewItemClickListener;
import com.sam_chordas.android.stockhawk.rest.Utils;
import com.sam_chordas.android.stockhawk.service.StockIntentService;
import com.sam_chordas.android.stockhawk.service.StockTaskService;
import com.google.android.gms.gcm.GcmNetworkManager;
import com.google.android.gms.gcm.PeriodicTask;
import com.google.android.gms.gcm.Task;
import com.melnykov.fab.FloatingActionButton;
import com.sam_chordas.android.stockhawk.touch_helper.SimpleItemTouchHelperCallback;

import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

public class MyStocksActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor>{

  /**
   * Fragment managing the behaviors, interactions and presentation of the navigation drawer.
   */

  /**
   * Used to store the last screen title. For use in {@link #restoreActionBar()}.
   */
  private CharSequence mTitle;
  private Intent mServiceIntent;
  private ItemTouchHelper mItemTouchHelper;
  private static final int CURSOR_LOADER_ID = 0;
  private QuoteCursorAdapter mCursorAdapter;
  private Context mContext;
  private Cursor mCursor;
  private Timer mTimer;
  private Handler mHandler;
  private SharedPreferences mCfg;
  private TextView mTextViewSymbolListEmpty;
  private TextView mTextViewDataStatus;
  private DataStatus mDataStatus = DataStatus.Normal;

  enum DataStatus {
    Normal,
    NotUpToDate,
    NetworkDown,
  };

  static final int REFRESH_RATE_MS = 3000;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_my_stocks);

    mContext = this;
    mCfg = PreferenceManager.getDefaultSharedPreferences(mContext);
    mHandler = new Handler();

    mServiceIntent = new Intent(this, StockIntentService.class);
    mServiceIntent.putExtra("tag", "periodic");

    mTextViewDataStatus = (TextView)findViewById(R.id.textView_data_status);

    mTextViewSymbolListEmpty = (TextView)findViewById(R.id.textView_symbol_list_empty);
    RecyclerView recyclerView = (RecyclerView) findViewById(R.id.recycler_view);
    recyclerView.setLayoutManager(new LinearLayoutManager(this));
    getLoaderManager().initLoader(CURSOR_LOADER_ID, null, this);

    mCursorAdapter = new QuoteCursorAdapter(this, null);
    recyclerView.addOnItemTouchListener(new RecyclerViewItemClickListener(this,
            new RecyclerViewItemClickListener.OnItemClickListener() {
              @Override public void onItemClick(View v, int position) {
                //TODO:
                // do something on item click

                Intent intent = new Intent(mContext, MyChartActivity.class);
                intent.putExtra("position", position);
                startActivity(intent);
              }
            }));
    recyclerView.setAdapter(mCursorAdapter);


    FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
    fab.attachToRecyclerView(recyclerView);
    //fab.hide();
    fab.setOnClickListener(new View.OnClickListener() {
      @Override public void onClick(View v) {
        if (mDataStatus != DataStatus.NetworkDown){
          new MaterialDialog.Builder(mContext).title(R.string.symbol_search)
              .content(R.string.content_test)
              .inputType(InputType.TYPE_CLASS_TEXT)
              .input(R.string.input_hint, R.string.input_prefill, new MaterialDialog.InputCallback() {
                @Override public void onInput(MaterialDialog dialog, CharSequence input) {
                  // On FAB click, receive user input. Make sure the stock doesn't already exist
                  // in the DB and proceed accordingly

                  //Symbol Must be In UpperCase, Due to the Yahoo API Bug
                  Cursor c = getContentResolver().query(
                          QuoteExProvider.getUri_Quotes(),
                          new String[] { QuoteExDatabase.QuoteColumns.SYMBOL },
                          QuoteExDatabase.QuoteColumns.SYMBOL + "= ?",
                          new String[] { input.toString().toUpperCase() }, null);

                    try {
                      if (c != null && c.getCount() != 0) {
                        Toast toast =
                                Toast.makeText(MyStocksActivity.this, getString(R.string.symbol_exists),
                                        Toast.LENGTH_LONG);
                        toast.setGravity(Gravity.CENTER, Gravity.CENTER, 0);
                        toast.show();

                      } else {
                        // Add the stock to DB
                        if (input.toString().matches("[^\"']+")) {
                          mServiceIntent.putExtra("tag", "add");
                          mServiceIntent.putExtra("symbol", input.toString());
                          startService(mServiceIntent);
                        } else {
                          Toast.makeText(MyStocksActivity.this, getString(R.string.invalid_input),
                                  Toast.LENGTH_LONG).show();

                        }

                      }
                    } finally {
                      if(c != null)
                        c.close();
                    }

                }
              })
              .show();
        } else {
          networkToast();
        }

      }
    });

    ItemTouchHelper.Callback callback = new SimpleItemTouchHelperCallback(mCursorAdapter);
    mItemTouchHelper = new ItemTouchHelper(callback);
    mItemTouchHelper.attachToRecyclerView(recyclerView);

    mTitle = getTitle();


    if(GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(mContext) == ConnectionResult.SUCCESS) {
      long period = QuoteExProvider.HISTORY_INTERVAL;
      long flex = 50L;
      String periodicTag = "periodic";

      PeriodicTask periodicTask = new PeriodicTask.Builder()
              .setService(StockTaskService.class)
              .setPeriod(period)
              //.setPersisted(true)
              .setFlex(flex)
              .setTag(periodicTag).setUpdateCurrent(true)
              .setRequiredNetwork(Task.NETWORK_STATE_CONNECTED)
              .setRequiresCharging(false)
              .build();
      GcmNetworkManager.getInstance(this).schedule(periodicTask);
    } else {
      Toast.makeText(this, getString(R.string.google_play_service_error), Toast.LENGTH_LONG).show();
    }

  }

  private void updateDataStatus(DataStatus s) {
    mDataStatus = s;
    if(mDataStatus == DataStatus.Normal) {
      mTextViewDataStatus.setVisibility(View.GONE);

    } else if(mDataStatus == DataStatus.NotUpToDate) {
      mTextViewDataStatus.setVisibility(View.VISIBLE);
      mTextViewDataStatus.setText(getString(R.string.data_outdate));

    } else if(mDataStatus == DataStatus.NetworkDown) {
      mTextViewDataStatus.setVisibility(View.VISIBLE);
      mTextViewDataStatus.setText(getString(R.string.network_lost));
    }

  }

  class UpdateTask extends TimerTask {
    ConnectivityManager mCM;
    Intent mIntent;

    public UpdateTask() {
      mCM = (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
      mIntent = new Intent(mContext, StockIntentService.class);
      //mIntent.putExtra("tag", "periodic");
    }

    @Override
    public void run() {
      NetworkInfo activeNetwork = mCM.getActiveNetworkInfo();
      boolean isConnected = activeNetwork != null && activeNetwork.isConnectedOrConnecting();

      final DataStatus s;
      if(!isConnected) {
        s = DataStatus.NetworkDown;
      } else if((new Date().getTime() - mCfg.getLong("LastUpdateTS", 0L)) > REFRESH_RATE_MS * 2) {
        s = DataStatus.NotUpToDate;
      } else {
        s = DataStatus.Normal;
      }

      if(s != DataStatus.NetworkDown && !StockIntentService.sIsWorking) {
        if(mCfg.getBoolean("symbols_init", false))
          mIntent.putExtra("tag", "periodic");
        else {
          mIntent.putExtra("tag", "init");
          mCfg.edit().putBoolean("symbols_init", true).commit();
        }
        startService(mIntent);
      }
      if(s != mDataStatus) {
        mHandler.post(new Runnable() {
          @Override
          public void run() {
            updateDataStatus(s);
          }
        });
      }
    }
  }

  private void setupTimer() {
    if(mTimer != null) { mTimer.cancel(); mTimer = null; }

    mTimer = new Timer();
    mTimer.scheduleAtFixedRate(new UpdateTask(), 0, REFRESH_RATE_MS);
  }

  @Override
  public void onResume() {
    super.onResume();
    getLoaderManager().restartLoader(CURSOR_LOADER_ID, null, this);

    setupTimer();

  }

  @Override
  protected void onPause() {
    super.onPause();

    mTimer.cancel();
    mTimer = null;
  }

  public void networkToast(){
    Toast.makeText(mContext, getString(R.string.network_toast), Toast.LENGTH_SHORT).show();
  }

  public void restoreActionBar() {
    ActionBar actionBar = getSupportActionBar();
    actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
    actionBar.setDisplayShowTitleEnabled(true);
    actionBar.setTitle(mTitle);
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
      getMenuInflater().inflate(R.menu.my_stocks, menu);
      restoreActionBar();
      return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    // Handle action bar item clicks here. The action bar will
    // automatically handle clicks on the Home/Up button, so long
    // as you specify a parent activity in AndroidManifest.xml.
    int id = item.getItemId();

    //noinspection SimplifiableIfStatement
    if (id == R.id.action_settings) {
      return true;
    }

    if (id == R.id.action_change_units){
      // this is for changing stock changes from percent value to dollar value
      Utils.showPercent = !Utils.showPercent;
      this.getContentResolver().notifyChange(QuoteExProvider.getUri_Quotes(), null);
    }

    return super.onOptionsItemSelected(item);
  }

  @Override
  public Loader<Cursor> onCreateLoader(int id, Bundle args){
    // This narrows the return to only the stocks that are most current.
    return new CursorLoader(this, QuoteExProvider.getUri_Quotes(),
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
        null);
  }

  @Override
  public void onLoadFinished(Loader<Cursor> loader, Cursor data){
    mTextViewSymbolListEmpty.setVisibility(data == null || data.getCount() <= 0 ? View.VISIBLE : View.GONE);
    mCursor = data;
    mCursorAdapter.swapCursor(data);
  }

  @Override
  public void onLoaderReset(Loader<Cursor> loader){
    mCursorAdapter.swapCursor(null);
  }

}
