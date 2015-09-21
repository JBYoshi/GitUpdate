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

import java.util.*;

import org.eclipse.jgit.api.*;
import org.eclipse.jgit.lib.*;
import org.eclipse.jgit.transport.*;

import com.google.common.collect.*;

import jbyoshi.gitupdate.*;
import jbyoshi.gitupdate.ui.*;

public class Push extends SingleProcessor {

	@Override
	public void process(Repository repo, Git git, ReportData data) throws Exception {
		Map<String, Ref> pushRefs = Utils.getLocalBranches(repo);
		pushRefs = Maps.filterKeys(pushRefs, (k) -> new BranchConfig(repo.getConfig(), k).getRemote() != null);
		Map<String, ObjectId> pushBranches = Maps.transformValues(pushRefs, Ref::getObjectId);
		pushBranches = Maps.filterValues(pushBranches, (v) -> v != null);

		if (!pushBranches.isEmpty()) {
			PushCommand push = git.push().setCredentialsProvider(Prompts.INSTANCE).setTimeout(5);
			for (String branch : pushBranches.keySet()) {
				push.add(Constants.R_HEADS + branch);
			}
			for (PushResult result : push.call()) {
				for (RemoteRefUpdate update : result.getRemoteUpdates()) {
					if (update.getStatus() == RemoteRefUpdate.Status.OK) {
						String branchName = Utils.getShortBranch(update.getSrcRef());
						ObjectId oldId = pushBranches.get(branchName);
						String old = oldId.equals(ObjectId.zeroId()) ? "new branch" : oldId.name();
						data.newChild(branchName + ": " + old + " -> " + update.getNewObjectId().name());
					}
				}
			}
		}
	}

}
