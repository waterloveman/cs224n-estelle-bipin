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
	Vector<HashSet<List<String>>> NgramVocabulary;//list of Ngram types
	//HashMap<Integer, List<List<String>>> invertedTable; 
	List<HashMap<Integer, HashSet<List<String>>>> invertedTable; 
	List<double[]> countOfCountsTable;
	double[] smoothedCountOfCountsTable;
	int order;
	
	public NgramCounter(int ord){
		order = ord;
		table = new Table();
		totalNgrams = new Integer(0);
		vocabulary = new HashSet<String>();
		NgramVocabulary = new Vector<HashSet<List<String>>>(order);
		for (int i=0; i<order; i++){
			List<String> unk = new ArrayList<String>();
			for(int j=0; j<i+1; j++){
				unk.add("--UNK--");
			}
			HashSet<List<String>> newset = new HashSet<List<String>>();
			newset.add(unk);
			NgramVocabulary.add(newset);
		}
		//invertedTable = new HashMap<Integer, List<List<String>>>();
		invertedTable = new ArrayList<HashMap<Integer, HashSet<List<String>>>>();
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
		// insert ngrams of all orders in the vocabulary tables
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
		for(int ord = 0; ord<order; ord++){
			System.out.println("NgramCounter.invertTable() : doing "+(ord+1)+"-grams...");
			HashSet<List<String>> vocab = NgramVocabulary.get(ord);
			HashMap<Integer, HashSet<List<String>>> invTable = new HashMap<Integer, HashSet<List<String>>>();
			float totalTypes = (float)vocab.size();
			int counter = 0;
			for(List<String> ngram : vocab){
				counter ++;
				if (counter%1000==0){
					System.out.println("NgramCounter.invertTable() : Doing ngram type number "+counter+". ("+counter/totalTypes*100+"%)");
				}
				int count = (int)getCount(ngram);
				if (!invTable.containsKey(count)){
					HashSet<List<String>> newNgramSet = new HashSet<List<String>>();
					newNgramSet.add(ngram);
					invTable.put(count,newNgramSet);
				}else{
					HashSet<List<String>> ngramSet = invTable.get(count);
					ngramSet.add(ngram);
					invTable.put(count,ngramSet);
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
			System.out.println("NgramCounter.invertTable() : finished "+(ord+1)+"-grams, found "+invTable.size()+" counts.");
			invertedTable.add(invTable);
		
		}
		
		return;
	}
	

	public void fillCountOfCountsTable(){
        int[] maxCount = new int[order];
		for (int ord=0; ord<order; ord++){
			// find biggest count
			maxCount[ord] = 0;
			for(Integer count : invertedTable.get(ord).keySet()){
				//System.out.println("count in table : "+count);
				maxCount[ord] = Math.max(maxCount[ord], (int)count);
			}
		    //System.out.println("Max count for "+(ord+1)+"-grams : "+maxCount[ord]);
		}
		// declare count table
		countOfCountsTable = new ArrayList<double[]>(); // we store all counts, including zero
		for (int i=0; i<order; i++){
			double[] table = new double[maxCount[i]+1];
			countOfCountsTable.add(table);
		}

		// fill in count table
		for (int ord=0; ord<order; ord++){
			double[] table = countOfCountsTable.get(ord);
			table[0] = 0;
			for (Integer count : invertedTable.get(ord).keySet()){
				table[count] = invertedTable.get(ord).get(count).size();
			}
		}
				
		System.out.print("Count table size : ");
		for (int i=0; i<order; i++){
			System.out.print(countOfCountsTable.get(i).length+" "+(i+1)+"-gram counts, ");
		}
		System.out.println("that's pretty cool!");
		return;
	}
	
	
	public int[] checkZerosInCountOfCountsTable(){
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
		
		int[] result = new int[order];
		for(int ord=0; ord<order; ord++){
			result[ord] = -1;
			double[] table = countOfCountsTable.get(ord);
			for (int i = 1; i<table.length; i++){
				if(table[i]==0){
					result[ord]=i; 
					break;
				}
			}
		}
		
		return result;
	}
	
	public void createSmoothedCountOfCountsTable(){
		//smoothedCountOfCountsTable = new double[countOfCountsTable.length];
		
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
