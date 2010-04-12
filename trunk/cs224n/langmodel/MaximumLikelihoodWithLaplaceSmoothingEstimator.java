package cs224n.langmodel;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


public class MaximumLikelihoodWithLaplaceSmoothingEstimator implements NgramProbabilityEstimator	{
	
	private NgramCounter ngc;
	private LanguageSpace ls;
	private int order;
	private BigInteger langaugeSpaceSize;
	HashMap<List<String>, Double> conditionalProbabilityMemory;
	HashMap<List<String>, Double> jointProbabilityMemory;
	
	public MaximumLikelihoodWithLaplaceSmoothingEstimator(NgramCounter n, int o){
		ngc = n;
		order = o;
		ls = new LanguageSpace(ngc.vocabulary, order);
		langaugeSpaceSize = ls.size();
		conditionalProbabilityMemory = new HashMap<List<String>, Double>(); 
		jointProbabilityMemory = new HashMap<List<String>, Double>();
	}

	// -----------------------------------------------------------------------

	/**
	 * Calculate the ML estimate of the ngram.
	 * To avoid a probability of '0', we pretend as if we have seen
	 * every ngram once more than it appears in the training corpus.
	 */
	public double getNgramJointProbability(List<String> ngram){
		
		// check if we've seen this before
		if (jointProbabilityMemory.containsKey(ngram)){
			return jointProbabilityMemory.get(ngram);
		}
		
		int o = ngram.size();
		double probability;

		// check to see if we've been asked for the
		// highest order ngram probability
		if (o == order){
			int ngramCount 	= ngc.getCount(ngram);
			int totalNgrams	= ngc.totalNgrams;
			
			double numerator	= ngramCount + 1;
			double denominator	= totalNgrams + langaugeSpaceSize.doubleValue();
			probability =  numerator / denominator;
			
		}
		else	{
			// sum over all higher order ngrams.
			// we can't just use the lower-order counts
			// because each of the higher-order counts 
			// have been smoothed
			
			// first, an optimization:
			// find out all the words that really do follow
			// this ngram in the training
			// these are the only ones which are non-zero
			// and have to be calculated in the sum.
			// we add the remaining zero-smoothed sums
			// in one shot later
			Set<String> localVocabulary = null;
			Table<String, Table> t = ngc.table;
			for (String word : ngram)	{
				if (!t.containsKey(word)){
					localVocabulary = new HashSet<String>();	// empty
					break;
				}
				t = t.get(word);
				localVocabulary = t.hash.keySet(); 
			}
			
			
			double sum = 0.0;
			for (String word : localVocabulary)	{
				List<String> tmpNgram = new ArrayList<String>(ngram);
				tmpNgram.add(word);
				sum += getNgramJointProbability(tmpNgram);
			}
			
			// ok, now for the unseen words the follow
			// we simply find the probability for one of them,
			// and multiply by the appropriate number of times.
			double unseenProbability;
			List<String> tmpNgram = new ArrayList<String>(ngram);
			tmpNgram.add("--UNK--");
			unseenProbability = getNgramJointProbability(tmpNgram);
			int nUnseens = ngc.vocabulary.size() - localVocabulary.size();
			sum += (nUnseens * unseenProbability);
			
			probability = sum;
		}
		
		jointProbabilityMemory.put(ngram, probability);
		return probability;

				
	}
	
	public double getNgramConditionalProbability(List<String> ngram) {
		
		// check if we've seen this before
		if (conditionalProbabilityMemory.containsKey(ngram)){
			return conditionalProbabilityMemory.get(ngram);
		}
		
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
			}
		}
		
		conditionalProbabilityMemory.put(ngram, probability);
		
		return probability;
	}
	
	public double estimatorCheckModel(int order, String distribution)	{
		if ("conditional".equals(distribution))	{
			return checkModelUsingConditionalProbability(order);
		}
		else	{
			return checkModelUsingJointProbability(order);
		}
	}
	
	private double checkModelUsingJointProbability(int o) {
        
		double sum = 0.0;
		LanguageSpace ls = new LanguageSpace(ngc.vocabulary, o);
		while (ls.hasMore())	{
			List<String> ngram = ls.getNext();
			sum += getNgramJointProbability(ngram);
		}
		
		return sum;
		
		/*
		// second version : doesn't use all combinations, so goes faster
		return estimator.checkModel();
		//
		*/
	}
	
	/**
	 * Sums over every conditional probability we can create.
	 * This should equal the number of tokens we've seen.
	 * The language-space object returns every possible set of tokens we
	 * might encounter, including a special UNK token, which takes care
	 * of novel words that we might encounter.
	 */
	private double checkModelUsingConditionalProbability(int o)	{
		double sum = 0.0;
		int n = 0;
		LanguageSpace ls = new LanguageSpace(ngc.vocabulary, o);
		while (ls.hasMore())	{
			List<String> ngram = ls.getNext();
			sum += getNgramConditionalProbability(ngram);
			if (getNgramConditionalProbability(ngram) > 0.00001){
				++n;
			}
		}
		
		return sum / (n / ngc.vocabulary.size());
		
	}

	
}
