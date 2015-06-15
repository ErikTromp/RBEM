package nl.et4it;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.ImmutableTriple;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.JsonToken;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;

public class RBEMEmotion {
	/*
	 * This is the actual model of the algorithm, it's kept in-memory for fast
	 * access
	 * 
	 * It works with indexing as follows:
	 * 
	 * The first key is the pattern type, then for each pattern type: The next
	 * key is the pattern itself, defined by a concatenation of the first word
	 * and POS-tag pair of the pattern, then for each such pattern: The next key
	 * is the actual length (number of (word, POS-tag)-pairs of the pattern,
	 * then for each pattern of such length: The next key is the complete
	 * concatenation of the pattern's (word, POS-tag) pairs, finally: A list of
	 * actual (word, POS-tag)-pairs, in-order as a List whose size is always 2
	 */
	HashMap<String, HashMap<String, HashMap<Integer, HashMap<String, List<Pair<String, String>>>>>> model = new HashMap<String, HashMap<String, HashMap<Integer, HashMap<String, List<Pair<String, String>>>>>>();
	Integer emissionRange = 3;

	public RBEMEmotion() {
		// Initialize the model
		model.put(
				"amplifiers",
				new HashMap<String, HashMap<Integer, HashMap<String, List<Pair<String, String>>>>>());
		model.put(
				"attenuators",
				new HashMap<String, HashMap<Integer, HashMap<String, List<Pair<String, String>>>>>());
		model.put(
				"continuators",
				new HashMap<String, HashMap<Integer, HashMap<String, List<Pair<String, String>>>>>());
		model.put(
				"leftflips",
				new HashMap<String, HashMap<Integer, HashMap<String, List<Pair<String, String>>>>>());
		model.put(
				"neutrals",
				new HashMap<String, HashMap<Integer, HashMap<String, List<Pair<String, String>>>>>());
		model.put(
				"objectives",
				new HashMap<String, HashMap<Integer, HashMap<String, List<Pair<String, String>>>>>());
		model.put(
				"rightflips",
				new HashMap<String, HashMap<Integer, HashMap<String, List<Pair<String, String>>>>>());
		model.put(
				"stops",
				new HashMap<String, HashMap<Integer, HashMap<String, List<Pair<String, String>>>>>());
		model.put(
				"joy",
				new HashMap<String, HashMap<Integer, HashMap<String, List<Pair<String, String>>>>>());
		model.put(
				"sadness",
				new HashMap<String, HashMap<Integer, HashMap<String, List<Pair<String, String>>>>>());
		model.put(
				"trust",
				new HashMap<String, HashMap<Integer, HashMap<String, List<Pair<String, String>>>>>());
		model.put(
				"disgust",
				new HashMap<String, HashMap<Integer, HashMap<String, List<Pair<String, String>>>>>());
		model.put(
				"fear",
				new HashMap<String, HashMap<Integer, HashMap<String, List<Pair<String, String>>>>>());
		model.put(
				"anger",
				new HashMap<String, HashMap<Integer, HashMap<String, List<Pair<String, String>>>>>());
		model.put(
				"surprise",
				new HashMap<String, HashMap<Integer, HashMap<String, List<Pair<String, String>>>>>());
		model.put(
				"anticipation",
				new HashMap<String, HashMap<Integer, HashMap<String, List<Pair<String, String>>>>>());
	}

	/**
	 * Given a set of words and POS-tag, this function makes a list that can be
	 * used directly by RBEM
	 * 
	 * @param words
	 *            List<String> The words
	 * @param tags
	 *            List<String> The POS-tags
	 * @return List<List<String>> The vector
	 */
	private List<Pair<String, String>> makeVector(List<String> words,
			List<String> tags) throws Exception {
		// See if there is an equal numebr of words and tags
		if (words.size() != tags.size())
			throw new Exception(
					"The number of words is unequal to the number of POS-tags");

		// Result list
		List<Pair<String, String>> vector = new ArrayList<Pair<String, String>>();
		// Go through words and tags
		for (int i = 0; i < words.size(); i++)
			vector.add(new ImmutablePair<String, String>(words.get(i)
					.toLowerCase(), tags.get(i)));

		// Return the vector
		return vector;
	}

	/**
	 * Matches a single pattern for which there is clue it might potentially
	 * occur within the sentence
	 * 
	 * @param sentence
	 *            List[(String, String)] The remaining sentence that needs to be
	 *            processed, represented as (token, tag) pairs
	 * @param pattern
	 *            List[(String, String)] The remaining pattern that needs to be
	 *            processed, represented as (token, tag) pairs
	 * @param offset
	 *            Int The current offset, the position within the sentence we
	 *            are at
	 * @param (Boolean, Int) A tuple indicating first whether the pattern was
	 *        found or not and the end position of the pattern We do not return
	 *        the starting position because the initial call to this function
	 *        will know (in offset) the starting position
	 */
	private Pair<Boolean, Integer> matchPattern(
			List<Pair<String, String>> sentence,
			List<Pair<String, String>> pattern, int offset) {
		if (pattern.isEmpty() || offset > 500) {
			// Found the right pattern, return where we found it
			return new ImmutablePair<Boolean, Integer>(true, offset - 1);
		} else {
			// Get the first pair of word, POS-tag that we need to match
			Pair<String, String> member = pattern.get(0);
			// Pattern is not depleted yet, maybe sentence is though
			if (sentence.isEmpty()) {
				// Sentence is depleted, we failed to match our pattern
				return new ImmutablePair<Boolean, Integer>(false, -1);
			} else {
				// Get the token and tag of the current sentence member
				String sToken = sentence.get(0).getLeft();
				String sTag = sentence.get(0).getRight();

				// Get the sentence that is yet to be parsed as well as the
				// pattern yet to be parsed
				List<Pair<String, String>> remainingSentence = sentence
						.subList(1, sentence.size());
				List<Pair<String, String>> remainingPattern = pattern.subList(
						1, pattern.size());

				// We need to traverse further based on our pattern member
				if (member.getLeft().equals("*")
						&& member.getRight().equals("*")) {
					// We are dealing with a multi-position wildcard here
					if (!remainingSentence.isEmpty()) {
						if (remainingPattern.isEmpty()) {
							// Pattern wasn't found
							return new ImmutablePair<Boolean, Integer>(false,
									-1);
						} else {
							// Since we are dealing with a wild-card, we need to
							// peek forward
							Pair<String, String> nextPatternMember = remainingPattern
									.get(0);
							Pair<String, String> nextSentenceMember = remainingSentence
									.get(0);

							// Check for match
							if ((nextPatternMember.getLeft().equals(
									nextSentenceMember.getLeft()) && nextPatternMember
									.getRight().equals(
											nextSentenceMember.getRight()))
									|| (nextPatternMember.getLeft().equals("_") && nextPatternMember
											.getRight().equals(
													nextSentenceMember
															.getRight()))
									|| (nextPatternMember.getLeft().equals(
											sToken) && nextPatternMember
											.getRight().equals(sTag))
									|| (nextPatternMember.getLeft().equals("_") && nextPatternMember
											.getRight().equals(sTag))) {
								// Next also matches, shift by 2
								return matchPattern(remainingSentence.subList(
										1, remainingSentence.size()),
										remainingPattern.subList(1,
												remainingPattern.size()),
										offset + 2);
							} else {
								// No direct match, maybe next element
								return matchPattern(remainingSentence, pattern,
										offset + 1);
							}
						}
					} else
						return new ImmutablePair<Boolean, Integer>(false, -1);
				} else if (member.getLeft().equals("_")
						&& member.getRight().equals("_")) {
					// Single-position wildcard
					return matchPattern(remainingSentence, remainingPattern,
							offset + 1);
				} else if (member.getLeft().equals("_")
						&& member.getRight().equals(member.getRight())) {
					// Just match the tag
					if (sTag.equals(member.getRight())) {
						// Match, continue
						return matchPattern(remainingSentence,
								remainingPattern, offset + 1);
					} else
						return new ImmutablePair<Boolean, Integer>(false, -1);
				} else {
					// Regular match, check if current pairs match
					if (sToken.equals(member.getLeft())
							&& sTag.equals(member.getRight())) {
						// Match, continue
						return matchPattern(remainingSentence,
								remainingPattern, offset + 1);
					} else
						return new ImmutablePair<Boolean, Integer>(false, -1);
				}
			}
		}
	}

