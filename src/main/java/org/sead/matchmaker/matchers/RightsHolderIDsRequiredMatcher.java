/*
 *
 * Copyright 2015 University of Michigan
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 *
 *
 * @author myersjd@umich.edu
 */

package org.sead.matchmaker.matchers;

import org.bson.Document;
import org.bson.types.BasicBSONList;
import org.sead.matchmaker.Matcher;
import org.sead.matchmaker.MatchmakerConstants;
import org.sead.matchmaker.RuleResult;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.UniformInterfaceException;
import com.sun.jersey.api.client.WebResource;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Iterator;

import javax.ws.rs.core.MediaType;

public class RightsHolderIDsRequiredMatcher implements Matcher {

	public RuleResult runRule(Document aggregation, Object rightsHolders,
			BasicBSONList affiliations, Document preferences,
			Document statsDocument, Document profile, Object context) {
		RuleResult result = new RuleResult();
		boolean idsRequired = profile.getBoolean("Rights Holder IDs Required",
				false);
		if (!idsRequired) {
			return result;
		}

		boolean atleastonecreator = false;
		BasicBSONList nonIDscreators = new BasicBSONList();
		if (rightsHolders != null) {
			if (rightsHolders instanceof ArrayList) {
				Iterator<String> iter = ((ArrayList<String>) rightsHolders)
						.iterator();

				while (iter.hasNext()) {
					String creator = iter.next();
					atleastonecreator = true;
					String idString = getInternalId(creator);
					if (idString == null) {
						nonIDscreators.add(creator);
					}
				}

			} else {
				// BasicDBObject - single value
				atleastonecreator = true;
				String idString = getInternalId(((String) rightsHolders));
				if (idString == null) {
					nonIDscreators.add((String) rightsHolders);
				}
			}
		}

		if (!atleastonecreator) {
			result.setResult(-1, "No Rights Holders found");
			return result;
		} else {
			if (nonIDscreators.size() > 0) {
				StringBuffer missingIDs = new StringBuffer();
				for (Object name : nonIDscreators) {
					if (missingIDs.length() != 0) {
						missingIDs.append(", ");
					}
					missingIDs.append(name);
				}

				result.setResult(
						-1,
						"Rights Holders listed that do not have global identifiers (e.g. orcid.org/<id#>: "
								+ missingIDs.toString());
			} else {
				result.setResult(1, "All Rights Holders have global IDs");
			}
		}
		return result;
	}

	public String getName() {
		return "Rights Holder IDs Required";
	}

	public Document getDescription() {
		return new Document("Rule Name", getName())
				.append("Repository Trigger",
						" \"Rights Holder IDs Required\": \"http://sead-data.net/terms/RightsHolderIdsRequired\" : true,  ")
				.append("Publication Trigger",
						"\"Rights Holder\": http://purl.org/dc/terms/rightsHolder entries at the top level in the publication request");
	}

	// FixMe - Copied from org.seadpdt.ROServices, should be one shared method
	// Parse the string to get the internal ID that the profile would have been
	// stored under.
	// If the value is a plain string name, or a 1.5 style name:VIVO URL, return
	// null since we
	// have no profiles
	private String getInternalId(String personID) {
		WebResource pdtResource = Client.create().resource(
				MatchmakerConstants.pdtUrl);
		ClientResponse response;
		try {
			response = pdtResource
					.path(MatchmakerConstants.PDT_PEOPLE + "/canonical/"
							+ URLEncoder.encode(personID, "UTF-8"))
					.accept(MediaType.TEXT_PLAIN).get(ClientResponse.class);

			if (response.getStatus() == 200) {
				return response.getEntity(String.class);
			}
		} catch (UniformInterfaceException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;

	}

}