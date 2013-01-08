/**
 * 
 */
package com.ljb.voadict;

import java.util.HashMap;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.provider.BaseColumns;
import android.text.TextUtils;
import android.util.Log;


/**
 * @author ljb
 *
 */
public class VOADictDB{
	
	private SQLiteDatabase    mDB = null;
	private static final String WORDS_TABLE_NAME   = "words";  
	private static final String MEANS_TABLE_NAME   = "means";
	private static final String TAG = VOADictDB.class.getSimpleName();  

    private static HashMap<String, String> sWordsProjectionMap;
    private static HashMap<String, String> sMeansProjectionMap;
    
    private static final String DB_NAME = "libvoadictdb.so";
    private static final boolean DEBUG = false;
    
    private static String getDBPath(Context context){
        return context.getApplicationInfo().dataDir + "/lib/"+DB_NAME;
    }
    public static VOADictDB GetDBInstance(Context context){
        String dbPath =  getDBPath(context);
        logd("open db "+dbPath);
        return new VOADictDB(dbPath);
    }
    private static void logd(String string) {
        if (DEBUG) {
            Log.d(TAG, string);
        }
    }
    
    public void Close() {
        mDB.close();
    }

    /**
	 * 
	 */
	private VOADictDB(String libdirPath) {
	    mDB = SQLiteDatabase.openDatabase(libdirPath,null,SQLiteDatabase.OPEN_READONLY|SQLiteDatabase.NO_LOCALIZED_COLLATORS);
	}

	public Cursor getWords(String input){
	    if (TextUtils.isEmpty(input)) {
            return query(WORDS_TABLE_NAME, null, null, null, null);
        }
        String sqlinput = input.replace("'", "''");
        return query(WORDS_TABLE_NAME, null, 
                    Words.WORD + " LIKE '" + sqlinput + "%'", null, null);
	}

    public Cursor getMeansByWords(String word){
        if (TextUtils.isEmpty(word)) {
            throw new IllegalArgumentException("getMeansByWords word can not be empty");
        }
        String quoteWord = word.replace("'", "''"); 
        Cursor cursor = query(MEANS_TABLE_NAME, null, 
                Means.WORD+ "='"+quoteWord+"'", null, null);
        logd("get means "+cursor.getCount());
        return cursor;
    }
	

	/* query the QQ friends
	 * @see android.content.ContentProvider#query(android.net.Uri, java.lang.String[], java.lang.String, java.lang.String[], java.lang.String)
	 */
	public Cursor query(String tablename, String[] projection, String selection,
			String[] selectionArgs, String sortOrder) {
        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
        if (tablename.equalsIgnoreCase(WORDS_TABLE_NAME)) {
            qb.setTables(tablename);
            qb.setProjectionMap(sWordsProjectionMap);
        }else if (tablename.equalsIgnoreCase(MEANS_TABLE_NAME)) {
            qb.setTables(MEANS_TABLE_NAME);
            qb.setProjectionMap(sMeansProjectionMap);            
        }else{
            throw new IllegalArgumentException("Unknown table " + tablename);
        }

        // If no sort order is specified use the default
        String orderBy;
        if (TextUtils.isEmpty(sortOrder)) {
            orderBy = Words.DEFAULT_SORT_ORDER;
        } else {
            orderBy = sortOrder;
        }

        // Get the database and run the query
        Cursor c = qb.query(mDB, projection, selection, selectionArgs, null, null, orderBy);

        return c;
	}
    
	    
    public static final class Words implements BaseColumns {
        // This class cannot be instantiated
        private Words() {
        }

        /**
         * The default sort order for this table
         */
        public static final String DEFAULT_SORT_ORDER = "_id ASC";

        // 表数据列
        /**
         * The word
         * <P>
         * Type: TEXT
         * </P>
         */
        public static final String WORD = "word";
        /**
         * The category of the word
         * <P>
         * Type: TEXT
         * </P>
         */
        public static final String CATEGORY = "category";

    }

    public static final class Means implements BaseColumns {
        // This class cannot be instantiated
        private Means() {
        }

        // 表数据列
        /**
         * The word
         * <P>
         * Type: TEXT
         * </P>
         */
        public static final String WORD = "word";
        /**
         * The English mean of the word
         * <P>
         * Type: TEXT
         * </P>
         */
        public static final String ENGMEAN = "engmean";
        /**
         * The Chinese mean of the word
         * <P>
         * Type: TEXT
         * </P>
         */
        public static final String ZHMEAN = "zhmean";
        /**
         * The English sample of the word
         * <P>
         * Type: TEXT
         * </P>
         */
        public static final String ENGSAMPLE = "engsample";
        /**
         * The Chinese sample of the word
         * <P>
         * Type: TEXT
         * </P>
         */
        public static final String ZHSAMPLE = "zhsample";

    }
	
    static {
        sWordsProjectionMap = new HashMap<String, String>();
        sWordsProjectionMap.put(Words._ID, Words._ID);
        sWordsProjectionMap.put(Words.WORD, Words.WORD);
        sWordsProjectionMap.put(Words.CATEGORY, Words.CATEGORY);

        sMeansProjectionMap = new HashMap<String, String>();
        sMeansProjectionMap.put(Means._ID, Means._ID);
        sMeansProjectionMap.put(Means.WORD,Means.WORD);
        sMeansProjectionMap.put(Means.ENGMEAN,Means.ENGMEAN);
        sMeansProjectionMap.put(Means.ZHMEAN,Means.ZHMEAN);
        sMeansProjectionMap.put(Means.ENGSAMPLE,Means.ENGSAMPLE);
        sMeansProjectionMap.put(Means.ZHSAMPLE,Means.ZHSAMPLE);
    }
}
