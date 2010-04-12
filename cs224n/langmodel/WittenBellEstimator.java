package cs224n.langmodel;

import java.util.ArrayList;
import java.util.List;

public class WittenBellEstimator implements NgramProbabilityEstimator {
	
	NgramCounter ngc;
	int order;
	
	public WittenBellEstimator(NgramCounter n, int o){
		ngc = n;
		order = o;
	}
	
	// -----------------------------------------------------------------------

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
	
	public double getNgramConditionalProbability(List<String> ngram) {
		
		double probability = 0.0;
		
//		System.out.println("ngram: " + ngram + " ngvs " + ngc.NgramVocabulary.size());
		if (ngram.size() == 1){
			// no conditioning on previous words
			int T = ngc.vocabulary.size() - 1;
			if (T == 0)	T = 1;		// border case.
			int N = ngc.totalNgrams;
			int Z = ngc.nZeroCountNgrams(1);
			
//			System.out.print("ngram: " + ngram + "\n\tT: " + T + "\n\tN: " + N + "\n\tZ: " + Z + "\n\t");
			
			int ngramCount = ngc.getCount(ngram);
			if (ngramCount == 0)	{
				probability = (double)T / (Z * (N+T));
//				System.out.println(probability);
			}
			else	{
				probability = (double)ngramCount / (N+T);
//				System.out.println(probability);
			}
		}
		else	{
			// condition on previous words
			
			int ngramCount = ngc.getCount(ngram);
			if (ngramCount == 0)	{
				List<String> prevWords = new ArrayList<String>(ngram);
				prevWords.remove(ngram.size()-1); // remove last word
	 
				int T = nTypesFollow(prevWords);
				if (T == 0)	T = 1;		// border case.
				int N = nTokensFollow(prevWords);
				int Z = nZeroCountTypesFollow(prevWords);
				
//				System.out.println("T: " + T + "\nZ: " + Z + "\nN: " + N);
				
				if (Z*(N+T) > 0)	{
					probability = (double)T / (Z * (N+T));
				}
				else	{
					probability = 0.0;
				}
				
			}
			else	{
				List<String> prevWords = new ArrayList<String>(ngram);
				prevWords.remove(ngram.size()-1); // remove last word
				
				int prevWordsCount = ngc.getCount(prevWords);
				int T = nTypesFollow(prevWords);
				
//				System.out.println(ngramCount + "\n" + prevWordsCount + "\n" + T);
				probability = (double)ngramCount / (prevWordsCount+T);
			}
			
		}
		
		return probability;
	}
	
	private int nTypesFollow(List<String> ngram)	{
		
		// walk through out ngramCounter data-structure
		// and find out the number of types that this
		// ngram is a prefix to
		Table<String, Table> t = ngc.table;
		for (String word : ngram)	{
			
			if (!t.containsKey(word))	{
				return 0;
			}
		
			t = (Table<String, Table>)t.get(word);
		}
		
		return t.hash.size();
	}

	private int nZeroCountTypesFollow(List<String> ngram)	{
		return ngc.vocabulary.size() - nTypesFollow(ngram);
	}
	
	private int nTokensFollow(List<String> ngram)	{
		return ngc.getCount(ngram);
	}

	
	/* debug */
	boolean checkModel()	{
		double sum = 0;
		ArrayList<String> tmp1 = new ArrayList<String>(); tmp1.add("a");
		for (String a : ngc.vocabulary){
			System.out.print(a + "\t");
			for (String word : ngc.vocabulary)	{
				List<String> ngram = new ArrayList<String>();
				ngram.add(word);
				ngram.add(a);
				sum += getNgramConditionalProbability(ngram);
				System.out.format("%.2f ", getNgramConditionalProbability(ngram));
//				System.out.println("Checking ngram " + ngram);
//				System.out.println("\tcond: " + getNgramConditionalProbability(ngram));
//				System.out.println("\tjoint: " + getNgramJointProbability(prevWords));
//				System.out.println("\ttotal: " + getNgramConditionalProbability(ngram)*getNgramJointProbability(prevWords));
			}
			System.out.println("");
		}
		System.out.println("Sum: " + sum);
		return true;
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
