package cs224n.langmodel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.Vector;

import cs224n.langmodel.Table;

public class NgramCounter {

	Table 		table;
	Integer 	totalNgrams;
	Set<String>	vocabulary;
	
	public NgramCounter(){
		table = new Table();
		totalNgrams = new Integer(0);
		vocabulary = new HashSet<String>();
		
		vocabulary.add("--UNK--");
	}
  /**
   * Add a ngram into the counter.
   * The ngram is represented as an array of strings.
   */	
	void insert(List<String> ngram)	{
		// begin at top level table
		Table t = table; 
		
		// then, for each word in the ngram
		for (String str : ngram)	{
			
			// first add the word to our vocabulary
			vocabulary.add(str);
			
			// increment instance-count for this level
			t.childCount++;
			
			// if the gram is unseen, add it to the table
			if (!t.containsKey(str))	{	t.put(str, new Table());	}
			
			// dive into higher-order table
			t = (Table)t.get(str);
		}
		t.childCount++;		// for final gram
		
		totalNgrams++;
	}
	
	
  /**
   * Return the count of the {uni|bi|tri|...}gram
   * The ngram is represented as an array of strings.
   */
	Integer getCount(List<String> ngram)	{
		// begin at the top level table
		Table t = table;
		
		// then, for each word in the ngram
		for (String str : ngram)	{
			
			// get the next higher-order table
			t = (Table<String, Table>)t.get(str);
			
			// if it doesn't exist, return 0
			if (t == null)	{
				return 0;
			}
			
		}
		
		return t.childCount;
	}
	
	
	public static void main(String[] args)	{
		
		NgramCounter ngc = new NgramCounter();
		
		// create a sample ngram-counter by inserting
		// trigrams from a sentence
		List<String> sentence = Arrays.asList("i", "am", "sam", "sam", "am", "i", "who", "am", "i", "?");
		for (int i = 0; i < sentence.size()-2; ++i)	{
			List<String> trigram = Arrays.asList(sentence.get(i), sentence.get(i+1), sentence.get(i+2));
			ngc.insert(trigram);
		}
		
		// check counts
		List<String> unigram	= Arrays.asList("am");
		List<String> bigram		= Arrays.asList("am", "i");
		List<String> trigram	= Arrays.asList("who", "am", "i");
		System.out.println( "unigram "	+ ngc.getCount(unigram) );
		System.out.println( "bigram "	+ ngc.getCount(bigram) );
		System.out.println( "trigram "	+ ngc.getCount(trigram) );
		
		// print vocabulary
		System.out.println(ngc.vocabulary);
		LanguageSpace ls = new LanguageSpace(ngc.vocabulary, 2);
		while (ls.hasMore())	{
			List<String> ngram = ls.getNext();
			System.out.println(ngram);
		}		
	}
}
