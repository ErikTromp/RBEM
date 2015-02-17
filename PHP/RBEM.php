<?php

class EmissionMiner {
	
	public static $positives = array(); // All positive tokens
	public static $negatives = array(); // All negative tokens
	public static $amplifiers = array(); // All amplifiers with their POS-tag and token
	public static $attenuators = array(); // All attenuators with their POS-tag and token
	public static $rightflips = array(); // All bi-directional flips with their POS-tag and token
	public static $leftflips = array(); // All left-directional flips with their POS-tag and token
	public static $continuators = array();	// All tokens of continuators
	public static $stops = array(); // General stops (same like left ftip, but doesn't flip)
	public static $emissionRange = 4;
	
	/**
	 * Creates a vector that can serve as input for the emission miner
	 * @param array $tokens An array containing the tokens
	 * @param array $tags An array containing the tags
	 */
	public static function makeVector($tokens, $tags) {
		$return = array();
		// Go through the tags
		for ($i = 0; $i < count($tags); $i++) {
			// Get current token and tag
			$tag = $tags[$i];
			$token = $tokens[$i];
			
			// Add them to the array
			$return[] = array(
				'token' => strtolower(trim($token)),
				'tag' => trim($tag)
			);
		}
		
		return $return;
	}
	
	/**
	 * Adds a pattern to the model
	 * @param array $pattern The pattern to add. An array of (token, POS) members
	 * @param string $type The pattern's type 
	 */
	public static function addPattern($pattern, $type) {
		// Detemine pattern length
		$length = count($pattern);
		
		// Get the first element for indexing
		$first = true;
		$index = '';
		foreach ($pattern as $i => $member) {
			$token = strtolower(trim($member['token']));
			$pattern[$i]['token'] = $token;
            if (empty($token)) {
                echo $type."<br /><pre>";
                print_r($pattern);
                die();
            }
			$tag = trim($member['tag']);
			if ($first) {
				$key = $token.$tag;
				$first = false;
			}

			// Add to complete index
			$index .= $token.$tag;
		}
		
		// Check what type of pattern we are adding
		if ($type == 'positive') {
			// Initialize array if required
			if (!isset(self::$positives[$key])) {
				self::$positives[$key] = array();
			}
			// Also initialize deeper array
			if (!isset(self::$positives[$key][$length])) {
				self::$positives[$key][$length] = array();
			}
			self::$positives[$key][$length][$index] = $pattern;
		}
		elseif ($type == 'negative') {
			// Initialize array if required
			if (!isset(self::$negatives[$key])) {
				self::$negatives[$key] = array();
			}
			// Also initialize deeper array
			if (!isset(self::$negatives[$key][$length])) {
				self::$negatives[$key][$length] = array();
			}
			self::$negatives[$key][$length][$index] = $pattern;
		}
		elseif ($type == 'amplifier') {
			// Initialize array if required
			if (!isset(self::$amplifiers[$key])) {
				self::$amplifiers[$key] = array();
			}
			// Also initialize deeper array
			if (!isset(self::$amplifiers[$key][$length])) {
				self::$amplifiers[$key][$length] = array();
			}
			self::$amplifiers[$key][$length][$index] = $pattern;
		}
		elseif ($type == 'attenuator') {
			// Initialize array if required
			if (!isset(self::$attenuators[$key])) {
				self::$attenuators[$key] = array();
			}
			// Also initialize deeper array
			if (!isset(self::$attenuators[$key][$length])) {
				self::$attenuators[$key][$length] = array();
			}
			self::$attenuators[$key][$length][$index] = $pattern;
		}
		elseif ($type == 'rightflip') {
			// Initialize array if required
			if (!isset(self::$rightflips[$key])) {
				self::$rightflips[$key] = array();
			}
			// Also initialize deeper array
			if (!isset(self::$rightflips[$key][$length])) {
				self::$rightflips[$key][$length] = array();
			}
			self::$rightflips[$key][$length][$index] = $pattern;
		}
		elseif ($type == 'leftflip') {
			// Initialize array if required
			if (!isset(self::$leftflips[$key])) {
				self::$leftflips[$key] = array();
			}
			// Also initialize deeper array
			if (!isset(self::$leftflips[$key][$length])) {
				self::$leftflips[$key][$length] = array();
			}
			self::$leftflips[$key][$length][$index] = $pattern;
		}
		elseif ($type == 'continuator') {
			// Initialize array if required
			if (!isset(self::$continuators[$key])) {
				self::$continuators[$key] = array();
			}
			// Also initialize deeper array
			if (!isset(self::$continuators[$key][$length])) {
				self::$continuators[$key][$length] = array();
			}
			self::$continuators[$key][$length][$index] = $pattern;
		}
		elseif ($type == 'stop') {
			// Initialize array if required
			if (!isset(self::$stops[$key])) {
				self::$stops[$key] = array();
			}
			// Also initialize deeper array
			if (!isset(self::$stops[$key][$length])) {
				self::$stops[$key][$length] = array();
			}
			self::$stops[$key][$length][$index] = $pattern;
		}
	}

