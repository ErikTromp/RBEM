package nl.et4it.examples;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;

import nl.et4it.RBEMEmotion;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

public class RBEMEmotionExample {
	public static void main(String[] args) {
		RBEMEmotion rbem = new RBEMEmotion();
		rbem.loadModel("en_UK");

		try {
			List<Pair<String, String>> sentences = new LinkedList<Pair<String, String>>();
			sentences.add(new ImmutablePair<String, String>(
					"this new car is so good !", "DT JJ NN VBZ RB JJ SENT"));
			sentences.add(new ImmutablePair<String, String>(
					"this new car is so bad !", "DT JJ NN VBZ RB JJ SENT"));
			sentences.add(new ImmutablePair<String, String>(
					"I can ' t remember what I said",
					"PP NN POS NN VV WP PP VVD"));
			sentences.add(new ImmutablePair<String, String>(
					"this car is not so good .", "DT NN VBZ RB RB JJ SENT"));
			sentences.add(new ImmutablePair<String, String>(
					"this car used to be good , but it is now very bad",
					"DT NN VVN TO VB JJ , CC PP VBZ RB RB JJ"));

			for (Pair<String, String> sentence : sentences) {
				String tokens = sentence.getLeft();
				String tags = sentence.getRight();

				// Classify it
				HashMap<String, Double> emotions = rbem.classify(tokens, tags,
						true);
				System.out.println(tokens);
				for (Entry<String, Double> entry : emotions.entrySet())
					System.out
							.println(entry.getKey() + ": " + entry.getValue());
				System.out.println();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
