package cs224n.langmodel;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;


public class MaximumLikelihoodWithLaplaceSmoothingEstimator implements NgramProbabilityEstimator	{
	
	private NgramCounter ngc;
	private LanguageSpace ls;
	private int order;
	private BigInteger langaugeSpaceSize;
	
	public MaximumLikelihoodWithLaplaceSmoothingEstimator(NgramCounter n, int o){
		ngc = n;
		order = o;
		ls = new LanguageSpace(ngc.vocabulary, order);
		langaugeSpaceSize = ls.size();
	}

	// -----------------------------------------------------------------------

	/**
	 * Calculate the ML estimate of the ngram.
	 * To avoid a probability of '0', we pretend as if we have seen
	 * every ngram once more than it appears in the training corpus.
	 */
	public double getNgramProbability(List<String> ngram){
		
		int o = ngram.size();

		// check to see if we've been asked for the
		// highest order ngram probability
		if (o == order){
			int ngramCount 	= ngc.getCount(ngram);
			int totalNgrams	= ngc.totalNgrams;
			
			double numerator	= ngramCount + 1;
			double denominator	= totalNgrams + langaugeSpaceSize.doubleValue();
			return numerator / denominator;
			
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
				sum += getNgramProbability(tmpNgram);
			}
			return sum;
		}
		
				
	}
	
}
