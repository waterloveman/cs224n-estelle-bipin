package cs224n.langmodel;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

public class MaximumLikelihoodWithDeltaSmoothingEstimator implements NgramProbabilityEstimator	{
	
	private Double delta;
	private NgramCounter ngc;
	private LanguageSpace ls;
	private int order;
	private BigInteger langaugeSpaceSize;
	
	
	public MaximumLikelihoodWithDeltaSmoothingEstimator(NgramCounter n, int o, double d){
		ngc = n;
		order = o;
		delta = d;
		ls = new LanguageSpace(ngc.vocabulary, order);
		langaugeSpaceSize = ls.size();
	}
	
	public double getNgramProbability(List<String> ngram){
		
	    // get count of the ngram
		int ngramCount 	= ngc.getCount(ngram);
		// get count of history
		List<String> n_1gram = new ArrayList<String>();
		for (int i=0; i<ngram.size()-1; i++){
			n_1gram.add(ngram.get(i));
		}
		int n_1gramCount = ngc.getCount(n_1gram);
		int totalNgrams	= ngc.totalNgrams;
		int o = ngram.size();
		
		// check to see if we've been asked for the
		// highest order ngram probability
		if (o == order){
			double numerator 	= ngramCount + delta;
			double denominator 	= totalNgrams + langaugeSpaceSize.doubleValue() * delta;  
			//double denominator = n_1gramCount + langaugeSpaceSize.doubleValue() * delta;
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
	
	public double checkModel(){
		double sum = 0.0;
		// total number of seen N-grams (types):
		double seenNGrams = ngc.NgramVocabulary.size();
		// total number of unseen N-grams (types):
		double unseenNGrams = langaugeSpaceSize.doubleValue() - seenNGrams;
		
		// probability mass of the seen values :
		// read the whole table of the NGramCounter :
		Table t = ngc.table;
		for (Object w1 : t.keySet()){
			
		}
		
		return sum;
		
	}
	
}

