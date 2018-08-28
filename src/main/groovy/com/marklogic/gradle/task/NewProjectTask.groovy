package com.marklogic.gradle.task

import com.marklogic.appdeployer.scaffold.ScaffoldGenerator
import com.marklogic.gradle.task.MarkLogicTask
import org.gradle.api.tasks.TaskAction

class NewProjectTask extends MarkLogicTask {

	@TaskAction
	void newProject() {
		println "Welcome to the new project wizard. Please answer the following questions to start a new project."
		println "Note that this will overwrite your current build.gradle and gradle.properties files, and backup copies of each will be made."
		ant.input(message: "Application name:", addproperty: "mlAppName", defaultvalue: "myApp")
		ant.input(message: "Host to deploy to:", addproperty: "mlHost", defaultvalue: "localhost")
		ant.input(message: "MarkLogic admin username:", addproperty: "mlUsername", defaultvalue: "admin")
		ant.input(message: "MarkLogic admin password:", addproperty: "mlPassword", defaultvalue: "admin")
		ant.input(message: "REST API port (leave blank for no REST API server):", addproperty: "mlRestPort")
		if (ant.mlRestPort) {
			ant.input(message: "Test REST API port (intended for running automated tests; leave blank for no server):", addproperty: "mlTestRestPort")
		}
		ant.input(message: "Do you want support for multiple environments? ", validargs: "y,n", addproperty: "mlPropertiesPlugin", defaultvalue: "y")
		ant.input(message: "Do you want resource files for a content database and set of users/roles created?", validargs: "y,n", addproperty: "mlScaffold", defaultvalue: "y")

		def now = new Date()

		def propertiesText = "# Properties generated by mlNewProject at ${now}" +
			"\n# See the Property Reference page in the ml-gradle Wiki for a list of all supported properties" +
			"\nmlAppName=${ant.mlAppName}" +
			"\nmlHost=${ant.mlHost}" +
			"\nmlUsername=${ant.mlUsername}" +
			"\nmlPassword=${ant.mlPassword}"

		if (ant.mlRestPort) {
			propertiesText += "\nmlRestPort=${ant.mlRestPort}"
			if (ant.mlTestRestPort) {
				propertiesText += "\nmlTestRestPort=${ant.mlTestRestPort}"
			}
		} else {
			propertiesText += "\nmlNoRestServer=true"
		}

		if (ant.mlPropertiesPlugin == "y") {
			def text = 'plugins {' +
				'\n  id "net.saliman.properties" version "1.4.6"' +
				'\n  id "com.marklogic.ml-gradle" version "3.3.0"' +
				'\n}'
			println "Updating build.gradle so that the Gradle properties plugin can be applied"
			writeFile("build.gradle", text)
			propertiesText += "\n\n# Controls which additional properties file is used by the Gradle properties plugin - https://github.com/stevesaliman/gradle-properties-plugin"
			propertiesText += "\n# Defaults to 'local' when the property doesn't exist, and gradle-local.properties should be ignored by version control"
			propertiesText += "\n# environmentName="
		}

		writeFile("gradle.properties", propertiesText)

		if (ant.mlPropertiesPlugin == "y") {
			writeFile("gradle-dev.properties", "# Generated by mlNewProject at " + now)
			writeFile("gradle-local.properties", "# Generated by mlNewProject at " + now + "\n# Please be sure to ignore this file in version control!")
			writeFile("gradle-qa.properties", "# Generated by mlNewProject at " + now)
			writeFile("gradle-prod.properties", "# Generated by mlNewProject at " + now)
		}

		makeDirectory("src/main/ml-config")
		makeDirectory("src/main/ml-modules")

		if (ant.mlScaffold == "y") {
			println "Writing project scaffolding files"
			def appConfig = getAppConfig()
			appConfig.setName(ant.mlAppName)
			appConfig.setHost(ant.mlHost)
			appConfig.setRestAdminUsername(ant.mlUsername)
			appConfig.setRestAdminPassword(ant.mlPassword)
			if (ant.mlRestPort) {
				appConfig.setRestPort(Integer.parseInt(ant.mlRestPort))
				if (ant.mlTestRestPort) {
					appConfig.setTestRestPort(Integer.parseInt(ant.mlTestRestPort))
				}
			} else {
				appConfig.setNoRestServer(true)
			}
			new ScaffoldGenerator().generateScaffold(".", appConfig)
		}
	}

	void makeDirectory(String path) {
		println "Making directory: " + path
		new File(path).mkdirs()
	}

	void writeFile(String filename, String text) {
		File file = new File(filename);
		if (file.exists()) {
			new File("backup-" + filename).write(file.text)
		}
		println "Writing: " + filename
		file.write(text)
	}
}

