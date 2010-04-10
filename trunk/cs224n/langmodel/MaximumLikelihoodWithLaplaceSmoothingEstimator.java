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
	public double getNgramJointProbability(List<String> ngram){
		
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
	
}