	/**
	 * Matches a a sentence against a single pattern
	 * @param array $sentence The sentence consisting of token,tag pairs
	 * @param array $pattern The pattern consisting of token,tag pairs and
	 * possibly also wildcards
	 * @param integer $currentPosition The current position in the pattern we
	 * are trying to match
	 * @param integer $offset The current position in the sentence we are matching
	 * against. Everything before this position has been investigated
	 * @param integer $endPos Return variable storing the end position of the
	 * pattern if it was matched against the sentence. If no match was found,
	 * this will be -1
	 * @return boolean True if the pattern could be matched, false otherwise
	 */
	private static function matchPattern($sentence, $pattern, $currentPosition, $offset, &$endPos) {
		if ($currentPosition >= count($pattern)) {
			// End of pattern found, we have a match
			$endPos = $offset - 1;
			return true;
		}
		elseif ($offset >= count($sentence)) {
			// End of sentence found, pattern was not found
			$endPos = -1;
			return false;
		}
		else {
			// Get the current member and its token and tag
			$member = $pattern[$currentPosition];
			$patternToken = $member['token'];
			$patternTag = $member['tag'];
			// Get the sentence's token and tag
			$member = $sentence[$offset];
			$sentenceToken = $member['token'];
			$sentenceTag = $member['tag'];
			
			// Check if we are dealing with a wildcard or not
			if ($patternToken == '_') {
				// Single position (exactly 1) wildcard, check if we need to match
				// tag or not at all
				if ($patternTag == '*') {
					// Check the next position
					$nextEntry = $sentence[$offset + 1];
					$nextMember = $pattern[$currentPosition + 1];
					if (!($nextEntry['token'] == $nextMember['token'] &&
						$nextEntry['tag'] == $nextMember['tag'])) {
						// Next entries don't match, return false
						$endPos = -1;
						return false;
					}
					else {
						// We can skip 2 positions!
						$remainder = self::matchPattern($sentence, $pattern, $currentPosition + 2, $offset + 2, $ep);
						$endPos = $ep;
						return $remainder;
					}
				}
				else {
					// Check if the current tag matches
					if ($patternTag != $sentenceTag) {
						// Next entries don't match, return false
						$endPos = -1;
						return false;
					}
					else {
						// We can skip 2 positions!
						$remainder = self::matchPattern($sentence, $pattern, $currentPosition + 1, $offset + 1, $ep);
						$endPos = $ep;
						return $remainder;
					}
				}
			}
			elseif ($patternToken == '*') {
				$nextMember = $pattern[$currentPosition + 1];
				if (isset($sentence[$offset + 1])) {
					$nextEntry = $sentence[$offset + 1];
				}
				else {
					// No further entries found, use non-existing dummy entry
					$nextEntry = array('token' => '', 'tag' => '');
				}
				
				// Multi-position (0 or more) wildcard, first check if we have a
				// direct match
				if ($sentenceToken == $nextMember['token'] &&
					$sentenceTag == $nextMember['tag']) {
					// We have a direct match, continue with the remainder
					$remainder = self::matchPattern($sentence, $pattern, $currentPosition + 2, $offset + 1, $ep);
					$endPos = $ep;
					return $remainder;
				}

				// We need to look at the next position of the pattern to see
				// if we have a match
				if (!($nextEntry['token'] == $nextMember['token'] &&
					$nextEntry['tag'] == $nextMember['tag'])) {
					// Next entries don't match, we may need to look further down the sentence
					$remainder = self::matchPattern($sentence, $pattern, $currentPosition, $offset + 1, $ep);
					$endPos = $ep;
					return $remainder;
				}
				else {
					// We can skip 2 positions!
					$remainder = self::matchPattern($sentence, $pattern, $currentPosition + 2, $offset + 2, $ep);
					$endPos = $ep;
					return $remainder;
				}
			}
			else {
				// No wildcard, next entry in sentence should
				// exactly match next pattern
				if (!($sentenceToken == $patternToken &&
					$sentenceTag == $patternTag)) {
					// No match
					$endPos = -1;
					return false;
				}
				else {
					// Match the next member to the remainder of the sentence
					$remainder = self::matchPattern($sentence, $pattern, $currentPosition + 1, $offset + 1, $ep);
					$endPos = $ep;
					return $remainder;
				}
			}
		}
	}
	
