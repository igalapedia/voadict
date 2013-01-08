package com.ljb.makedictdb;

import java.io.File;

public class MakeDictDb {

    static String[] getList(File file) {
        String[] children = new String[]{
                "a.lrc",
                "b.lrc",
                "c.lrc",
                "d.lrc",
                "e.lrc",
                "f.lrc",
                "g.lrc",
                "h.lrc",
                "i.lrc",
                "j.lrc",
                "k.lrc",
                "l.lrc",
                "m.lrc",
                "n.lrc",
                "o.lrc",
                "p.lrc",
                "q.lrc",
                "r.lrc",
                "s.lrc",
                "t.lrc",
                "u.lrc",
                "v.lrc",
                "w.lrc",
                "y.lrc",
                "z.lrc",                
                "z1.lrc",
                "z2.lrc",                
        };
        String[] fileStrings = new String[children.length];
        for (int i = 0; i < children.length; i++) {
            fileStrings[i] = file.getAbsolutePath()+"/"+children[i];
        }
        return fileStrings;
    }    
    
    static void testParse(String lyricFileString) {
        LyricParser parser = new LyricParser(lyricFileString, null);
        try {
            parser.Parse();
            parser.dumpWords();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
    /**
     * @param args
     */
    public static void main(String[] args) {
        final String USAGE = "program lyricdir dbfile\n" +
        		"     generate db file\n" +
        		"program lyric\n" +
        		"     just test parse is ok";
        
        if (args.length != 2 && args.length != 1) {
            System.out.println(USAGE);
            return;
        }
        if (args.length == 1) {
            testParse(args[0]);
            return;
        }
        String lyricDirString = args[0];
        File lyricDir =  new File(lyricDirString);
        if (!lyricDir.isDirectory()) {
            System.out.println(USAGE);
            return;
        }
        
        String[] files = getList(lyricDir);
        System.out.println(files);
        for (String string : files) {
            String lyricFileString = string;
            String dictDbFileString = args[1];
            
            LyricParser parser = new LyricParser(lyricFileString, dictDbFileString);
            try {
                parser.Parse();
                parser.appendToDict();
            }
            catch (Exception e) {
                e.printStackTrace();
                return;
            }
        }
    }

}
