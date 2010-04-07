package cs224n.langmodel;

import java.util.List;

public interface NgramProbabilityEstimator {

	public double getNgramProbability(List<String> ngram);
	
}
