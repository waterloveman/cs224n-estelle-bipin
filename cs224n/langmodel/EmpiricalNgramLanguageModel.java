package cs224n.langmodel;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.*;

import javax.sound.midi.SysexMessage;

import cs224n.langmodel.NgramCounter;

public class EmpiricalNgramLanguageModel implements LanguageModel	{
	
	// order of the n-gram.
	// unigrams have order-1
	// bigrams have order-2 etc.
	private int order = 1;
	
	// this will keep track of the counts of 
	// the ngrams we've seen in training
	private NgramCounter ngc;
	
	// used to estimate the probability of
	// an ngram
	private NgramProbabilityEstimator estimator;
	
	private static final String STOP = "</S>";
	private static final String START = "<S>";
	
	private String estimatorName = "";


	// -----------------------------------------------------------------------

	/**
	 * Constructs a new, empty ngram language model.
	 */
	public EmpiricalNgramLanguageModel() {
		ngc = new NgramCounter(order);
	
	}
	
	public void initialize(String estimator){
		estimatorName = estimator;
	}

	/**
	 * Constructs a ngram language model from a collection of sentences.  A
	 * special stop token is appended to each sentence, and then the
	 * frequencies of all words (including the stop token) over the whole
	 * collection of sentences are compiled.
	 */
	public EmpiricalNgramLanguageModel(Collection<List<String>> sentences, String estimator) {
		this();
		initialize(estimator);
		train(sentences);
	}


	// -----------------------------------------------------------------------

	/**
	 * Constructs a ngram language model from a collection of sentences.  A
	 * special stop token is appended to each sentence, and then the
	 * frequencies of all words (including the stop token) over the whole
	 * collection of sentences are compiled.
	 */
	public void train(Collection<List<String>> sentences) {
		
		// create a new ngram-counter every time
		// we train.
		// this way, we over-ride any previously
		// learnt knowledge
		ngc = new NgramCounter(order);
		
		// for every sentence, store the counts and the vocabulary
		float totalSentences = (float)sentences.size();
		System.out.println("\nReading "+totalSentences+" sentences...");
		int sentenceCounter = 0;
		for (List<String> sentence : sentences) {
		  sentenceCounter++;
		  if (sentenceCounter%1000==0){
			  System.out.println("   Reading sentence "+sentenceCounter+" ("+sentenceCounter/totalSentences*100+"%)");
		  }
		  List<String> stoppedSentence = new ArrayList<String>(sentence);
		  // add a STOP at the end, and N-1 START at the beginning
		  stoppedSentence.add(STOP);
		  for (int i=0; i<order-1; i++){
			  stoppedSentence.add(0,START);
		  }
		  
		  // debug :
		  //System.out.println("training on sentence :");
		  //for (int k=0; k<stoppedSentence.size(); k++){
			//  System.out.println(stoppedSentence.get(k)+" ");
		  //}
		  
		  // run a sliding window to extract every ngram
		  // and add it to our ngram counter
		  List<List<String>> ngrams = getNgramsFromSentence(stoppedSentence);
		  for (List<String> ngram: ngrams) {
			ngc.insert(ngram);
		  }
		  
		  
		}
		/*System.out.println(sentences.size()+" sentences read, containing "+ngc.totalNgrams+" tokens.");
		//debug :
	//System.out.println("(More precisely : "+ngc.NgramTokens[0]+" unigrams and "+ngc.NgramTokens[1]+" bigrams.)");
		
		System.out.print("Total n-grams : ");
		for (int i=0; i<order; i++){
			System.out.print(ngc.NgramVocabulary.get(i).size()+" "+(i+1)+"-grams, ");
		}
		System.out.println("tadaaaa!");
		*/
		
		 // debug : print out ngram vocab
		/*Set<List<String>> vocabUnigram = ngc.NgramVocabulary.get(0);
		Set<List<String>> vocabBigram = ngc.NgramVocabulary.get(1);		
		System.out.println("Unigram vocab :");
		for (List<String> ngram : vocabUnigram){
			  for (int i=0; i<ngram.size(); i++){
				  System.out.print(ngram.get(i)+" ");
			  }
			  System.out.println("");
		}
		System.out.println("Bigram vocab :");
		for (List<String> ngram : vocabBigram){
			  for (int i=0; i<ngram.size(); i++){
				  System.out.print(ngram.get(i)+" ");
			  }
			  System.out.println("");
		}		
		*/
		
		// build an estimator based on the the ngrams
		// we've seen in training.
		// create your own estimator here. this will be 
		// used in the rest of the functions like
		// generating-sentences, working out sentence-probabilities etc.
		if ("DeletedInterpolationEstimator".equals(estimatorName))	{
			estimator = new DeletedInterpolationEstimator(ngc, new MaximumLikelihoodWithDeltaSmoothingEstimator(ngc, order, 0.0001));
		}
		else if ("smoothedGoodTuring".equals(estimatorName))	{
			estimator = new smoothedGoodTuringEstimator(ngc,order);
		}
		else if ("WittenBell".equals(estimatorName))	{
			estimator = new WittenBellEstimator(ngc, order);
			WittenBellEstimator w = new WittenBellEstimator(ngc, order);
			w.checkModel();
			System.exit(0);
		}
		else if ("AbsoluteDiscounting".equals(estimatorName))	{
			estimator = new AbsoluteDiscountingEstimator(ngc, order, 0.75);
		}
		else if ("MaximumLikelihoodWithDeltaSmoothingEstimator".equals(estimatorName))	{
			estimator = new MaximumLikelihoodWithDeltaSmoothingEstimator(ngc, order, 0.0001);
		}
		else if ("MaximumLikelihoodWithLaplaceSmoothingEstimator".equals(estimatorName))	{
			estimator = new MaximumLikelihoodWithLaplaceSmoothingEstimator(ngc, order);
		}
		else if ("MaximumLikelihoodEstimator".equals(estimatorName))	{ 
			estimator = new MaximumLikelihoodEstimator(ngc);
		}
		else	{
			System.err.println("Unknown estimator requested");
			System.exit(-1);
		}
		
	}
	
