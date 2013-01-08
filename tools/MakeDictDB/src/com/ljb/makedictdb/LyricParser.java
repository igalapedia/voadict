package com.ljb.makedictdb;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Date;

import com.ljb.makedictdb.DictWord.Mean;

public class LyricParser {
    private String mDictDbFileString;
    private String mLyricFileString;
    private List<DictWord> mDictWords; 
    private List<List<LrcSentence>> mSentences;
    private final String WORDS_TABLENAME = "words";
    private final String MEANS_TABLENAME = "means";
    private final String CREATE_WORDS_TABLE = "CREATE TABLE words (_id INTEGER PRIMARY KEY AUTOINCREMENT, word varchar UNIQUE,category varchar);";
    private final String CREATE_MEANS_TABLE = "CREATE TABLE means (_id INTEGER PRIMARY KEY AUTOINCREMENT, word varchar,engmean varchar,zhmean varchar, engsample varchar,zhsample varchar, FOREIGN KEY(word) REFERENCES words(word));";
    
    public LyricParser(String lyricFileString,String dictDbFileString) {
        mDictDbFileString = dictDbFileString;
        mLyricFileString = lyricFileString;
        mDictWords = new ArrayList<DictWord>();
        mSentences = new ArrayList<List<LrcSentence>>();
    }

