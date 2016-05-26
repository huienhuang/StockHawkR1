package com.sam_chordas.android.stockhawk.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Created by pk on 5/16/2016.
 */
public class QuoteExDatabase extends SQLiteOpenHelper {

    public static final int DATABASE_VERSION = 3;
    public static final String DATABASE_NAME = "quotes.db";
    private static QuoteExDatabase sInstance;

    public QuoteExDatabase(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    public static QuoteExDatabase getInstance(Context context) {
        if (sInstance == null) {
            synchronized (QuoteExDatabase.class) {
                if (sInstance == null)
                    sInstance = new QuoteExDatabase(context);
            }
        }
        return sInstance;
    }

    static public class QuoteColumns {
        public static final String _ID = "_id";
        public static final String SYMBOL = "symbol";
        public static final String PERCENT_CHANGE = "percent_change";
        public static final String CHANGE = "change";
        public static final String BIDPRICE = "bid_price";
        public static final String UPDATED_TS = "updated_ts";
        public static final String QUOTE_ID = "quote_id";
    }
    public static final String TABLE_QUOTE = "quotes";
    public static final String SQL_CREATE_TABLE_QUOTE = String.format("create table %s (" +
                "%s INTEGER PRIMARY KEY AUTOINCREMENT," +
                "%s TEXT UNIQUE," +
                "%s TEXT," +
                "%s TEXT," +
                "%s TEXT," +
                "%s INTEGER" +
                ")",
                TABLE_QUOTE,
                QuoteColumns._ID,
                QuoteColumns.SYMBOL,
                QuoteColumns.PERCENT_CHANGE,
                QuoteColumns.CHANGE,
                QuoteColumns.BIDPRICE,
                QuoteColumns.UPDATED_TS
                );
    public static final String SQL_DROP_TABLE_QUOTE = String.format("drop table if exists %s", TABLE_QUOTE);

    public static final String TABLE_QUOTE_HISTORY = "quote_history";
    public static final String SQL_CREATE_TABLE_QUOTE_HISTORY = String.format("create table %s (" +
                        "%s INTEGER PRIMARY KEY AUTOINCREMENT," +
                        "%s INTEGER," +
                        "%s TEXT," +
                        "%s TEXT," +
                        "%s TEXT," +
                        "%s INTEGER" +
                        ")",
            TABLE_QUOTE_HISTORY,
            QuoteColumns._ID,
            QuoteColumns.QUOTE_ID,
            QuoteColumns.PERCENT_CHANGE,
            QuoteColumns.CHANGE,
            QuoteColumns.BIDPRICE,
            QuoteColumns.UPDATED_TS);
    public static final String SQL_CREATE_INDEX_QUOTE_HISTORY = String.format("create index %s_%s on %s(%s)",
            TABLE_QUOTE_HISTORY, QuoteColumns.QUOTE_ID, TABLE_QUOTE_HISTORY, QuoteColumns.QUOTE_ID);
    public static final String SQL_DROP_TABLE_QUOTE_HISTORY = String.format("drop table if exists %s", TABLE_QUOTE_HISTORY);
    public static final String SQL_DROP_INDEX_QUOTE_HISTORY = String.format("drop index if exists %s_id", TABLE_QUOTE_HISTORY);


    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_TABLE_QUOTE);
        db.execSQL(SQL_CREATE_TABLE_QUOTE_HISTORY);
        db.execSQL(SQL_CREATE_INDEX_QUOTE_HISTORY);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL(SQL_DROP_TABLE_QUOTE);
        db.execSQL(SQL_DROP_TABLE_QUOTE_HISTORY);
        db.execSQL(SQL_DROP_INDEX_QUOTE_HISTORY);

        onCreate(db);
    }

    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        onUpgrade(db, oldVersion, newVersion);
    }

}
