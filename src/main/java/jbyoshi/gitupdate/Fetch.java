package jbyoshi.gitupdate;

import org.eclipse.jgit.api.*;
import org.eclipse.jgit.api.errors.*;
import org.eclipse.jgit.lib.*;
import org.eclipse.jgit.transport.*;

public final class Fetch extends RemoteProcessor {

	private int fetches;

	@Override
	public void process(Repository repo, Git git, String remote, String fullRemote) throws GitAPIException {
		System.out.println("\tFetching remote " + remote);
		FetchResult result = git.fetch().setRemoveDeletedRefs(true).setCredentialsProvider(Prompts.INSTANCE)
				.setRemote(remote).call();
		for (TrackingRefUpdate update : result.getTrackingRefUpdates()) {
			if (update.getRemoteName().equals(Constants.R_HEADS + Constants.HEAD)) {
				continue;
			}
			System.out.print("\t\t" + Utils.getShortBranch(update.getRemoteName()) + ": ");
			String oldId = update.getOldObjectId().name();
			if (update.getOldObjectId().equals(ObjectId.zeroId())) {
				oldId = "new branch";
			}
			String newId = update.getNewObjectId().name();
			if (update.getNewObjectId().equals(ObjectId.zeroId())) {
				newId = "deleted";
			}
			System.out.println(oldId + " -> " + newId);
			fetches++;
		}
	}

	@Override
	public void report() {
		System.out.println(fetches + " branch" + (fetches == 1 ? "" : "es") + " fetched.");
	}

}
