.PHONY: all clean build run test

# Default classpath
CLASSPATH = out/app.jar:out/models.jar:out/utils.jar:out/substitute-manager.jar:libs/gson-2.10.1.jar:libs/kotlinx-coroutines-core-jvm-1.7.3.jar:libs/kotlin-stdlib-1.9.0.jar:libs/kotlin-stdlib-common-1.9.0.jar:libs/kotlin-stdlib-jdk7-1.9.0.jar:libs/kotlin-stdlib-jdk8-1.9.0.jar

all: build

# Clean build artifacts
clean:
	rm -rf out temp META-INF/MANIFEST.MF

# Build the project
build:
	mkdir -p out
	@echo "Compiling models..."
	kotlinc src/main/kotlin/substitutemanager/models/Models.kt -d out/models.jar
	@echo "Compiling utils..."
	kotlinc -cp out/models.jar src/main/kotlin/substitutemanager/utils/CsvUtils.kt -d out/utils.jar
	@echo "Compiling SubstituteManager..."
	kotlinc -cp out/models.jar:out/utils.jar:libs/gson-2.10.1.jar src/main/kotlin/substitutemanager/SubstituteManager.kt -d out/substitute-manager.jar
	@echo "Compiling Main..."
	kotlinc -cp out/models.jar:out/utils.jar:out/substitute-manager.jar:libs/gson-2.10.1.jar:libs/kotlinx-coroutines-core-jvm-1.7.3.jar:libs/kotlin-stdlib-1.9.0.jar:libs/kotlin-stdlib-common-1.9.0.jar:libs/kotlin-stdlib-jdk7-1.9.0.jar:libs/kotlin-stdlib-jdk8-1.9.0.jar src/main/kotlin/substitutemanager/Main.kt -include-runtime -d out/app.jar
	@echo "Build completed successfully!"

# Create all-in-one JAR
package: build
	mkdir -p META-INF
	@echo "Manifest-Version: 1.0" > META-INF/MANIFEST.MF
	@echo "Main-Class: substitutemanager.MainKt" >> META-INF/MANIFEST.MF
	@echo "Class-Path: ." >> META-INF/MANIFEST.MF
	@echo ""  >> META-INF/MANIFEST.MF
	mkdir -p temp
	cd temp && \
	jar xf ../out/app.jar && \
	jar xf ../out/models.jar && \
	jar xf ../out/utils.jar && \
	jar xf ../out/substitute-manager.jar && \
	jar xf ../libs/gson-2.10.1.jar && \
	jar xf ../libs/kotlinx-coroutines-core-jvm-1.7.3.jar && \
	jar xf ../libs/kotlin-stdlib-1.9.0.jar && \
	jar xf ../libs/kotlin-stdlib-common-1.9.0.jar && \
	jar xf ../libs/kotlin-stdlib-jdk7-1.9.0.jar && \
	jar xf ../libs/kotlin-stdlib-jdk8-1.9.0.jar
	jar cfm out/substitute-manager-all-in-one.jar META-INF/MANIFEST.MF -C temp .
	rm -rf temp
	@echo "All-in-one JAR created at out/substitute-manager-all-in-one.jar"

# Run the application in interactive mode
run: build
	java -cp $(CLASSPATH) substitutemanager.MainKt

# Run with a specific command
run-cmd: build
	java -cp $(CLASSPATH) substitutemanager.MainKt $(ARGS)

# Example usage:
# make run-cmd ARGS="view"
# make run-cmd ARGS="assign 'Sir Bakir Shah' Monday"
# make run-cmd ARGS="verify"
# make run-cmd ARGS="clear"

# Run the all-in-one JAR
run-jar: package
	java -jar out/substitute-manager-all-in-one.jar $(ARGS)

# Test all commands
test: build
	@echo "\n----Testing View Command----"
	java -cp $(CLASSPATH) substitutemanager.MainKt view
	@echo "\n----Testing Assign Command----"
	java -cp $(CLASSPATH) substitutemanager.MainKt assign "Sir Bakir Shah" "Monday"
	@echo "\n----Testing View Command After Assignment----"
	java -cp $(CLASSPATH) substitutemanager.MainKt view
	@echo "\n----Testing Verify Command----"
	java -cp $(CLASSPATH) substitutemanager.MainKt verify
	@echo "\n----Testing Clear Command----"
	java -cp $(CLASSPATH) substitutemanager.MainKt clear
	@echo "\n----Testing View Command After Clear----"
	java -cp $(CLASSPATH) substitutemanager.MainKt view
	@echo "\n----All Tests Completed Successfully----"
