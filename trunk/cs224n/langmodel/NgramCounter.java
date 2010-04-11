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
	//HashMap<Integer, List<List<String>>> invertedTable; 
	List<HashMap<Integer, HashSet<List<String>>>> invertedTable; 
	List<double[]> countOfCountsTable;
	List<double[]> smoothedCountOfCountsTable;
	List<double[]> GTcountTable;
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
				if (counter%10000==0){
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
	
	// this generates files to be plotted, names "Nc_n-gram_plot.txt", with n the order of the ngrams.
	// Example : the gnuplot commands to print the unigram and bigram files are:
	// set style line 1 lt 5 lw 10
	// plot "Nc_1-gram_plot.txt" with imp ls 1 
	// plot "Nc_2-gram_plot.txt" with imp ls 1
	// To get a log log plot, use :
	// set log xy  #use unset log xy to go back to linear plots.
	public void printCountOfCountsTableToFile(){
		for (int ord=0; ord<order; ord++){
			// Create file for each n-gram order
			String fileName = "../Nc_"+(ord+1)+"-gram_plot.txt";
			String title = "# table of counts of counts (N_c) for "+(ord+1)+"-grams, unsmoothed\n"; 
			double[] table = countOfCountsTable.get(ord);
			printTableToFile(table, fileName, title);
		}
	}
	
	public void printTableToFile(double[] table, String fileName, String title){
		try{
			FileWriter fstream = new FileWriter(fileName);
			BufferedWriter out = new BufferedWriter(fstream);
			out.write("#"+title);
			for (int i=0; i<table.length; i++){
				out.write(i+"\t"+table[i]+"\n");
			}
			out.close();
		}catch (Exception e){
			System.out.println("Error : " + e.getMessage());
		}
	}
	
	public void createSmoothedCountOfCountsTable(){
		// declare the smoothed table
		smoothedCountOfCountsTable = new ArrayList<double[]>(order);
		for (int ord=0; ord<order; ord++){
			double[] table = countOfCountsTable.get(ord);
			double[] smoothedTable = new double[table.length];
			
			// decide the value of xMin
			int xMin = 500; 
			// TODO : this should be tuned : try a few values with on the hold-out data set.
			// fill in the values before xMin : unsmoothed.
			smoothedTable[0] = 0;
			for (int i=1; i<xMin; i++){
				smoothedTable[i] = table[i];
			}
			
			// find the exponent alpha (see wikipedia page :  http://en.wikipedia.org/wiki/Power_law#Estimating_the_exponent_from_empirical_data)			
			double alphaSum = 0;
			double totalSamples = 0; // total number of samples drawn from the power law. This is called "n" in the papers.
			for (int i=xMin; i<table.length; i++){
				alphaSum += table[i]*Math.log(i/(xMin-0.5)); // we drew the value x=i table[i] times.
				totalSamples += table[i];
			}
			double alpha = 1+ totalSamples/alphaSum;
			
			// fill in the smoothed table : not scaled, it estimated a probability distribution and should sum up to 1.
			// Nc_smooth(x) = ((alpha-1)/xMin) * (x/xMin)^(-alpha)
			double checkSmoothedSum = 0;
			for (int i=xMin; i<table.length; i++){
				smoothedTable[i] = (alpha-1)/xMin*Math.pow(i/((double)xMin),-alpha); // with unnormalized expression in wikipedia
				checkSmoothedSum += smoothedTable[i];
			}
			printTableToFile(smoothedTable, "../Nc_"+(ord+1)+"-gram_plot_smoothed_probaDistrib.txt", "#\n");
			// check : this should sum up to 1 (roughly, it's a discrete approximation.)
			System.out.println("Smoothed distribution sums up to : "+checkSmoothedSum);
			
			// Scale the smoothed table so that it sums up to totalSamples.
			// This will imitate the histogram that we would get if we sampled totalSamples times from our probability distribution.
			double scaledCheckSmoothedSum = 0;
			for(int i=xMin; i<table.length; i++){
				smoothedTable[i] = smoothedTable[i]/checkSmoothedSum*totalSamples;
				scaledCheckSmoothedSum += smoothedTable[i];
			}
			System.out.println("Smoothed distribution, scaled, now sums up to : "+scaledCheckSmoothedSum);
			System.out.println("Total number of samples is : "+totalSamples);
			printTableToFile(smoothedTable, "../Nc_"+(ord+1)+"-gram_plot_smoothed_scaled.txt", "#\n");
			
			// compute the squared error to the real values
			double squaredError = 0;
			for (int i=xMin; i<table.length; i++){
				squaredError += (table[i]-smoothedTable[i])*(table[i]-smoothedTable[i]);
			}
			squaredError = squaredError/(table.length-xMin);
			System.out.println("Squared error of the fit with xMin = "+xMin+": "+squaredError);
			
			// add to the 2d table
			smoothedCountOfCountsTable.add(smoothedTable);
		}
		
		return;
	}
	
	public void createGTcountTable(){
		GTcountTable = new ArrayList<double[]>(order);
		for (int ord=0; ord<order; ord++){
			double[] Nc = smoothedCountOfCountsTable.get(ord);// we use the smoothed Nc table.
			double[] GTtable = new double[Nc.length];
			GTtable[0] = 0;
			// for large counts, we take the real value of the count 
			//(the word has been seen many times so the count is reliable.)
			int k = 50; // TODO : tune this parameter, and set it in the constructor
			for(int i=k+1; i<GTtable.length; i++){
				GTtable[i] = i;
			}
			// for smaller counts, we use the GT approximation
			for(int i=1; i<=k; i++){
				GTtable[i] = ( (i+1)*Nc[i+1]/Nc[i] - i*(k+1)*Nc[k+1]/Nc[1] ) / (1 - (k+1)*Nc[k+1]/Nc[1] ) ;
			}
			GTcountTable.add(GTtable);
			printTableToFile(GTtable, "../GTtable_"+(ord+1)+"-grams.txt", "#\n");
			
			//debug : look at smoothed counts
			double[] smoothedGTtable = new double[k+1];
			for (int i=0; i<k+1; i++){
				smoothedGTtable[i] = GTtable[i];
			}
			printTableToFile(smoothedGTtable, "../GTtable_"+(ord+1)+"-grams_smoothed.txt", "#\n");

		}
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