	/**
	 * Matches a number of patterns against a sentence
	 * @param array $sentence A sentence consisting of token,tag pairs
	 * @param array of array $haystack An array of patterns
	 * @param array $hashIndex Return variable to hash the found patterns, useful
	 * when defining uniqueness
	 * @return array All (maximal) patterns that match the sentence
	 */
	private static function matchPatterns($sentence, $haystack, &$hashIndex) {
		$hashIndex = array();
		// Find all matches
		$matches = array();
		// Get the length
		$length = count($sentence);
		for ($i = 0; $i < $length; $i++) {
			$matchedPatterns = array();
			// Determine key
			$key = $sentence[$i]['token'].$sentence[$i]['tag'];
			// Token could also be a wildcard
			$keyToken = "_".$sentence[$i]['tag'];
			
			// Find all patterns that match the start we just defined
			if (isset($haystack[$key])) {
				foreach ($haystack[$key] as $patternLength => $patterns) {
					// Go through all patterns
					foreach ($patterns as $index => $currentPattern) {
						// Check if this pattern matches
						if (count($currentPattern) == 1) {
							// Length is one, equal to the key so we have a match
							$matchedPatterns[$index] = array(
								'pattern' => $currentPattern,
								'start' => $i,
								'center' => $i,
								'end' => $i
							);
						}
						else {
							// Pattern encompasses more than just one position, now
							// we need to match this pattern against the remainder
							// of the sentence
							$match = true;
							$endPos = -1;
							// Get the next entry of the sentence (if possible)
							if ($i + 1 < $length) {
								// The first already matches (due to key match),
								// start looking from thereon
								$match = self::matchPattern($sentence, $currentPattern, 1, $i + 1, $endPos);
								
								// If we have a match, add the pattern
								if ($match) {
									// Check if the pattern isn't too far apart
									if ($endPos - $i <= self::$emissionRange) {
										$matchedPatterns[$index] = array(
											'pattern' => $currentPattern,
											'start' => $i,
											'center' => ($i + $endPos) / 2,
											'end' => $endPos
										);
									}
								}
							}
						}
					}
				}
			}

			// Find all patterns that match the start we just defined
			if (isset($haystack[$keyToken])) {
				foreach ($haystack[$keyToken] as $patternLength => $patterns) {
					// Go through all patterns
					foreach ($patterns as $index => $currentPattern) {
						// Check if this pattern matches
						if (count($currentPattern) == 1) {
							// Length is one, equal to the key so we have a match
							$matchedPatterns[$index] = array(
								'pattern' => $currentPattern,
								'start' => $i,
								'center' => $i,
								'end' => $i
							);
						}
						else {
							// Pattern encompasses more than just one position, now
							// we need to match this pattern against the remainder
							// of the sentence
							$match = true;
							$endPos = -1;
							// Get the next entry of the sentence (if possible)
							if ($i + 1 < $length) {
								// The first already matches (due to key match),
								// start looking from thereon
								$match = self::matchPattern($sentence, $currentPattern, 1, $i + 1, $endPos);
								
								// If we have a match, add the pattern
								if ($match) {
									// Check if the pattern isn't too far apart
									if ($endPos - $i <= self::$emissionRange) {
										$matchedPatterns[$index] = array(
											'pattern' => $currentPattern,
											'start' => $i,
											'center' => ($i + $endPos) / 2,
											'end' => $endPos
										);
									}
								}
							}
						}
					}
				}
			}
			
			// Now determine what pattern actually to use
			$maxLength = 0;
			$maxPattern = null;
			foreach ($matchedPatterns as $pattern) {
				$n = count($pattern['pattern']);
				if ($n > $maxLength) {
					$maxLength = $n;
					$maxPattern = $pattern;
				}
			}
			// Add pattern (if any)
			if (!is_null($maxPattern)) {
				$matches[] = $maxPattern;
				$first = 1;
				foreach ($maxPattern['pattern'] as $member) {
					$add = true;
					if (isset($hashIndex[$member['token'].$member['tag']])) {
						if ($hashIndex[$member['token'].$member['tag']]['length'] >
							count($pattern)) {
							$add = false;
						}
					}
					if ($add) {
						$hashIndex[$member['token'].$member['tag']] = array(
								'length' => count($pattern['pattern']),
								'first' => $first
						);
					}
					
					if ($member['token'] != '_') {
						$add = true;
						if (isset($hashIndex['_'.$member['tag']])) {
							if ($hashIndex['_'.$member['tag']]['length'] >
								count($pattern['pattern'])) {
								$add = false;
							}
						}
						if ($add) {
							$hashIndex['_'.$member['tag']] = array(
								'length' => count($pattern['pattern']),
								'first' => $first
							);
						}
					}
					$first = 0;
				}
			}
		}

		return $matches;
	}
	
