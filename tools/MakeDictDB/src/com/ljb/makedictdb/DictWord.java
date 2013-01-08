package com.ljb.makedictdb;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class DictWord {

    public class Mean {
        String engMean; // 英文解释
        String zhMean; // 中文解释        
        String engSample; // 英文例子
        String zhSample; // 中文例子
        @Override
        public String toString() {
            return String.format("%s(%s)\n\t%s(%s)\n", engMean,zhMean,engSample,zhSample);
        }
    }

    String word; // 单词
    String category;//分类
    List<Mean> MeanList;

    public DictWord() {
        MeanList = new ArrayList<DictWord.Mean>();
    }
    @Override
    public String toString() {
        StringBuffer string = new StringBuffer();
        string.append(String.format("%s-%s\n", category,word));
        for (Iterator<Mean> iterator = MeanList.iterator(); iterator.hasNext();) {
            Mean mean = (Mean) iterator.next();
            string.append(" "+mean.toString());
        }
        return string.toString();
    }
}