package cs224n.langmodel;


import java.util.ArrayList;
import java.util.List;
import java.util.*;
import java.io.*;
import cs224n.assignments.*;
import cs224n.util.*;

public class smoothedGoodTuringEstimator implements NgramProbabilityEstimator {
	
	NgramCounter ngc;
	List<HashMap<Integer, HashSet<List<String>>>> invertedTable; 
	List<double[]> countOfCountsTable;
	List<double[]> smoothedCountOfCountsTable;
	List<double[]> GTcountTable;
	List<double[]> GTprobaTable;
	int order;
	double[] smoothedNgramTokens; // this is the equivalent to the number of tokens, but computed with the smoothed counts of counts.
	double[] normFactor; // normalizing factor used for the probability
	Collection<List<String>> validSentences;
	HashMap<String, int[]> unkCounts;
	HashMap<List<String>, Double> unkWeights;
	HashMap<List<String>, Double> unkProbability;
		
	private static final String STOP = "</S>";
	private static final String START = "<S>";
	private static final String UNK = "--UNK--";
	
	public smoothedGoodTuringEstimator(NgramCounter n, int ord, Collection<List<String>> sentences){
		ngc = n;
		order = ord;
		validSentences = sentences;
		//trainEstimator();// this builds all the different tables
	}
	
	public smoothedGoodTuringEstimator(NgramCounter n, int ord){
		ngc = n;
		order = ord;
		String validFile = "../data/europarl-validate.sent.txt";
		try{
			validSentences = Sentences.Reader.readSentences(validFile);
		}catch (Exception e){
			System.out.println("Error : "+e.getMessage());
		}
		trainEstimator(10, 70);// this builds all the different tables
	}
	
	// -----------------------------------------------------------------------

	/**
	 * Return the Maximum Likelihood estimate of the ngram.
	 * Since we're not doing any smoothing, we can directly
	 * use the counts of the lower-order ngrams.
	 */
	public double getNgramJointProbability(List<String> ngram){
		//System.out.println("\tngram : "+ngram);
		int ngramCount 	= ngc.getCount(ngram);		
		//System.out.println("\tngramCount : "+ngramCount);
		int ord = ngram.size();
		//System.out.println("ngram size : "+ord);
		double[] table = GTprobaTable.get(ord-1);
		double proba = table[ngramCount];
		//System.out.println("probability returned : "+GTprobaTable.get(ord-1)[ngramCount]);
		return GTprobaTable.get(ord-1)[ngramCount];
	}
	
	
	public double getNgramConditionalProbability(List<String> ngram) {
		double probability = 0.0;
		
		if (ngram.size() == 1){
			// no conditioning on previous words
			probability = getNgramJointProbability(ngram);
		}
		else	{
			// condition on previous words
			List<String> prevWords = new ArrayList<String>(ngram);
			prevWords.remove(ngram.size()-1); // remove last word
			
			if (getNgramJointProbability(prevWords) != 0)	{
				// probability = P(ngram) / P(prevWords)
				probability =  getNgramJointProbability(ngram) / getNgramJointProbability(prevWords);
				if (ngram.get(ngram.size()-1).equals("UNK")){
					//System.out.println("Ngram "+ngram+" : P("+ngram+")/P("+prevWords+") = "+getNgramJointProbability(ngram)+"/"+getNgramJointProbability(prevWords));
				}
			}
		}
		
		return probability;
	}