	/**
	 * Polarizes a sentence.
	 * @param array $tokens The tokens of the sentence where each element is a
	 * tuple containing the token and the POS
	 * @param array $polarizedTokens Return variable storing the normalized
	 * polarity score for each token
	 * @param array $sentiment The sentiment score. >0 means positive, <0 means negative
	 * @return {'positive', 'neutral', 'negative') The polarity
	 */
	public static function polarizeSentence($tokens, &$polarizedTokens = null, &$sentiment, &$patternsFound) {
		foreach ($tokens as $index => $data) {
			$data['token'] = strtolower(trim($data['token']));
			$tokens[$index] = $data;
		}

		$N = count($tokens);
		$emissions = array_pad(array(), $N, 0.0);
		$stops = array();
		$continues = array();
		$patternsFound = false;
		
		// Collect all patterns
		$leftflipPatterns = self::matchPatterns($tokens, self::$leftflips, $leftflipIndex);
		$stopPatterns = self::matchPatterns($tokens, self::$stops, $stopIndex);
		$continuatorPatterns = self::matchPatterns($tokens, self::$continuators, $continuatorIndex);
		$positivePatterns = self::matchPatterns($tokens, self::$positives, $positiveIndex);
		$negativePatterns = self::matchPatterns($tokens, self::$negatives, $negativeIndex);
		$amplifierPatterns = self::matchPatterns($tokens, self::$amplifiers, $amplifierIndex);
		$attenuatorPatterns = self::matchPatterns($tokens, self::$attenuators, $attenuatorIndex);
		$rightflipPatterns = self::matchPatterns($tokens, self::$rightflips, $rightflipIndex);
		
		// Remove subsumed ones from indexes
		self::removeSubsumed($leftflipIndex, $stopIndex, $continuatorIndex, $positiveIndex,
			$negativeIndex, $amplifierIndex, $attenuatorIndex, $rightflipIndex);

		// First set all stops
		foreach ($leftflipPatterns as $pattern) {
			// Check if we are not dealing with a subsumed pattern
			$key = $pattern['pattern'][0];
			$key = $key['token'].$key['tag'];
			if (isset($leftflipIndex[$key])) {
				if ($leftflipIndex[$key]['first'] == 1) {
					$stops[$pattern['start']] = 1;
				}
			}
		}
		foreach ($stopPatterns as $pattern) {
			// Check if we are not dealing with a subsumed pattern
			$key = $pattern['pattern'][0];
			$key = $key['token'].$key['tag'];
			if (isset($stopIndex[$key])) {
				if ($stopIndex[$key]['first'] == 1) {
					$stops[$pattern['start']] = 1;
				}
			}
		}
		
		// Next look for continuators that may cancel stops
		foreach ($continuatorPatterns as $pattern) {
			// Check if we are not dealing with a subsumed pattern
			$key = $pattern['pattern'][0];
			$key = $key['token'].$key['tag'];
			if (isset($continuatorIndex[$key])) {
				if ($continuatorIndex[$key]['first'] == 1) {
					// Remove the previously positioned stop; find the stop located
					// closest to the left of the continuator, if any
					$maxPos = -1;
					$removeStop = false;
					foreach ($stops as $position => $one) {
						if ($position < $pattern['start'] &&
							$position > $maxPos &&
							$position > $pattern['start'] - self::$emissionRange) {
							// Update maxPos and make sure we remove the stop
							$maxPos = $position;
							$removeStop = true;
						}
					}
					
					// Add continuator
					$continues[$pattern['start']] = 1;
					
					// Remove the stop if required
					if ($removeStop) {
						unset($stops[$maxPos]);
					}
				}
			}
		}
		
		// Look for all positives
		foreach ($positivePatterns as $pattern) {
			// Check if we are not dealing with a subsumed pattern
			$key = $pattern['pattern'][0];
			$key = $key['token'].$key['tag'];
			if (isset($positiveIndex[$key])) {
				if ($positiveIndex[$key]['first'] == 1) {
					$patternsFound = true;
					// We can still go left and right
					$goLeft = true;
					$goRight = true;
		
					// Emit positive sentiment to itself and neighbors
					for ($j = 0; $j < $N; $j++) {
						$emissionValue = exp(-$j);
						
						// Set emission value to the left
						if (floor($pattern['center'] - $j) >= 0 && $goLeft) {
							if (isset($continues[floor($pattern['center'] - $j)])) {
								$emissionValue *= 2;
							}
							$emissions[floor($pattern['center'] - $j)] += $emissionValue;
						}
						// Emission value to the right
						if (ceil($pattern['center'] + $j) < $N &&
							($j != 0 || ceil($pattern['center']) != $pattern['center'])
							&& $goRight) {
							if (isset($continues[ceil($pattern['center'] + $j)])) {
								$emissionValue *= 2;
							}
							$emissions[ceil($pattern['center'] + $j)] += $emissionValue;
						}
						
						// Check for stop
						if (isset($stops[floor($pattern['center'] - $j)])) {
							$goLeft = false;
						}
						if (isset($stops[ceil($pattern['center'] + $j)])) {
							$goRight = false;
						}
					}
				}
			}
		}
			
		// Look for all negatives
		foreach ($negativePatterns as $pattern) {
			// Check if we are not dealing with a subsumed pattern
			$key = $pattern['pattern'][0];
			$key = $key['token'].$key['tag'];
			if (isset($negativeIndex[$key])) {
				if ($negativeIndex[$key]['first'] == 1) {
					$patternsFound = true;
					// We can still go left and right
					$goLeft = true;
					$goRight = true;
		
					// Emit positive sentiment to itself and neighbors
					for ($j = 0; $j < $N; $j++) {
						$emissionValue = -exp(-$j);
						
						// Set emission value to the left
						if (floor($pattern['center'] - $j) >= 0 && $goLeft) {
							if (isset($continues[floor($pattern['center'] - $j)])) {
								$emissionValue *= 2;
							}
							$emissions[floor($pattern['center'] - $j)] += $emissionValue;
						}
						// Emission value to the right
						if (ceil($pattern['center'] + $j) < $N &&
							($j != 0 || ceil($pattern['center']) != $pattern['center'])
							&& $goRight) {
							if (isset($continues[ceil($pattern['center'] + $j)])) {
								$emissionValue *= 2;
							}
							$emissions[ceil($pattern['center'] + $j)] += $emissionValue;
						}
						
						// Check for stop
						if (isset($stops[floor($pattern['center'] - $j)])) {
							$goLeft = false;
						}
						if (isset($stops[ceil($pattern['center'] + $j)])) {
							$goRight = false;
						}
					}
				}
			}
		}

		// Now we look for amplifiers
		foreach ($amplifierPatterns as $pattern) {
			// Check if we are not dealing with a subsumed pattern
			$key = $pattern['pattern'][0];
			$key = $key['token'].$key['tag'];
			if (isset($amplifierIndex[$key])) {
				if ($amplifierIndex[$key]['first'] == 1) {
					// We can still go left and right
					$goLeft = true;
					$goRight = true;
		
					// Emit positive sentiment to itself and neighbors
					for ($j = 1; $j <= self::$emissionRange; $j++) {
						$emissionValue = 1 + exp(-$j);
						
						// Set emission value to the left
						if (floor($pattern['center'] - $j) >= 0 && $goLeft) {
							$emissions[floor($pattern['center'] - $j)] *= $emissionValue;
						}
						// Emission value to the right
						if (ceil($pattern['center'] + $j) < $N &&
							($j != 0 || ceil($pattern['center']) != $pattern['center'])
							&& $goRight) {
							$emissions[ceil($pattern['center'] + $j)] *= $emissionValue;
						}
						
						// Check for stop
						if (isset($stops[floor($pattern['center'] - $j)])) {
							$goLeft = false;
						}
						if (isset($stops[ceil($pattern['center'] + $j)])) {
							$goRight = false;
						}
					}
				}
			}
		}
		
		// Look for attenuators
		foreach ($attenuatorPatterns as $pattern) {
			// Check if we are not dealing with a subsumed pattern
			$key = $pattern['pattern'][0];
			$key = $key['token'].$key['tag'];
			if (isset($attenuatorIndex[$key])) {
				if ($attenuatorIndex[$key]['first'] == 1) {
					// We can still go left and right
					$goLeft = true;
					$goRight = true;
		
					// Emit positive sentiment to itself and neighbors
					for ($j = 1; $j <= self::$emissionRange; $j++) {
						$emissionValue = 1 - exp(-$j);
						
						// Set emission value to the left
						if (floor($pattern['center'] - $j) >= 0 && $goLeft) {
							$emissions[floor($pattern['center'] - $j)] *= $emissionValue;
						}
						// Emission value to the right
						if (ceil($pattern['center'] + $j) < $N &&
							($j != 0 || ceil($pattern['center']) != $pattern['center'])
							&& $goRight) {
							$emissions[ceil($pattern['center'] + $j)] *= $emissionValue;
						}
						
						// Check for stop
						if (isset($stops[floor($pattern['center'] - $j)])) {
							$goLeft = false;
						}
						if (isset($stops[ceil($pattern['center'] + $j)])) {
							$goRight = false;
						}
					}
				}
			}
		}

		// Handle rightflips
		foreach ($rightflipPatterns as $pattern) {
			// Check if we are not dealing with a subsumed pattern
			$key = $pattern['pattern'][0];
			$key = $key['token'].$key['tag'];
			if (isset($rightflipIndex[$key])) {
				if ($rightflipIndex[$key]['first'] == 1) {
					// We can still go right
					$goRight = true;
		
					// Flip surrounding sentiments
					for ($j = 0; $j <= self::$emissionRange; $j++) {
						// To the right
						if (ceil($pattern['center'] + $j) < $N && $goRight) {
							$emissions[ceil($pattern['center'] + $j)] = -1 * $emissions[ceil($pattern['center'] + $j)];
						}
						
						if (isset($stops[ceil($pattern['center'] + $j)]) && $j > 0) {
							$goRight = false;
						}
					}
				}
			}
		}
		
		// Finally handle leftflips
		foreach ($leftflipPatterns as $pattern) {
			// Check if we are not dealing with a subsumed pattern
			$key = $pattern['pattern'][0];
			$key = $key['token'].$key['tag'];
			if (isset($leftflipIndex[$key])) {
				if ($leftflipIndex[$key]['first'] == 1) {
					// We can still go right
					$goLeft = true;
		
					// Flip surrounding sentiments
					for ($j = 0; $j <= self::$emissionRange; $j++) {
						// To the right
						if (floor($pattern['center'] - $j) >= 0 && $goLeft) {
							$emissions[floor($pattern['center'] - $j)] = -1 * $emissions[floor($pattern['center'] - $j)];
						}
						
						if (isset($stops[floor($pattern['center'] - $j)]) && $j > 0) {
							$goLeft = false;
						}
					}
				}
			}
		}
		
		// Compute total sentiment
		$sentiment = 0;
		$max = 0;
		for ($i = 0; $i < count($tokens); $i++) {
			$token = $tokens[$i]['token'];
			$emission = $emissions[$i];
			$sentiment += $emission;			
			if (abs($emission) > $max) {
				$max = abs($emission);
			}
		}
		
		// Output normalized polarities
		if (isset($polarizedTokens)) {
			for ($i = 0; $i < count($tokens); $i++) {
				$token = $tokens[$i]['token'];
				$emission = $emissions[$i];
				if ($max != 0) {
					$polarizedTokens[$i] = round($emission / $max, 6);
				}
				else {
					$polarizedTokens[$i] = 0;
				}
			}
		}
		
		if ($sentiment < 0) {
			return 'negative';
		}
		elseif ($sentiment > 0) {
			return 'positive';
		}
		else {
			return 'neutral';
		}
	}
	
