/*
 * Copyright (c) 2015 JBYoshi
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package jbyoshi.gitupdate.processor;

import org.eclipse.jgit.api.*;
import org.eclipse.jgit.api.errors.*;
import org.eclipse.jgit.lib.*;
import org.eclipse.jgit.transport.*;

import jbyoshi.gitupdate.*;

public final class Fetch extends RemoteProcessor {

	@Override
	public void process(Repository repo, Git git, String remote, String fullRemote, Report report)
			throws GitAPIException {
		FetchResult result = git.fetch().setRemoveDeletedRefs(true).setCredentialsProvider(Prompts.INSTANCE)
				.setRemote(remote).call();
		for (TrackingRefUpdate update : result.getTrackingRefUpdates()) {
			if (update.getRemoteName().equals(Constants.R_HEADS + Constants.HEAD)) {
				continue;
			}
			StringBuilder text = new StringBuilder(Utils.getShortBranch(update.getRemoteName())).append(": ");
			String oldId = update.getOldObjectId().name();
			if (update.getOldObjectId().equals(ObjectId.zeroId())) {
				oldId = "new branch";
			}
			String newId = update.getNewObjectId().name();
			if (update.getNewObjectId().equals(ObjectId.zeroId())) {
				newId = "deleted";
			}
			text.append(oldId).append(" -> ").append(newId);
			report.newChild(text.toString()).modified();
		}
	}

}
