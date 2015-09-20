package jbyoshi.gitupdate;

import java.io.*;
import java.util.*;

import org.eclipse.jgit.lib.*;

public final class Utils {
	private Utils() {
	}

	public static Map<String, Ref> getLocalBranches(Repository repo) throws IOException {
		return repo.getRefDatabase().getRefs(Constants.R_HEADS);
	}

	public static String getShortBranch(String fullBranch) {
		if (fullBranch.startsWith(Constants.R_HEADS)) {
			return fullBranch.substring(Constants.R_HEADS.length());
		}
		if (fullBranch.startsWith(Constants.R_REMOTES)) {
			return fullBranch.substring(Constants.R_REMOTES.length());
		}
		return fullBranch;
	}
}
