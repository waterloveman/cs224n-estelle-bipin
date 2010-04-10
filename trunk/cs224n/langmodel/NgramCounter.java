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

	Table 		table;// stores the probabilities
	Integer 	totalNgrams; // number of Ngrams : tokens
	Set<String>	vocabulary; // list of unigram types
	Set<List<String>> NgramVocabulary;//list of Ngram types
	//HashMap<Integer, List<List<String>>> invertedTable; 
	HashMap<Integer, HashSet<List<String>>> invertedTable; 
	double[] countOfCountsTable;
	double[] smoothedCountOfCountsTable;
	
	public NgramCounter(){
		table = new Table();
		totalNgrams = new Integer(0);
		vocabulary = new HashSet<String>();
		NgramVocabulary = new HashSet<List<String>>();
		//invertedTable = new HashMap<Integer, List<List<String>>>();
		invertedTable = new HashMap<Integer, HashSet<List<String>>>();
		//invertedTable = new ArrayList<List<List<String>>>();
        //invertedTable = new ArrayList<List<String>>();
        
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
		NgramVocabulary.add(ngram);
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
	
	public void invertTable(){
		/*List word = new ArrayList<String>();
		word.add("--UNK--");
		int count = getCount(word);
		System.out.println("unk count : "+count);*/
		
		//NOTE :
		// when you use list.add(object, index), if there is already something at that index, 
		// it will NOT be overwritten, it will be shifted to the right.
		// list.get(index) gets the object without removing it from the list.
		// list.remove(index) gets the object and removes it from the list.
		
		//countTable = new double[NgramVocabulary.size()+1]; // this is a bit too big but never mind
		float totalTypes = (float)NgramVocabulary.size();
		int counter = 0;
		for(List<String> ngram : NgramVocabulary){
			counter ++;
			if (counter%1000==0){
				System.out.println("Doing ngram type number "+counter+". ("+counter/totalTypes*100+"%)");
			}
			int count = (int)getCount(ngram);
			if (!invertedTable.containsKey(count)){
				HashSet<List<String>> newNgramSet = new HashSet<List<String>>();
				newNgramSet.add(ngram);
				invertedTable.put(count,newNgramSet);
			}else{
				HashSet<List<String>> ngramSet = invertedTable.get(count);
				ngramSet.add(ngram);
				invertedTable.put(count,ngramSet);
			}
			
           /* if (!invertedTable.containsKey(count)){
				//List<List<String>> newNgramList = new ArrayList<List<String>>();

				newNgramList.add(ngram);
				invertedTable.put(count, newNgramList);
				countTable[count]++;
            }else{
            	List<List<String>> ngramList = invertedTable.get(count);
            	if (!ngramList.contains(ngram)){
            		ngramList.add(ngram);
            		countTable[count]++;
            	}
            }*/
			
		}
		
		
		return;
	}
	

	public void fillCountOfCountsTable(){
		// find biggest count
		int maxCount = 0;
		for(Integer count : invertedTable.keySet()){
			maxCount = Math.max(maxCount, (int)count);
		}
	
		// declare count table
	    int countTableSize = maxCount+1;
		countOfCountsTable = new double[countTableSize]; // we store all counts, including zero
		System.out.println("Count table size : "+countOfCountsTable.length);

		// fill in count table
		countOfCountsTable[0] = 0;
		for (Integer count : invertedTable.keySet())
		{
		   countOfCountsTable[count] = invertedTable.get(count).size();
		}
		return;
	}
	
	
	public int checkZerosInCountOfCountsTable(){
		// findbiggest non zero count
		/*int biggestNonZeroIdx = countTable.length - 1;
		for (int i=biggestNonZeroIdx; i>-1; i--){
			if (countTable[i]!=0){
				biggestNonZeroIdx = i;
				break;
			}
		}*/
		
		// resize the array. 
		// TODO : does this take too much time for large model?
	/*	double[] newTable = new double[biggestNonZeroIdx+1];
		for (int i=0; i<biggestNonZeroIdx+1; i++){
			newTable[i] = countTable[i];
		}
		countTable = newTable;
		*/
		
		for (int i = 1; i<countOfCountsTable.length; i++){
			if(countOfCountsTable[i]==0){
				return i; 
			}
		}
		
		return -1;
	}
	
	public void createSmoothedCountOfCountsTable(){
		smoothedCountOfCountsTable = new double[countOfCountsTable.length];
		
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
