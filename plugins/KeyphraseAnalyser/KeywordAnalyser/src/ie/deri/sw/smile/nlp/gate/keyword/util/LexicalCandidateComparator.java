/*
 * 
 * LexicalCandidateComparator.java, provides keyword/keyphrase extraction as a GATE plugin
 * Copyright (C) 2008  Alexander Schutz
 * National University of Ireland, Galway
 * Digital Enterprise Research Institute
 * 
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 *
 */

package ie.deri.sw.smile.nlp.gate.keyword.util;

import ie.deri.sw.smile.nlp.gate.keyword.LexicalCandidate;

import java.util.Comparator;

public class LexicalCandidateComparator implements Comparator<LexicalCandidate> {

	public int compare(LexicalCandidate arg0, LexicalCandidate arg1) {
		if (arg0.getRelevance() != (double)-1 && arg1.getRelevance() != (double)-1 && arg0.getRelevance() > arg1.getRelevance()){
			return -1;
		} else if (arg0.getRelevance() != (double)-1 && arg1.getRelevance() != (double)-1 && arg0.getRelevance() < arg1.getRelevance() ){
			return 1;
		} else {
			if (arg0.getFrequency() > arg1.getFrequency()){
				return -1;
			} else if ( arg0.getFrequency() < arg1.getFrequency()){
				return 1;
			} else {
				return arg0.getStringValue().compareTo(arg1.getStringValue());
			}
		}
	}

}
