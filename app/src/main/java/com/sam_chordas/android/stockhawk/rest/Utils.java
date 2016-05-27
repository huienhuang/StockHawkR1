package com.sam_chordas.android.stockhawk.rest;

import android.content.ContentProviderOperation;
import android.util.Log;
import com.sam_chordas.android.stockhawk.data.QuoteExDatabase;
import com.sam_chordas.android.stockhawk.data.QuoteExProvider;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


/**
 * Created by sam_chordas on 10/8/15.
 */
public class Utils {

  private static String LOG_TAG = Utils.class.getSimpleName();

  public static boolean showPercent = true;

  public static ArrayList quoteJsonToContentVals(String JSON, boolean isUpdate) throws JSONException {
    ArrayList<ContentProviderOperation> batchOperations = new ArrayList<>();
    JSONObject jsonObject = null;
    JSONArray resultsArray = null;

      jsonObject = new JSONObject(JSON);
      if (jsonObject != null && jsonObject.length() != 0){
        jsonObject = jsonObject.getJSONObject("query");
        int count = Integer.parseInt(jsonObject.getString("count"));
        if (count == 1){
          jsonObject = jsonObject.getJSONObject("results")
              .getJSONObject("quote");
          ContentProviderOperation op = buildBatchOperation(jsonObject, isUpdate);
          batchOperations.add(op);
        } else{
          resultsArray = jsonObject.getJSONObject("results").getJSONArray("quote");

          if (resultsArray != null && resultsArray.length() != 0){
            for (int i = 0; i < resultsArray.length(); i++){
              jsonObject = resultsArray.getJSONObject(i);
              ContentProviderOperation op = buildBatchOperation(jsonObject, isUpdate);
              batchOperations.add(op);
            }
          }
        }
      }

    return batchOperations;
  }

  public static String truncateBidPrice(String bidPrice){
    bidPrice = String.format("%.2f", Float.parseFloat(bidPrice));
    return bidPrice;
  }

  public static String truncateChange(String change, boolean isPercentChange){
    if(isPercentChange)
      change = change.substring(0, change.length() - 1);
    double round = Math.round(Double.parseDouble(change) * 100) / 100.0;
    change = String.format("%+.2f%s", round, isPercentChange ? "%" : "");
    return change;
  }

  public static ContentProviderOperation buildBatchOperation(JSONObject jsonObject, boolean isUpdate)
          throws JSONException
  {
    ContentProviderOperation.Builder builder;
    if(isUpdate)
      builder= ContentProviderOperation.newUpdate(QuoteExProvider.getUri_Quotes());
    else
      builder= ContentProviderOperation.newInsert(QuoteExProvider.getUri_Quotes());

    //try {
      if(jsonObject.get("Volume") == JSONObject.NULL) {
        Log.e(LOG_TAG, String.format("symbol [%s] doesn't exist!", jsonObject.getString("symbol")));
        return null;
      }

      //SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");

      String change = jsonObject.getString("Change");

      //Symbol Must be In UpperCase, Due to the Yahoo API Bug
      builder.withValue(QuoteExDatabase.QuoteColumns.SYMBOL, jsonObject.getString("symbol").toUpperCase());
      builder.withValue(QuoteExDatabase.QuoteColumns.BIDPRICE, truncateBidPrice(jsonObject.getString("Bid")));
      //builder.withValue(QuoteExDatabase.QuoteColumns.BIDPRICE, new Random().nextFloat());
      builder.withValue(QuoteExDatabase.QuoteColumns.PERCENT_CHANGE, truncateChange(
          jsonObject.getString("ChangeinPercent"), true));
      builder.withValue(QuoteExDatabase.QuoteColumns.CHANGE, truncateChange(change, false));
      //builder.withValue(QuoteExDatabase.QuoteColumns.ISCURRENT, 1);
      //if (change.charAt(0) == '-'){
      //  builder.withValue(QuoteColumns.ISUP, 0);
      //}else{
      //  builder.withValue(QuoteColumns.ISUP, 1);
      //}

      builder.withValue(QuoteExDatabase.QuoteColumns.UPDATED_TS, new Date().getTime() );

    //} catch (Exception e) {
    //  e.printStackTrace();
    //  return null;
    //}
    return builder.build();
  }
}