	/**
	 * Matches patterns of a given pattern type against a sentence
	 * 
	 * @param sentence
	 *            List[(String, String)] The sentence represented as a list of
	 *            (token, tag) tuples
	 * @param patternType
	 *            String The pattern type to match the known patterns of
	 * @return List[(List[(String, String)], Int, Int)] A list containing all
	 *         matched patterns as (token, tag) pairs and their respective start
	 *         and end positions within the sentence
	 */
	private List<Triple<List<Pair<String, String>>, Integer, Integer>> matchPatterns(
			List<Pair<String, String>> sentence, String patternType) {
		// This will return all our patterns found, together with start- and
		// end-index
		List<Triple<List<Pair<String, String>>, Integer, Integer>> foundPatterns = new ArrayList<Triple<List<Pair<String, String>>, Integer, Integer>>();

		// Since the model is highly indexed, we iterate through the sentence
		// and match it against our model
		List<Pair<String, String>> runningSentence = sentence;
		for (int i = 0; i < sentence.size(); i++) {
			// Get the current pair
			Pair<String, String> tuple = sentence.get(i);
			// Compute first index, concatenation of word and POS-tag, taking
			// the possibility of a wildcard into account
			String firstIndex = tuple.getLeft() + tuple.getRight();
			String firstIndexWildcard = "_" + tuple.getRight();

			// Match the regular pattern
			if (model.get(patternType).containsKey(firstIndex)) {
				// We are dealing with a regulard index (word AND pos-tag
				// given), find all matches
				for (Entry<Integer, HashMap<String, List<Pair<String, String>>>> entry1 : model
						.get(patternType).get(firstIndex).entrySet()) {
					// Get length and the patterns present in this pattern group
					HashMap<String, List<Pair<String, String>>> patterns = entry1
							.getValue();
					// Now get all the patterns of this length
					for (Entry<String, List<Pair<String, String>>> entry2 : patterns
							.entrySet()) {
						// Get the full index of the pattern and the pattern
						// itself
						List<Pair<String, String>> pattern = entry2.getValue();

						// Do a match
						Pair<Boolean, Integer> pFound = matchPattern(
								runningSentence, pattern, i);
						// If we found a pattern, we ought to return it
						if (pFound.getLeft()) {
							// Add it
							foundPatterns
									.add(new ImmutableTriple<List<Pair<String, String>>, Integer, Integer>(
											pattern, i, pFound.getRight()));
						}
					}
				}
			}
			// Now do the same for the wildcard first-index
			if (model.get(patternType).containsKey(firstIndexWildcard)) {
				for (Entry<Integer, HashMap<String, List<Pair<String, String>>>> entry1 : model
						.get(patternType).get(firstIndexWildcard).entrySet()) {
					// Get length and the patterns present in this pattern group
					HashMap<String, List<Pair<String, String>>> patterns = entry1
							.getValue();
					// Now get all the patterns of this length
					for (Entry<String, List<Pair<String, String>>> entry2 : patterns
							.entrySet()) {
						// Get the full index of the pattern and the pattern
						// itself
						List<Pair<String, String>> pattern = entry2.getValue();

						// Do a match
						Pair<Boolean, Integer> pFound = matchPattern(
								runningSentence, pattern, i);
						// If we found a pattern, we ought to return it
						if (pFound.getLeft()) {
							// Add it
							foundPatterns
									.add(new ImmutableTriple<List<Pair<String, String>>, Integer, Integer>(
											pattern, i, pFound.getRight()));
						}
					}
				}
			}

			// Move sliding window of running sentence
			runningSentence = runningSentence
					.subList(1, runningSentence.size());
		}

		return foundPatterns;
	}

	/**
	 * Checks if a pattern - based on its start and end positions - is subsumed
	 * within another
	 * 
	 * @param start
	 *            Int The starting position of the pattern
	 * @param end
	 *            Int The ending position of the pattern
	 * @param patternList
	 *            List[(List[(String, String)], Int, Int)] The list of patterns
	 *            to match against
	 */
	private Boolean isNotSubsumed(
			Integer start,
			Integer end,
			List<Triple<List<Pair<String, String>>, Integer, Integer>> patternList) {
		if (patternList.isEmpty())
			return true;

		Boolean result = true;
		// Go through all patterns to find subsumed ones
		for (int i = 0; i < patternList.size(); i++) {
			Integer matchStart = patternList.get(i).getMiddle();
			Integer matchEnd = patternList.get(i).getRight();

			// Check the offsets
			if (matchStart <= start && matchEnd > end) {
				// SUbsumed
				result = false;
				break;
			}
		}
		return result;
	}

