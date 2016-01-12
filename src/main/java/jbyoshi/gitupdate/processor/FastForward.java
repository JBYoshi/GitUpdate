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

import java.io.*;
import java.text.*;

import org.eclipse.jgit.api.*;
import org.eclipse.jgit.api.errors.*;
import org.eclipse.jgit.internal.*;
import org.eclipse.jgit.lib.*;
import org.eclipse.jgit.lib.RefUpdate.*;
import org.eclipse.jgit.revwalk.*;

import jbyoshi.gitupdate.*;

public class FastForward extends BranchProcessor {

	@Override
	public void process(Repository repo, Git git, String branch, Ref ref, Report report)
			throws GitAPIException, IOException {
		tryFastForward(repo, ref, repo.getRef(new BranchConfig(repo.getConfig(), branch).getTrackingBranch()), report);
		// TODO Don't hardcode this
		tryFastForward(repo, ref, repo.getRef(Constants.R_REMOTES + "upstream/" + branch), report);
	}

	private static boolean tryFastForward(Repository repo, Ref ref, Ref target, Report report)
			throws GitAPIException, IOException {
		if (ref == null || target == null) {
			return false;
		}
		target = repo.peel(target);
		if (!ref.equals(repo.getRef(Constants.HEAD).getTarget())) {
			try (RevWalk revWalk = new RevWalk(repo)) {
				ObjectId targetId = target.getPeeledObjectId();
				if (targetId == null) {
					targetId = target.getObjectId();
				}

				RevCommit targetCommit = revWalk.lookupCommit(targetId);
				ObjectId sourceId = ref.getObjectId();
				RevCommit sourceCommit = revWalk.lookupCommit(sourceId);
				if (revWalk.isMergedInto(sourceCommit, targetCommit)) {
					RefUpdate refUpdate = repo.updateRef(ref.getName());
					refUpdate.setNewObjectId(targetCommit);
					refUpdate.setRefLogMessage("Fast forward", false);
					refUpdate.setExpectedOldObjectId(sourceId);
					Result rc = refUpdate.update();
					switch (rc) {
					case NEW:
					case FAST_FORWARD:
						report.newChild(ref.getName() + " -> " + target.getName()).modified();
						return true;
					case REJECTED:
					case LOCK_FAILURE:
						report.newErrorChild(new ConcurrentRefUpdateException(JGitText.get().couldNotLockHEAD,
								refUpdate.getRef(), rc));
						break;
					case NO_CHANGE:
						break;
					default:
						report.newErrorChild(new JGitInternalException(MessageFormat
								.format(JGitText.get().updatingRefFailed, ref.getName(), targetId.toString(), rc)));
						break;
					}
				}
				return false;
			}
		}
		try {
			MergeResult result = Git.wrap(repo).merge().setFastForward(MergeCommand.FastForwardMode.FF_ONLY)
					.include(target.getTarget()).call();
			if (result.getMergeStatus() == MergeResult.MergeStatus.ALREADY_UP_TO_DATE) {
				// Ignore
			} else if (result.getMergeStatus() == MergeResult.MergeStatus.FAST_FORWARD) {
				report.newChild("Fast-forwarded " + ref.getName() + " to " + target.getName()).modified();
				return true;
			} else {
				report.newChild("Fast-forward failed: status " + result.getMergeStatus()).error();
			}
		} catch (NoHeadException e) {
			// Ignore
		}
		return false;
	}

}
