package nl.et4it.examples;

import java.util.LinkedList;
import java.util.List;

import nl.et4it.RBEMPolarity;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

public class RBEMPolarityExample {
	public static void main(String[] args) {
		RBEMPolarity rbem = new RBEMPolarity();
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
				Pair<String, Double> polarity = rbem.classify(tokens, tags);
				System.out.println(tokens);
				System.out.println(polarity.getLeft());
				System.out.println(polarity.getRight());
				System.out.println();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
