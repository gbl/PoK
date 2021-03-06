package de.guntram.bukkit.pursuitofknowledge;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

class QA {
    private String question;
    private String answer;
    private boolean used;
    
    QA(String q) {
        question=q;
        answer="unknown";
        used=false;
    }
    QA(String q, String a) {
        question=q;
        answer=a;
        used=false;
    }
    
    void setQuestion(String s) { question=s; }
    void setAnswer(String s)   { answer=s;   }
    void setUsed()             { used=true; }
    
    String getQuestion() { return question; }
    String getAnswer()   { return answer; }
    boolean wasUsed()    { return used; }
    
    String getShowableAnswer() {
        int pos;
        if ((pos=answer.indexOf('|'))>0) {
            return answer.substring(0, pos);
        } else {
            return answer;
        }
    }
}

/**
 *
 * @author gbl
 */
public class QAList {

    private String inputFileName;
    private List<QA> entries;
    private int currentQuestion;
    private final Logger logger;
    private Pattern curPattern;
    
    /**
     *
     * @param file
     * @param logger
     */
    public QAList(File file, Logger logger) {
        this.logger=logger;
        init(file, null);
    }

    public QAList(File file, Logger logger, GameMode mode) {
        this.logger=logger;
        init(file, mode);
    }
    
    public void init(File file, GameMode mode) {
        inputFileName=file.getName();
        entries=new ArrayList<QA>();
        currentQuestion=-1;
        QA qa=null;
        BufferedReader reader=null;
        try {
            reader=new BufferedReader(new FileReader(file));
            boolean expectAnswer=false;
            String line;
            while ((line=reader.readLine())!=null) {
                if (line.isEmpty() || line.charAt(0)=='#')
                    continue;
                if (expectAnswer) {
                    logger.fine("Saving answer "+line);
                    qa.setAnswer(line);
                    entries.add(qa);
                    expectAnswer=false;
                } else {
                    logger.fine("got question "+line);
                    if (mode!=null && mode.isAllScramble()) {
                        String scrambled=scramble(line);
                        qa=new QA(scrambled, line);
                        entries.add(qa);
                    } else {
                        if (mode!=null && mode.isLineScramble()) {
                            line=scrambleMarked(line);
                        }
                        qa=new QA(line);
                        expectAnswer=true;
                    }
                }
            }
        } catch (IOException ex) {
            logger.log(Level.SEVERE, "Error reading "+file.getAbsolutePath(), ex);
        } finally {
            try {
                if (reader!=null)
                    reader.close();
            } catch (IOException ex) {
                logger.log(Level.SEVERE, null, ex);
            }
        }
        logger.info("Have "+entries.size()+" QA entries");
    }
    
    /**
     *
     */
    public void randomize() {
        List<QA> newEntries=new ArrayList<QA>(entries.size());
        while (entries.size()>0) {
            int index=(int)(Math.random()*entries.size());
            newEntries.add(entries.get(index));
            entries.remove(index);
        }
        entries=newEntries;
    }
    
    /**
     *
     * @return
     */
    public QA nextQA() {
        curPattern=null;
        logger.fine("nextQA: currentquestion="+currentQuestion+" and have "+entries.size()+"entries");
        for (;;) {
            String answer=null;
            try {
                currentQuestion++;
                if (currentQuestion>=entries.size()) {
                    curPattern=null;
                    return null;
                }
                entries.get(currentQuestion).setUsed();
                answer=entries.get(currentQuestion).getAnswer();
                if (!answer.equals(answer.toLowerCase()))
                    curPattern=Pattern.compile(answer);
                else
                    curPattern=Pattern.compile(answer, Pattern.CASE_INSENSITIVE);
                return entries.get(currentQuestion);
            } catch (PatternSyntaxException ex) {
                logger.log(Level.WARNING, "Bad pattern syntax: {0}", answer);
                continue;
            }
        }
    }
    
    /**
     *
     * @return
     */
    public QA currentQA() {
        if (currentQuestion<0)
            return null;
        return entries.get(currentQuestion);
    }
    
    /**
     *
     * @param answer
     * @return
     */
    public boolean checkAnswer(String answer) {
        if (curPattern==null)
            return false;
        Matcher m=curPattern.matcher(answer);
        // do not use matches() here, we want to match a part only, not the whole input.
        return m.find();
    }
    
    /**
     *
     * @return
     */
    public boolean hasMoreQuestions() {
        return currentQuestion < entries.size()-1;
    }
    
    public int getAskedQuestions() {
        return currentQuestion+1;
    }
    
    public int getTotalQuestions() {
        return (entries==null ? 0 : entries.size());
    }
    
    private static String scramble(String line) {
        StringBuilder sb=new StringBuilder(line.length());
        char[] ary=line.toCharArray();
        for (int i=0; i<ary.length; i++) {
            int index=(int) (Math.random()*ary.length);
            while (ary[index]==0) {
                if (++index==ary.length)
                    index=0;
            }
            sb.append(ary[index]);
            ary[index]=0;
        }
        return sb.toString();
    }
    
    private String scrambleMarked(String line) {
        int start, end;
        
        for (;;) {
            if ((start=line.indexOf('{'))!=-1
            &&  (end  =line.indexOf('}'))!=-1) {
                logger.log(Level.FINE, "first part {0}", line.substring(0, start));
                logger.log(Level.FINE, "to scramble: {0}", line.substring(start+1, end));
                logger.log(Level.FINE, "last part {0}", line.substring(end+1));
                line=line.substring(0, start)+
                     scramble(line.substring(start+1, end))+
                     line.substring(end+1);
            }
            else {
                break;
            }
        }
        return line;
    }
}
