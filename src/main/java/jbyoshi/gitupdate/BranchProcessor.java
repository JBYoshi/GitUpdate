package jbyoshi.gitupdate;

import java.io.*;
import java.util.*;

import org.eclipse.jgit.api.*;
import org.eclipse.jgit.api.errors.*;
import org.eclipse.jgit.lib.*;

public abstract class BranchProcessor extends Processor<Map.Entry<String, Ref>> {

	@Override
	public final Iterable<Map.Entry<String, Ref>> getKeys(Repository repo) throws IOException {
		return Utils.getLocalBranches(repo).entrySet();
	}

	@Override
	public final void process(Repository repo, Git git, Map.Entry<String, Ref> branch)
			throws GitAPIException, IOException {
		process(repo, git, branch.getKey(), branch.getValue());
	}

	public abstract void process(Repository repo, Git git, String branch, Ref ref) throws GitAPIException, IOException;
}