    /*
     * [mm:ss.SSS]advertise
     * [mm:ss.SSS]--- [word] xxx
     * [mm:ss.SSS]content
     * [mm:ss.SSS]--- [word] xxx
     * [mm:ss.SSS]content
     * [mm:ss.SSS]--- end
     */
    public void LoadLrc() throws ParseException, NumberFormatException, IOException {
        List<LrcSentence> wordSentences = null;
        BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(mLyricFileString), "GBK"));
        String line = null;
        boolean start = false;
        LrcSentence localLrcSentence = null;
        while ((line = br.readLine()) != null) {
            line = line.trim();
            if ((line.length() < 11) || (line.charAt(0) != '['))
                continue;
            if (!start) {
                if (line.indexOf("---") == -1) {
                    continue;
                }else{
                    //find first --- []
                    start = true;
                }
            }
            int n = line.indexOf("]");
            if (n != -1) {
                String startTime = line.substring(1, n);
                SimpleDateFormat formatter = new SimpleDateFormat("mm:ss.SSS");
                Date parsed = formatter.parse(startTime);
                if (localLrcSentence != null) {
                    //insert the time to last sentence stop time
                    localLrcSentence.stop = parsed.getTime();
                }
                localLrcSentence = new LrcSentence();

                if (line.indexOf("---") != -1) {
                    if (wordSentences != null) {
                        // find other word, push last word
                        mSentences.add(wordSentences);
                    }
                    // start other word
                    wordSentences = new ArrayList<LrcSentence>();
                    if (line.indexOf("--- end") != -1) {
                        // find end of file, break;
                        break;
                    }
                }                
                
                //add valid content
                localLrcSentence.start = parsed.getTime();
                localLrcSentence.content = line.substring(n + 1);
                wordSentences.add(localLrcSentence);
            }

        }
    }    

    public void Parse() throws Exception{
        LoadLrc();
        List<DictWord> ws = new ArrayList<DictWord>();
        List<List<LrcSentence>> words = mSentences;
        boolean supplement = false;
        boolean supplement2 = false;
        if (mLyricFileString.endsWith("z1.lrc") || mLyricFileString.endsWith("z2.lrc")) {
            supplement = true;
            if (mLyricFileString.endsWith("z2.lrc")) {
                supplement2 = true;
            }
        }
        String group = mLyricFileString.substring(mLyricFileString.length()-5, mLyricFileString.length()-4);
        System.out.println("file name is "+mLyricFileString);
        System.out.println("group is "+group);
        for (int i = 0; i < words.size(); i++) {
            List<LrcSentence> s = words.get(i);
            DictWord word = new DictWord();
            String string = s.get(0).content;
            List<LrcSentence> samples = s.subList(1, s.size());
            int indexQuote = string.indexOf("【");
            if (indexQuote == -1) {
                if (supplement) {
                    //skip ---
                    group = string.substring(4);
                    continue;
                }
                throw new Exception(String.format("can't found 【 in %s",string));
            }
            int indexBackQuote = string.indexOf("】");
            if (indexBackQuote == -1) {
                throw new Exception(String.format("can't found 】 in %s",string));
            }
            word.word = string.substring(indexQuote+1, indexBackQuote);
            word.category = group;
            
            String means = string.substring(indexBackQuote+1);
            int newlineIndex = means.indexOf("^");
            if (newlineIndex == -1) {
                throw new Exception(String.format("can't found ^ in %s",string));
            }
            String engMeansString = means.substring(0,newlineIndex);
            String zhMeanString = means.substring(newlineIndex+1);
            String[] engMeans = engMeansString.split(";");
            String[] zhMeans = zhMeanString.split("；");
            if(engMeans.length != zhMeans.length){
                System.out.println(String.format("word(%s) engMeans(%s) zhMeans(%s)",word.word,engMeansString,zhMeanString));
            }
            if (!supplement2 && engMeans.length < samples.size()) {
                System.out.println(String.format("word(%s) engMeans(%s) sample.size(%d)",word.word,engMeansString,samples.size()));
            }
            int maxcount = Math.max(engMeans.length, zhMeans.length);
            for (int j = 0; j < maxcount; j++) {
                Mean mean = word.new Mean();
                if (j < engMeans.length) {
                    mean.engMean = engMeans[j].trim();
                }
                if (j < zhMeans.length) {
                    mean.zhMean = zhMeans[j].trim();
                }
                if (j<samples.size()) {
                    String samplestring = samples.get(j).content;
                    int indexOfnewLine = samplestring.indexOf("^");
                    if (indexOfnewLine == -1) {
                        throw new Exception(String.format("can't found ^ in %s",samplestring));
                    }
                    
                    mean.engSample = samplestring.substring(0, indexOfnewLine).trim();
                    mean.zhSample = samplestring.substring(indexOfnewLine+1).trim();
                }
                word.MeanList.add(mean);
            }
            ws.add(word);
        }
        mDictWords = ws;
    }
    void dumpWords(){
        for (Iterator<DictWord> iterator = mDictWords.iterator(); iterator.hasNext();) {
            DictWord word = (DictWord) iterator.next();
            System.out.println(word.toString());
        }
    }
    
    public boolean tableIsExist(Statement statement, String tableName) throws SQLException{
        boolean result = false;
        if (tableName == null) {
            return false;
        }
        
        String selectString = String.format("SELECT count(*) as c FROM sqlite_master WHERE type='table' AND name='%s'", tableName);
        
        ResultSet rs;
            rs = statement.executeQuery(selectString);
            if (rs.next()) {
                int count = rs.getInt("c");
                if (count > 0) {
                    result = true;
                }
            }

        return result;
    }

    public boolean wordIsExist(Statement statement, String word){
        boolean result = false;
        
        String selectString = String.format("SELECT count(*) as c FROM words WHERE word='%s'", word);
        try {
            
        ResultSet rs;
            rs = statement.executeQuery(selectString);
            if (rs.next()) {
                int count = rs.getInt("c");
                if (count > 0) {
                    result = true;
                }
            }
        }
        catch (SQLException e) {
            e.printStackTrace();
            return false;
        }

        return result;
    }
    
    void ensureTableExist(Statement statement) throws SQLException{
        if (!tableIsExist(statement, WORDS_TABLENAME)) {
            statement.executeUpdate(CREATE_WORDS_TABLE);    
        }
        if (!tableIsExist(statement, MEANS_TABLENAME)) {
            statement.executeUpdate(CREATE_MEANS_TABLE);    
        }
    }

    String getSqlString(String s){
        if (s == null) {
            return "";
        }
        String sqlString = s;
        return sqlString.replace("'", "''");
    }
    
    void appendWordToDict(DictWord word,Statement statement) throws Exception{
        String worString = getSqlString(word.word);
        if (wordIsExist(statement, worString)) {
            //System.out.println(String.format("%s exist, do not insert",worString));
            return;
        }
        String wordInsert = String.format("insert into %s(word,category) values ('%s', '%s');",WORDS_TABLENAME,worString,getSqlString(word.category));
        //System.out.println(wordInsert);        
        statement.executeUpdate(wordInsert);
        for (int i = 0; i < word.MeanList.size(); i++) {
            Mean mean = word.MeanList.get(i);
            String sampleInsert = String.format("insert into %s(word,engmean,zhmean,engsample,zhsample) values ('%s', '%s', '%s', '%s', '%s');",
                    MEANS_TABLENAME,worString,
                    getSqlString(mean.engMean),getSqlString(mean.zhMean),
                    getSqlString(mean.engSample),getSqlString(mean.zhSample));
            //System.out.println(sampleInsert);
            statement.executeUpdate(sampleInsert);    
        }
    }
    
    void appendToDict() throws Exception{
        Class.forName("org.sqlite.JDBC");
        Connection connection = null;
        connection = DriverManager.getConnection(org.sqlite.JDBC.PREFIX+mDictDbFileString);
        //建立事务机制,禁止自动提交，设置回滚点   
        connection.setAutoCommit(false);            
        
        Statement statement = connection.createStatement();
        statement.setQueryTimeout(30);  // set timeout to 30 sec.

        ensureTableExist(statement);
        for (DictWord word : mDictWords) {
            appendWordToDict(word, statement);
        }
        connection.commit();
    }

}
