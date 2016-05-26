package com.sam_chordas.android.stockhawk.data;

import android.content.ContentProvider;
import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.OperationApplicationException;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.util.Log;

import java.util.ArrayList;


/**
 * Created by pk on 5/16/2016.
 */
public class QuoteExProvider extends ContentProvider {

    //record every 5 mins
    public static final int HISTORY_INTERVAL = 5 * 60;

    public static final String AUTHORITY = "com.sam_chordas.android.stockhawk.data.QuoteProvider";
    public static final Uri BASE_CONTENT_URI = Uri.parse("content://" + AUTHORITY);

    private static final int URI_CODE_QUOTES = 0;
    private static final int URI_CODE_QUOTES_SYMBOL = 1;
    private static final int URI_CODE_QUOTE_HISTORY_QUOTE_SYMBOL = 2;

    private static final UriMatcher MATCHER = new UriMatcher(UriMatcher.NO_MATCH);
    static {
        MATCHER.addURI(AUTHORITY, QuoteExDatabase.TABLE_QUOTE, URI_CODE_QUOTES);
        MATCHER.addURI(AUTHORITY, QuoteExDatabase.TABLE_QUOTE + "/*", URI_CODE_QUOTES_SYMBOL);
        MATCHER.addURI(AUTHORITY, QuoteExDatabase.TABLE_QUOTE_HISTORY + "/*/history", URI_CODE_QUOTE_HISTORY_QUOTE_SYMBOL);
    }

    private SQLiteOpenHelper database;

    public static Uri buildUri(String... paths)
    {
        Uri.Builder builder = BASE_CONTENT_URI.buildUpon();
        for(String path : paths)
            builder.appendPath(path);
        return builder.build();
    }

    public static Uri getUri_Quotes() {
        return buildUri(QuoteExDatabase.TABLE_QUOTE);
    }

    public static Uri getUri_QuoteBySymbol(String Symbol) {
        return buildUri(QuoteExDatabase.TABLE_QUOTE, Symbol);
    }

    public static Uri getUri_HistoryBySymbol(String Symbol) {
        return buildUri(QuoteExDatabase.TABLE_QUOTE_HISTORY, Symbol, "history");
    }


    @Override
    public boolean onCreate() {
        database = QuoteExDatabase.getInstance(getContext());
        return true;
    }

    private long _insert(SQLiteDatabase db, ContentValues values) {
        return db.insert(QuoteExDatabase.TABLE_QUOTE, null, values);
    }

    private Long getIdBySymbol(SQLiteDatabase db, String symbol) {
        long id = -1;
        Cursor cursor = db.query(QuoteExDatabase.TABLE_QUOTE,
                new String[] {QuoteExDatabase.QuoteColumns._ID},
                String.format("%s=?", QuoteExDatabase.QuoteColumns.SYMBOL),
                new String[] {symbol},
                null, null, null
        );
        if(cursor == null) return -1L;
        try {
            if(cursor.moveToFirst())
                id = cursor.getLong(0);
        } finally {
            cursor.close();
        }

        return id;
    }

    private int _delete(SQLiteDatabase db, String symbol) {
        long id = getIdBySymbol(db, symbol);
        if(id < 0) return 0;

        int count = db.delete(QuoteExDatabase.TABLE_QUOTE,
                String.format("%s=?", QuoteExDatabase.QuoteColumns._ID),
                new String[] {id + ""}
        );
        if(count > 0) {
            db.delete(QuoteExDatabase.TABLE_QUOTE_HISTORY,
                    String.format("%s=?", QuoteExDatabase.QuoteColumns.QUOTE_ID),
                    new String[] {id + ""}
            );
        }

        return count;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        SQLiteDatabase db = database.getReadableDatabase();
        Cursor cursor = null;
        switch(MATCHER.match(uri)) {
            case URI_CODE_QUOTES: {
                cursor = db.query(QuoteExDatabase.TABLE_QUOTE,
                        projection,
                        selection,
                        selectionArgs,
                        null, null,
                        sortOrder
                );
                cursor.setNotificationUri(getContext().getContentResolver(), uri);
                break;
            }
            case URI_CODE_QUOTES_SYMBOL: {
                cursor = db.query(QuoteExDatabase.TABLE_QUOTE,
                        projection,
                        String.format("%s=?", QuoteExDatabase.QuoteColumns.SYMBOL),
                        new String[] {uri.getPathSegments().get(1)},
                        null, null, null
                );
                cursor.setNotificationUri(getContext().getContentResolver(), uri);
                break;
            }
            case URI_CODE_QUOTE_HISTORY_QUOTE_SYMBOL: {
                Long quote_id = getIdBySymbol(db, uri.getPathSegments().get(1));
                if(quote_id >= 0) {
                    cursor = db.query(QuoteExDatabase.TABLE_QUOTE_HISTORY,
                            projection,
                            String.format("%s=?", QuoteExDatabase.QuoteColumns.QUOTE_ID),
                            new String[]{quote_id + ""},
                            null, null, sortOrder
                    );
                    cursor.setNotificationUri(getContext().getContentResolver(), uri);
                }
                break;
            }
            default: {
                throw new IllegalArgumentException("Unknown URI " + uri);
            }
        }

        return cursor;
    }