	public void invertTable(){
		invertedTable = new ArrayList<HashMap<Integer, HashSet<List<String>>>>();
		System.out.println("Inverted table : order is "+order);
		for(int ord = 0; ord<order; ord++){
			System.out.println("NgramCounter.invertTable() : doing "+(ord+1)+"-grams...");
			HashSet<List<String>> vocab = ngc.NgramVocabulary.get(ord);
			HashMap<Integer, HashSet<List<String>>> invTable = new HashMap<Integer, HashSet<List<String>>>();
			float totalTypes = (float)vocab.size();
			int counter = 0;
			for(List<String> ngram : vocab){
				counter ++;
				if (counter%10000==0){
					System.out.println("NgramCounter.invertTable() : Doing ngram type number "+counter+". ("+counter/totalTypes*100+"%)");
				}
				int count = (int)ngc.getCount(ngram);
				if (!invTable.containsKey(count)){
					HashSet<List<String>> newNgramSet = new HashSet<List<String>>();
					newNgramSet.add(ngram);
					invTable.put(count,newNgramSet);
				}else{
					HashSet<List<String>> ngramSet = invTable.get(count);
					ngramSet.add(ngram);
					invTable.put(count,ngramSet);
				}
			
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
			//table[0] = 0; // original version
			int V_unigram = ngc.NgramVocabulary.get(0).size(); 
			table[0] = Math.pow(V_unigram, ord+1) - ngc.NgramVocabulary.get(ord).size(); // J&M first edition version : N0 = number_of_unseen_ngrams = V_ngram - number_of_ngrams_seen
			System.out.println("N0 for "+(ord+1)+"-grams : "+table[0]);
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
	
	public void printCountOfCountsTableToFile(){
		for (int ord=0; ord<order; ord++){
			// Create file for each n-gram order
			String fileName = "../Nc_"+(ord+1)+"-gram_plot.txt";
			String title = "# table of counts of counts (N_c) for "+(ord+1)+"-grams, unsmoothed\n"; 
			double[] table = countOfCountsTable.get(ord);
			printTableToFile(table, fileName, title);
		}
	}
	
	// this generates files to be plotted, names "Nc_n-gram_plot.txt", with n the order of the ngrams.
	// Example : the gnuplot commands to print the unigram and bigram files are:
	// set style line 1 lt 5 lw 10
	// plot "Nc_1-gram_plot.txt" with imp ls 1 
	// plot "Nc_2-gram_plot.txt" with imp ls 1
	// To get a log log plot, use :
	// set log xy  #use unset log xy to go back to linear plots.
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
	
	//public void 
	
	public void createSmoothedCountOfCountsTable(int xMinValue){
		// declare the smoothed table
		smoothedCountOfCountsTable = new ArrayList<double[]>(order);
		// declare the smoothed token count table
		smoothedNgramTokens = new double[order];
		for (int ord=0; ord<order; ord++){
			double[] table = countOfCountsTable.get(ord);
			double[] smoothedTable = new double[table.length];
			
			// decide the value of xMin
			int xMin = xMinValue; 
			System.out.println("Training estimator with xMin = "+xMin);
			if (xMin>table.length){
				xMin = table.length/2;
			}
			// TODO : this should be tuned : try a few values with on the hold-out data set.
			// fill in the values before xMin : unsmoothed.
			//smoothedTable[0] = 0; // in original version, N0=0. In J&M 1st edition version, N0 = V^N - seen_Ngrams
			for (int i=0; i<xMin; i++){
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
			//debug output :
			System.out.println("Smoothed part of distribution, scaled, now sums up to : "+scaledCheckSmoothedSum);
			System.out.println("Total number of samples in the smoothed part is : "+totalSamples);
			printTableToFile(smoothedTable, "../Nc_"+(ord+1)+"-gram_plot_smoothed_scaled.txt", "#\n");
			
			// debug : check that the whole distribution adds up correctly
			double cSmooth = 0;
			double cOrig = 0;
			for(int i=1; i<table.length; i++){
				cSmooth += smoothedTable[i];
				cOrig += table[i];
			}
			System.out.println("Whole smoothed distribution, scaled, now sums up to : "+cSmooth);
			System.out.println("Total number of samples in the distribution : "+cOrig);	
			
			// Compute the new token count
			// With the original distribution, sum(Nc*c)=Number_of_tokens. 
			// But with the smoothed distribution, sum(Nc*c) is slightly different. (it doesn't really correspond to any real count.)
			// We store it to use it for the normalisation of the probability distribution later. 
			double tokenSum = 0;
			for (int i=1; i<table.length; i++){
				if (table[i]!=0){
					tokenSum += smoothedTable[i] * i;
				}
			}
		    smoothedNgramTokens[ord] = tokenSum;
			System.out.println("With the smoothed distrib, the token count is "+tokenSum+" (it should be "+ngc.NgramTokens[ord]+")");
			
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
	
	public void createGTcountTable(int kValue){
		GTcountTable = new ArrayList<double[]>(order);
		normFactor = new double[order];
		for (int ord=0; ord<order; ord++){
			double[] Nc = smoothedCountOfCountsTable.get(ord);// we use the smoothed Nc table.
			double[] GTtable = new double[Nc.length];
			//GTtable[0] = 0;
			GTtable[0] = Nc[1]; // 0*=N1 : this assumes N0=1, we have only one unseen ngram.
			// for large counts, we take the real value of the count 
			//(the word has been seen many times so the count is reliable.)
			int k = kValue; 
			if(k>GTtable.length){
				k = GTtable.length/2;
			}
			for(int i=k+1; i<GTtable.length; i++){
				GTtable[i] = i;
			}
			// for smaller counts, we use the GT approximation
			for(int i=1; i<=k; i++){
				GTtable[i] = ( (i+1)*Nc[i+1]/Nc[i] - i*(k+1)*Nc[k+1]/Nc[1] ) / (1 - (k+1)*Nc[k+1]/Nc[1] ) ;
			}
			
			// simpler version : we smooth all the counts.
			/*for (int i=1; i<GTtable.length-1; i++){
				GTtable[i] = (i+1)*Nc[i+1]/Nc[i];
			}
			GTtable[GTtable.length-1] = GTtable.length-1;//last count is not smoothed.
			*/
			
			// debug : check what the counts sum up to: sum (Nc * c*) = N normally.
			double checkSum = GTtable[0];//we assume N0=1;
			for (int i=1; i<GTtable.length; i++){
				if(countOfCountsTable.get(ord)[i] != 0){
					checkSum += Nc[i]*GTtable[i];
				}
			}
			normFactor[ord] = checkSum;
			System.out.println("The GT counts add up to : "+checkSum);
			System.out.println("so normalized by "+ngc.NgramTokens[ord]+", we get : "+checkSum/ngc.NgramTokens[ord]);
			
			printTableToFile(GTtable, "../GTtable_"+(ord+1)+"-grams.txt", "#\n");
			// add to the 2d table
			GTcountTable.add(GTtable);
			
			//debug : look at smoothed counts
			double[] smoothedGTtable = new double[k+1];
			for (int i=0; i<k+1; i++){
				smoothedGTtable[i] = GTtable[i];
			}
			printTableToFile(smoothedGTtable, "../GTtable_"+(ord+1)+"-grams_smoothed.txt", "#\n");

		}
	}
	
	public void createGTprobaTable(){
		GTprobaTable = new ArrayList<double[]>(order);
		for (int ord=0; ord<order; ord++){
			double[] countTable = GTcountTable.get(ord);
			double[] probaTable = new double[countTable.length];
			//double normFact = smoothedNgramTokens[ord]; // if you want the whole table to sum up to 1 (including the Nc that were smoothed to a non-zero value, and don't correspond to any word)
			double normFact = ngc.NgramTokens[ord]; // if you want the existing words to sum up to 1
			//double normFact = normFactor[ord];
			System.out.println("Number of tokens for "+(ord+1)+"-grams : "+ngc.NgramTokens[ord]);
			System.out.println("Normalizing factor : "+normFact);
			probaTable[0] = smoothedCountOfCountsTable.get(ord)[1] / normFact; 
			// P(w:C(w)=0) = N1/N. This assumes N0=1 : only one unseen ngram. This implies 0*=N1.
			double probaSum = probaTable[0];
			for (int i=1; i<probaTable.length; i++){
				probaTable[i] = countTable[i]/normFact;
				if (countOfCountsTable.get(ord)[i] != 0){
					probaSum += countOfCountsTable.get(ord)[i]*probaTable[i]; 
					// Note : we use the unsmoothed Nc here, because is reality we will call the porbability for the real words. 
				}
			}
			// debug : check the probability sums up to 1
			System.out.println("The smoothed GT proba of order "+(ord+1)+" sums up to "+probaSum);
			
			// renormalization :
			for(int i=0; i<probaTable.length; i++){
				probaTable[i] = probaTable[i]/probaSum;
			}
			
			// add to 2d table
			GTprobaTable.add(probaTable);
		}
	}
	
	public void trainEstimator(int xMinValue, int kValue){

		// build the inverted table : 
		// count c -> list of ngrams seen c times
		// build count table :
		// count c -> number of ngram types seen c times
		System.out.println("Building inverted table...");
		invertTable();
		System.out.println("Inverted table built.");
		
		// debug : print out inverted table
		/*System.out.println("Inverted table : unigrams");
		for (Integer count : ngc.invertedTable.get(0).keySet()){
			  System.out.println("Count "+count);
			  HashSet<List<String>> ngramList = ngc.invertedTable.get(0).get(count);
			  Iterator<List<String>> iter = ngramList.iterator();
			  while(iter.hasNext())	{
				  List<String> ngram = iter.next();
				  for (int j=0; j<ngram.size(); j++){
					  System.out.print(" "+ngram.get(j));
				  }
				  System.out.print(", ");
			  }
			  System.out.println("");
		}
		System.out.println("Inverted table : bigrams");
		for (Integer count : ngc.invertedTable.get(1).keySet()){
			  System.out.println("Count "+count);
			  HashSet<List<String>> ngramList = ngc.invertedTable.get(1).get(count);
			  Iterator<List<String>> iter = ngramList.iterator();
			  while(iter.hasNext())	{
				  List<String> ngram = iter.next();
				  for (int j=0; j<ngram.size(); j++){
					  System.out.print(" "+ngram.get(j));
				  }
				  System.out.print(", ");
			  }
			  System.out.println("");
		}*/
		
		System.out.println("Filling the count of counts table for "+invertedTable.size()+" counts...");
		fillCountOfCountsTable();
		System.out.println("Count of counts table filled.");
		
		// debug : check : you should find the same number of n-grams
	/*	double[] table = countOfCountsTable.get(0);
		double sum = 0;
		for (int i=0; i<table.length; i++){
			sum += table[i];
		}
		System.out.println("Check : We should have "+sum+" unigrams types.");
		table = countOfCountsTable.get(1);
		sum = 0;
		for (int i=0; i<table.length; i++){
			sum += table[i];
		}
		System.out.println("Check : We should have "+sum+" bigrams types.");	*/
		
		// debug : print out countOfCountsTable table
		/*for(int i=0; i<ngc.countOfCountsTable.length; i++){
			if (ngc.countOfCountsTable[i]!=0){
			  System.out.print("Count "+i+" : "+ngc.countOfCountsTable[i]+" ngrams");
			  if (ngc.countOfCountsTable[i]==1){
				  System.out.print("("+ngc.invertedTable.get(i)+")");
			  }
			  System.out.println("");
			}
		}
		System.out.println("Count of counts table for bigrams :");
		table = ngc.countOfCountsTable.get(1);
		for(int i=0; i<table.length; i++){
			if (table[i]!=0){
			  System.out.print("   Count "+i+" : "+table[i]+" ngrams");
			  if (table[i]==1){
				  System.out.print("("+ngc.invertedTable.get(1).get(i)+")");
			  }
			  System.out.println("");
			}
		}
		*/
		
		// print the count of counts to a file, for plotting
		System.out.println("Plotting countOfCounts to file...");
		printCountOfCountsTableToFile();
		System.out.println("\tPlotting done.");
		
		//debug
	/*	System.out.println("Bigram with biggest count : "+(countOfCountsTable.get(1).length-1)+" counts : "+invertedTable.get(1).get(countOfCountsTable.get(1).length-1));
		System.out.println("Unigram with biggest count : "+(countOfCountsTable.get(0).length-1)+" counts : "+invertedTable.get(0).get(countOfCountsTable.get(0).length-1));
		*/
		// check for any zeros : this function returns the index of the smallest zero count
        // if the return is negative, there's a problem. TODO throw a proper error
		int[] zeroIdx = checkZerosInCountOfCountsTable();
    /*    System.out.println("first non zero for unigrams : "+zeroIdx[0]);
        System.out.println("first non zero for bigrams : "+zeroIdx[1]);
      */
		
        // Smooth the count of counts table : fit a power log
        // N_0 is still left to be zero (number of bigram types seen 0 times.)
        // The rest of the distribution is smoothed.
        System.out.println("Smoothing the counts of counts...");
        createSmoothedCountOfCountsTable(xMinValue);
        System.out.println("\tSmoothing done.");
		
        // Build the new counts table : GTcount[c] is the smoothed count value, c*.
        System.out.println("Creating GT table...");
        createGTcountTable(kValue);
        System.out.println("\tGT table created.");
        
        // Build the GT probability table
        System.out.println("Creating GT probability table...");
        createGTprobaTable();
        System.out.println("\tGT probability table created.");
        
        // if the order is 2, then we need to use the validation data to count (word UNK), (UNK word) and (UNK UNK).
        if (order == 2){
        	System.out.println("ORDER 2 : training the complicated hacky model");
        	int totalUnkCount = countUnksInValidation();
            
            // debug : print the unkCounts table
            /*for (String word : unkCounts.keySet()){
            	if(!word.equals("--UNK--")){
            		if (unkCounts.get(word)[0]>0 || unkCounts.get(word)[1]>0){
            			//System.out.println("C("+word+" UNK) = "+unkCounts.get(word)[0]+", C(UNK "+word+") = "+unkCounts.get(word)[1]);
            		}
            	}
            }
            System.out.println("C(UNK UNK) = "+unkCounts.get("--UNK--")[0]);
            */
        	
        	// compute a delta smoothed probability for the unknown bigrams
        	// This will be used as weights for splitting the unknown probability mass between unknown bigrams.
        	computeUnkWeights(totalUnkCount);
        	
        	//Multiply this probability by the leftover probability weight : 
        	// P(UNK_bigram) = GTprobaTable.get(1)[0];
        	computeUnkProbability();
        	
        	// check if P(UNK word) < P(UNK), P(word UNK)<P(word), P(UNK UNK)<P(UNK)
        	boolean good = true;
        	int badCounter = 0;
        	for (List<String> bigram : unkProbability.keySet()){
        		List<String> unigram = new ArrayList<String>();
        		unigram.add(bigram.get(0));
        		if (unkProbability.get(bigram) < getNgramJointProbability(unigram)){
        		//	System.out.println("Not good : P("+bigram+")="+unkProbability.get(bigram)+"  <  P("+bigram.get(0)+")="+getNgramJointProbability(unigram));
        			good = false;
        			badCounter++;
        		}
        	}
        	if (good){
        		System.out.println("Piece of luck! The bigram distribution holds!!");
        	}else{
        		System.out.println("The conditional distribution won't work.... "+badCounter+" out of "+unkProbability.size()+" won't work.");
        	}

        	
        }

	}
	
	public void computeUnkProbability(){
		unkProbability = new HashMap<List<String>, Double>();
		double unkProbaMass = GTprobaTable.get(1)[0];
		for (List<String> bigram : unkWeights.keySet()){
			unkProbability.put(bigram, unkWeights.get(bigram)* unkProbaMass);
		}
		
		// check this sums up to unkProbaMass
		double checkSum = 0;
		for(List<String> bigram : unkProbability.keySet()){
			checkSum += unkProbability.get(bigram);
		}
    	if (Math.abs(checkSum-unkProbaMass) < 0.000001){
    		System.out.println("GOOD !! Unknown distribution sums up to "+checkSum+" (exact  value : "+unkProbaMass+")");
    	}else{
    		System.out.println("BAAAAAD !! Unknown distribution sums up to "+checkSum+", it should sum up to : "+unkProbaMass);
    	}
		
	}
	
	public void computeUnkWeights(int totalUnkCount){
//		 compute the delta-smoothed probability.
    	double delta = 0.01;
    	int vSize = unkCounts.size()*2 -1;
    	
    	unkWeights = new HashMap<List<String>, Double>();
    	double proba;
    	System.out.println("TotalUnkCount : "+totalUnkCount);
    	for (String word : unkCounts.keySet()){
    		if(!word.equals(UNK)){
    			// (word UNK)
    			List<String> word_UNK = new ArrayList<String>();
    			word_UNK.add(word);
    			word_UNK.add("--UNK--");
    			proba = (unkCounts.get(word)[0] + delta )/ (totalUnkCount + delta*vSize);
    			if (proba>0.01){
    				System.out.println("Big Proba : "+proba+" for "+word_UNK);
    			}
    			unkWeights.put(word_UNK, proba);
    			// (UNK word)
    			List<String> UNK_word = new ArrayList<String>();
    			UNK_word.add("--UNK--");
    			UNK_word.add(word);
    			proba = (unkCounts.get(word)[1] + delta )/ (totalUnkCount + delta*vSize);
    			if (proba>0.01){
    				System.out.println("Big Proba : "+proba+" for "+UNK_word);
    			}
    			unkWeights.put(UNK_word, proba);
    		}
    	}
    	// (UNK UNK)
		List<String> UNK_UNK = new ArrayList<String>();
		UNK_UNK.add("--UNK--");
		UNK_UNK.add("--UNK--");
		proba = (unkCounts.get("--UNK--")[0] + delta )/ (totalUnkCount + delta*vSize);
		if (proba>0.01){
			System.out.println("Big Proba : "+proba+" for "+UNK_UNK);
		}
		unkWeights.put(UNK_UNK, proba);
		
    	// check proba :
		double checkSum = 0;
    	for (List<String> bigram : unkWeights.keySet()){
    		checkSum += unkWeights.get(bigram);
    	}
    	if (Math.abs(checkSum-1) < 0.000001){
    		System.out.println("GOOD !! Unknown distribution sums up to "+checkSum+".");
    	}else{
    		System.out.println("BAAAAAD !! Unknown distribution sums up to "+checkSum+".");
    	}

	}
	
	public int countUnksInValidation(){
		int unkCounter = 0;
		unkCounts = new HashMap<String, int[]>();
		for (List<String> ngram : ngc.NgramVocabulary.get(0)){
			String word = ngram.get(0);
			unkCounts.put(word,new int[2]); // the first value will be C(word UNK), the second C(UNK word)
		}
		unkCounts.put("--UNK--",new int[1]); // this will be C(UNK UNK)
		
		// for each sentence in the validation set :
		for (List<String> sentence : validSentences){
//			 add the START and STOP tokens to the sentences 
			sentence.add(STOP);
			sentence.add(0,START); // only one START, we are in a bigram model
			
			// look for UNK words
			for (int i = 1; i < sentence.size()-1; ++i){
				String word = sentence.get(i);
				if (!unkCounts.containsKey(word)){
					String wBefore = sentence.get(i-1);
					String wAfter = sentence.get(i+1);
					//System.out.println("found : "+wBefore+" "+word+"(UNK) "+wAfter);
					if(unkCounts.containsKey(wBefore)){
						unkCounts.get(wBefore)[0]++;// (word UNK)
						unkCounter++;
					}else{// (UNK UNK)
						unkCounts.get("--UNK--")[0]++;
						unkCounter++;
						System.out.println("Two UNKS in a row : "+wBefore+" "+word);
					}
					if (unkCounts.containsKey(wAfter)){
						unkCounts.get(wAfter)[1]++; // (UNK word)
						unkCounter++;
					}else{
						// Nothing, we don't want to count the (UNK UNK) twice!
					}
				}
			}

		}
		return unkCounter;
	}
	
	public double estimatorCheckModel(int ord, String distribution){
		// we need to sum up on all the words of the vocabulary, plus the UNK word.
		System.out.println("smoothedGoodTuringEstimator: checking "+distribution+" distribution of order "+ord+"...");
		double checkSum = 0;
		if (distribution.equals("joint")){
			HashSet<List<String>> vocab = ngc.NgramVocabulary.get(ord-1);
			for (List<String> ngram : vocab){
				checkSum += getNgramJointProbability(ngram);
		//		System.out.println("Adding "+ngram+" : joint P = "+getNgramJointProbability(ngram));
			}
			// add the UNK word
			List<String> unk = new ArrayList<String>();
			for (int i=0; i<ord; i++){
				unk.add("UNK");
			}
			checkSum += getNgramJointProbability(unk);
		//	System.out.println("Adding "+unk+" : joint P = "+getNgramJointProbability(unk));
		//	System.out.println("(count(unk) = "+ngc.getCount(unk)+")");
			
		}else if (distribution.equals("conditional")){
			// if the order is 1, then the conditional distrib doesn't exist.
			// we return the unigram joint distribution.
			if(ord==1){
				System.out.println("The order is 1 : returning the joint distribution.");
				return estimatorCheckModel(1,"joint");
			}
			// For the conditional distribution, for a given history, we compute the sum over the possible words.
			// We return the mean over the histories.
			// All the unseen words are mapped to UNK.
			HashSet<List<String>> historyVocab = ngc.NgramVocabulary.get(ord-2);
			HashSet<List<String>> unigramVocab = ngc.NgramVocabulary.get(0);
			HashSet<List<String>> NgramVocab = ngc.NgramVocabulary.get(ord-1);
			double sum = 0;
			checkSum = 0;
			for (List<String> history : historyVocab){
				sum = 0;
			//	System.out.println("History : "+history);
				for(List<String> unigram : unigramVocab){
					//build the full ngram
					List<String> ngram = new ArrayList<String>(history);
					ngram.add(unigram.get(0));
					// if the n-gram has been seen, get its conditional probability
					if (NgramVocab.contains(ngram)){
						sum += getNgramConditionalProbability(ngram);
				//		System.out.println("\tngram : "+ngram+" : P("+unigram+"|"+history+") = "+getNgramConditionalProbability(ngram));
					}
				}
				// add the unk unigram
				List<String> ngram = new ArrayList<String>(history);
				ngram.add("UNK");
				//sum += getNgramConditionalProbability(ngram);
				//System.out.println("\tngram : "+ngram+" : P(UNK|"+history+") = "+getNgramConditionalProbability(ngram));
				System.out.println("For history "+history+", the sum is : "+sum);
				// add to general sum
				checkSum += sum;
			}
			// get average of general sum
			checkSum = checkSum/historyVocab.size();

		}else{
			System.out.println("Type of distributino unknown. Sum not computed!");
		}
		return checkSum;
	}
}