	// -----------------------------------------------------------------------

	/**
	 * Generate all possible ngrams from a sentence of size 'order'.
	 * We do this by running a sliding window over the sentence
	 * and picking up ngrams
	 */	
	List<List<String>> getNgramsFromSentence(List<String> sentence)	{
		List<List<String>> ngrams = new ArrayList<List<String>>();
		
		for (int i = 0; i < sentence.size() - order + 1; ++i)	{
			List<String> ngram = getNgramAtIndex(sentence, i, order);
			ngrams.add(ngram);
		}
		
		return ngrams;
		
	}

	
	// -----------------------------------------------------------------------

	/**
	 * Helper fnc. which given a position and a size, will 
	 * return the ngram STARTTING at that position.
	 * If the start position is negative, the N-gram will start with <BOS> words.
	 */	
	List<String> getNgramAtIndex(List<String> sentence, int index, int size)	{
		List<String> ngram = new ArrayList<String>();
		for (int j = 0; j < size; ++j)	{
				ngram.add( j, sentence.get(index+j) );
		}		
		return ngram;
	}


	// -----------------------------------------------------------------------

	/**
	 * Returns the probability, according to the model, of the word specified
	 * by the argument sentence and index. If the words before it (its history) 
	 * play no role (as in the case of unigrams, or if it's the first word in 
	 * the sentence, then we return the probability of seeing the word in isolation.
	 * Otherwise, we determine the probability of the word as:
	 * P(Wn | Wn-1,n-order+1) = P(Wn-1,n-order+1 Wn) / P(Wn-1,n-order+1)
	 */
	public double getWordProbability(List<String> sentence, int index) {
		double probability = 0.0;
		if (order == 1 || index == 0){
			List<String> ngram = getNgramAtIndex(sentence, index, 1);
			probability = estimator.getNgramConditionalProbability(ngram);
		}
		else	{
			// todo: change this to directly calculate
			// ngram, not through prevWords. Be careful
			// to take care of edge cases.
			List<String> prevWords = getPrevWords(sentence, index);  
			List<String> ngram = new ArrayList<String>(prevWords);
			ngram.add(sentence.get(index));
						
			probability = estimator.getNgramConditionalProbability(ngram);
		}
		return probability;
	}
	/*
	public double getWordProbability(List<String> sentence, int index) {
		double probability = 0.0;
		
		if (order == 1 || index == 0){
			// no conditioning on previous words
			List<String> ngram = getNgramAtIndex(sentence, index, 1);
			probability = estimator.getNgramJointProbability(ngram);
		}
		else	{
			// condition on previous words
			List<String> prevWords = getPrevWords(sentence, index);  
			List<String> ngram = new ArrayList<String>(prevWords);
			ngram.add(sentence.get(index));
			
			if (estimator.getNgramJointProbability(prevWords) != 0)	{
				// probability = P(ngram) / P(prevWords)
				probability =  estimator.getNgramJointProbability(ngram) / estimator.getNgramJointProbability(prevWords);
			}
		}
		
		return probability;
	}
	*/
	
	// -----------------------------------------------------------------------

	/**
	 * Helper fnc. to return previous (order-1) words.
	 * Takes care of ensuring that if there are too few words, 
	 * all of them are returned. 
	 */
	List<String> getPrevWords(List<String> sentence, int index){
		int historyBeginIndex = index - order + 1;
		int historyLength;
		if (historyBeginIndex < 0)	{
			// there isn't enough history to condition on
			historyBeginIndex = 0;
			historyLength = index;
		}
		else	{
			historyLength = order - 1;
		}
		List<String> ngramHistory = getNgramAtIndex(sentence, historyBeginIndex, historyLength);
		return ngramHistory;
	}

