package com.ljb.voadict;

import java.util.Locale;

import com.baidu.mobstat.StatService;

import android.app.Activity;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.media.AudioManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.speech.tts.TextToSpeech;
import android.speech.tts.TextToSpeech.OnInitListener;
import android.text.TextUtils;
import android.util.Log;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.ViewFlipper;

public class WordDetailActivity extends Activity{
    private static final boolean DEBUG = false;    
    private ViewFlipper mFlipper = null;
    private Cursor mCursor = null;
    private SharedPreferences mPrefs = null;
    private static final int REQ_TTS_STATUS_CHECK = 0;
    protected static final float FLING_MIN_DISTANCE = 50;  
    protected static final float FLING_MIN_VELOCITY = 100;
    private static final String TTSENABLE_KEY = "tts_enable";
    private TextToSpeech mTts = null;      
    private GestureDetector mGestureDetector = null;
    private AudioManager mAudioManager = null;
    private VOADictDB mDb = null;
    private static void logd(String log) {
        if (DEBUG) {
            Log.d("WordDetailActivity", log);
        }
    }    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.word_detail);
        mDb  =  VOADictDB.GetDBInstance(this);
        Intent intent =  getIntent();
        int position = intent.getIntExtra(MainActivity.EXTRA_POSITION, 0);
        String queryString = intent.getStringExtra(MainActivity.EXTRA_QUERY);
        mCursor = mDb.getWords(queryString);
        mCursor.moveToPosition(position);
        mPrefs = PreferenceManager.getDefaultSharedPreferences(this);

        mFlipper = (ViewFlipper) findViewById(R.id.viewFlipper);
        mAudioManager = (AudioManager) getSystemService(Service.AUDIO_SERVICE);
        
        loadMeanList(mCursor.getString(mCursor.getColumnIndex(VOADictDB.Words.WORD)),mFlipper.getCurrentView());
        
        mGestureDetector = new GestureDetector(this,new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                if (e1.getX() - e2.getX() > FLING_MIN_DISTANCE
                        && Math.abs(velocityX) > FLING_MIN_VELOCITY) {
                    //下一页
                    if(!mCursor.isLast()){
                        mCursor.moveToNext();
                        mFlipper.setInAnimation(WordDetailActivity.this, R.anim.push_left_in);
                        mFlipper.setOutAnimation(WordDetailActivity.this, R.anim.push_left_out);                        
                        mFlipper.showNext();
                        View view = mFlipper.getCurrentView();
                        final String wordString = mCursor.getString(mCursor.getColumnIndex(VOADictDB.Words.WORD));
                        logd(String.format("Load %s into view %d", wordString,mFlipper.getDisplayedChild()));
                        loadMeanList(wordString,view);
                    }
                  
                } else if (e2.getX() - e1.getX() > FLING_MIN_DISTANCE
                        && Math.abs(velocityX) > FLING_MIN_VELOCITY) {
                    //上一页
                    if (!mCursor.isFirst()) {
                        mCursor.moveToPrevious();
                        mFlipper.setInAnimation(WordDetailActivity.this, R.anim.push_right_in);
                        mFlipper.setOutAnimation(WordDetailActivity.this, R.anim.push_right_out);                        
                        mFlipper.showPrevious();
                        View view = mFlipper.getCurrentView();
                        final String wordString = mCursor.getString(mCursor.getColumnIndex(VOADictDB.Words.WORD));
                        logd(String.format("Load %s into view %d", wordString,mFlipper.getDisplayedChild()));                        
                        loadMeanList(wordString,view);
                    }
                }
                return super.onFling(e1, e2, velocityX, velocityY);                
            }
        }); 

    }
    
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQ_TTS_STATUS_CHECK) {
            switch (resultCode) {
            case TextToSpeech.Engine.CHECK_VOICE_DATA_PASS:
            // 这个返回结果表明TTS Engine可以用
            {
                initTTS();
                logd("TTS Engine is installed!");

            }
                break;
            case TextToSpeech.Engine.CHECK_VOICE_DATA_BAD_DATA:
                // 需要的语音数据已损坏
            case TextToSpeech.Engine.CHECK_VOICE_DATA_MISSING_DATA:
                // 缺少需要语言的语音数据
            case TextToSpeech.Engine.CHECK_VOICE_DATA_MISSING_VOLUME:
            // 缺少需要语言的发音数据
            {
                // 这三种情况都表明数据有错,重新下载安装需要的数据
                logd("Need language stuff:" + resultCode);
                Intent dataIntent = new Intent();
                dataIntent.setAction(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA);
                startActivity(dataIntent);
            }
                break;
            case TextToSpeech.Engine.CHECK_VOICE_DATA_FAIL:
                // 检查失败
            default:
                logd("Got a failure. TTS apparently not available");
                break;
            }
        }
        else {
            // 其他Intent返回的结果
        }
    }

    private void initTTS() {
        mTts = new TextToSpeech(this, new OnInitListener() {
            @Override
            public void onInit(int status) {
                // TTS Engine初始化完成
                if (status == TextToSpeech.SUCCESS) {
                    int result = mTts.setLanguage(Locale.US);
                    boolean enable = false;
                    // 设置发音语言
                    if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED)
                    // 判断语言是否可用
                    {
                        logd("Language is not available");
                        mPrefs.edit().putBoolean(TTSENABLE_KEY, false).commit();
                    }
                    else {
                        mPrefs.edit().putBoolean(TTSENABLE_KEY, true).commit();
                        enable = true;
                    }
                    View view = mFlipper.getCurrentView();
                    String word = ((TextView)view.findViewById(R.id.textViewWord)).getText().toString();
                    loadTTSButton(word, view);
                    if (enable) {
                        mTts.speak(word, TextToSpeech.QUEUE_ADD, null);
                    }
                }
            }
        });
    }

    private void loadMeanList(final String word,View view) {
        TextView wordTextView = (TextView) view.findViewById(R.id.textViewWord);
        wordTextView.setText(word);

        loadTTSButton(word, view);
        
        ListView meanListView = (ListView) view.findViewById(R.id.listViewMeans);
        
        Cursor cursor = mDb.getMeansByWords(word);
        SimpleCursorAdapter adapter = new SimpleCursorAdapter(this, R.layout.list_item_mean, 
                cursor, new String[] { VOADictDB.Means.ENGMEAN, VOADictDB.Means.ENGSAMPLE}, 
                new int[] { R.id.textMean,R.id.itemSample});
        adapter.setViewBinder(new SimpleCursorAdapter.ViewBinder() {
            @Override
            public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
                if (columnIndex == cursor.getColumnIndexOrThrow(VOADictDB.Means.ENGMEAN)) {
                    String mean = String.format("%s", 
                            cursor.getString(cursor.getColumnIndex(VOADictDB.Means.ENGMEAN)));
                    if (TextUtils.isEmpty(mean)) {
                        //如果英文含义是空，那就用中文的含义
                        mean = String.format("%s", 
                                cursor.getString(cursor.getColumnIndex(VOADictDB.Means.ZHMEAN)));
                    }
                    ((TextView)view).setText(mean);
                }else if (columnIndex == cursor.getColumnIndexOrThrow(VOADictDB.Means.ENGSAMPLE)) {
                    TextView textView = (TextView) view.findViewById(R.id.textSample);
                    String engSample = cursor.getString(cursor.getColumnIndex(VOADictDB.Means.ENGSAMPLE));
                    if (TextUtils.isEmpty(engSample)) {
                        //没有sample，就不显示
                        return true;
                    }
                    
                    String sample = String.format("%s:%s %s",
                            getString(R.string.sample),
                            cursor.getString(cursor.getColumnIndex(VOADictDB.Means.ENGSAMPLE)),
                            cursor.getString(cursor.getColumnIndex(VOADictDB.Means.ZHSAMPLE)));
                    textView.setText(sample);
                }
                return true;
            }
        });

        meanListView.setAdapter(adapter);
        meanListView.setLongClickable(true);
        meanListView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return mGestureDetector.onTouchEvent(event);
            }
        });
    }

    private void loadTTSButton(final String word, View view) {
        final Button ttsButton = (Button)view.findViewById(R.id.buttonTTS);

        boolean checked = mPrefs.contains(TTSENABLE_KEY);
        if (checked) {
            final boolean ttsEnable = mPrefs.getBoolean(TTSENABLE_KEY, false);
            ttsButton.setEnabled(ttsEnable);
            if (ttsEnable) {
                ttsButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (mTts!=null) {
                            mTts.speak(word, TextToSpeech.QUEUE_ADD, null);    
                        }else{
                            initTTS();
                        }
                    }
                });
            }
        }else{
            ttsButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // 检查TTS数据是否已经安装并且可用
                    try {
                        Intent checkIntent = new Intent();
                        checkIntent.setAction(TextToSpeech.Engine.ACTION_CHECK_TTS_DATA);
                        startActivityForResult(checkIntent, REQ_TTS_STATUS_CHECK);
                    }
                    catch (Exception e) {
                        e.printStackTrace();
                        mPrefs.edit().putBoolean(TTSENABLE_KEY, false).commit();
                        ttsButton.setEnabled(false);
                    }
                }
            });
        }
    }
    @Override
    protected void onResume() {
        StatService.onResume(this);
        super.onResume();
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        StatService.onPause (this);
        if (mTts != null)
        // activity暂停时也停止TTS
        {
            mTts.stop();
        }
    }

    @Override
    protected void onDestroy() {
        //add for exception java.lang.IllegalArgumentException: Receiver not registered: android.widget.ZoomButtonsController$1@41dd9550 
        //see: http://blog.csdn.net/a345017062/article/details/6838449
        findViewById(R.id.adViewInclude).setVisibility(View.GONE);
        mDb.Close();
        super.onDestroy();
        // 释放TTS的资源
        if (mTts != null) {
            mTts.shutdown();    
        }
    }
    
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
        case KeyEvent.KEYCODE_BACK:
            super.onBackPressed();
            //overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            return true;
        case KeyEvent.KEYCODE_VOLUME_UP:
            mAudioManager.adjustStreamVolume(
                AudioManager.STREAM_MUSIC,
                AudioManager.ADJUST_RAISE,
                AudioManager.FLAG_PLAY_SOUND | AudioManager.FLAG_SHOW_UI);
            return true;
        case KeyEvent.KEYCODE_VOLUME_DOWN:
            mAudioManager.adjustStreamVolume(
                AudioManager.STREAM_MUSIC,
                AudioManager.ADJUST_LOWER,
                AudioManager.FLAG_PLAY_SOUND | AudioManager.FLAG_SHOW_UI);
            return true;
        default:
            break;
        }        
        return super.onKeyDown(keyCode, event);
    }

}
