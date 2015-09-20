package jbyoshi.gitupdate;

import java.util.*;

import org.eclipse.jgit.api.*;
import org.eclipse.jgit.lib.*;

public abstract class SingleProcessor extends Processor<Void> {

	@Override
	public final Iterable<Void> getKeys(Repository repo) throws Exception {
		return Collections.singleton(null);
	}

	@Override
	public final void process(Repository repo, Git git, Void key) throws Exception {
		process(repo, git);
	}

	public abstract void process(Repository repo, Git git) throws Exception;

}
