// This Jenkinsfile is used by Jenkins to run the DiagramExporter step of Reactome's release.
// It requires that the DiagramConverter step has been run successfully before it can be run.

import org.reactome.release.jenkins.utilities.Utilities

// Shared library maintained at 'release-jenkins-utils' repository.
def utils = new Utilities()
pipeline{
	agent any

	environment {
		ECR_URL = '851227637779.dkr.ecr.us-east-1.amazonaws.com/diagram-exporter'
	        CONT_NAME = 'diagram_exporter_container'
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
		stage('Pull diagram exporter Docker container') {
			steps{
				script {
                			sh "docker pull ${ECR_URL}:latest"
					sh """
						if docker ps -a --format '{{.Names}}' | grep -Eq '${CONT_NAME}'; then
							docker rm -f ${CONT_NAME}_SVG
                                                        docker rm -f ${CONT_NAME}_PNG
                                                        docker rm -f ${CONT_NAME}_SBGN
						fi
					"""
				}
			}
		}
		// Execute the jar file to produce SVG, PNG, and SBGN diagram files.
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
					sh "rm -rf output/*"
					withCredentials([usernamePassword(credentialsId: 'neo4jUsernamePassword', passwordVariable: 'pass', usernameVariable: 'user')]){
						sh """
                                                   docker run \\
						   -v ${diagramFolderPath}:/data/diagram:ro \\
                                                   -v ${ehldFolderPath}:/data/ehld:ro \\
                                                   -v ${pwd()}/output:/app/output \\
						   --net=host
						   --name ${CONT_NAME}_SVG \\
						       ${ECR_URL}:latest \\
	                                               /bin/bash -c "java -Xmx${env.JAVA_MEM_MAX}m -jar target/diagram-exporter-exec.jar --user \$user --password \$pass --format svg --input /data/diagram --ehld /data/ehld --summary /data/ehld/svgsummary.txt --target \"Homo sapiens\" --output /app/output --verbose"
                                                """

						sh """
                                                   docker run \\
						   -v ${diagramFolderPath}:/data/diagram:ro \\
                                                   -v ${ehldFolderPath}:/data/ehld:ro \\
                                                   -v ${pwd()}/output:/app/output \\
						   --net=host
						   --name ${CONT_NAME}_PNG \\
						       ${ECR_URL}:latest \\
					   	   /bin/bash -c "java -Xmx${env.JAVA_MEM_MAX}m -jar target/diagram-exporter-exec.jar --user \$user --password \$pass --format png --input /data/diagram --ehld /data/ehld --summary /data/ehld/svgsummary.txt --target \"Homo sapiens\" --output /app/output --verbose"
	                                        """
						
						sh """
                                                   docker run \\
						   -v ${diagramFolderPath}:/data/diagram:ro \\
                                                   -v ${ehldFolderPath}:/data/ehld:ro \\
                                                   -v ${pwd()}/output:/app/output \\
						   --net=host
						   --name ${CONT_NAME}_SBGN \\
						       ${ECR_URL}:latest \\
					   	   /bin/bash -c "java -Xmx${env.JAVA_MEM_MAX}m -jar target/diagram-exporter-exec.jar --user \$user --password \$pass --format sbgn --input /data/diagram --target \"Homo sapiens\" --output /app/output --verbose"
	                                       """
					}
				}
			}
		}
		stage('Post: Generate DiagramExporter archives and move them to the downloads folder') {
		    steps{
		        script{
				def releaseVersion = utils.getReleaseVersion()
				def svgArchive = "diagrams.svg.tgz"
				def pngArchive = "diagrams.png.tgz"
				def sbgnArchive = "homo_sapiens.sbgn.tar.gz"
				def downloadPath = "${env.ABS_DOWNLOAD_PATH}/${releaseVersion}"

				sh "cd output/svg/Modern/; tar -zcf ${svgArchive} *.svg; mv ${svgArchive} ../../"
				sh "cd output/png/Modern/; tar -zcf ${pngArchive} *.png; mv ${pngArchive} ../../"
				sh "cd output/sbgn/; tar -zcf ${sbgnArchive} *.sbgn; mv ${sbgnArchive} ../"

				sh "cp ${svgArchive} ${downloadPath}/"
				sh "cp ${pngArchive} ${downloadPath}/"
				sh "cp ${sbgnArchive} ${downloadPath}/"
		        }
		    }
		}
		// Move output contents to the download/XX folder, and archive everything on S3.
		stage('Post: Archive Outputs'){
			steps{
				script{
					def releaseVersion = utils.getReleaseVersion()
					def dataFiles = ["diagrams.svg.tgz", "diagrams.png.tgz", "homo_sapiens.sbgn.tar.gz"]
					def logFiles = []
					def foldersToDelete = ["output"]
					utils.cleanUpAndArchiveBuildFiles("diagram_exporter", dataFiles, logFiles, foldersToDelete)
				}
			}
		}
	}
}
