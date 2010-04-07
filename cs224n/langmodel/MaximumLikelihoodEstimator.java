package cs224n.langmodel;

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
	public double getNgramProbability(List<String> ngram){
		int ngramCount 	= ngc.getCount(ngram);
		int totalNgrams	= ngc.totalNgrams;
		
		return (double)ngramCount/totalNgrams;
	}

	
}
