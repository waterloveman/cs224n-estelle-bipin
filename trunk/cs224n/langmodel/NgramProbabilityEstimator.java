package cs224n.langmodel;

import java.util.List;

public interface NgramProbabilityEstimator {

	public double getNgramJointProbability(List<String> ngram);
	public double getNgramConditionalProbability(List<String> ngram);
	public double estimatorCheckModel(int order, String distribution);
	
}
