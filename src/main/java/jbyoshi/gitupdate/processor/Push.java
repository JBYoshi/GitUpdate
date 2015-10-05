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

public class Push extends Processor {

	@Override
	public void registerTasks(Repository repo, Git git, Task root) throws Exception {
		Task me = root.newChild(getClass().getSimpleName());
		// Group the branches by their remotes.
		Multimap<String, String> branchList = HashMultimap.create();
		for (String branch : Utils.getLocalBranches(repo).keySet()) {
			branchList.put(new BranchConfig(repo.getConfig(), branch).getRemote(), branch);
		}
		for (Map.Entry<String, Collection<String>> remote : branchList.asMap().entrySet()) {
			me.newChild(remote.getKey(), report -> {
				try {
					process(repo, git, remote.getKey(), Constants.R_REMOTES + remote.getKey() + "/", remote.getValue(),
							report);
				} catch (Exception e) {
					report.newErrorChild(e);
				}
			});
		}
	}

	private static void process(Repository repo, Git git, String remote, String fullRemote, Collection<String> branches,
			Report report) throws Exception {
		// Figure out if anything needs to be pushed.
		boolean canPush = false;
		for (Iterator<String> it = branches.iterator(); it.hasNext();) {
			String branch = it.next();
			BranchConfig config = new BranchConfig(repo.getConfig(), branch);
			ObjectId target = repo.getRef(branch).getObjectId();

			Ref remoteRef = repo.getRef(config.getRemoteTrackingBranch());
			if (remoteRef == null || !target.equals(remoteRef.getObjectId())) {
				canPush = true;
				System.out.println("Can push " + repo.getWorkTree().getName() + " " + branch + " -> "
						+ config.getRemoteTrackingBranch());
				break;
			} else {
				System.out.println("Cannot push " + repo.getWorkTree().getName() + " " + branch + " -> "
						+ config.getRemoteTrackingBranch());
			}
		}

		if (!canPush) {
			return;
		}

		PushCommand push = git.push().setCredentialsProvider(Prompts.INSTANCE).setTimeout(5)
				.setRemote(remote);
		for (String branch : branches) {
			push.add(Constants.R_HEADS + branch);
		}
		for (PushResult result : push.call()) {
			for (RemoteRefUpdate update : result.getRemoteUpdates()) {
				if (update.getStatus() == RemoteRefUpdate.Status.OK) {
					String branchName = Utils.getShortBranch(update.getSrcRef());
					ObjectId oldId = update.getExpectedOldObjectId();
					String old = oldId.equals(ObjectId.zeroId()) ? "new branch" : oldId.name();
					report.newChild(branchName + ": " + old + " -> " + update.getNewObjectId().name())
					.modified();
				}
			}
		}
	}

}
