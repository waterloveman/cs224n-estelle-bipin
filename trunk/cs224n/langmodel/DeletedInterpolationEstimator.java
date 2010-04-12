package cs224n.langmodel;

import java.util.ArrayList;
import java.util.List;

public class DeletedInterpolationEstimator implements NgramProbabilityEstimator	{
	
	private NgramCounter ngc;
	private NgramProbabilityEstimator baseEstimator;
	
	public DeletedInterpolationEstimator(NgramCounter n, NgramProbabilityEstimator baseEst)	{
		ngc = n;
		baseEstimator = baseEst;
	}
	/**
	 * Return the Maximum Likelihood estimate of the ngram.
	 * Since we're not doing any smoothing, we can directly
	 * use the counts of the lower-order ngrams.
	 */
	public double getNgramJointProbability(List<String> ngram){
		
		// todo: figure out the best way to do this
		// Here, I invoke the chain rule to calculate
		// the joint probability
		if (ngram.size() == 1){
			return getNgramConditionalProbability(ngram);
		}
		else	{
			List<String> prevWords = new ArrayList<String>(ngram);
			prevWords.remove(ngram.size()-1); // remove last word
			
			// p(ngram) = p(last-word|prevwords) * p(prevwords)
			return getNgramConditionalProbability(ngram) * getNgramJointProbability(prevWords);
			
		}
		
	}
	
	public double getNgramConditionalProbability(List<String> ngram)	{
		
		double lambda1 = 0.75;
		double lambda2 = 0.25;
	
		if (ngram.size() == 1){
			return baseEstimator.getNgramConditionalProbability(ngram);
		}
		else	{
			ArrayList<String> n_minus_1_gram = new ArrayList(ngram);
			n_minus_1_gram.remove(0);
			
			return 	lambda1 * baseEstimator.getNgramConditionalProbability(ngram) +
					lambda2 * baseEstimator.getNgramConditionalProbability(n_minus_1_gram);
		}
	}
		
	public double estimatorCheckModel(int order, String distribution)	{
		return checkModelUsingConditionalProbability(order);
//		if ("conditional".equals(distribution))	{
//			return checkModelUsingConditionalProbability(order);
//		}
//		else	{
//			return checkModelUsingJointProbability(order);
//		}
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