	/**
	 * Removes all patterns that are somehow subsumed by other patterns
	 */
	private HashMap<String, List<Triple<List<Pair<String, String>>, Integer, Integer>>> removeSubsumed(
			List<Triple<List<Pair<String, String>>, Integer, Integer>> amplifierPatterns,
			List<Triple<List<Pair<String, String>>, Integer, Integer>> attenuatorPatterns,
			List<Triple<List<Pair<String, String>>, Integer, Integer>> continuatorPatterns,
			List<Triple<List<Pair<String, String>>, Integer, Integer>> leftflipPatterns,
			List<Triple<List<Pair<String, String>>, Integer, Integer>> neutralPatterns,
			List<Triple<List<Pair<String, String>>, Integer, Integer>> objectivePatterns,
			List<Triple<List<Pair<String, String>>, Integer, Integer>> rightflipPatterns,
			List<Triple<List<Pair<String, String>>, Integer, Integer>> stopPatterns,
			List<Triple<List<Pair<String, String>>, Integer, Integer>> joyPatterns,
			List<Triple<List<Pair<String, String>>, Integer, Integer>> sadnessPatterns,
			List<Triple<List<Pair<String, String>>, Integer, Integer>> fearPatterns,
			List<Triple<List<Pair<String, String>>, Integer, Integer>> angerPatterns,
			List<Triple<List<Pair<String, String>>, Integer, Integer>> trustPatterns,
			List<Triple<List<Pair<String, String>>, Integer, Integer>> disgustPatterns,
			List<Triple<List<Pair<String, String>>, Integer, Integer>> surprisePatterns,
			List<Triple<List<Pair<String, String>>, Integer, Integer>> anticipationPatterns) {

		// TODO: Make this bit tidier
		// Go through the patterns per group and see whether they are contained
		// within another pattern or not (yeah it's ugly)
		List<Triple<List<Pair<String, String>>, Integer, Integer>> newAmplifierPatterns = new ArrayList<Triple<List<Pair<String, String>>, Integer, Integer>>();
		{
			List<Triple<List<Pair<String, String>>, Integer, Integer>> pList = attenuatorPatterns;
			pList.addAll(continuatorPatterns);
			pList.addAll(leftflipPatterns);
			pList.addAll(neutralPatterns);
			pList.addAll(objectivePatterns);
			pList.addAll(rightflipPatterns);
			pList.addAll(stopPatterns);
			pList.addAll(joyPatterns);
			pList.addAll(sadnessPatterns);
			pList.addAll(fearPatterns);
			pList.addAll(angerPatterns);
			pList.addAll(trustPatterns);
			pList.addAll(disgustPatterns);
			pList.addAll(surprisePatterns);
			pList.addAll(anticipationPatterns);
			for (int i = 0; i < amplifierPatterns.size(); i++) {
				Triple<List<Pair<String, String>>, Integer, Integer> elem = amplifierPatterns
						.get(i);
				if (isNotSubsumed(elem.getMiddle(), elem.getRight(), pList))
					newAmplifierPatterns.add(elem);
			}
		}

		List<Triple<List<Pair<String, String>>, Integer, Integer>> newAttenuatorPatterns = new ArrayList<Triple<List<Pair<String, String>>, Integer, Integer>>();
		{
			List<Triple<List<Pair<String, String>>, Integer, Integer>> pList = amplifierPatterns;
			pList.addAll(continuatorPatterns);
			pList.addAll(leftflipPatterns);
			pList.addAll(neutralPatterns);
			pList.addAll(objectivePatterns);
			pList.addAll(rightflipPatterns);
			pList.addAll(stopPatterns);
			pList.addAll(joyPatterns);
			pList.addAll(sadnessPatterns);
			pList.addAll(fearPatterns);
			pList.addAll(angerPatterns);
			pList.addAll(trustPatterns);
			pList.addAll(disgustPatterns);
			pList.addAll(surprisePatterns);
			pList.addAll(anticipationPatterns);
			for (int i = 0; i < attenuatorPatterns.size(); i++) {
				Triple<List<Pair<String, String>>, Integer, Integer> elem = attenuatorPatterns
						.get(i);
				if (isNotSubsumed(elem.getMiddle(), elem.getRight(), pList))
					newAttenuatorPatterns.add(elem);
			}
		}

		List<Triple<List<Pair<String, String>>, Integer, Integer>> newContinuatorPatterns = new ArrayList<Triple<List<Pair<String, String>>, Integer, Integer>>();
		{
			List<Triple<List<Pair<String, String>>, Integer, Integer>> pList = amplifierPatterns;
			pList.addAll(attenuatorPatterns);
			pList.addAll(leftflipPatterns);
			pList.addAll(neutralPatterns);
			pList.addAll(objectivePatterns);
			pList.addAll(rightflipPatterns);
			pList.addAll(stopPatterns);
			pList.addAll(joyPatterns);
			pList.addAll(sadnessPatterns);
			pList.addAll(fearPatterns);
			pList.addAll(angerPatterns);
			pList.addAll(trustPatterns);
			pList.addAll(disgustPatterns);
			pList.addAll(surprisePatterns);
			pList.addAll(anticipationPatterns);
			for (int i = 0; i < continuatorPatterns.size(); i++) {
				Triple<List<Pair<String, String>>, Integer, Integer> elem = continuatorPatterns
						.get(i);
				if (isNotSubsumed(elem.getMiddle(), elem.getRight(), pList))
					newContinuatorPatterns.add(elem);
			}
		}

		List<Triple<List<Pair<String, String>>, Integer, Integer>> newLeftflipPatterns = new ArrayList<Triple<List<Pair<String, String>>, Integer, Integer>>();
		{
			List<Triple<List<Pair<String, String>>, Integer, Integer>> pList = amplifierPatterns;
			pList.addAll(attenuatorPatterns);
			pList.addAll(continuatorPatterns);
			pList.addAll(neutralPatterns);
			pList.addAll(objectivePatterns);
			pList.addAll(rightflipPatterns);
			pList.addAll(stopPatterns);
			pList.addAll(joyPatterns);
			pList.addAll(sadnessPatterns);
			pList.addAll(fearPatterns);
			pList.addAll(angerPatterns);
			pList.addAll(trustPatterns);
			pList.addAll(disgustPatterns);
			pList.addAll(surprisePatterns);
			pList.addAll(anticipationPatterns);
			for (int i = 0; i < leftflipPatterns.size(); i++) {
				Triple<List<Pair<String, String>>, Integer, Integer> elem = leftflipPatterns
						.get(i);
				if (isNotSubsumed(elem.getMiddle(), elem.getRight(), pList))
					newLeftflipPatterns.add(elem);
			}
		}

		List<Triple<List<Pair<String, String>>, Integer, Integer>> newNeutralPatterns = new ArrayList<Triple<List<Pair<String, String>>, Integer, Integer>>();
		{
			List<Triple<List<Pair<String, String>>, Integer, Integer>> pList = amplifierPatterns;
			pList.addAll(attenuatorPatterns);
			pList.addAll(continuatorPatterns);
			pList.addAll(leftflipPatterns);
			pList.addAll(objectivePatterns);
			pList.addAll(rightflipPatterns);
			pList.addAll(stopPatterns);
			pList.addAll(joyPatterns);
			pList.addAll(sadnessPatterns);
			pList.addAll(fearPatterns);
			pList.addAll(angerPatterns);
			pList.addAll(trustPatterns);
			pList.addAll(disgustPatterns);
			pList.addAll(surprisePatterns);
			pList.addAll(anticipationPatterns);
			for (int i = 0; i < neutralPatterns.size(); i++) {
				Triple<List<Pair<String, String>>, Integer, Integer> elem = neutralPatterns
						.get(i);
				if (isNotSubsumed(elem.getMiddle(), elem.getRight(), pList))
					newNeutralPatterns.add(elem);
			}
		}

		List<Triple<List<Pair<String, String>>, Integer, Integer>> newObjectivePatterns = new ArrayList<Triple<List<Pair<String, String>>, Integer, Integer>>();
		{
			List<Triple<List<Pair<String, String>>, Integer, Integer>> pList = amplifierPatterns;
			pList.addAll(attenuatorPatterns);
			pList.addAll(continuatorPatterns);
			pList.addAll(leftflipPatterns);
			pList.addAll(neutralPatterns);
			pList.addAll(rightflipPatterns);
			pList.addAll(stopPatterns);
			pList.addAll(joyPatterns);
			pList.addAll(sadnessPatterns);
			pList.addAll(fearPatterns);
			pList.addAll(angerPatterns);
			pList.addAll(trustPatterns);
			pList.addAll(disgustPatterns);
			pList.addAll(surprisePatterns);
			pList.addAll(anticipationPatterns);
			for (int i = 0; i < objectivePatterns.size(); i++) {
				Triple<List<Pair<String, String>>, Integer, Integer> elem = objectivePatterns
						.get(i);
				if (isNotSubsumed(elem.getMiddle(), elem.getRight(), pList))
					newObjectivePatterns.add(elem);
			}
		}

		List<Triple<List<Pair<String, String>>, Integer, Integer>> newRightflipPatterns = new ArrayList<Triple<List<Pair<String, String>>, Integer, Integer>>();
		{
			List<Triple<List<Pair<String, String>>, Integer, Integer>> pList = amplifierPatterns;
			pList.addAll(attenuatorPatterns);
			pList.addAll(continuatorPatterns);
			pList.addAll(leftflipPatterns);
			pList.addAll(neutralPatterns);
			pList.addAll(objectivePatterns);
			pList.addAll(stopPatterns);
			pList.addAll(joyPatterns);
			pList.addAll(sadnessPatterns);
			pList.addAll(fearPatterns);
			pList.addAll(angerPatterns);
			pList.addAll(trustPatterns);
			pList.addAll(disgustPatterns);
			pList.addAll(surprisePatterns);
			pList.addAll(anticipationPatterns);
			for (int i = 0; i < rightflipPatterns.size(); i++) {
				Triple<List<Pair<String, String>>, Integer, Integer> elem = rightflipPatterns
						.get(i);
				if (isNotSubsumed(elem.getMiddle(), elem.getRight(), pList))
					newRightflipPatterns.add(elem);
			}
		}

		List<Triple<List<Pair<String, String>>, Integer, Integer>> newStopPatterns = new ArrayList<Triple<List<Pair<String, String>>, Integer, Integer>>();
		{
			List<Triple<List<Pair<String, String>>, Integer, Integer>> pList = amplifierPatterns;
			pList.addAll(attenuatorPatterns);
			pList.addAll(continuatorPatterns);
			pList.addAll(leftflipPatterns);
			pList.addAll(neutralPatterns);
			pList.addAll(objectivePatterns);
			pList.addAll(rightflipPatterns);
			pList.addAll(joyPatterns);
			pList.addAll(sadnessPatterns);
			pList.addAll(fearPatterns);
			pList.addAll(angerPatterns);
			pList.addAll(trustPatterns);
			pList.addAll(disgustPatterns);
			pList.addAll(surprisePatterns);
			pList.addAll(anticipationPatterns);
			for (int i = 0; i < stopPatterns.size(); i++) {
				Triple<List<Pair<String, String>>, Integer, Integer> elem = stopPatterns
						.get(i);
				if (isNotSubsumed(elem.getMiddle(), elem.getRight(), pList))
					newStopPatterns.add(elem);
			}
		}

		List<Triple<List<Pair<String, String>>, Integer, Integer>> newJoyPatterns = new ArrayList<Triple<List<Pair<String, String>>, Integer, Integer>>();
		{
			List<Triple<List<Pair<String, String>>, Integer, Integer>> pList = amplifierPatterns;
			pList.addAll(attenuatorPatterns);
			pList.addAll(continuatorPatterns);
			pList.addAll(leftflipPatterns);
			pList.addAll(neutralPatterns);
			pList.addAll(objectivePatterns);
			pList.addAll(rightflipPatterns);
			pList.addAll(stopPatterns);
			pList.addAll(sadnessPatterns);
			pList.addAll(fearPatterns);
			pList.addAll(angerPatterns);
			pList.addAll(trustPatterns);
			pList.addAll(disgustPatterns);
			pList.addAll(surprisePatterns);
			pList.addAll(anticipationPatterns);
			for (int i = 0; i < joyPatterns.size(); i++) {
				Triple<List<Pair<String, String>>, Integer, Integer> elem = joyPatterns
						.get(i);
				if (isNotSubsumed(elem.getMiddle(), elem.getRight(), pList))
					newJoyPatterns.add(elem);
			}
		}

		List<Triple<List<Pair<String, String>>, Integer, Integer>> newSadnessPatterns = new ArrayList<Triple<List<Pair<String, String>>, Integer, Integer>>();
		{
			List<Triple<List<Pair<String, String>>, Integer, Integer>> pList = amplifierPatterns;
			pList.addAll(attenuatorPatterns);
			pList.addAll(continuatorPatterns);
			pList.addAll(leftflipPatterns);
			pList.addAll(neutralPatterns);
			pList.addAll(objectivePatterns);
			pList.addAll(rightflipPatterns);
			pList.addAll(joyPatterns);
			pList.addAll(stopPatterns);
			pList.addAll(fearPatterns);
			pList.addAll(angerPatterns);
			pList.addAll(trustPatterns);
			pList.addAll(disgustPatterns);
			pList.addAll(surprisePatterns);
			pList.addAll(anticipationPatterns);
			for (int i = 0; i < sadnessPatterns.size(); i++) {
				Triple<List<Pair<String, String>>, Integer, Integer> elem = sadnessPatterns
						.get(i);
				if (isNotSubsumed(elem.getMiddle(), elem.getRight(), pList))
					newSadnessPatterns.add(elem);
			}
		}

		List<Triple<List<Pair<String, String>>, Integer, Integer>> newFearPatterns = new ArrayList<Triple<List<Pair<String, String>>, Integer, Integer>>();
		{
			List<Triple<List<Pair<String, String>>, Integer, Integer>> pList = amplifierPatterns;
			pList.addAll(attenuatorPatterns);
			pList.addAll(continuatorPatterns);
			pList.addAll(leftflipPatterns);
			pList.addAll(neutralPatterns);
			pList.addAll(objectivePatterns);
			pList.addAll(rightflipPatterns);
			pList.addAll(joyPatterns);
			pList.addAll(sadnessPatterns);
			pList.addAll(stopPatterns);
			pList.addAll(angerPatterns);
			pList.addAll(trustPatterns);
			pList.addAll(disgustPatterns);
			pList.addAll(surprisePatterns);
			pList.addAll(anticipationPatterns);
			for (int i = 0; i < fearPatterns.size(); i++) {
				Triple<List<Pair<String, String>>, Integer, Integer> elem = fearPatterns
						.get(i);
				if (isNotSubsumed(elem.getMiddle(), elem.getRight(), pList))
					newFearPatterns.add(elem);
			}
		}

		List<Triple<List<Pair<String, String>>, Integer, Integer>> newAngerPatterns = new ArrayList<Triple<List<Pair<String, String>>, Integer, Integer>>();
		{
			List<Triple<List<Pair<String, String>>, Integer, Integer>> pList = amplifierPatterns;
			pList.addAll(attenuatorPatterns);
			pList.addAll(continuatorPatterns);
			pList.addAll(leftflipPatterns);
			pList.addAll(neutralPatterns);
			pList.addAll(objectivePatterns);
			pList.addAll(rightflipPatterns);
			pList.addAll(joyPatterns);
			pList.addAll(sadnessPatterns);
			pList.addAll(fearPatterns);
			pList.addAll(stopPatterns);
			pList.addAll(trustPatterns);
			pList.addAll(disgustPatterns);
			pList.addAll(surprisePatterns);
			pList.addAll(anticipationPatterns);
			for (int i = 0; i < angerPatterns.size(); i++) {
				Triple<List<Pair<String, String>>, Integer, Integer> elem = angerPatterns
						.get(i);
				if (isNotSubsumed(elem.getMiddle(), elem.getRight(), pList))
					newAngerPatterns.add(elem);
			}
		}

		List<Triple<List<Pair<String, String>>, Integer, Integer>> newTrustPatterns = new ArrayList<Triple<List<Pair<String, String>>, Integer, Integer>>();
		{
			List<Triple<List<Pair<String, String>>, Integer, Integer>> pList = amplifierPatterns;
			pList.addAll(attenuatorPatterns);
			pList.addAll(continuatorPatterns);
			pList.addAll(leftflipPatterns);
			pList.addAll(neutralPatterns);
			pList.addAll(objectivePatterns);
			pList.addAll(rightflipPatterns);
			pList.addAll(joyPatterns);
			pList.addAll(sadnessPatterns);
			pList.addAll(fearPatterns);
			pList.addAll(angerPatterns);
			pList.addAll(stopPatterns);
			pList.addAll(disgustPatterns);
			pList.addAll(surprisePatterns);
			pList.addAll(anticipationPatterns);
			for (int i = 0; i < trustPatterns.size(); i++) {
				Triple<List<Pair<String, String>>, Integer, Integer> elem = trustPatterns
						.get(i);
				if (isNotSubsumed(elem.getMiddle(), elem.getRight(), pList))
					newTrustPatterns.add(elem);
			}
		}

		List<Triple<List<Pair<String, String>>, Integer, Integer>> newDisgustPatterns = new ArrayList<Triple<List<Pair<String, String>>, Integer, Integer>>();
		{
			List<Triple<List<Pair<String, String>>, Integer, Integer>> pList = amplifierPatterns;
			pList.addAll(attenuatorPatterns);
			pList.addAll(continuatorPatterns);
			pList.addAll(leftflipPatterns);
			pList.addAll(neutralPatterns);
			pList.addAll(objectivePatterns);
			pList.addAll(rightflipPatterns);
			pList.addAll(joyPatterns);
			pList.addAll(sadnessPatterns);
			pList.addAll(fearPatterns);
			pList.addAll(angerPatterns);
			pList.addAll(trustPatterns);
			pList.addAll(stopPatterns);
			pList.addAll(surprisePatterns);
			pList.addAll(anticipationPatterns);
			for (int i = 0; i < disgustPatterns.size(); i++) {
				Triple<List<Pair<String, String>>, Integer, Integer> elem = disgustPatterns
						.get(i);
				if (isNotSubsumed(elem.getMiddle(), elem.getRight(), pList))
					newDisgustPatterns.add(elem);
			}
		}

		List<Triple<List<Pair<String, String>>, Integer, Integer>> newSurprisePatterns = new ArrayList<Triple<List<Pair<String, String>>, Integer, Integer>>();
		{
			List<Triple<List<Pair<String, String>>, Integer, Integer>> pList = amplifierPatterns;
			pList.addAll(attenuatorPatterns);
			pList.addAll(continuatorPatterns);
			pList.addAll(leftflipPatterns);
			pList.addAll(neutralPatterns);
			pList.addAll(objectivePatterns);
			pList.addAll(rightflipPatterns);
			pList.addAll(joyPatterns);
			pList.addAll(sadnessPatterns);
			pList.addAll(fearPatterns);
			pList.addAll(angerPatterns);
			pList.addAll(trustPatterns);
			pList.addAll(disgustPatterns);
			pList.addAll(stopPatterns);
			pList.addAll(anticipationPatterns);
			for (int i = 0; i < surprisePatterns.size(); i++) {
				Triple<List<Pair<String, String>>, Integer, Integer> elem = surprisePatterns
						.get(i);
				if (isNotSubsumed(elem.getMiddle(), elem.getRight(), pList))
					newSurprisePatterns.add(elem);
			}
		}

		List<Triple<List<Pair<String, String>>, Integer, Integer>> newAnticipationPatterns = new ArrayList<Triple<List<Pair<String, String>>, Integer, Integer>>();
		{
			List<Triple<List<Pair<String, String>>, Integer, Integer>> pList = amplifierPatterns;
			pList.addAll(attenuatorPatterns);
			pList.addAll(continuatorPatterns);
			pList.addAll(leftflipPatterns);
			pList.addAll(neutralPatterns);
			pList.addAll(objectivePatterns);
			pList.addAll(rightflipPatterns);
			pList.addAll(joyPatterns);
			pList.addAll(sadnessPatterns);
			pList.addAll(fearPatterns);
			pList.addAll(angerPatterns);
			pList.addAll(trustPatterns);
			pList.addAll(disgustPatterns);
			pList.addAll(surprisePatterns);
			pList.addAll(stopPatterns);
			for (int i = 0; i < anticipationPatterns.size(); i++) {
				Triple<List<Pair<String, String>>, Integer, Integer> elem = anticipationPatterns
						.get(i);
				if (isNotSubsumed(elem.getMiddle(), elem.getRight(), pList))
					newAnticipationPatterns.add(elem);
			}
		}

		// Return all the non-subsumed patterns
		HashMap<String, List<Triple<List<Pair<String, String>>, Integer, Integer>>> result = new HashMap<String, List<Triple<List<Pair<String, String>>, Integer, Integer>>>();
		result.put("amplifiers", newAmplifierPatterns);
		result.put("attenuators", newAttenuatorPatterns);
		result.put("continuators", newContinuatorPatterns);
		result.put("leftflips", newLeftflipPatterns);
		result.put("neutrals", newNeutralPatterns);
		result.put("objectives", newObjectivePatterns);
		result.put("rightflips", newRightflipPatterns);
		result.put("stops", newStopPatterns);
		result.put("joy", newJoyPatterns);
		result.put("sadness", newSadnessPatterns);
		result.put("fear", newFearPatterns);
		result.put("anger", newAngerPatterns);
		result.put("trust", newTrustPatterns);
		result.put("disgust", newDisgustPatterns);
		result.put("surprise", newSurprisePatterns);
		result.put("anticipation", newAnticipationPatterns);
		return result;
	}

