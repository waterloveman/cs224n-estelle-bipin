package cs224n.langmodel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.Vector;
import java.io.*;

import cs224n.langmodel.Table;

public class NgramCounter {

	Table 		table;// stores the probabilities
	Integer 	totalNgrams; // number of Ngrams : tokens
	Set<String>	vocabulary; // list of unigram types
	Vector<HashSet<List<String>>> NgramVocabulary;//list of Ngram types
	int[] NgramTokens; // total number of tokens, for each order.
	//List<HashMap<Integer, HashSet<List<String>>>> invertedTable; 
	//List<double[]> countOfCountsTable;
	//List<double[]> smoothedCountOfCountsTable;
	//List<double[]> GTcountTable;
	//List<double[]> GTprobaTable;
	int order;
	
	public NgramCounter(int ord){
		order = ord;
		table = new Table();
		totalNgrams = new Integer(0);
		vocabulary = new HashSet<String>();
		NgramVocabulary = new Vector<HashSet<List<String>>>(order);
		for (int i=0; i<order; i++){
			HashSet<List<String>> newset = new HashSet<List<String>>();
			NgramVocabulary.add(newset);
		}
        NgramTokens = new int[order];
		vocabulary.add("--UNK--");
	}
  /**
   * Add a ngram into the counter.
   * The ngram is represented as an array of strings.
   */	
	void insert(List<String> ngram)	{
		//debug
		//System.out.println("inserting : "+ngram.get(0)+" "+ngram.get(1));
		
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
		
		// insert ngrams of all orders in the vocabulary and token tables
		//System.out.println("inserting : "+ngram.get(0)+" "+ngram.get(1));
		for (int i=order-1; i>-1; i--){
		//int i = 0;
		//ngram.remove(1);
		//	System.out.println("ngram is now reduced to :"+ngram);
			//System.out.println("hashset #"+i+" before : "+NgramVocabulary.get(0));
			HashSet<List<String>> vocab = NgramVocabulary.remove(i);
		//	System.out.println("Size of total table after removal : "+NgramVocabulary.size());
			vocab.add(ngram);
		//	System.out.println("hashset #"+i+" after insertion: "+vocab);
			NgramVocabulary.add(i,vocab);
			NgramTokens[i]++;
	//		System.out.println("hashset #"+i+" after : "+NgramVocabulary.get(i));
	//		System.out.println("size of total table : "+NgramVocabulary.size());
			List<String> oldngram = ngram;
			ngram = new ArrayList<String>();
			for(int j=0; j<i; j++){
				ngram.add(oldngram.get(j));
			}
	//		System.out.println("===");
		}
		//System.out.println("=========================");
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
	
	
	
	
	
	public int nZeroCountNgrams(int order)	{
		return (int)Math.pow(vocabulary.size(), order) - nNonZeroCountNgrams();
	}
	
	public int nNonZeroCountNgrams()	{
		return NgramVocabulary.size();
	}
	
	public static void main(String[] args)	{
		
		NgramCounter ngc = new NgramCounter(2);
		
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
