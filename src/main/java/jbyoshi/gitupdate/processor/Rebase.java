package jbyoshi.gitupdate.processor;

import java.io.*;

import org.eclipse.jgit.api.*;
import org.eclipse.jgit.api.errors.*;
import org.eclipse.jgit.lib.*;

import jbyoshi.gitupdate.*;

public final class Rebase extends BranchProcessor {

	@Override
	public void process(Repository repo, Git git, String branch, Ref ref, Report report)
			throws IOException, GitAPIException {
		String trackingBranch = new BranchConfig(repo.getConfig(), branch).getTrackingBranch();
		if (trackingBranch == null) {
			return;
		}

		Ref oldHead = repo.getRef(Constants.HEAD).getTarget();
		if (!oldHead.equals(ref)) {
			try {
				git.checkout().setName(ref.getName()).setCreateBranch(false).call();
			} catch (RefAlreadyExistsException e) {
				throw new AssertionError(e);
			} catch (RefNotFoundException e) {
				throw new AssertionError(e);
			} catch (InvalidRefNameException e) {
				throw new AssertionError(e);
			}
		}

		try {
			RebaseResult result = git.rebase().setUpstream(trackingBranch).call();
			switch (result.getStatus()) {
			case STOPPED:
			case CONFLICTS:
				Report conflicts = report.newChild("Conflicts").error();
				result.getConflicts().forEach(file -> conflicts.newChild(file).error());
				git.rebase().setOperation(RebaseCommand.Operation.ABORT).call();
				break;
			case UP_TO_DATE:
				break;
			default:
				Report details = report.newChild(result.getStatus().toString());
				if (result.getStatus().isSuccessful()) {
					details.modified();
				} else {
					details.error();
				}
				break;
			}
		} catch (NoHeadException e) {
			throw new AssertionError(e);
		} finally {
			git.checkout().setName(branch).call();
		}
	}

}