	/**
	 * This is where the magic happens. An unlabeled sentence is classified on
	 * polarity based on the model that is currently in memory
	 * 
	 * @param tokens
	 * @param tags
	 * @return
	 */
	public HashMap<String, Double> classify(String[] tokens, String[] tags,
			Boolean normalize) throws Exception {
		// Create an RBEM vector of the input
		List<Pair<String, String>> sentence = makeVector(Arrays.asList(tokens),
				Arrays.asList(tags));
		// We have four different axes of emissions
		String[] axes = { "joySadness", "fearAnger", "trustDisgust",
				"surpriseAnticipation" };
		HashMap<String, Double[]> emissions = new HashMap<String, Double[]>();
		for (String axis : axes) {
			Double[] ems = new Double[sentence.size()];
			for (int i = 0; i < sentence.size(); i++)
				ems[i] = 0.0;
			emissions.put(axis, ems);
		}

		// Now we fetch all the patterns and then remove the ones that are
		// subsumed
		HashMap<String, List<Triple<List<Pair<String, String>>, Integer, Integer>>> patterns = removeSubsumed(
				matchPatterns(sentence, "amplifiers"),
				matchPatterns(sentence, "attenuators"),
				matchPatterns(sentence, "continuators"),
				matchPatterns(sentence, "leftflips"),
				matchPatterns(sentence, "neutrals"),
				matchPatterns(sentence, "objectives"),
				matchPatterns(sentence, "rightflips"),
				matchPatterns(sentence, "stops"),
				matchPatterns(sentence, "joy"),
				matchPatterns(sentence, "sadness"),
				matchPatterns(sentence, "fear"),
				matchPatterns(sentence, "anger"),
				matchPatterns(sentence, "trust"),
				matchPatterns(sentence, "disgust"),
				matchPatterns(sentence, "surprise"),
				matchPatterns(sentence, "anticipation"));

		// Commence the rule application

		// First set stops (stop and left flip patterns)
		HashMap<Integer, Boolean> stops = new HashMap<Integer, Boolean>();
		for (int i = 0; i < patterns.get("leftflips").size(); i++) {
			int start = patterns.get("leftflips").get(i).getMiddle();

			// Add the stop pattern
			stops.put(start, true);
		}
		for (int i = 0; i < patterns.get("stops").size(); i++) {
			int start = patterns.get("stops").get(i).getMiddle();

			// Add the stop pattern
			stops.put(start, true);
		}

		// Continuators may remove stop patterns
		for (int i = 0; i < patterns.get("continuators").size(); i++) {
			int start = patterns.get("continuators").get(i).getMiddle();

			// Find the stop pattern that is closest to the left of the
			// continuator, if any
			int maxPos = -1;
			Boolean removeStop = false;
			// Go through stops
			for (int position : stops.keySet()) {
				if (position < start && position > maxPos
						&& position > start - emissionRange) {
					maxPos = position;
					removeStop = true;
				}
			}

			// Remove the stop if required
			if (removeStop)
				stops.remove(maxPos);
		}

		// Joy emissions
		for (int i = 0; i < patterns.get("joy").size(); i++) {
			int start = patterns.get("joy").get(i).getMiddle();
			int end = patterns.get("joy").get(i).getRight();

			int center = (int) Math
					.floor(((double) start + (double) end) / 2.0);
			// Check for stops
			Boolean goLeft = true;
			Boolean goRight = true;
			for (int j = 0; j <= Math.max(center, tokens.length - center - 1); j++) {
				// Emit left as long as allowed
				if (goLeft && j != 0 && center - j >= 0
						&& j < emissions.get("joySadness").length)
					emissions.get("joySadness")[center - j] += Math.exp(-j);
				if (goRight && center + j < emissions.get("joySadness").length)
					emissions.get("joySadness")[center + j] += Math.exp(-j);

				// Stops
				if (stops.containsKey(center - j))
					goLeft = false;
				if (stops.containsKey(center + j))
					goRight = false;
			}
		}

		// Sadness emissions
		for (int i = 0; i < patterns.get("sadness").size(); i++) {
			int start = patterns.get("sadness").get(i).getMiddle();
			int end = patterns.get("sadness").get(i).getRight();

			int center = (int) Math
					.floor(((double) start + (double) end) / 2.0);
			// Check for stops
			Boolean goLeft = true;
			Boolean goRight = true;
			for (int j = 0; j <= Math.max(center, tokens.length - center - 1); j++) {
				// Emit left as long as allowed
				if (goLeft && j != 0 && center - j >= 0
						&& j < emissions.get("joySadness").length)
					emissions.get("joySadness")[center - j] -= Math.exp(-j);
				if (goRight && center + j < emissions.get("joySadness").length)
					emissions.get("joySadness")[center + j] -= Math.exp(-j);

				// Stops
				if (stops.containsKey(center - j))
					goLeft = false;
				if (stops.containsKey(center + j))
					goRight = false;
			}
		}

		// Fear emissions
		for (int i = 0; i < patterns.get("fear").size(); i++) {
			int start = patterns.get("fear").get(i).getMiddle();
			int end = patterns.get("fear").get(i).getRight();

			int center = (int) Math
					.floor(((double) start + (double) end) / 2.0);
			// Check for stops
			Boolean goLeft = true;
			Boolean goRight = true;
			for (int j = 0; j <= Math.max(center, tokens.length - center - 1); j++) {
				// Emit left as long as allowed
				if (goLeft && j != 0 && center - j >= 0
						&& j < emissions.get("fearAnger").length)
					emissions.get("fearAnger")[center - j] += Math.exp(-j);
				if (goRight && center + j < emissions.get("fearAnger").length)
					emissions.get("fearAnger")[center + j] += Math.exp(-j);

				// Stops
				if (stops.containsKey(center - j))
					goLeft = false;
				if (stops.containsKey(center + j))
					goRight = false;
			}
		}

		// Anger emissions
		for (int i = 0; i < patterns.get("anger").size(); i++) {
			int start = patterns.get("anger").get(i).getMiddle();
			int end = patterns.get("anger").get(i).getRight();

			int center = (int) Math
					.floor(((double) start + (double) end) / 2.0);
			// Check for stops
			Boolean goLeft = true;
			Boolean goRight = true;
			for (int j = 0; j <= Math.max(center, tokens.length - center - 1); j++) {
				// Emit left as long as allowed
				if (goLeft && j != 0 && center - j >= 0
						&& j < emissions.get("fearAnger").length)
					emissions.get("fearAnger")[center - j] -= Math.exp(-j);
				if (goRight && center + j < emissions.get("fearAnger").length)
					emissions.get("fearAnger")[center + j] -= Math.exp(-j);

				// Stops
				if (stops.containsKey(center - j))
					goLeft = false;
				if (stops.containsKey(center + j))
					goRight = false;
			}
		}

		// Trust emissions
		for (int i = 0; i < patterns.get("trust").size(); i++) {
			int start = patterns.get("trust").get(i).getMiddle();
			int end = patterns.get("trust").get(i).getRight();

			int center = (int) Math
					.floor(((double) start + (double) end) / 2.0);
			// Check for stops
			Boolean goLeft = true;
			Boolean goRight = true;
			for (int j = 0; j <= Math.max(center, tokens.length - center - 1); j++) {
				// Emit left as long as allowed
				if (goLeft && j != 0 && center - j >= 0
						&& j < emissions.get("trustDisgust").length)
					emissions.get("trustDisgust")[center - j] += Math.exp(-j);
				if (goRight
						&& center + j < emissions.get("trustDisgust").length)
					emissions.get("trustDisgust")[center + j] += Math.exp(-j);

				// Stops
				if (stops.containsKey(center - j))
					goLeft = false;
				if (stops.containsKey(center + j))
					goRight = false;
			}
		}

		// Disgust emissions
		for (int i = 0; i < patterns.get("disgust").size(); i++) {
			int start = patterns.get("disgust").get(i).getMiddle();
			int end = patterns.get("disgust").get(i).getRight();

			int center = (int) Math
					.floor(((double) start + (double) end) / 2.0);
			// Check for stops
			Boolean goLeft = true;
			Boolean goRight = true;
			for (int j = 0; j <= Math.max(center, tokens.length - center - 1); j++) {
				// Emit left as long as allowed
				if (goLeft && j != 0 && center - j >= 0
						&& j < emissions.get("trustDisgust").length)
					emissions.get("trustDisgust")[center - j] -= Math.exp(-j);
				if (goRight
						&& center + j < emissions.get("trustDisgust").length)
					emissions.get("trustDisgust")[center + j] -= Math.exp(-j);

				// Stops
				if (stops.containsKey(center - j))
					goLeft = false;
				if (stops.containsKey(center + j))
					goRight = false;
			}
		}

		// Surprise emissions
		for (int i = 0; i < patterns.get("surprise").size(); i++) {
			int start = patterns.get("surprise").get(i).getMiddle();
			int end = patterns.get("surprise").get(i).getRight();

			int center = (int) Math
					.floor(((double) start + (double) end) / 2.0);
			// Check for stops
			Boolean goLeft = true;
			Boolean goRight = true;
			for (int j = 0; j <= Math.max(center, tokens.length - center - 1); j++) {
				// Emit left as long as allowed
				if (goLeft && j != 0 && center - j >= 0
						&& j < emissions.get("surpriseAnticipation").length)
					emissions.get("surpriseAnticipation")[center - j] += Math
							.exp(-j);
				if (goRight
						&& center + j < emissions.get("surpriseAnticipation").length)
					emissions.get("surpriseAnticipation")[center + j] += Math
							.exp(-j);

				// Stops
				if (stops.containsKey(center - j))
					goLeft = false;
				if (stops.containsKey(center + j))
					goRight = false;
			}
		}

		// Anticipation emissions
		for (int i = 0; i < patterns.get("anticipation").size(); i++) {
			int start = patterns.get("anticipation").get(i).getMiddle();
			int end = patterns.get("anticipation").get(i).getRight();

			int center = (int) Math
					.floor(((double) start + (double) end) / 2.0);
			// Check for stops
			Boolean goLeft = true;
			Boolean goRight = true;
			for (int j = 0; j <= Math.max(center, tokens.length - center - 1); j++) {
				// Emit left as long as allowed
				if (goLeft && j != 0 && center - j >= 0
						&& j < emissions.get("surpriseAnticipation").length)
					emissions.get("surpriseAnticipation")[center - j] -= Math
							.exp(-j);
				if (goRight
						&& center + j < emissions.get("surpriseAnticipation").length)
					emissions.get("surpriseAnticipation")[center + j] -= Math
							.exp(-j);

				// Stops
				if (stops.containsKey(center - j))
					goLeft = false;
				if (stops.containsKey(center + j))
					goRight = false;
			}
		}

		// Remove emissions based on objective patterns
		for (int i = 0; i < patterns.get("objectives").size(); i++) {
			int start = patterns.get("objectives").get(i).getMiddle();
			int end = patterns.get("objectives").get(i).getRight();

			int center = (int) Math
					.floor(((double) start + (double) end) / 2.0);
			// Check for stops
			Boolean goLeft = true;
			Boolean goRight = true;
			// Cancel existing emissions to the left and/or right
			for (int j = 0; j <= Math.max(center, tokens.length - center - 1); j++) {
				if (goLeft && j != 0 && center - j >= 0
						&& j < emissions.get("joySadness").length) {
					for (String axis : axes)
						emissions.get(axis)[center - j] = 0.0;
				}
				if (goRight && center + j < emissions.get("joySadness").length) {
					for (String axis : axes)
						emissions.get(axis)[center + j] = 0.0;
				}

				// Stops
				if (stops.containsKey(center - j))
					goLeft = false;
				if (stops.containsKey(center + j))
					goRight = false;
			}
		}

		// Amplifiers strengthen emissions
		for (int i = 0; i < patterns.get("amplifiers").size(); i++) {
			int start = patterns.get("amplifiers").get(i).getMiddle();
			int end = patterns.get("amplifiers").get(i).getRight();

			int center = (int) Math
					.floor(((double) start + (double) end) / 2.0);
			// Check for stops
			Boolean goLeft = true;
			Boolean goRight = true;
			// Cancel existing emissions to the left and/or right
			for (int j = 0; j <= Math.min(emissionRange,
					Math.max(center, tokens.length - center - 1)); j++) {
				if (goLeft && j != 0 && center - j >= 0
						&& j < emissions.get("joySadness").length) {
					for (String axis : axes)
						emissions.get(axis)[center - j] *= 1 + Math.exp(-j);
				}
				if (goRight && center + j < emissions.get("joySadness").length) {
					for (String axis : axes)
						emissions.get(axis)[center + j] *= 1 + Math.exp(-j);
				}

				// Stops
				if (stops.containsKey(center - j))
					goLeft = false;
				if (stops.containsKey(center + j))
					goRight = false;
			}
		}

		// Attenuators weaken emissions
		for (int i = 0; i < patterns.get("attenuators").size(); i++) {
			int start = patterns.get("attenuators").get(i).getMiddle();
			int end = patterns.get("attenuators").get(i).getRight();

			int center = (int) Math
					.floor(((double) start + (double) end) / 2.0);
			// Check for stops
			Boolean goLeft = true;
			Boolean goRight = true;
			// Cancel existing emissions to the left and/or right
			for (int j = 0; j <= Math.min(emissionRange,
					Math.max(center, tokens.length - center - 1)); j++) {
				if (goLeft && j != 0 && center - j >= 0
						&& j < emissions.get("joySadness").length) {
					for (String axis : axes)
						emissions.get(axis)[center - j] *= 1 - Math.exp(-j);
				}
				if (goRight && center + j < emissions.get("joySadness").length) {
					for (String axis : axes)
						emissions.get(axis)[center + j] *= 1 - Math.exp(-j);
				}

				// Stops
				if (stops.containsKey(center - j))
					goLeft = false;
				if (stops.containsKey(center + j))
					goRight = false;
			}
		}

		// Flip the sign of emissions to the right
		for (int i = 0; i < patterns.get("rightflips").size(); i++) {
			int start = patterns.get("rightflips").get(i).getMiddle();
			int end = patterns.get("rightflips").get(i).getRight();

			int center = (int) Math
					.floor(((double) start + (double) end) / 2.0);
			// Check for stops
			Boolean goRight = true;
			// Cancel existing emissions to the left and/or right
			for (int j = 0; j <= Math.min(emissionRange,
					Math.max(center, tokens.length - center - 1)); j++) {
				if (goRight && center + j < emissions.get("joySadness").length) {
					for (String axis : axes)
						emissions.get(axis)[center + j] *= -1;
				}

				// Stops
				if (stops.containsKey(center + j))
					goRight = false;
			}
		}

		// Flip the sign of emissions to the left
		for (int i = 0; i < patterns.get("leftflips").size(); i++) {
			int start = patterns.get("leftflips").get(i).getMiddle();
			int end = patterns.get("leftflips").get(i).getRight();

			int center = (int) Math
					.floor(((double) start + (double) end) / 2.0);
			// Check for stops
			Boolean goLeft = true;
			// Cancel existing emissions to the left and/or right
			for (int j = 0; j <= Math.min(emissionRange,
					Math.max(center, tokens.length - center - 1)); j++) {
				if (goLeft && j != 0 && center - j >= 0
						&& j < emissions.get("joySadness").length) {
					for (String axis : axes)
						emissions.get(axis)[center - j] *= 1 - Math.exp(-j);
				}

				// Stops
				if (stops.containsKey(center - j))
					goLeft = false;
			}
		}

		// Compute scores
		HashMap<String, Double> scores = new HashMap<String, Double>();
		Double maxScore = 0.0;
		for (String axis : axes) {
			// Sum over axis scores
			Double score = 0.0;
			for (Double em : emissions.get(axis))
				score += em;
			scores.put(axis, score);

			if (Math.abs(score) > maxScore)
				maxScore = Math.abs(score);
		}

		// Normalize if we have to
		if (normalize && maxScore != 0.0) {
			for (String axis : axes)
				scores.put(axis, scores.get(axis) / maxScore);
		}

		return scores;
	}

