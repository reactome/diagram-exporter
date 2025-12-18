package org.reactome.server.tools.diagram.exporter.verifier;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.reactome.release.verifier.*;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Joel Weiser (joel.weiser@oicr.on.ca)
 * Created 12/16/2025
 */
public class DiagramExporterVerifier implements Verifier {
	private DefaultVerificationLogic logic;
	private int expectedFileCount;

	public static void main(String[] args) throws IOException {
		DiagramExporterVerifier verifier = new DiagramExporterVerifier();
		verifier.parseCommandLineArgs(args);
		verifier.run();
	}

	public DiagramExporterVerifier() {
		this.logic = new DefaultVerificationLogic(getStepName());
	}

	@Override
	public ParsedArguments parseCommandLineArgs(String[] args) {
		ParsedArguments config = logic.parseCommandLineArgs(args, getCommandLineParameters());
		this.expectedFileCount = config.getInt("expectedFileCount");
		return config;
	}

	@Override
	public List<CommandLineParameter> getCommandLineParameters() {
		List<CommandLineParameter> commandLineParameters = new ArrayList<>(logic.defaultParameters());
		commandLineParameters.add(
			CommandLineParameter.create(
				"expectedFileCount",
				OptionType.INTEGER,
				"",
				CommandLineParameter.IS_REQUIRED,
				'c',
				"expectedFileCount",
				"The number of files expected in the diagram tarball files"
			)
		);
		return commandLineParameters;
	}

	@Override
	public Results verifyStepRanCorrectly() throws IOException {
		Results finalResults = logic.verify();
		if (!finalResults.hasErrors()) {
			finalResults.mergeResults(verifyExpectedFileCount());
		}
		return finalResults;
	}

	@Override
	public String getStepName() {
		return "diagram_exporter";
	}

	private Results verifyExpectedFileCount() {
		Results results = new Results();

		for (String tarFilePath : getOutputDirectoryTarFilePaths()) {
			if (hasExpectedFileCount(tarFilePath)) {
				results.addInfoMessage(tarFilePath + " has " + getFileCount(tarFilePath) + " contained files");
			} else {
				results.addErrorMessage(
					tarFilePath + " has less than expected file count of " + getExpectedFileCount() + ": " +
						"only " + getFileCount(tarFilePath) + " files"
				);
			}
		}

		return results;
	}

	private List<String> getOutputDirectoryTarFilePaths() {
		try (var stream = Files.list(Paths.get(getOutputDirectory()))) {
			return stream
				.filter(Files::isRegularFile)
				.map(path -> path.toAbsolutePath().toString())
				.collect(Collectors.toList());
		} catch (IOException e) {
			throw new RuntimeException("Unable to list files in " + getOutputDirectory(), e);
		}
	}

	private String getOutputDirectory() {
		return logic.getOutputDirectory();
	}

	private boolean hasExpectedFileCount(String tarFilePath) {
		return getFileCount(tarFilePath) >= getExpectedFileCount();
	}

	private int getFileCount(String tarFilePath) {
		int fileCount = 0;

		try (FileInputStream fis = new FileInputStream(tarFilePath);
			 GzipCompressorInputStream gzipIn = new GzipCompressorInputStream(fis);
			 TarArchiveInputStream tarIn = new TarArchiveInputStream(gzipIn)) {

			TarArchiveEntry entry;
			while ((entry = tarIn.getNextTarEntry()) != null) {
				if (!entry.isDirectory()) {
					fileCount++;
				}
			}
		} catch (IOException e) {
			throw new RuntimeException("Unable to read tar file " + tarFilePath + " or its entries", e);
		}

		return fileCount;
	}

	private int getExpectedFileCount() {
		return this.expectedFileCount;
	}
}