	/**
	 * Removes all patterns that are subsumed by a pattern of a different type.
	 * Subsumption occurs when a pattern is strictly greater than another pattern.
	 * @param array $leftflipIndex Return variable. Initially contains all
	 * leftflips but on return contains only non-subsumed ones
	 * @param array $stopIndex Return variable. Initially contains all
	 * stops but on return contains only non-subsumed ones
	 * @param array $continuatorIndex Return variable. Initially contains all
	 * continuators but on return contains only non-subsumed ones
	 * @param array $positiveIndex Return variable. Initially contains all
	 * positives but on return contains only non-subsumed ones
	 * @param array $negativeIndex Return variable. Initially contains all
	 * negatives but on return contains only non-subsumed ones
	 * @param array $amplifierIndex Return variable. Initially contains all
	 * amplifiers but on return contains only non-subsumed ones
	 * @param array $attenuatorIndex Return variable. Initially contains all
	 * attenuators but on return contains only non-subsumed ones
	 * @param array $rightflipIndex Return variable. Initially contains all
	 * rightflips but on return contains only non-subsumed ones
	 */
	private static function removeSubsumed(&$leftflipIndex, &$stopIndex, &$continuatorIndex, &$positiveIndex,
		&$negativeIndex, &$amplifierIndex, &$attenuatorIndex, &$rightflipIndex) {
		// Go through each of them and match against all others separately
		foreach ($leftflipIndex as $key1 => $count1) {
			// Match against stopindex
			foreach ($stopIndex as $key2 => $count2) {
				if ($key1 == $key2) {
					if ($count1 > $count2) {
						unset($stopIndex[$key2]);
					}
					elseif ($count2 > $count1) {
						unset($leftflipIndex[$key1]);
					}
				}
			}
			
			// Match against continuatorIndex
			foreach ($continuatorIndex as $key2 => $count2) {
				if ($key1 == $key2) {
					if ($count1 > $count2) {
						unset($continuatorIndex[$key2]);
					}
					elseif ($count2 > $count1) {
						unset($leftflipIndex[$key1]);
					}
				}
			}
			
			// Match against positiveIndex
			foreach ($positiveIndex as $key2 => $count2) {
				if ($key1 == $key2) {
					if ($count1 > $count2) {
						unset($positiveIndex[$key2]);
					}
					elseif ($count2 > $count1) {
						unset($leftflipIndex[$key1]);
					}
				}
			}
			
			// Match against negativeIndex
			foreach ($negativeIndex as $key2 => $count2) {
				if ($key1 == $key2) {
					if ($count1 > $count2) {
						unset($negativeIndex[$key2]);
					}
					elseif ($count2 > $count1) {
						unset($leftflipIndex[$key1]);
					}
				}
			}
			
			// Match against amplifierIndex
			foreach ($amplifierIndex as $key2 => $count2) {
				if ($key1 == $key2) {
					if ($count1 > $count2) {
						unset($amplifierIndex[$key2]);
					}
					elseif ($count2 > $count1) {
						unset($leftflipIndex[$key1]);
					}
				}
			}
			
			// Match against attenuatorIndex
			foreach ($attenuatorIndex as $key2 => $count2) {
				if ($key1 == $key2) {
					if ($count1 > $count2) {
						unset($attenuatorIndex[$key2]);
					}
					elseif ($count2 > $count1) {
						unset($leftflipIndex[$key1]);
					}
				}
			}
			
			// Match against rightflipIndex
			foreach ($rightflipIndex as $key2 => $count2) {
				if ($key1 == $key2) {
					if ($count1 > $count2) {
						unset($rightflipIndex[$key2]);
					}
					elseif ($count2 > $count1) {
						unset($leftflipIndex[$key1]);
					}
				}
			}
		}
		
		foreach ($stopIndex as $key1 => $count1) {
			// Match against continuatorIndex
			foreach ($continuatorIndex as $key2 => $count2) {
				if ($key1 == $key2) {
					if ($count1 > $count2) {
						unset($continuatorIndex[$key2]);
					}
					elseif ($count2 > $count1) {
						unset($stopIndex[$key1]);
					}
				}
			}
			
			// Match against positiveIndex
			foreach ($positiveIndex as $key2 => $count2) {
				if ($key1 == $key2) {
					if ($count1 > $count2) {
						unset($positiveIndex[$key2]);
					}
					elseif ($count2 > $count1) {
						unset($stopIndex[$key1]);
					}
				}
			}
			
			// Match against negativeIndex
			foreach ($negativeIndex as $key2 => $count2) {
				if ($key1 == $key2) {
					if ($count1 > $count2) {
						unset($negativeIndex[$key2]);
					}
					elseif ($count2 > $count1) {
						unset($stopIndex[$key1]);
					}
				}
			}
			
			// Match against amplifierIndex
			foreach ($amplifierIndex as $key2 => $count2) {
				if ($key1 == $key2) {
					if ($count1 > $count2) {
						unset($amplifierIndex[$key2]);
					}
					elseif ($count2 > $count1) {
						unset($stopIndex[$key1]);
					}
				}
			}
			
			// Match against attenuatorIndex
			foreach ($attenuatorIndex as $key2 => $count2) {
				if ($key1 == $key2) {
					if ($count1 > $count2) {
						unset($attenuatorIndex[$key2]);
					}
					elseif ($count2 > $count1) {
						unset($stopIndex[$key1]);
					}
				}
			}
			
			// Match against rightflipIndex
			foreach ($rightflipIndex as $key2 => $count2) {
				if ($key1 == $key2) {
					if ($count1 > $count2) {
						unset($rightflipIndex[$key2]);
					}
					elseif ($count2 > $count1) {
						unset($stopIndex[$key1]);
					}
				}
			}
		}
		
		foreach ($continuatorIndex as $key1 => $count1) {
			// Match against positiveIndex
			foreach ($positiveIndex as $key2 => $count2) {
				if ($key1 == $key2) {
					if ($count1 > $count2) {
						unset($positiveIndex[$key2]);
					}
					elseif ($count2 > $count1) {
						unset($continuatorIndex[$key1]);
					}
				}
			}
			
			// Match against negativeIndex
			foreach ($negativeIndex as $key2 => $count2) {
				if ($key1 == $key2) {
					if ($count1 > $count2) {
						unset($negativeIndex[$key2]);
					}
					elseif ($count2 > $count1) {
						unset($continuatorIndex[$key1]);
					}
				}
			}
			
			// Match against amplifierIndex
			foreach ($amplifierIndex as $key2 => $count2) {
				if ($key1 == $key2) {
					if ($count1 > $count2) {
						unset($amplifierIndex[$key2]);
					}
					elseif ($count2 > $count1) {
						unset($continuatorIndex[$key1]);
					}
				}
			}
			
			// Match against attenuatorIndex
			foreach ($attenuatorIndex as $key2 => $count2) {
				if ($key1 == $key2) {
					if ($count1 > $count2) {
						unset($attenuatorIndex[$key2]);
					}
					elseif ($count2 > $count1) {
						unset($continuatorIndex[$key1]);
					}
				}
			}
			
			// Match against rightflipIndex
			foreach ($rightflipIndex as $key2 => $count2) {
				if ($key1 == $key2) {
					if ($count1 > $count2) {
						unset($rightflipIndex[$key2]);
					}
					elseif ($count2 > $count1) {
						unset($continuatorIndex[$key1]);
					}
				}
			}
		}
		
		foreach ($positiveIndex as $key1 => $count1) {
			// Match against negativeIndex
			foreach ($negativeIndex as $key2 => $count2) {
				if ($key1 == $key2) {
					if ($count1 > $count2) {
						unset($negativeIndex[$key2]);
					}
					elseif ($count2 > $count1) {
						unset($positiveIndex[$key1]);
					}
				}
			}
			
			// Match against amplifierIndex
			foreach ($amplifierIndex as $key2 => $count2) {
				if ($key1 == $key2) {
					if ($count1 > $count2) {
						unset($amplifierIndex[$key2]);
					}
					elseif ($count2 > $count1) {
						unset($positiveIndex[$key1]);
					}
				}
			}
			
			// Match against attenuatorIndex
			foreach ($attenuatorIndex as $key2 => $count2) {
				if ($key1 == $key2) {
					if ($count1 > $count2) {
						unset($attenuatorIndex[$key2]);
					}
					elseif ($count2 > $count1) {
						unset($positiveIndex[$key1]);
					}
				}
			}
			
			// Match against rightflipIndex
			foreach ($rightflipIndex as $key2 => $count2) {
				if ($key1 == $key2) {
					if ($count1 > $count2) {
						unset($rightflipIndex[$key2]);
					}
					elseif ($count2 > $count1) {
						unset($positiveIndex[$key1]);
					}
				}
			}
		}
		
		foreach ($negativeIndex as $key1 => $count1) {
			// Match against amplifierIndex
			foreach ($amplifierIndex as $key2 => $count2) {
				if ($key1 == $key2) {
					if ($count1 > $count2) {
						unset($amplifierIndex[$key2]);
					}
					elseif ($count2 > $count1) {
						unset($negativeIndex[$key1]);
					}
				}
			}
			
			// Match against attenuatorIndex
			foreach ($attenuatorIndex as $key2 => $count2) {
				if ($key1 == $key2) {
					if ($count1 > $count2) {
						unset($attenuatorIndex[$key2]);
					}
					elseif ($count2 > $count1) {
						unset($negativeIndex[$key1]);
					}
				}
			}
			
			// Match against rightflipIndex
			foreach ($rightflipIndex as $key2 => $count2) {
				if ($key1 == $key2) {
					if ($count1 > $count2) {
						unset($rightflipIndex[$key2]);
					}
					elseif ($count2 > $count1) {
						unset($negativeIndex[$key1]);
					}
				}
			}
		}
		
		foreach ($amplifierIndex as $key1 => $count1) {
			// Match against attenuatorIndex
			foreach ($attenuatorIndex as $key2 => $count2) {
				if ($key1 == $key2) {
					if ($count1 > $count2) {
						unset($attenuatorIndex[$key2]);
					}
					elseif ($count2 > $count1) {
						unset($amplifierIndex[$key1]);
					}
				}
			}
			
			// Match against rightflipIndex
			foreach ($rightflipIndex as $key2 => $count2) {
				if ($key1 == $key2) {
					if ($count1 > $count2) {
						unset($rightflipIndex[$key2]);
					}
					elseif ($count2 > $count1) {
						unset($amplifierIndex[$key1]);
					}
				}
			}
		}
		
		foreach ($attenuatorIndex as $key1 => $count1) {
			// Match against rightflipIndex
			foreach ($rightflipIndex as $key2 => $count2) {
				if ($key1 == $key2) {
					if ($count1 > $count2) {
						unset($rightflipIndex[$key2]);
					}
					elseif ($count2 > $count1) {
						unset($attenuatorIndex[$key1]);
					}
				}
			}
		}
	}
	