	/**
	 * Classifies a sentence based on a line of space-separated words and
	 * space-separated POS-tags
	 * 
	 * @param sentence
	 * @param tagLine
	 * @return
	 */
	public HashMap<String, Double> classify(String sentence, String tagLine,
			Boolean normalize) throws Exception {
		return classify(sentence.split(" "), tagLine.split(" "), normalize);
	}

	/**
	 * Adds a pattern to the currently loaded model
	 * 
	 * @param patternType
	 * @param pattern
	 */
	public Boolean addPattern(String patternType,
			List<Pair<String, String>> pattern) {
		// Check if this type of pattern is allowed
		String[] allowedPatterns = { "amplifiers", "attenuators",
				"continuators", "leftflips", "neutrals", "objectives",
				"rightflips", "stops", "joy", "sadness", "fear", "anger",
				"trust", "disgust", "surprise", "anticipation" };

		Boolean found = false;
		for (String allowedPattern : allowedPatterns) {
			if (patternType.equals(allowedPattern))
				found = true;
		}
		if (!found)
			return false; // Illegal pattern type
		else {
			if (pattern.size() > 0) {
				// Index is computed on first element
				String firstIndex = pattern.get(0).getLeft().trim()
						.toLowerCase()
						+ pattern.get(0).getRight().trim();
				// Now the total index
				String totalIndex = "";
				for (Pair<String, String> member : pattern)
					totalIndex += member.getLeft().trim().toLowerCase()
							+ member.getRight().trim();

				// We may need to initialize
				if (!model.get(patternType).containsKey(firstIndex))
					model.get(patternType)
							.put(firstIndex,
									new HashMap<Integer, HashMap<String, List<Pair<String, String>>>>());
				if (!model.get(patternType).get(firstIndex)
						.containsKey(pattern.size()))
					model.get(patternType)
							.get(firstIndex)
							.put(pattern.size(),
									new HashMap<String, List<Pair<String, String>>>());

				// Finally, add the pattern
				model.get(patternType).get(firstIndex).get(pattern.size())
						.put(totalIndex, pattern);

				return true;
			} else
				return false;
		}
	}

