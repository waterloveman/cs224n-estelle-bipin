package cs224n.langmodel;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.math.BigInteger;

public class LanguageSpace {

	private BigInteger index;
	private BigInteger maxPermutations;	

	private int order;
	private int vocabularySize;
	private Object[] words;
	
	LanguageSpace(Set<String> vocabulary, int o)	{
		order = o;
		index = BigInteger.valueOf(0);
		words = vocabulary.toArray();
		vocabularySize = vocabulary.size();
		maxPermutations = BigInteger.valueOf(vocabularySize).pow(order);
	}
	
	public BigInteger size()	{
		return maxPermutations;
	}
	
	public void reset()	{
		index = BigInteger.valueOf(0);
	}
	
	public boolean hasMore()	{
		return !index.equals(maxPermutations); 
	}
	
	public List<String> getNext()	{
		List<Integer> indices = getNumberInBase(index, vocabularySize, order);
		ArrayList<String> permutation = new ArrayList<String>();
		for (int j = 0; j < indices.size(); ++j)	{
			permutation.add(j, (String)words[indices.get(j)]);
		}
		
		index = index.add(BigInteger.valueOf(1));
		if (index.mod(BigInteger.valueOf(100000)).equals(BigInteger.valueOf(0)))	{
			System.err.print("LanguageSpace: Returning point " + index + " out of " + maxPermutations);
			System.err.format(" [%03.2f%%]\n", (index.doubleValue()/maxPermutations.doubleValue()*100));
		}
		return permutation;

	}

	private List<Integer> getNumberInBase(BigInteger number, int base, int size)	{
		LinkedList<Integer> result = new LinkedList<Integer>();
		
		while (!number.equals(BigInteger.valueOf(0)))	{
			int digit = number.mod(BigInteger.valueOf(base)).intValue();
			result.push(digit);
			number = number.divide(BigInteger.valueOf(base));
		}
		
		// pad to fill size
		while (result.size() != size){
			result.push(new Integer(0));
		}
	
		return result;
		
	}

	
}
