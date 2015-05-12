package de.fau.osr.gui;

import de.fau.osr.bl.Tracker;
import de.fau.osr.core.db.DataSource;
import de.fau.osr.core.vcs.base.Commit;
import de.fau.osr.core.vcs.base.CommitFile;
import de.fau.osr.core.vcs.interfaces.VcsClient;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.*;

/*
 * Adapter class. Providing the correct formatted input for the Library Facade and transforming
 * the output to match the needed Interface.
 */
public class GUITrackerToModelAdapter implements GuiModel {
	Tracker tracker;
	private Collection<CommitFile> commitFiles;
	private Collection<Commit> commits;

	public GUITrackerToModelAdapter(VcsClient vcs, DataSource ds, File repoFile, String reqPatternString)
			throws IOException, RuntimeException {

		tracker = new Tracker(vcs, ds, repoFile, reqPatternString);
	}

	@Override
	public String[] getAllRequirements() throws IOException {
        String[] result = convertCollectionToArray(tracker.getAllRequirements());
        //temp comparator, todo implement more universal one
        Arrays.sort(result, new Comparator<String>() {
            @Override
            public int compare(String o1, String o2) {
                return Integer.parseInt(o1) - Integer.parseInt(o2);
            }
        });
		return result;
	}

	@Override
	public String[] getCommitsFromRequirementID(String requirement) throws IOException {
		commits = tracker.getCommitsForRequirementID(requirement);
		return getMessagesFromCommits();
	}

	@Override
	public String[] getAllFiles() {
		Collection<String> collection = tracker.getAllFiles();
		return convertCollectionToArray(collection);
	}

	@Override
	public String[] getRequirementsFromFile(String filePath) throws IOException {
		String filePathTransformed = filePath.replace("\\", "/");
		Collection<String> requirements = tracker
				.getAllRequirementsForFile(filePathTransformed);
		return convertCollectionToArray(requirements);
	}


	@Override
	public String[] getCommitsFromFile(String filePath) {
		String filePathTransformed = filePath.replace("\\", "/");
		commits = tracker.getCommitsFromFile(filePathTransformed);
		return getMessagesFromCommits();
	}

	public String[] getFilesFromCommit(int commitIndex)
			throws FileNotFoundException {
		commitFiles = getCommit(commitIndex).files;
		return getCommitFileName();
	}

	@Override
	public String getChangeDataFromFileIndex(int filesIndex)
			throws FileNotFoundException {

		return getCommitFile(filesIndex).changedData;
	}

	@Override
	public String[] getCommitsFromDB() {
		commits = tracker.getCommits();
		return getMessagesFromCommits();
	}

	@Override
	public String[] getRequirementsFromCommit(int commitIndex)
			throws FileNotFoundException {
		Set<String> collection = new HashSet<String>(tracker
				.getRequirementsFromCommit(getCommit(commitIndex)));
		return convertCollectionToArray(collection);
	}

	@Override
	public String[] commitsFromRequirementAndFile(String requirementID,
			String filePath) throws IOException {
		Set<String> commits1 = new HashSet<String>(Arrays.asList(getCommitsFromFile(filePath)));
		Set<String> commits2 = new HashSet<String>(Arrays.asList(getCommitsFromRequirementID(requirementID)));
		
		commits1.retainAll(commits2);
		return convertCollectionToArray(commits1);
	}

	@Override
	public String[] commitsFromRequirementAndFile(String requirementID,
			int fileIndex) throws IOException {
		Collection<Commit> AllCommits = tracker.getCommitsForRequirementID(requirementID);
		CommitFile commitFile = getCommitFile(fileIndex);
		for(Commit commit: AllCommits){
			for(CommitFile commitFilecompare: commit.files){
				if(commitFile.equals(commitFilecompare)){
					commits = new ArrayList<Commit>();
					commits.add(commit);
					return getMessagesFromCommits();
				}
			}
		}
		throw new FileNotFoundException();
	}

	@Override
	public String[] getRequirementsFromFileAndCommit(int commitIndex,
			String filePath) throws IOException {
		Set<String> requirements1 = new HashSet<String>(Arrays.asList(getRequirementsFromCommit(commitIndex)));
		Set<String> requirements2 = new HashSet<String>(Arrays.asList(getRequirementsFromFile(filePath)));
		
		requirements1.retainAll(requirements2);
		return convertCollectionToArray(requirements1);
	}

	@Override
	public String[] getFilesFromRequirement(String requirementID) throws IOException {
		commitFiles = tracker.getCommitFilesForRequirementID(requirementID);
		return getCommitFileName(); 
	}

	@Override
	public void addRequirementCommitLinkage(String requirementID,
			int commitIndex) throws FileNotFoundException {
		String commitId = getCommit(commitIndex).id;
		try {
			tracker.addRequirementCommitRelation(requirementID, commitId);
		} catch (Exception e) {
			throw new RuntimeException(e.getMessage());
		}
		
	}

	private CommitFile getCommitFile(int filesIndex)
			throws FileNotFoundException {
		int i = 0;
		for (CommitFile commitFile : commitFiles) {
			if (i == filesIndex) {
				return commitFile;
			}
			i++;
		}
		throw new FileNotFoundException();
	}

	private Commit getCommit(int commitIndex) throws FileNotFoundException {
		int i = 0;
		for (Commit commit : commits) {
			if (i == commitIndex) {
				return commit;
			}
			i++;
		}
		throw new FileNotFoundException();
	}

	private String[] convertCollectionToArray(Collection<String> collection) {
		String[] array = new String[collection.size()];
		collection.toArray(array);
		return array;
	}

	private String[] getMessagesFromCommits() {
		String[] commitMessagesArray = new String[commits.size()];
		int i = 0;
		for (Commit commit : commits) {
			commitMessagesArray[i] = commit.message;
			i++;
		}
		return commitMessagesArray;
	}

	private String[] getCommitFileName() {
		String[] array;
		array = new String[commitFiles.size()];
		int j = 0;
		for (CommitFile commitfile : commitFiles) {
			array[j++] = commitfile.oldPath + " " + commitfile.commitState
					+ " " + commitfile.newPath;
		}
		
		return array;
	}

	@Override
	public String getCurrentRequirementPatternString() {
		return tracker.getCurrentRequirementPatternString();
	}

	@Override
	public String getCurrentRepositoryPath() {
		return tracker.getCurrentRepositoryPath();
	}
	
}