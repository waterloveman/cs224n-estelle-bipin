package cs224n.langmodel;

import java.util.ArrayList;
import java.util.List;

public class MaximumLikelihoodEstimator implements NgramProbabilityEstimator {
	
	NgramCounter ngc;
	
	public MaximumLikelihoodEstimator(NgramCounter n){
		ngc = n;
	}
	
	// -----------------------------------------------------------------------

	/**
	 * Return the Maximum Likelihood estimate of the ngram.
	 * Since we're not doing any smoothing, we can directly
	 * use the counts of the lower-order ngrams.
	 */
	public double getNgramJointProbability(List<String> ngram){
		int ngramCount 	= ngc.getCount(ngram);
		int totalNgrams	= ngc.totalNgrams;
		
		return (double)ngramCount/totalNgrams;
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


	
}
