package de.fau.osr.core.vcs.impl;

import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.diff.RawTextComparator;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.util.io.DisabledOutputStream;
import org.slf4j.LoggerFactory;

import de.fau.osr.core.vcs.base.CommitFile;
import de.fau.osr.core.vcs.base.CommitState;
import de.fau.osr.core.vcs.interfaces.VcsClient;
import de.fau.osr.parser.CommitMessageParser;
import de.fau.osr.parser.GitCommitMessageParser;

/**
 * @author Gayathery
 * @desc VCS Client implementation for Git
 *
 */
public class GitVcsClient implements VcsClient{

	Git git;
	String repositoryURI;
	Repository repo;
	Boolean isConnected;
	
	/**
	 * @param repositoryURI
	 * @author Gayathery
	 */
	public GitVcsClient(String repositoryURI)
	{
		this.repositoryURI = repositoryURI;
		isConnected = false;
		repo = null;
		git = null;
	}
	
	
	/* (non-Javadoc)
	 * @see org.amos.core.vcs.interfaces.VcsClient#connect()
	 * @author Gayathery
	 */
	
	public boolean connect()
	{
		FileRepositoryBuilder builder = new FileRepositoryBuilder();
		try {
			repo = builder.setGitDir(new File(repositoryURI)).setMustExist(true).build();
			git = new Git(repo);
		} catch (IOException e) {
			e.printStackTrace();
			return isConnected;
		}
		
		isConnected = true;
		return isConnected;
	}
	
	/* (non-Javadoc)
	 * @see org.amos.core.vcs.interfaces.VcsClient#getBranchList()
	 * @author Gayathery
	 */
	@Override
	public Iterator<String> getBranchList() {
		ArrayList<String> branchList = new ArrayList<String>();
		try {
			List<Ref> branches = git.branchList().call();
			
			for (Ref branch : branches) {
				branchList.add(branch.getName());
			}
		} catch (GitAPIException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return branchList.iterator();
	}
	
	
	/* (non-Javadoc)
	 * @see org.amos.core.vcs.interfaces.VcsClient#getCommitList()
	 * @author Gayathery
	 */
	@Override
	public Iterator<String> getCommitList() {
		try {
			final Iterator<RevCommit> commits = git.log().all().call().iterator();
			return new Iterator<String>() {
				@Override
				public boolean hasNext() {
					return commits.hasNext();
				}

				@Override
				public String next() {
					return commits.next().getName();
				}

				@Override
				public void remove() {
					throw new UnsupportedOperationException();
				}

			};

		} catch (GitAPIException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

	
	private void getTreeDiffFiles(RevTree a, RevTree b, Set<CommitFile> s, String commitID) throws IOException {
		DiffFormatter dif = new DiffFormatter(DisabledOutputStream.INSTANCE);
		dif.setRepository(repo);
		dif.setDiffComparator(RawTextComparator.DEFAULT);
		dif.setDetectRenames(true);
		List<DiffEntry> diffs =  dif.scan(a, b);

		for (DiffEntry diff : diffs) {
			CommitState commitState;
			switch(diff.getChangeType())
			{
			case ADD:
				commitState = CommitState.ADDED;
				break;
			case MODIFY:
				commitState = CommitState.MODIFIED;
				break;
			case RENAME:
				commitState = CommitState.RENAMED;
				break;
			case DELETE:
				commitState = CommitState.DELETED;
				break;
			case COPY:
				commitState = CommitState.COPIED;
				break;
			default:
				throw new RuntimeException("Encountered an unknown DiffEntry.ChangeType " + diff.getChangeType() + ". Please report a bug.");
			}
			CommitFile commitFile = new CommitFile(new File(diff.getOldPath()),new File(diff.getNewPath()),commitState,commitID);
			s.add(commitFile);
			LoggerFactory.getLogger(getClass()).debug(
					MessageFormat.format("({0} {1} {2})",
							diff.getChangeType().name(),
							diff.getNewMode().getBits(),
							diff.getNewPath()));
		}
	}

	/* (non-Javadoc)
	 * @see org.amos.core.vcs.interfaces.VcsClient#getCommitFiles(java.lang.String)
	 * @author Gayathery
	 */
	@Override
	public Iterator<CommitFile> getCommitFiles(String commitID) {
		return getCommitFilesInternal(commitID).iterator();
	}
	/* (non-Javadoc)
	 * @see org.amos.core.vcs.interfaces.VcsClient#getCommitFilesInternal(java.lang.String)
	 * @author Gayathery
	 */
	private Set<CommitFile> getCommitFilesInternal(String commitID) {

		HashSet<CommitFile> commitFilesList = new HashSet<>();

		try {
			Repository repo = git.getRepository();
			ObjectId obj = repo.resolve(commitID);
			RevWalk revWalk = new RevWalk(repo);
			RevCommit commit = revWalk.parseCommit(obj);
			RevCommit[] parents = commit.getParents();
			if(parents.length == 0) {
				getTreeDiffFiles(commit.getTree(), null, commitFilesList,commit.getName());
			}
			for(RevCommit parent : parents) {
				revWalk.parseBody(parent);
				getTreeDiffFiles(parent.getTree(), commit.getTree(), commitFilesList,commit.getName());
			}
		} catch (IOException e1) {

			e1.printStackTrace();
		} catch (ArrayIndexOutOfBoundsException e1) {

			e1.printStackTrace();
		}

		return commitFilesList;
	}

	/* (non-Javadoc)
	 * @see org.amos.core.vcs.interfaces.VcsClient#getCommitMessage(java.lang.String)
	 * @author Florian Gerdes
	 */
	@Override
	public String getCommitMessage(String commitID) {
		RevCommit commit = null;
		try {
			Repository repo = git.getRepository();
			ObjectId obj = repo.resolve(commitID);
			commit = (new RevWalk(repo)).parseCommit(obj);
		} catch (IOException e1) {

			e1.printStackTrace();
		} catch (ArrayIndexOutOfBoundsException e1) {

			e1.printStackTrace();
		}
		return commit.getFullMessage();
	}
	
	/* (non-Javadoc)
	 * @see org.amos.core.vcs.interfaces.VcsClient#getCommitFilesForRequirementID(java.lang.String)
	 * @author Gayathery
	 */
	public Iterator<CommitFile> getCommitFilesForRequirementID(String requirementID)
	{
		HashSet<CommitFile> commitFilesList = new HashSet<CommitFile>();
		
		try {
			Iterator<RevCommit> commits = git.log().all().call().iterator();
			CommitMessageParser commitMessageparser = new GitCommitMessageParser();
			while(commits.hasNext())
			{
				RevCommit currentCommit = commits.next();
				if(commitMessageparser.parse(getCommitMessage(currentCommit.getName())).contains(Integer.valueOf(requirementID)))
				{
					commitFilesList.addAll(((getCommitFilesInternal(currentCommit.getName()))));
					
				}
			}
		} catch (GitAPIException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return commitFilesList.iterator();
	}
}