	// -----------------------------------------------------------------------

	static int ns = 0;
	/**
	 * Returns the probability, according to the model, of the specified
	 * sentence.  This is the product of the probabilities of each word in
	 * the sentence (including a final stop token).
	 */
	public double getSentenceProbability(List<String> sentence) {
		
		List<String> stoppedSentence = new ArrayList<String>(sentence);
		stoppedSentence.add(STOP);
		for (int i=0; i<order-1; i++){
			stoppedSentence.add(0,START);
		}
		
		// replace all words not in the vocabulary with a special token
	/*	Set<String> vocabulary = ngc.vocabulary;
		ArrayList<String> modifiedSentence = new ArrayList<String>();
		for (int i = 0; i < sentence.size(); ++i){
			String word = sentence.get(i);
			if (!vocabulary.contains(word)){
				modifiedSentence.add(i, "--UNK--");
			}
			else	{
				modifiedSentence.add(i, word);
			}
		}
		sentence = modifiedSentence;
		*/
		
		// find the product of the probabilities of
		// every word
		++ns;
//		System.out.println("Checking getSentence for sentence-" + ns + ": " + sentence);
		double probability = 1.0;
		for (int index = 0; index < stoppedSentence.size(); index++) {
			probability *= getWordProbability(stoppedSentence, index);
		}
		return probability;
	}

	public double checkModel()	{
		// Choose one of the two methods to
		// check your model. The conditional-probability
		// table way is slightly more clunkier, since
		// it has to take unseen columns into account.
		
		//return checkModelUsingJointProbability();
		//return checkModelUsingConditionalProbability();
		int ord = 1; // order of the distribution you want to check
		String distribution = "joint"; // type of distribution that you want to check
		//String distribution = "conditional";
		return estimator.estimatorCheckModel(order, distribution);
	}
	
	/**
	 * Returns the sum of the probabilities of the every ngram
	 * we might encounter. This should be equal to 1.
	 * The language-space object returns every possible set of tokens we
	 * might encounter, including a special UNK token, which takes care
	 * of novel words that we might encounter.
	 */
	private double checkModelUsingJointProbability() {
        
		double sum = 0.0;
		LanguageSpace ls = new LanguageSpace(ngc.vocabulary, order);
		while (ls.hasMore())	{
			List<String> ngram = ls.getNext();
			sum += estimator.getNgramJointProbability(ngram);
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
	private double checkModelUsingConditionalProbability()	{
		double sum = 0.0;
		int n = 0;
		LanguageSpace ls = new LanguageSpace(ngc.vocabulary, order);
		while (ls.hasMore())	{
			List<String> ngram = ls.getNext();
			sum += estimator.getNgramConditionalProbability(ngram);
			if (estimator.getNgramConditionalProbability(ngram) > 0.00001){
				++n;
			}
		}
		
		return sum / (n / ngc.vocabulary.size());
		
	}
	
	
	/**
	 * Returns a random word sampled according to the model.  A simple
	 * "roulette-wheel" approach is used: first we generate a sample uniform
	 * on [0, 1]; then we step through the vocabulary eating up probability
	 * mass until we reach our sample.
	 * We do this by finding the probability of the next word, for every 
	 * word, based on the sentence built so far.
	 */
	public String generateWord(List<String> sentenceThusFar) {
		double sample = Math.random();
		double sum = 0.0;
		
		// not really reqd. but we'll use it
		// to aid debugging for now
		boolean foundBest = false;
		String best = "";
		
		// for every word, find the probability
		// that it follows the sentence we has until now
		for (String word : ngc.vocabulary) {
			List<String> tmpSentence = new ArrayList<String>(sentenceThusFar);
			tmpSentence.add(word);
			int index = tmpSentence.size()-1;
			
			sum += getWordProbability(tmpSentence, index);	
			if (sum > sample && !foundBest)	{
				// we could return here, but I keep going.
				// helps debugging
				//return word;		
				best = word;
				foundBest = true;
			}
			
		}
		// debug
		//System.out.println("Sum: " + sum);
		return best;
	}

	/**
	 * Returns a random sentence sampled according to the model.  We generate
	 * words until the stop token is generated, and return the concatenation.
	 */
	public List<String> generateSentence() {

		List<String> sentence = new ArrayList<String>();
		for (int i=1; i<order; i++){
			sentence.add(START);
		}
		String word = generateWord(sentence);
		while (!word.equals(STOP)) {
		sentence.add(word);
		word = generateWord(sentence);
		}
		return sentence;
	}

	

}