    @Override
    public ContentProviderResult[] applyBatch(ArrayList<ContentProviderOperation> operations) throws OperationApplicationException {
        ContentProviderResult[] results = null;
        SQLiteDatabase db = database.getWritableDatabase();
        db.beginTransaction();
        try {
            results = super.applyBatch(operations);
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
        return results;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        SQLiteDatabase db = database.getWritableDatabase();
        int count = 0;

        switch (MATCHER.match(uri)) {
            case URI_CODE_QUOTES_SYMBOL: {
                count = _delete(db, uri.getPathSegments().get(1));
                if(count > 0)
                    getContext().getContentResolver().notifyChange(uri, null);
                break;
            }
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }

        return count;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        SQLiteDatabase db = database.getWritableDatabase();
        Uri ret_uri = null;

        switch (MATCHER.match(uri)) {
            case URI_CODE_QUOTES: {
                long id = _insert(db, values);
                if(id >= 0) {
                    getContext().getContentResolver().notifyChange(uri, null);
                    ret_uri = ContentUris.withAppendedId(uri, id);
                }
                break;
            }
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }

        return ret_uri;
    }

    private int rawQueryEx(SQLiteDatabase db, String sql, String[] args) {
        Cursor cursor;

        cursor = db.rawQuery(sql, args);
        if(cursor == null) return 0;
        cursor.getCount();
        cursor.close();

        cursor = db.rawQuery("select changes()", null);
        if(cursor == null) return 0;

        try {
            cursor.moveToFirst();
            return cursor.getInt(0);
        } finally {
            cursor.close();
        }
    }

    private int _update(SQLiteDatabase db, String symbol, ContentValues values) {
        int count = 0;
        db.beginTransaction();
        try {
            count = __update(db, symbol, values);
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }

        return count;
    }

    private int __update(SQLiteDatabase db, String symbol, ContentValues values) {
        long quote_id = getIdBySymbol(db, symbol);
        if(quote_id < 0) return 0;

        long ts = values.getAsLong(QuoteExDatabase.QuoteColumns.UPDATED_TS);

        //main purpose, lock the database
        //update quotes set update_ts=update_ts where _id=? and update_ts<?
        int count;
        count = rawQueryEx(
                db,
                String.format("update %s set %s=%s where %s=? and %s<?",
                        QuoteExDatabase.TABLE_QUOTE,
                        QuoteExDatabase.QuoteColumns.UPDATED_TS,
                        QuoteExDatabase.QuoteColumns.UPDATED_TS,
                        QuoteExDatabase.QuoteColumns._ID,
                        QuoteExDatabase.QuoteColumns.UPDATED_TS
                        ),
                new String[] {quote_id + "", ts + ""}
        );
        if(count <= 0) return 0;

        Cursor cursor = db.query(
                QuoteExDatabase.TABLE_QUOTE,
                new String[] {
                        QuoteExDatabase.QuoteColumns.CHANGE,
                        QuoteExDatabase.QuoteColumns.BIDPRICE,
                        QuoteExDatabase.QuoteColumns.PERCENT_CHANGE,
                        QuoteExDatabase.QuoteColumns.UPDATED_TS,
                },
                String.format("%s=?",
                        QuoteExDatabase.QuoteColumns._ID
                ),
                new String[]{
                        quote_id + "",
                },
                null, null, null
        );
        cursor.moveToFirst();
        Long c_updated_ts = cursor.getLong(3);
        String c_change = cursor.getString(0);
        String c_bidprice = cursor.getString(1);
        String c_percent_change = cursor.getString(2);
        cursor.close();
        count = 0;
        if(c_updated_ts / (HISTORY_INTERVAL * 1000) != ts / (HISTORY_INTERVAL * 1000)) {
            ContentValues v1 = new ContentValues();
            v1.put(QuoteExDatabase.QuoteColumns.CHANGE, c_change);
            v1.put(QuoteExDatabase.QuoteColumns.BIDPRICE, c_bidprice);
            v1.put(QuoteExDatabase.QuoteColumns.PERCENT_CHANGE, c_percent_change);
            v1.put(QuoteExDatabase.QuoteColumns.UPDATED_TS, c_updated_ts);
            v1.put(QuoteExDatabase.QuoteColumns.QUOTE_ID, quote_id);
            db.insert(QuoteExDatabase.TABLE_QUOTE_HISTORY, null, v1);
            count = 1;
        }

        ContentValues v0 = new ContentValues();
        v0.put(QuoteExDatabase.QuoteColumns.UPDATED_TS, ts);
        String i_change = values.getAsString(QuoteExDatabase.QuoteColumns.CHANGE) ;
        String i_bidprice = values.getAsString(QuoteExDatabase.QuoteColumns.BIDPRICE);
        String i_percent_change = values.getAsString(QuoteExDatabase.QuoteColumns.PERCENT_CHANGE);
        if(!c_change.equals(i_change) || !c_bidprice.equals(i_bidprice) || !c_percent_change.equals(i_percent_change)) {
            v0.put(QuoteExDatabase.QuoteColumns.CHANGE, i_change);
            v0.put(QuoteExDatabase.QuoteColumns.BIDPRICE, i_bidprice);
            v0.put(QuoteExDatabase.QuoteColumns.PERCENT_CHANGE, i_percent_change);
            count = 1;
        }
        db.update(QuoteExDatabase.TABLE_QUOTE, v0,
                String.format("%s=?",
                        QuoteExDatabase.QuoteColumns._ID
                ),
                new String[] {
                        quote_id + "",
                }
        );

        return count;
    }

    @Override
    public int bulkInsert(Uri uri, ContentValues[] values) {
        SQLiteDatabase db = database.getWritableDatabase();
        int count = 0;

        db.beginTransaction();
        try {
            switch (MATCHER.match(uri)) {
                case URI_CODE_QUOTES: {
                    for(ContentValues v : values) {
                        if(_insert(db, v) >= 0)
                            count++;
                    }
                    if(count > 0)
                        getContext().getContentResolver().notifyChange(uri, null);
                    break;
                }
                default:
                    throw new IllegalArgumentException("Unknown URI " + uri);
            }

            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }


        return count;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        SQLiteDatabase db = database.getWritableDatabase();
        int count = 0;

        switch (MATCHER.match(uri)) {
            case URI_CODE_QUOTES: {
                count = _update(db, values.getAsString(QuoteExDatabase.QuoteColumns.SYMBOL), values);
                if(count > 0) {
                    getContext().getContentResolver().notifyChange(uri, null);
                }
                break;
            }
            case URI_CODE_QUOTES_SYMBOL: {
                count = _update(db, uri.getPathSegments().get(1), values);
                if(count > 0)
                    getContext().getContentResolver().notifyChange(uri, null);
                break;
            }
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }

        return count;
    }

    @Override
    public String getType(Uri uri) {
        switch(MATCHER.match(uri)) {
            case URI_CODE_QUOTES: {
                return "vnd.android.cursor.dir/quote";
            }
            case URI_CODE_QUOTES_SYMBOL: {
                return "vnd.android.cursor.item/quote";
            }
            case URI_CODE_QUOTE_HISTORY_QUOTE_SYMBOL: {
                return "vnd.android.cursor.dir/quote_history";
            }
            default: {
                throw new IllegalArgumentException("Unknown URI " + uri);
            }
        }
    }

}
