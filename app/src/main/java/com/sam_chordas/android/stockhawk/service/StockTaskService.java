package com.sam_chordas.android.stockhawk.service;

import android.content.ContentProviderResult;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.gcm.GcmNetworkManager;
import com.google.android.gms.gcm.GcmTaskService;
import com.google.android.gms.gcm.TaskParams;
import com.sam_chordas.android.stockhawk.R;
import com.sam_chordas.android.stockhawk.data.QuoteExDatabase;
import com.sam_chordas.android.stockhawk.data.QuoteExProvider;
import com.sam_chordas.android.stockhawk.rest.Utils;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.atomic.AtomicLong;

import okhttp3.FormBody;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.RequestBody;

/**
 * Created by sam_chordas on 9/30/15.
 * The GCMTask service is primarily for periodic tasks. However, OnRunTask can be called directly
 * and is used for the initialization and adding task as well.
 */
public class StockTaskService extends GcmTaskService {
  private String LOG_TAG = StockTaskService.class.getSimpleName();

  private OkHttpClient client = new OkHttpClient();
  private Context mContext;
  private boolean isUpdate;
  private SharedPreferences mCfg;

  public static final String ACTION_DATA_UPDATED = "com.sam_chordas.android.stockhawk.ACTION_DATA_UPDATED";


  public static void notifyDataUpdated(Context context) {
    Intent dataUpdatedIntent = new Intent(ACTION_DATA_UPDATED)
            .setPackage(context.getPackageName());
    context.sendBroadcast(dataUpdatedIntent);
  }

  public StockTaskService(){
    this(null);
  }

  public StockTaskService(Context context){
    mContext = context == null ? this : context;
  }


  String fetchSymbols(ArrayList<String> symbols) throws IOException{

    HttpUrl url = HttpUrl.parse("https://query.yahooapis.com/v1/public/yql").newBuilder()
            //.addQueryParameter("diagnostics", "true")
            .addQueryParameter("format", "json")
            .addQueryParameter("env", "store://datatables.org/alltableswithkeys")
            .addQueryParameter("q", "select symbol,Volume,Change,Bid,ChangeinPercent from yahoo.finance.quotes where symbol in (\""
                    + TextUtils.join("\",\"", symbols)
                    +"\")")
            .build();

    Request request = new Request.Builder()
            .url(url)
            .build();

    //Log.i("PKT", url.toString());
    Response response = client.newCall(request).execute();
    String res = response.body().string();
    //Log.i("PKT", "___" + res);
    return res;
  }


  private ArrayList<String> getSymbols(TaskParams params) {
    ArrayList<String> symbols = new ArrayList<String>();

    String act = params.getTag();
    if(act.equals("init")) {
      symbols.add("YHOO");
      symbols.add("AAPL");
      symbols.add("GOOG");
      symbols.add("MSFT");
      isUpdate = false;

    } else if (act.equals("add")) {
      isUpdate = false;
      symbols.add(params.getExtras().getString("symbol"));

    } else if(act.equals("periodic")) {
      isUpdate = true;
      Cursor cursor = mContext.getContentResolver().query(QuoteExProvider.getUri_Quotes(),
              new String[] { "Distinct " + QuoteExDatabase.QuoteColumns.SYMBOL }, null,
              null, null);

      if(cursor != null) {
        try{
          while (cursor.moveToNext()) {
            symbols.add(cursor.getString(cursor.getColumnIndex("symbol")));
          }
        } finally {
          cursor.close();
        }
      }

    } else {
      throw new IllegalArgumentException("Unknown TaskParams: " + params.getTag());
    }

    return symbols;
  }

  @Override
  public int onRunTask(TaskParams params){
    int ret = GcmNetworkManager.RESULT_FAILURE;
    int count = 0;

    try {
      ArrayList<String> symbols = getSymbols(params);
      if(symbols != null && symbols.size() > 0) {
        String getResponse = fetchSymbols(symbols);
        ContentProviderResult results[] = mContext.getContentResolver().applyBatch(QuoteExProvider.AUTHORITY,
                Utils.quoteJsonToContentVals(getResponse, isUpdate));
        if(results != null) {
          for(ContentProviderResult result: results) {
            if(result.count == null)
              count += result.uri != null ? 1 : 0;
            else
              count += result.count > 0 ? 1 : 0;
          }
        }

      }

      if(isUpdate) {
        if(mCfg == null)
          mCfg = PreferenceManager.getDefaultSharedPreferences(mContext);
        mCfg.edit().putLong("LastUpdateTS", new Date().getTime()).commit();
      }
      ret = GcmNetworkManager.RESULT_SUCCESS;

      if(count > 0)
        notifyDataUpdated(mContext);

    }catch (Exception e){
      e.printStackTrace();
      //Log.e(LOG_TAG, "Error >>", e);

      if(!isUpdate)
        showMsg(mContext.getApplicationContext(), mContext.getString(R.string.symbol_error));

    }
    return ret;
  }


  void showMsg(final Context context, final String msg) {

    new Handler(Looper.getMainLooper()).post(new Runnable() {
      @Override
      public void run() {
        Toast.makeText(context, msg, Toast.LENGTH_LONG).show();
        //Log.i("PKT", "Toast");
      }
    });

  }

}
