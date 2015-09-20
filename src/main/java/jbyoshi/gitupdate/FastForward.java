package jbyoshi.gitupdate;

import java.io.*;
import java.text.*;

import org.eclipse.jgit.api.*;
import org.eclipse.jgit.api.errors.*;
import org.eclipse.jgit.internal.*;
import org.eclipse.jgit.lib.*;
import org.eclipse.jgit.lib.RefUpdate.*;
import org.eclipse.jgit.revwalk.*;

public class FastForward extends BranchProcessor {

	private int fastForwards;

	@Override
	public void process(Repository repo, Git git, String branch, Ref ref) throws GitAPIException, IOException {
		tryFastForward(repo, ref, repo.getRef(new BranchConfig(repo.getConfig(), branch).getTrackingBranch()));
		// TODO Don't hardcode this
		tryFastForward(repo, ref, repo.getRef("refs/remotes/upstream/" + branch));
	}

	private boolean tryFastForward(Repository repo, Ref ref, Ref target) throws GitAPIException, IOException {
		if (ref == null || target == null) {
			return false;
		}
		target = repo.peel(target);
		if (!ref.equals(repo.getRef(Constants.HEAD).getTarget())) {
			RevWalk revWalk = new RevWalk(repo);

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
					System.out.println("Fast-forwarded " + ref.getName() + " to " + target.getName());
					fastForwards++;
					return true;
				case REJECTED:
				case LOCK_FAILURE:
					System.err.println(new ConcurrentRefUpdateException(JGitText.get().couldNotLockHEAD,
							refUpdate.getRef(), rc));
					break;
				case NO_CHANGE:
					break;
				default:
					System.err.println(new JGitInternalException(MessageFormat
							.format(JGitText.get().updatingRefFailed, ref.getName(), targetId.toString(), rc)));
					break;
				}
			}
			return false;
		}
		try {
			MergeResult result = Git.wrap(repo).merge().setFastForward(MergeCommand.FastForwardMode.FF_ONLY)
					.include(target.getTarget()).call();
			if (result.getMergeStatus() == MergeResult.MergeStatus.ALREADY_UP_TO_DATE) {
				// Ignore
			} else if (result.getMergeStatus() == MergeResult.MergeStatus.FAST_FORWARD) {
				System.out.println("Fast-forwarded " + ref.getName() + " to " + target.getName());
				fastForwards++;
				return true;
			} else {
				System.err.println("Fast-forward failed: status " + result.getMergeStatus());
			}
		} catch (NoHeadException e) {
			// Ignore
		} catch (ConcurrentRefUpdateException e) {
			System.err.println(e);
		} catch (CheckoutConflictException e) {
			System.err.println(e);
		} catch (WrongRepositoryStateException e) {
			System.err.println(e);
		}
		return false;
	}

	@Override
	public void report() {
		System.out.println(fastForwards + " branch" + (fastForwards == 1 ? "" : "es") + " fast forwarded.");
	}

}
