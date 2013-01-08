package com.ljb.voadict;

import com.baidu.mobstat.StatService;

import android.os.Bundle;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Toast;

public class MainActivity extends Activity {
    private ListView mWordsListView = null;
    private EditText mWordEditText = null;
    private String[] mColumStrings = { VOADictDB.Words.WORD};
    private ImageView mClearTextImageView = null;
    private DictSeekBar mSeekBar = null;
    private int[] mMarkIndex = {0,96,180,314,394,468,545,583,640,690,700,709,761,840,872,908,1009,1013,1096,1279,1370,1383,1400,1467,1474};
    private VOADictDB mDb = null;
    public static final String EXTRA_POSITION = "position";
    public static final String EXTRA_QUERY = "query";
    private static final boolean DEBUG = false;
    private static void logd(String log) {
        if (DEBUG) {
            Log.d("DictMainActivity", log);
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        StatService.setOn(this, StatService.EXCEPTION_LOG);
        setContentView(R.layout.activity_main);
        mDb =  VOADictDB.GetDBInstance(this);
        initView();
    }

    @Override
    protected void onResume() {
        super.onResume();
        StatService.onResume(this);
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        StatService.onPause (this);
    }
    @Override
    protected void onStop() {
        super.onStop();
    }
    @Override
    protected void onDestroy() {
        mDb.Close();
        super.onDestroy();
    }
    /*
     * list view 根据输入的字符串过滤联系人
     */
    private void wordsListViewFilteTo(String input) {
        Cursor cursor = null;
        
        if (TextUtils.isEmpty(input)) {
            cursor = mDb.getWords(null);            
        }else{
            if (!TextUtils.isPrintableAsciiOnly(input)) {
                Toast.makeText(this, R.string.input_english, Toast.LENGTH_SHORT).show();
                return;
            }
            cursor = mDb.getWords(input);
        }
        SimpleCursorAdapter adapter = new SimpleCursorAdapter(this, R.layout.list_item_word, 
                cursor, mColumStrings, new int[] { R.id.textWord});
        
        mWordsListView.setAdapter(adapter);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menu_about) {
            AlertDialog.Builder aboutDialog=new AlertDialog.Builder(this);
            aboutDialog.setIcon(R.drawable.ic_voadict);
            aboutDialog.setTitle(R.string.app_name);
            aboutDialog.setMessage(R.string.about_content);
            aboutDialog.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                }
            });
            aboutDialog.create().show();            
        }
        return true;
    }
    
    @SuppressWarnings("unused")
    private void generateMarkIndex() {
        int[] markIndex = new int[mSeekBar.mLables.length];
        for (int i = 0; i < mSeekBar.mLables.length; i++) {
            markIndex[i] = foundListPosition(i);
        }
        StringBuffer array = new StringBuffer();
        for (int i = 0; i < markIndex.length; i++) {
            if (i != 0) {
                array.append(',');    
            }
            array.append(markIndex[i]);
        }
        logd("mMarkIndex={"+array+"};");
    }

    private int foundListPosition(final int progress) {
        String[] lables = mSeekBar.mLables;
        Cursor cursor = ((SimpleCursorAdapter) mWordsListView.getAdapter()).getCursor();
        cursor.moveToFirst();

        while (cursor.moveToNext()) {
            String group = cursor.getString(cursor.getColumnIndex(VOADictDB.Words.CATEGORY));
            if (group.equalsIgnoreCase(lables[progress])) {
                return cursor.getPosition();
            }
        }
        return 0;
    }
    
    private int mapMarkIndex(final int progress) {
        int index = progress;
        if (progress>=mMarkIndex.length) {
            index = mMarkIndex[mMarkIndex.length-1];
        }
        if (index < 0) {
            index = 0;
        }
        int postion = mMarkIndex[index];
        return postion;
    }
    
    private void initView(){
        mSeekBar = (DictSeekBar) findViewById(R.id.seekBarDict);
        mSeekBar.setOnSeekBarChangeListener(new DictSeekBar.OnSeekBarChangeListener() {
            
            @Override
            public void onStopTrackingTouch(DictSeekBar seekBar) {
            }
            
            @Override
            public void onStartTrackingTouch(DictSeekBar seekBar) {
            }
            
            @Override
            public void onProgressChanged(DictSeekBar seekBar, final int progress, boolean fromUser) {
                if (fromUser) {
                    mWordsListView.setSelection(mapMarkIndex(progress));
                }
            }
        });
        
        mWordsListView = (ListView) findViewById(R.id.listViewWords);
        
        mWordsListView.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Cursor cursor = (Cursor) parent.getItemAtPosition(position);
                String queryString = mWordEditText.getEditableText().toString();
                Intent intent = new Intent(view.getContext(),WordDetailActivity.class);
                intent.putExtra(EXTRA_POSITION, position);
                intent.putExtra(EXTRA_QUERY, queryString);
                logd(String.format("view:%s position %d query %s",
                        cursor.getString(cursor.getColumnIndex(VOADictDB.Words.WORD)),
                        position,queryString));
                startActivity(intent);
                //overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            }
        });

        mWordEditText = (EditText) findViewById(R.id.editTextWord);

        mWordEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if(s.toString().length() == 0) {
                    mClearTextImageView.setVisibility(View.GONE);
                    mSeekBar.setVisibility(View.VISIBLE);
                }
                else {
                    mClearTextImageView.setVisibility(View.VISIBLE);
                    mSeekBar.setVisibility(View.GONE);
                }
                
                wordsListViewFilteTo(s.toString());
            }
        });
        wordsListViewFilteTo(null);
        mClearTextImageView = (ImageView) findViewById(R.id.clear_image);
        mClearTextImageView.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                mWordEditText.getText().clear();
            }
        });
    }   

}
