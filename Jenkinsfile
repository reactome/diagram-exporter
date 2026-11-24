// This Jenkinsfile is used by Jenkins to run the DiagramExporter step of Reactome's release: the
// release's SVG and PNG diagram files. The SBGN file (homo_sapiens.sbgn.tar.gz) is made by the
// 'SBGNExporter' step (reactome/sbgn-exporter) instead.
// It requires that the DiagramConverter step has been run successfully before it can be run.

import org.reactome.release.jenkins.utilities.Utilities

// Shared library maintained at 'release-jenkins-utils' repository.
def utils = new Utilities()
pipeline{
	agent any

	environment {
		OUTPUT_FOLDER = 'diagrams'
		ECR_URL = '851227637779.dkr.ecr.us-east-1.amazonaws.com/diagram-exporter'
		ECR_SERVER = '851227637779.dkr.ecr.us-east-1.amazonaws.com'
		CONT_NAME = 'diagram_exporter_container'
		CONT_ROOT = '/opt/diagram-exporter'
	}

	stages{
		// This stage checks that the upstream project DiagramConverter was run successfully.
		stage('Check DiagramConverter build succeeded'){
			steps{
				script{
					utils.checkUpstreamBuildsSucceeded("File-Generation/job/DiagramConverter/")
				}
			}
		}

		stage('Login to AWS ECR') {
			steps {
				script {
					sh """
						aws ecr get-login-password --region us-east-1 | \
						docker login --username AWS --password-stdin ${ECR_SERVER}
					"""
				}
			}
		}

		stage('Pull diagram exporter Docker container') {
			steps{
				script {
					sh "docker pull ${ECR_URL}:latest"
					sh """
						if docker ps -a --format '{{.Names}}' | grep -Eq '${CONT_NAME}'; then
							docker rm -f ${CONT_NAME}_SVG
							docker rm -f ${CONT_NAME}_PNG
						fi
					"""
				}
			}
		}

		// Execute the jar file to produce SVG and PNG diagram files.
		stage('Main: Run Diagram-Exporter'){
			steps{
				script{
					def releaseVersion = utils.getReleaseVersion()
					def diagramFolderPath = "${env.ABS_DOWNLOAD_PATH}/${releaseVersion}/diagram/"
					def ehldFolderPath = "${env.ABS_DOWNLOAD_PATH}/${releaseVersion}/ehld/"
					dir("${env.ABS_DOWNLOAD_PATH}/${releaseVersion}/")
						{
						// It looks like there's some external process to create and set up these directories.
						// We should check that they exist before proceeding.
						if(!fileExists("diagram"))
						{
							error ("${diagramFolderPath} doesn't seem to exist. Please ensure that this directory exists before continuing.")
						}

						if(!fileExists("ehld"))
						{
							error ("${ehldFolderPath} doesn't seem to exist. Please ensure that this directory exists before continuing.")
						}
					}
					sh "mkdir -p output"
					sh "sudo rm -rf output/*"
					sh "mkdir -p logs/svg logs/png"
					sh "sudo rm -rf logs/svg/* logs/png/*"
					
					try {
						sh "sudo service tomcat stop"
						withCredentials([usernamePassword(credentialsId: 'neo4jUsernamePassword', passwordVariable: 'pass', usernameVariable: 'user')]){
							sh """
								docker run \\
									-v ${diagramFolderPath}:/data/diagram:ro \\
									-v ${ehldFolderPath}:/data/ehld:ro \\
									-v ${pwd()}/${env.OUTPUT_FOLDER}:${CONT_ROOT}/${env.OUTPUT_FOLDER}/ \\
									-v ${pwd()}/logs/svg:${CONT_ROOT}/logs \\
									--net=host \\
									--name ${CONT_NAME}_SVG \\
									${ECR_URL}:latest \\
									/bin/bash -c "java -Xmx${env.JAVA_MEM_MAX}m -jar target/diagram-exporter-exec.jar --format svg --user $user --password \'$pass\' --input /data/diagram --ehld /data/ehld --summary /data/ehld/svgsummary.txt --target \'Homo sapiens\' --output ./${env.OUTPUT_FOLDER} --verbose" \\
							   """

							sh """
								docker run \\
									-v ${diagramFolderPath}:/data/diagram:ro \\
									-v ${ehldFolderPath}:/data/ehld:ro \\
									-v ${pwd()}/${env.OUTPUT_FOLDER}:${CONT_ROOT}/${env.OUTPUT_FOLDER}/ \\
									-v ${pwd()}/logs/png:${CONT_ROOT}/logs \\
									--net=host \\
									--name ${CONT_NAME}_PNG \\
									${ECR_URL}:latest \\
									/bin/bash -c "java -Xmx${env.JAVA_MEM_MAX}m -jar target/diagram-exporter-exec.jar --format png --user $user --password \'$pass\' --input /data/diagram --ehld /data/ehld --summary /data/ehld/svgsummary.txt --target \'Homo sapiens\' --output ./${env.OUTPUT_FOLDER} --verbose"
							   """
						}
					} finally {
						sh "sudo service tomcat start"
					}
				}
			}
		}

		// Execute the verifier jar file checking for the existence and proper file sizes of the diagram exporter output
		stage('Post: Verify DiagramExporter ran correctly') {
			steps {
				script {
					def releaseVersion = utils.getReleaseVersion()
					def expectedFileCount = 1200
					def previousReleaseVersion = utils.getPreviousReleaseVersion()
					def previousList = "previous_files_and_sizes.txt"

					// The verifier expects every file in the previous release's list, and that list
					// can still hold SBGN files from before the 'SBGNExporter' step made them. Hand it
					// the list without them: it reads a list already in its working folder instead
					// of fetching one.
					sh "aws s3 --no-progress cp s3://reactome/private/releases/${previousReleaseVersion}/diagram_exporter/data/files_and_sizes.txt ${previousList}"
					sh "awk -F'\\t' '\$2 !~ /^sbgn\\//' ${previousList} > ${previousList}.kept"
					sh "mv ${previousList}.kept ${previousList}"

					sh """
						docker run \\
						--rm \\
						-v ${pwd()}/${previousList}:${CONT_ROOT}/files_and_sizes.txt:ro \\
						-v ${pwd()}/${env.OUTPUT_FOLDER}:${CONT_ROOT}/${env.OUTPUT_FOLDER}/ \\
						-v \$HOME/.aws:/root/.aws:ro \\
						-e AWS_REGION=us-east-1 \\
						--net=host \\
						--name ${CONT_NAME}_verifier \\
						${ECR_URL}:latest \\
						/bin/bash -c "java -jar target/diagram-exporter-verifier.jar --releaseNumber ${releaseVersion} --output ${CONT_ROOT}/${env.OUTPUT_FOLDER} --expectedFileCount ${expectedFileCount}"
					"""
					sh "rm ${previousList}"
				}
			}
		}

		// Creates a list of files and their sizes to use for comparison baseline during next release
		stage('Post: Create files and sizes list to upload for next release\'s verifier') {
			steps {
				script {
					def fileSizeList = "files_and_sizes.txt"
					def releaseVersion = utils.getReleaseVersion()

					sh "find ${env.OUTPUT_FOLDER} -type f -printf \"%s\t%P\n\" > ${fileSizeList}"
					sh "aws s3 --no-progress cp ${fileSizeList} s3://reactome/private/releases/${releaseVersion}/diagram_exporter/data/"
					sh "rm ${fileSizeList}"
				}
			}
		}


		stage('Logout from AWS ECR') {
			steps {
				script {
					echo "Logging out from AWS ECR..."
					sh "docker logout ${ECR_SERVER}"
				}
			}
		}

		stage('Post: Generate DiagramExporter archives and move them to the downloads folder') {
			steps{
				script{
					def releaseVersion = utils.getReleaseVersion()
					def svgArchive = "diagrams.svg.tgz"
					def pngArchive = "diagrams.png.tgz"
					def downloadPath = "${env.ABS_DOWNLOAD_PATH}/${releaseVersion}"

					sh "sudo chown -R jenkins:jenkins ${OUTPUT_FOLDER}" 

					sh "cd ${OUTPUT_FOLDER}/svg/Modern/; tar -zcf ${svgArchive} *.svg; mv ${svgArchive} ../../../"
					sh "cd ${OUTPUT_FOLDER}/png/Modern/; tar -zcf ${pngArchive} *.png; mv ${pngArchive} ../../../"

					sh "cp ${svgArchive} ${downloadPath}/"
					sh "cp ${pngArchive} ${downloadPath}/"
				}
			}
		}

		// Move output contents to the download/XX folder, and archive everything on S3.
		stage('Post: Archive Outputs'){
			steps{
				script{
					def releaseVersion = utils.getReleaseVersion()
					def dataFiles = ["diagrams.svg.tgz", "diagrams.png.tgz"]
					def logFiles = ["logs/*"]
					def foldersToDelete = ["${OUTPUT_FOLDER}"]
					utils.cleanUpAndArchiveBuildFiles("diagram_exporter", dataFiles, logFiles, foldersToDelete)
				}
			}
		}
	}
}