	/**
	 * Loads a model for a given language
	 * 
	 * @param language
	 */
	public void loadModel(String language) {
		if (this.getClass().getResource("/" + language + ".rbeme") != null) {
			try {
				// Stream JSON since loading it into memory is too
				// memory-intensive
				JsonFactory jFactory = new JsonFactory();
				JsonParser jParser = jFactory.createJsonParser(this.getClass()
						.getResourceAsStream("/" + language + ".rbeme"));

				// Continue until we find the end object
				while (jParser.nextToken() != JsonToken.END_OBJECT) {
					// Get the field name
					String patternType = jParser.getCurrentName();
					if (model.keySet().contains(patternType)) {
						jParser.nextToken();

						// Keep on going until we find the inner object's end
						while (jParser.nextToken() != JsonToken.END_OBJECT) {
							String firstIndex = jParser.getCurrentName();
							jParser.nextToken();

							// The length
							while (jParser.nextToken() != JsonToken.END_OBJECT) {
								String length = jParser.getCurrentName();
								jParser.nextToken();

								// Full indexes
								while (jParser.nextToken() != JsonToken.END_OBJECT) {
									String fullIndex = jParser.getCurrentName();
									jParser.nextToken();

									// Patterns
									while (jParser.nextToken() != JsonToken.END_ARRAY) {
										// Pattern members
										String token = null;
										String tag = null;
										while (jParser.nextToken() != JsonToken.END_OBJECT) {
											String fieldName = jParser
													.getCurrentName();
											jParser.nextToken();

											// Can either be token or tag
											if (fieldName.equals("token"))
												token = jParser.getText();
											else if (fieldName.equals("tag"))
												tag = jParser.getText();
										}

										// Add pattern member
										if (token != null && tag != null) {
											// Initialize index
											if (!model.get(patternType)
													.containsKey(firstIndex))
												model.get(patternType)
														.put(firstIndex,
																new HashMap<Integer, HashMap<String, List<Pair<String, String>>>>());
											// Initialize length
											if (!model
													.get(patternType)
													.get(firstIndex)
													.containsKey(
															Integer.parseInt(length)))
												model.get(patternType)
														.get(firstIndex)
														.put(Integer
																.parseInt(length),
																new HashMap<String, List<Pair<String, String>>>());
											// Initialize full index
											if (!model
													.get(patternType)
													.get(firstIndex)
													.get(Integer
															.parseInt(length))
													.containsKey(fullIndex))
												model.get(patternType)
														.get(firstIndex)
														.get(Integer
																.parseInt(length))
														.put(fullIndex,
																new LinkedList<Pair<String, String>>());

											// Add the member
											model.get(patternType)
													.get(firstIndex)
													.get(Integer
															.parseInt(length))
													.get(fullIndex)
													.add(new ImmutablePair<String, String>(
															token, tag));
										} else
											jParser.nextToken();
									}
								}
							}
						}
					}
				}

			} catch (Exception e) {
				System.out.println("Unable to load model");
				e.printStackTrace();
			}
		}
	}