	/**
	 * Stores (caches) the model for a given language
	 * @param string $lang The language
	 */
	public static function storeModel($lang) {
		$model = array(
			'amplifiers' => self::$amplifiers,
			'attenuators' => self::$attenuators,
			'rightflips' => self::$rightflips,
			'continuators' => self::$continuators,
			'emissionRange' => self::$emissionRange,
			'leftflip' => self::$leftflips,
			'negatives' => self::$negatives,
			'positives' => self::$positives,
			'stops' => self::$stops
		);
		// Cache model
        file_put_contents('Cache/emission_'.$lang, serialize($model));
	}
	
	/**
	 * Loads a model for a given language
	 * @param string $lang The language
	 */
	public static function loadModel($lang) {
		// Get cached model
		$model = unserialize(file_get_contents('Cache/emission_'.$lang));
		if (empty($model)) {
			return false;
		}

		self::$amplifiers = $model['amplifiers'];
		self::$attenuators = $model['attenuators'];
		self::$rightflips = $model['rightflips'];
		self::$continuators = $model['continuators'];
		self::$emissionRange = $model['emissionRange'];
		self::$leftflips = $model['leftflip'];
		self::$negatives = $model['negatives'];
		self::$positives = $model['positives'];
		self::$stops = $model['stops'];
		


		return true;
	}
	
	/**
	 * Sets the emission range
	 * @param unknown_type $range
	 */
	public static function setEmissionRange($range) {
		self::$emissionRange = $range;
	}
}
?>