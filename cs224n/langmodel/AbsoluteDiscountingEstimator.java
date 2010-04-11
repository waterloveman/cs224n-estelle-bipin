package cs224n.langmodel;

import java.util.ArrayList;
import java.util.List;

public class AbsoluteDiscountingEstimator implements NgramProbabilityEstimator {
	
	NgramCounter ngc;
	int order;
	double discount;
	
	public AbsoluteDiscountingEstimator(NgramCounter n, int o, double d){
		ngc = n;
		order = o;
		discount = d;
	}
	
	// -----------------------------------------------------------------------

	/**
	 * Return the Maximum Likelihood estimate of the ngram.
	 * Since we're not doing any smoothing, we can directly
	 * use the counts of the lower-order ngrams.
	 */
	public double getNgramJointProbability(List<String> ngram){

		int o = ngram.size();
		
		// check to see if we've been asked for the
		// highest order ngram probability
		if (o == order){
		    // get count of the ngram
			int ngramCount 	= ngc.getCount(ngram);
			int totalNgrams	= ngc.totalNgrams;

			// now we have two cases:
			// (1) ngramCount > 0 -> subtract delta
			// (2) ngramCount = 0 -> add delta
			if (ngramCount > 0) {
				return (ngramCount - discount) / totalNgrams; 
			}
			else	{
				int nNonZeroCountNgrams = ngc.nNonZeroCountNgrams();
				int nZeroCountNgrams = ngc.nZeroCountNgrams(order);
				double massLeft = discount * nNonZeroCountNgrams;
				double massPerUnknown = massLeft / nZeroCountNgrams;
				return (0 + massPerUnknown) / totalNgrams; 
			}
			
		}
		else	{ 
			// sum over all higher order ngrams.
			// we can't just use the lower-order counts
			// because each of the higher-order counts 
			// have been smoothed
			double sum = 0.0;
			for (String word : ngc.vocabulary)	{
				List<String> tmpNgram = new ArrayList<String>(ngram);
				tmpNgram.add(word);
				sum += getNgramJointProbability(tmpNgram);
			}
			return sum;
		}

		
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
			}
		}
		
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