	/**
	 * Stores a model into a language-specific file
	 * 
	 * @param language
	 */
	public void storeModel(String language) {
		// Stores the model
		try {
			// Initialize output JSON
			JSONObject output = new JSONObject();

			// Go through our model
			// HashMap<String, HashMap<String, HashMap<Integer, HashMap<String,
			// List<Pair<String, String>>>>>>
			for (Entry<String, HashMap<String, HashMap<Integer, HashMap<String, List<Pair<String, String>>>>>> e1 : model
					.entrySet()) {
				String patternType = e1.getKey();
				HashMap<String, HashMap<Integer, HashMap<String, List<Pair<String, String>>>>> fullPatterns = e1
						.getValue();

				// Go through the fullPatterns list
				JSONObject fullPatternJson = new JSONObject();
				for (Entry<String, HashMap<Integer, HashMap<String, List<Pair<String, String>>>>> e2 : fullPatterns
						.entrySet()) {
					String patternIndex = e2.getKey();
					HashMap<Integer, HashMap<String, List<Pair<String, String>>>> patternLengths = e2
							.getValue();

					// Go through the lengths and patterns in there
					JSONObject lengthJson = new JSONObject();
					for (Entry<Integer, HashMap<String, List<Pair<String, String>>>> e3 : patternLengths
							.entrySet()) {
						Integer length = e3.getKey();
						HashMap<String, List<Pair<String, String>>> patterns = e3
								.getValue();

						// Go through subindex and pattern members
						JSONObject subIndexedJson = new JSONObject();
						for (Entry<String, List<Pair<String, String>>> e4 : patterns
								.entrySet()) {
							String index = e4.getKey();
							List<Pair<String, String>> patternMembers = e4
									.getValue();

							// Go through all members
							JSONArray memberJson = new JSONArray();
							for (Pair<String, String> member : patternMembers) {
								String token = member.getLeft();
								String tag = member.getRight();

								memberJson.put(new JSONObject().put("token",
										token).put("tag", tag));
							}

							// Add to our JSON
							subIndexedJson.put(index, memberJson);
						}
						// Add to JSON
						lengthJson.put(length.toString(), subIndexedJson);
					}
					// Add to JSON
					fullPatternJson.put(patternIndex, lengthJson);
				}
				// Add to JSON
				output.put(patternType, fullPatternJson);
			}

			// Write to model file
			File modelFile = new File("src/main/resources/" + language
					+ ".rbeme");
			BufferedWriter br = new BufferedWriter(new OutputStreamWriter(
					new FileOutputStream(modelFile), "UTF8"));
			br.write(output.toString());
			br.flush();
			br.close();
		} catch (Exception e) {
			System.out.println("Unable to store model");
			e.printStackTrace();
		}
	}
}
